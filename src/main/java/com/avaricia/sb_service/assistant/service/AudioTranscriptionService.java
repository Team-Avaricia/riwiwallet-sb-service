package com.avaricia.sb_service.assistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.avaricia.sb_service.assistant.exception.TranscriptionException;

import java.util.Map;

/**
 * Service for transcribing audio files using OpenAI Whisper API.
 * Handles downloading audio from Telegram and converting speech to text.
 */
@Service
public class AudioTranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(AudioTranscriptionService.class);

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

    /**
     * Transcribes an audio file from Telegram using OpenAI Whisper.
     * 
     * @param fileId The Telegram file ID
     * @return The transcribed text, or null if transcription fails
     */
    public String transcribeAudio(String fileId) {
        log.info("🎤 Starting audio transcription for fileId: {}", fileId);
        
        try {
            String filePath = getFilePath(fileId);
            log.debug("📁 File path obtained: {}", filePath);
            
            byte[] audioData = downloadFile(filePath, fileId);
            log.debug("📥 Audio downloaded: {} bytes", audioData.length);
            
            String transcription = transcribeWithWhisper(audioData, filePath);
            log.info("✅ Transcription successful: '{}...'", 
                transcription.length() > 50 ? transcription.substring(0, 50) : transcription);
            
            return transcription;
            
        } catch (TranscriptionException e) {
            // Already logged, just re-throw with user message
            log.warn("🎤 Transcription failed: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("🎤 Unexpected error during transcription: {}", e.getMessage(), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String getFilePath(String fileId) {
        String url = telegramApiUrl + "/getFile?file_id=" + fileId;
        
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && Boolean.TRUE.equals(response.get("ok"))) {
                Map<String, Object> result = (Map<String, Object>) response.get("result");
                String filePath = (String) result.get("file_path");
                log.debug("📂 Telegram file path resolved: {}", filePath);
                return filePath;
            }
            
            log.error("❌ Telegram API returned error for fileId: {}", fileId);
            throw TranscriptionException.telegramDownloadFailed(fileId, 
                new RuntimeException("Telegram API returned unsuccessful response"));
                
        } catch (TranscriptionException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Failed to get file path from Telegram: {}", e.getMessage());
            throw TranscriptionException.telegramDownloadFailed(fileId, e);
        }
    }

    private byte[] downloadFile(String filePath, String fileId) {
        String url = telegramFileUrl + "/" + filePath;
        
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                null, 
                byte[].class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("📦 File downloaded successfully: {} bytes", response.getBody().length);
                return response.getBody();
            }
            
            log.error("❌ Failed to download file from Telegram: status={}", response.getStatusCode());
            throw TranscriptionException.telegramDownloadFailed(fileId, 
                new RuntimeException("Download returned status: " + response.getStatusCode()));
                
        } catch (TranscriptionException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Error downloading file from Telegram: {}", e.getMessage());
            throw TranscriptionException.telegramDownloadFailed(fileId, e);
        }
    }

    private String transcribeWithWhisper(byte[] audioData, String filePath) {
        String fileName = filePath.contains("/") 
            ? filePath.substring(filePath.lastIndexOf("/") + 1) 
            : filePath;
        
        log.debug("🎙️ Sending audio to Whisper API: {} ({} bytes)", fileName, audioData.length);
        
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
                String transcription = response.getBody().trim();
                log.debug("✅ Whisper transcription received: {} characters", transcription.length());
                return transcription;
            }
            
            log.error("❌ Whisper API returned unsuccessful response: {}", response.getStatusCode());
            throw TranscriptionException.whisperApiFailed(
                new RuntimeException("Whisper API returned status: " + response.getStatusCode()));
                
        } catch (TranscriptionException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Whisper API error: {}", e.getMessage(), e);
            throw TranscriptionException.whisperApiFailed(e);
        }
    }
}

