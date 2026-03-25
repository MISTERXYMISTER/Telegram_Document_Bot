package com.bot;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Component
public class BotInitializer {

    @Value("${bot.token}")
    private String token;

    @Value("${bot.path}")
    private String webhookUrl;

    private final WebhookBot webhookBot;

    public BotInitializer(WebhookBot webhookBot) {
        this.webhookBot = webhookBot;
    }

    @PostConstruct
    public void init() {
        try {
            String url = "https://api.telegram.org/bot" + token + "/setWebhook?url=" + webhookUrl;
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);
            System.out.println("✅ WEBHOOK SET RESPONSE: " + response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}