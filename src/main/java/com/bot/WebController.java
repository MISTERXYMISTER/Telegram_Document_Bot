package com.bot;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
public class WebController {

    private final WebhookBot webhookBot;

    public WebController(WebhookBot webhookBot) {
        this.webhookBot = webhookBot;
    }

    @GetMapping("/")
    public String home() {
        return "Bot is running";
    }

    @PostMapping("/webhook")
    public void handleWebhook(@RequestBody Update update) {
        webhookBot.onWebhookUpdateReceived(update);
    }
}