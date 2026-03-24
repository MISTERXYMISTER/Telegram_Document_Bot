package com.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.util.*;

@Component
public class WebhookBot extends TelegramWebhookBot {

    private Map<Long, UserState> userStates = new HashMap<>();
    private Map<Long, List<String>> tempFiles = new HashMap<>();

    @Value("${bot.username}")
    private String username;

    @Value("${bot.token}")
    private String token;

    @Value("${bot.webhook-path}")
    private String webhookPath;

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public String getBotPath() {
        return webhookPath;
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {

        try {
            if (!update.hasMessage()) return null;

            Message msg = update.getMessage();
            Long chatId = msg.getChatId();
            String userId = chatId.toString();

            userStates.putIfAbsent(chatId, UserState.IDLE);

            // FILE UPLOAD
            if (msg.hasDocument() || msg.hasPhoto()) {

                String fileId = msg.hasDocument()
                        ? msg.getDocument().getFileId()
                        : msg.getPhoto().get(msg.getPhoto().size() - 1).getFileId();

                tempFiles.putIfAbsent(chatId, new ArrayList<>());
                tempFiles.get(chatId).add(fileId);

                userStates.put(chatId, UserState.WAITING_FOR_TAG);

                return SendMessage.builder()
                        .chatId(chatId.toString())
                        .text("📄 File received. Send tag (aadhaar, pan...)")
                        .build();
            }

            if (msg.hasText()) {
                String text = msg.getText().toLowerCase();

                if (text.equals("/start")) {
                    return SendMessage.builder()
                            .chatId(chatId.toString())
                            .text("⚡ Waking up...\nSend file or type tag.")
                            .build();
                }

                if (text.equals("/mydocs")) {
                    var tags = Database.getTags(userId);
                    return SendMessage.builder()
                            .chatId(chatId.toString())
                            .text("📂 " + tags)
                            .build();
                }

                if (text.equals("/delete")) {
                    userStates.put(chatId, UserState.WAITING_FOR_TAG);
                    return SendMessage.builder()
                            .chatId(chatId.toString())
                            .text("Send tag to delete")
                            .build();
                }

                // SAVE TAG
                if (userStates.get(chatId) == UserState.WAITING_FOR_TAG) {

                    if (tempFiles.containsKey(chatId)) {
                        for (String f : tempFiles.get(chatId)) {
                            Database.saveFile(userId, text, f);
                        }

                        tempFiles.remove(chatId);
                        userStates.put(chatId, UserState.IDLE);

                        return SendMessage.builder()
                                .chatId(chatId.toString())
                                .text("✅ Saved!")
                                .build();
                    } else {
                        Database.deleteTag(userId, text);
                        userStates.put(chatId, UserState.IDLE);

                        return SendMessage.builder()
                                .chatId(chatId.toString())
                                .text("🗑 Deleted!")
                                .build();
                    }
                }

                // RETRIEVE
                var files = Database.getFiles(userId, text);

                if (files.isEmpty()) {
                    return SendMessage.builder()
                            .chatId(chatId.toString())
                            .text("❌ Not found")
                            .build();
                }

                for (String f : files) {
                    SendDocument doc = new SendDocument();
                    doc.setChatId(chatId.toString());
                    doc.setDocument(new InputFile(f));
                    execute(doc);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}