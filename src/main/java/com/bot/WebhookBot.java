package com.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class WebhookBot {

    private final String token = System.getenv("BOT_TOKEN");

    private final DefaultAbsSender sender = new DefaultAbsSender(null) {
        @Override
        public String getBotToken() {
            return token;
        }
    };

    public void handleUpdate(Update update) {

        if (update == null || !update.hasMessage()) return;

        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();

        try {
            if (text != null && text.equals("/start")) {
                sendMessage(chatId, "⚡ Waking up...\nSend file or type tag");
                return;
            }

            if (text != null) {
                sendMessage(chatId, "You said: " + text);
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(Long chatId, String text) throws TelegramApiException {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .build();

        sender.execute(msg);
    }
}