package com.bot;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
public class WebhookController {

    @Autowired
    private WebhookBot bot;

    @PostMapping("/webhook")
    public void onUpdate(@RequestBody Update update) {
        bot.onWebhookUpdateReceived(update);
    }

    @GetMapping("/")
    public String home() {
        return "Bot is running";
    }
}