package com.avaricia.sb_service.assistant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramService {

    private final RestTemplate restTemplate;
    private final String telegramApiUrl;

    public TelegramService(@Value("${telegram.bot.token}") String botToken) {
        this.restTemplate = new RestTemplate();
        this.telegramApiUrl = "https://api.telegram.org/bot" + botToken;
    }

    public void sendMessage(Long chatId, String text) {
        String url = telegramApiUrl + "/sendMessage";
        
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        body.put("parse_mode", "Markdown");
        
        restTemplate.postForObject(url, body, String.class);
    }
}
