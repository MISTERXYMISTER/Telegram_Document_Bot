package com.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.*;

import java.util.*;

@Component
public class WebhookBot extends TelegramWebhookBot {

    @Value("${bot.username}")
    private String username;

    @Value("${bot.token}")
    private String token;

    @Value("${bot.path}")
    private String path;

    private final Map<Long, UserState> userStates = new HashMap<>();
    private final Map<Long, List<String>> tempFiles = new HashMap<>();
    private final Map<Long, String> tempTag = new HashMap<>();

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
        return path;
    }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {

        if (!update.hasMessage()) return null;

        Message msg = update.getMessage();
        Long chatId = msg.getChatId();
        String userId = chatId.toString();

        userStates.putIfAbsent(chatId, UserState.IDLE);

        try {

            if (msg.hasDocument() || msg.hasPhoto()) {

                String fileId = msg.hasDocument()
                        ? msg.getDocument().getFileId()
                        : msg.getPhoto().get(msg.getPhoto().size() - 1).getFileId();

                tempFiles.putIfAbsent(chatId, new ArrayList<>());
                tempFiles.get(chatId).add(fileId);

                userStates.put(chatId, UserState.WAITING_FOR_TAG);

                return new SendMessage(chatId.toString(),
                        "📄 File received. Send passkey (aadhaar, pan, etc)");
            }

            if (msg.hasText()) {

                String text = msg.getText().toLowerCase();

                if (text.equals("/start")) {
                    return new SendMessage(chatId.toString(),
                            "🔥 Welcome to DocVault\nUpload files or type keyword to retrieve");
                }

                if (text.equals("/mydocs")) {
                    Set<String> tags = Database.getTags(userId);
                    return new SendMessage(chatId.toString(),
                            "📂 " + tags);
                }

                if (userStates.get(chatId) == UserState.WAITING_FOR_TAG) {

                    String tag = text;

                    if (Database.exists(userId, tag)) {

                        tempTag.put(chatId, tag);
                        userStates.put(chatId, UserState.WAITING_FOR_REPLACE_DECISION);

                        return new SendMessage(chatId.toString(),
                                "⚠️ Already exists.\nType:\n1 = Replace\n2 = Add new");
                    }

                    Database.save(userId, tag, tempFiles.get(chatId));

                    tempFiles.remove(chatId);
                    userStates.put(chatId, UserState.IDLE);

                    return new SendMessage(chatId.toString(), "✅ Saved");
                }

                if (userStates.get(chatId) == UserState.WAITING_FOR_REPLACE_DECISION) {

                    String tag = tempTag.get(chatId);

                    if (text.equals("1")) {
                        Database.replace(userId, tag, tempFiles.get(chatId));
                    } else {
                        Database.save(userId, tag, tempFiles.get(chatId));
                    }

                    tempFiles.remove(chatId);
                    tempTag.remove(chatId);
                    userStates.put(chatId, UserState.IDLE);

                    return new SendMessage(chatId.toString(), text.equals("1") ? "♻️ Replaced" : "➕ Added");
                }

                List<String> files = Database.search(userId, text);

                if (files.isEmpty()) {
                    return new SendMessage(chatId.toString(), "❌ Not found");
                }

                SendMessage responseMsg = new SendMessage(chatId.toString(), "📄 Found " + files.size() + " file(s)");
                
                for (String f : files) {
                    try {
                        SendDocument doc = new SendDocument();
                        doc.setChatId(chatId.toString());
                        doc.setDocument(new InputFile(f));
                        execute(doc);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                return responseMsg;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new SendMessage(chatId.toString(), "❌ Error: " + e.getMessage());
        }

        return null;
    }
}