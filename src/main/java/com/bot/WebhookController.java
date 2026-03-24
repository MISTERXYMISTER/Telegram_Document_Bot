package com.bot;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

@RestController
public class WebhookController {

    @Autowired
    private WebhookBot bot;

    @PostMapping("/webhook")
    public ResponseEntity<String> onUpdate(@RequestBody Update update) {

        // Run async to avoid Telegram timeout
        new Thread(() -> {
            try {
                bot.handleUpdate(update);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        return ResponseEntity.ok("OK");
    }

    @GetMapping("/")
    public String home() {
        return "Bot is running";
    }
}