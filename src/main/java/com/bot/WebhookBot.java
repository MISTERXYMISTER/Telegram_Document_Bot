package com.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.objects.*;

import java.util.*;

@Component
public class WebhookBot extends TelegramLongPollingBot {

    @Value("${bot.username}")
    private String username;

    @Value("${bot.token}")
    private String token;

    // STATE MANAGEMENT
    private Map<Long, UserState> userStates = new HashMap<>();
    private Map<Long, List<String>> tempFiles = new HashMap<>();
    private Map<Long, String> tempTag = new HashMap<>();

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (!update.hasMessage()) return;

        Message msg = update.getMessage();
        Long chatId = msg.getChatId();
        String userId = chatId.toString();

        userStates.putIfAbsent(chatId, UserState.IDLE);

        try {

            // 📤 FILE UPLOAD
            if (msg.hasDocument() || msg.hasPhoto()) {

                String fileId = msg.hasDocument()
                        ? msg.getDocument().getFileId()
                        : msg.getPhoto().get(msg.getPhoto().size() - 1).getFileId();

                tempFiles.putIfAbsent(chatId, new ArrayList<>());
                tempFiles.get(chatId).add(fileId);

                userStates.put(chatId, UserState.WAITING_FOR_TAG);

                execute(new SendMessage(chatId.toString(),
                        "📄 File received. Send passkey (aadhaar, pan, etc)"));
                return;
            }

            // 📝 TEXT INPUT
            if (msg.hasText()) {

                String text = msg.getText().toLowerCase();

                // START
                if (text.equals("/start")) {
                    execute(new SendMessage(chatId.toString(),
                            "🔥 Welcome to DocVault\nUpload files or type keyword to retrieve"));
                    return;
                }

                // 📂 LIST TAGS
                if (text.equals("/mydocs")) {
                    Set<String> tags = Database.getTags(userId);
                    execute(new SendMessage(chatId.toString(),
                            "📂 " + tags));
                    return;
                }

                // 🏷 SAVE TAG
                if (userStates.get(chatId) == UserState.WAITING_FOR_TAG) {

                    String tag = text;

                    if (Database.exists(userId, tag)) {

                        tempTag.put(chatId, tag);
                        userStates.put(chatId, UserState.WAITING_FOR_REPLACE_DECISION);

                        execute(new SendMessage(chatId.toString(),
                                "⚠️ Already exists.\nType:\n1 = Replace\n2 = Add new"));
                        return;
                    }

                    Database.save(userId, tag, tempFiles.get(chatId));

                    tempFiles.remove(chatId);
                    userStates.put(chatId, UserState.IDLE);

                    execute(new SendMessage(chatId.toString(), "✅ Saved"));
                    return;
                }

                // 🔁 REPLACE / ADD
                if (userStates.get(chatId) == UserState.WAITING_FOR_REPLACE_DECISION) {

                    String tag = tempTag.get(chatId);

                    if (text.equals("1")) {
                        Database.replace(userId, tag, tempFiles.get(chatId));
                        execute(new SendMessage(chatId.toString(), "♻️ Replaced"));
                    } else {
                        Database.save(userId, tag, tempFiles.get(chatId));
                        execute(new SendMessage(chatId.toString(), "➕ Added"));
                    }

                    tempFiles.remove(chatId);
                    tempTag.remove(chatId);
                    userStates.put(chatId, UserState.IDLE);
                    return;
                }

                // 🔍 RETRIEVE
                List<String> files = Database.search(userId, text);

                if (files.isEmpty()) {
                    execute(new SendMessage(chatId.toString(), "❌ Not found"));
                    return;
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
    }
}