package xyz.dbotfactory.dbot.handler.impl;

import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import xyz.dbotfactory.dbot.handler.BotMessageHelper;
import xyz.dbotfactory.dbot.handler.CommonConsts;
import xyz.dbotfactory.dbot.handler.UpdateHandler;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.model.meta.ChatMetaInfo;
import xyz.dbotfactory.dbot.service.ChatService;

import java.util.ArrayList;

import static xyz.dbotfactory.dbot.model.ChatState.COLLECTING_ITEMS;

@Component
@Log
public class H1NewReceiptCommandUpdateHandler implements UpdateHandler, CommonConsts {

    public static final String COMMAND_NAME = "/new_receipt";
    private static final String RECEIPT_EMOJI = "🧾";
    private static final String SPEECH_SEND_RECEIPT = RECEIPT_EMOJI + " <i>Now send receipt information.</i>\n\n" +
            "<i>Receipt info should be in the next format (with spaces):</i>\n\n" +
            "<code>amount price-for-unit name-for-item</code>\n\n" +
            "<i>For example:</i>\n\n" +
            "<code>2 465.5 Almond milk</code>";

    private final TelegramLongPollingBot bot;
    private final ChatService chatService;
    private final BotMessageHelper botMessageHelper;

    @Autowired
    public H1NewReceiptCommandUpdateHandler(ChatService chatService, TelegramLongPollingBot bot,
                                            BotMessageHelper botMessageHelper) {
        this.chatService = chatService;
        this.bot = bot;
        this.botMessageHelper = botMessageHelper;
    }

    @Override
    public boolean canHandle(Update update, Chat chat) {
        return update.hasMessage() &&
                update.getMessage().isCommand() &&
                (update.getMessage().getText().equals(COMMAND_NAME + "@" + bot.getBotUsername()) ||
                        update.getMessage().getText().equals(COMMAND_NAME)) &&
                chat.getChatState() == ChatState.NO_ACTIVE_RECEIPT &&
                !update.getMessage().getChat().isUserChat();
    }

    @Override
    @SneakyThrows
    public void handle(Update update, Chat chat) {
        Receipt receipt = Receipt.builder()
                .items(new ArrayList<>())
                .userBalances(new ArrayList<>())
                .isActive(true)
                .build();
        chat.getReceipts().add(receipt);
        chat.setChatState(COLLECTING_ITEMS);
        if (chat.getChatMetaInfo() == null) {
            chat.setChatMetaInfo(ChatMetaInfo.builder().pmUserIds("").build());
        }

        SendMessage message = new SendMessage()
                .setChatId(chat.getTelegramChatId())
                .setText(SPEECH_SEND_RECEIPT)
                .setParseMode(ParseMode.HTML);

        Message sentMessage = bot.execute(message);

        if (update.hasMessage()) {
            botMessageHelper.deleteMessage(bot, update.getMessage());
            botMessageHelper.executeExistingTasks(this.getClass().getSimpleName(), chat.getChatMetaInfo(), bot,
                    update.getMessage().getFrom().getId());

        } else if (update.hasCallbackQuery()) {
            botMessageHelper.executeExistingTasks(this.getClass().getSimpleName(), chat.getChatMetaInfo(), bot,
                    update.getCallbackQuery().getFrom().getId());
        }

        botMessageHelper.addNewTask(H2AddReceiptItemMessageUpdateHandler.class.getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(H4ItemsCollectingFinishedButtonUpdateHandler.class.getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(DiscardReceiptUpdateHandler.class.getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);
        botMessageHelper.addNewTask(this.getClass().getSimpleName(),
                chat.getChatMetaInfo(), sentMessage);

        chatService.save(chat);
        log.info("Chat " + chat.getId() + " is now in " + chat.getChatState() + " state");
    }
}
