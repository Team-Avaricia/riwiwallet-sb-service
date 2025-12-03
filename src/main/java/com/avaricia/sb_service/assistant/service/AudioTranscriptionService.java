package com.avaricia.sb_service.assistant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Service for transcribing audio files using OpenAI Whisper API.
 * Handles downloading audio from Telegram and converting speech to text.
 */
@Service
public class AudioTranscriptionService {

    private final RestTemplate restTemplate;
    private final String telegramApiUrl;
    private final String telegramFileUrl;
    private final String openAiApiKey;
    private static final String WHISPER_API_URL = "https://api.openai.com/v1/audio/transcriptions";

    public AudioTranscriptionService(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${spring.ai.openai.api-key}") String openAiApiKey) {
        this.restTemplate = new RestTemplate();
        this.telegramApiUrl = "https://api.telegram.org/bot" + botToken;
        this.telegramFileUrl = "https://api.telegram.org/file/bot" + botToken;
        this.openAiApiKey = openAiApiKey;
    }

    public String transcribeAudio(String fileId) {
        try {
            String filePath = getFilePath(fileId);
            System.out.println("📁 File path obtained: " + filePath);
            
            byte[] audioData = downloadFile(filePath);
            System.out.println("📥 Audio downloaded: " + audioData.length + " bytes");
            
            String transcription = transcribeWithWhisper(audioData, filePath);
            System.out.println("🎤 Transcription: " + transcription);
            
            return transcription;
        } catch (Exception e) {
            System.err.println("Error transcribing audio: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String getFilePath(String fileId) {
        String url = telegramApiUrl + "/getFile?file_id=" + fileId;
        
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        
        if (response != null && Boolean.TRUE.equals(response.get("ok"))) {
            Map<String, Object> result = (Map<String, Object>) response.get("result");
            return (String) result.get("file_path");
        }
        
        throw new RuntimeException("Failed to get file path from Telegram");
    }

    private byte[] downloadFile(String filePath) {
        String url = telegramFileUrl + "/" + filePath;
        
        ResponseEntity<byte[]> response = restTemplate.exchange(
            url, 
            HttpMethod.GET, 
            null, 
            byte[].class
        );
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }
        
        throw new RuntimeException("Failed to download file from Telegram");
    }

    private String transcribeWithWhisper(byte[] audioData, String filePath) {
        String fileName = filePath.contains("/") 
            ? filePath.substring(filePath.lastIndexOf("/") + 1) 
            : filePath;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(openAiApiKey);

        ByteArrayResource fileResource = new ByteArrayResource(audioData) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("model", "whisper-1");
        body.add("language", "es");
        body.add("response_format", "text");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                WHISPER_API_URL,
                HttpMethod.POST,
                requestEntity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().trim();
            }
        } catch (Exception e) {
            System.err.println("Whisper API error: " + e.getMessage());
            throw new RuntimeException("Failed to transcribe audio with Whisper", e);
        }

        throw new RuntimeException("Failed to transcribe audio with Whisper");
    }
}
