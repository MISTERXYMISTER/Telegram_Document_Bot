package com.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class WebhookBot extends TelegramLongPollingBot {

    @Value("${bot.username}")
    private String botUsername;

    @Value("${bot.token}")
    private String botToken;

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {

            String text = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (text.equals("/start")) {
                try {
                    execute(new SendMessage(chatId.toString(), "🔥 Bot is working perfectly!"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}