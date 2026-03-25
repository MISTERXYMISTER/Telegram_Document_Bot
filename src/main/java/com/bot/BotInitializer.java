package com.bot;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class BotInitializer {

    @Value("${bot.token}")
    private String token;

    @Value("${bot.path}")
    private String webhookPath;

    @Value("${external.url}")
    private String externalUrl;

    private final WebhookBot webhookBot;

    public BotInitializer(WebhookBot webhookBot) {
        this.webhookBot = webhookBot;
    }

    @PostConstruct
    public void init() {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                String fullWebhookUrl = externalUrl + webhookPath;
                String url = "https://api.telegram.org/bot" + token + "/setWebhook?url=" + fullWebhookUrl;
                RestTemplate restTemplate = new RestTemplate();
                String response = restTemplate.getForObject(url, String.class);
                System.out.println("✅ WEBHOOK SET TO: " + fullWebhookUrl);
                System.out.println("✅ WEBHOOK SET RESPONSE: " + response);
            } catch (Exception e) {
                System.out.println("⚠️ WEBHOOK SET FAILED: " + e.getMessage());
            }
        }).start();
        System.out.println("🚀 BOT STARTED (webhook setup in background)");
    }
}