package com.avaricia.sb_service.assistant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WhatsAppService {

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String accessToken;

    public WhatsAppService(
            @Value("${whatsapp.phone.number.id}") String phoneNumberId,
            @Value("${whatsapp.access.token}") String accessToken) {
        this.restTemplate = new RestTemplate();
        this.apiUrl = "https://graph.facebook.com/v24.0/" + phoneNumberId + "/messages";
        this.accessToken = accessToken;
    }

    public void sendMessage(String to, String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> body = new HashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("to", to);
        body.put("type", "text");
        
        Map<String, String> textBody = new HashMap<>();
        textBody.put("body", text);
        body.put("text", textBody);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        
        try {
            restTemplate.postForObject(apiUrl, request, String.class);
            System.out.println("Mensaje enviado a " + to);
        } catch (Exception e) {
            System.err.println("Error enviando mensaje: " + e.getMessage());
        }
    }
}
