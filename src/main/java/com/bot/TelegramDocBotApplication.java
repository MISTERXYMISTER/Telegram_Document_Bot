package com.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TelegramDocBotApplication {

    public static void main(String[] args) {
        Database.init();
        SpringApplication.run(TelegramDocBotApplication.class, args);
    }
}
