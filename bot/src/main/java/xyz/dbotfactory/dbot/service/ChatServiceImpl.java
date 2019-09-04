package xyz.dbotfactory.dbot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.dbotfactory.dbot.model.Chat;
import xyz.dbotfactory.dbot.model.ChatState;
import xyz.dbotfactory.dbot.model.Receipt;
import xyz.dbotfactory.dbot.repo.ChatRepository;

import javax.transaction.Transactional;
import java.util.ArrayList;


@Service
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;

    @Autowired
    public ChatServiceImpl(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    @Override
    public Chat findOrCreateChatByTelegramId(long chatId) {
        Chat chat = chatRepository.findFirstByTelegramChatId(chatId);
        if (chat == null) {
            Chat newChat = Chat.builder()
                    .telegramChatId(chatId)
                    .chatState(ChatState.NO_ACTIVE_RECEIPT)
                    .receipts(new ArrayList<>())
                    .build();
            newChat = chatRepository.save(newChat);
            return newChat;
        } else {
            return chat;
        }
    }

    @Override
    public Receipt getActiveReceipt(Chat chat) {
        return chat.getReceipts()
                .stream()
                .filter(Receipt::isActive)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("no active receipt found"));
    }

    @Override
    public void save(Chat chat) {
        chatRepository.save(chat);
    }
}