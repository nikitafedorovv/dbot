package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.handler.BotMessageHelper;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.*;
import xyz.dbotfactory.dbot.service.ChatService;
import xyz.dbotfactory.dbot.service.ReceiptService;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import static xyz.dbotfactory.dbot.BigDecimalUtils.create;

@Component
public class H10SharesStatusButtonUpdateHandler implements UpdateHandler, CommonConsts {

    private static DecimalFormat df2 = new DecimalFormat("#.##");

    private final ChatService chatService;
    private final ReceiptService receiptService;
    private final BotMessageHelper botMessageHelper;

    private final TelegramLongPollingBot bot;

    @Autowired
    public H10SharesStatusButtonUpdateHandler(ChatService chatService, ReceiptService receiptService,
                                              TelegramLongPollingBot bot, BotMessageHelper botMessageHelper) {
        this.chatService = chatService;
        this.receiptService = receiptService;
        this.bot = bot;
        this.botMessageHelper = botMessageHelper;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            if (data.startsWith(FINISHED_SETTING_SHARES_CALLBACK_DATA)) {
                String[] ids = data.substring(FINISHED_SETTING_SHARES_CALLBACK_DATA.length()).split(DELIMITER);
                int receiptId = Integer.parseInt(ids[1]);
                long tgGroupChatId = Long.parseLong(ids[0]);

                Chat groupChat = chatService.findOrCreateChatByTelegramId(tgGroupChatId);

                return chatService.getActiveReceipt(groupChat).getId() == receiptId &&
                        groupChat.getChatState() == ChatState.DETECTING_OWNERS;
            }
        }
        return false;
    }

    @Override
    @SneakyThrows
    public void handle(Update update, Chat chat) {
        String[] ids = update.getCallbackQuery()
                .getData().substring(FINISHED_SETTING_SHARES_CALLBACK_DATA.length()).split(DELIMITER);

        int receiptId = Integer.parseInt(ids[1]);
        long tgGroupChatId = Long.parseLong(ids[0]);
        Chat groupChat = chatService.findOrCreateChatByTelegramId(tgGroupChatId);

        Receipt receipt = chatService.getActiveReceipt(groupChat);
        String response;
        if (receipt.getItems().size() != 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("<b>These items are still not picked:</b> \n");
            for (ReceiptItem item : receipt.getItems()) {

                BigDecimal pickedShare = item.getShares()
                        .stream()
                        .map(Share::getShare)
                        .reduce(BigDecimal::add)
                        .orElse(create(0));
                if (item.getAmount().compareTo(pickedShare) != 0) {

                    BigDecimal unpickedShare = item.getAmount().subtract(pickedShare);
                    sb.append("<pre>").append(item.getName()).append(" x ").append(df2.format(unpickedShare))
                            .append("</pre>");
                }
            }
            response = sb.toString();
        } else {
            response = "<i>All items are picked!</i>";
        }

        SendMessage message = new SendMessage()
                .setChatId(chat.getTelegramChatId())
                .setText(response)
                .setParseMode(ParseMode.HTML);
        Message sentMessage = bot.execute(message);

        botMessageHelper.addNewTask(SHARES_DONE_TASK_NAME, groupChat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(DiscardReceiptUpdateHandler.class.getSimpleName(),
                groupChat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(H1NewReceiptCommandUpdateHandler.class.getSimpleName(),
                groupChat.getChatMetaInfo(), sentMessage);
        botMessageHelper.executeExistingTasks(this.getClass().getSimpleName(),
                groupChat.getChatMetaInfo(), bot, update.getCallbackQuery().getFrom().getId());

        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery()
                .setCallbackQueryId(update.getCallbackQuery().getId());
        bot.execute(answerCallbackQuery);

        chatService.save(groupChat);
    }
}
