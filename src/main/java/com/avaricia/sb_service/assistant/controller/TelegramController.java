package com.avaricia.sb_service.assistant.controller;

import java.util.HashMap;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.avaricia.sb_service.assistant.service.AudioTranscriptionService;
import com.avaricia.sb_service.assistant.service.MessageProcessorService;
import com.avaricia.sb_service.assistant.service.TelegramService;
import com.avaricia.sb_service.assistant.service.UserMappingService;
import com.avaricia.sb_service.assistant.service.UserMappingService.LinkResult;

@RestController
@RequestMapping("/telegram")
@Tag(name = "Telegram Webhook", description = "Webhook endpoint for Telegram Bot API integration")
public class TelegramController {

    private final MessageProcessorService messageProcessor;
    private final TelegramService telegramService;
    private final AudioTranscriptionService audioTranscriptionService;
    private final UserMappingService userMappingService;

    public TelegramController(
            MessageProcessorService messageProcessor, 
            TelegramService telegramService,
            AudioTranscriptionService audioTranscriptionService,
            UserMappingService userMappingService) {
        this.messageProcessor = messageProcessor;
        this.telegramService = telegramService;
        this.audioTranscriptionService = audioTranscriptionService;
        this.userMappingService = userMappingService;
    }

    @SuppressWarnings("unchecked")
    @Operation(
        summary = "Telegram Webhook",
        description = "Receives updates from Telegram Bot API. This endpoint is called by Telegram when a user sends a message to the bot."
    )
    @ApiResponse(responseCode = "200", description = "Update processed successfully")
    @PostMapping("/webhook")
    public ResponseEntity<String> onUpdate(@RequestBody HashMap<String, Object> update) {
        System.out.println("Update recibido: " + update);
        
        // Verify message exists
        if (!update.containsKey("message")) {
            return ResponseEntity.ok("OK");
        }
        
        HashMap<String, Object> message = (HashMap<String, Object>) update.get("message");
        
        // Get chat_id for response
        HashMap<String, Object> chat = (HashMap<String, Object>) message.get("chat");
        Long chatId = ((Number) chat.get("id")).longValue();
        
        // Get telegram user info
        HashMap<String, Object> from = (HashMap<String, Object>) message.get("from");
        Long telegramId = ((Number) from.get("id")).longValue();
        String username = (String) from.get("username");
        String firstName = (String) from.get("first_name");
        
        // Extract text from message (text or transcribed audio)
        String text = extractTextFromMessage(message, chatId);
        
        if (text == null || text.isEmpty()) {
            return ResponseEntity.ok("OK");
        }
        
        System.out.println("📨 Mensaje de " + (username != null ? username : firstName) + " (ID: " + telegramId + "): " + text);
        
        try {
            // Check if it's a link command (/start LINK_xxx)
            if (text.startsWith("/start LINK_")) {
                handleLinkCommand(chatId, telegramId, username, firstName, text);
                return ResponseEntity.ok("OK");
            }
            
            // Check if it's just /start (welcome message)
            if (text.equals("/start")) {
                handleStartCommand(chatId, telegramId, firstName);
                return ResponseEntity.ok("OK");
            }
            
            // Check if user is linked
            String userId = userMappingService.getUserId(telegramId, username, firstName);
            
            if (userId == null) {
                // User not linked - send instructions
                sendNotLinkedMessage(chatId, firstName);
                return ResponseEntity.ok("OK");
            }
            
            // Process message and get response
            String response = messageProcessor.processMessage(telegramId, text);
            System.out.println("📤 Respuesta: " + response);
            
            // Send response to user on Telegram
            telegramService.sendMessage(chatId, response);
        } catch (Exception e) {
            System.err.println("Error procesando mensaje: " + e.getMessage());
            e.printStackTrace();
            telegramService.sendMessage(chatId, "Lo siento, hubo un error procesando tu mensaje.");
        }

        return ResponseEntity.ok("OK");
    }
    
    /**
     * Handles the /start LINK_xxx command for account linking.
     */
    private void handleLinkCommand(Long chatId, Long telegramId, String username, 
                                    String firstName, String text) {
        // Extract the link code (everything after "/start LINK_")
        String linkCode = text.substring(12); // Length of "/start LINK_"
        
        System.out.println("🔗 Link attempt - Code: " + linkCode + ", Telegram ID: " + telegramId);
        
        // Try to link the account
        LinkResult result = userMappingService.linkTelegramAccount(
            linkCode, telegramId, username, firstName
        );
        
        if (result.isSuccess()) {
            String welcomeMsg = String.format(
                "✅ *¡Cuenta vinculada exitosamente!*\n\n" +
                "¡Hola %s! 👋\n\n" +
                "Tu cuenta de Telegram está ahora conectada. Ya puedes usar el asistente financiero.\n\n" +
                "💡 *Prueba diciendo:*\n" +
                "• \"Gasté 50k en comida\"\n" +
                "• \"¿Cuánto dinero tengo?\"\n" +
                "• \"Recibí mi sueldo de 2M\"",
                result.getUserName() != null ? result.getUserName() : firstName
            );
            telegramService.sendMessage(chatId, welcomeMsg);
        } else {
            telegramService.sendMessage(chatId, result.getMessage());
        }
    }
    
    /**
     * Handles the basic /start command (without link code).
     */
    private void handleStartCommand(Long chatId, Long telegramId, String firstName) {
        // Check if user is already linked
        String userId = userMappingService.getUserId(telegramId);
        
        if (userId != null) {
            // User already linked
            String welcomeBack = String.format(
                "¡Hola de nuevo, %s! 👋\n\n" +
                "Tu cuenta ya está vinculada. ¿En qué puedo ayudarte hoy?\n\n" +
                "💡 *Comandos útiles:*\n" +
                "• \"¿Cuánto dinero tengo?\"\n" +
                "• \"Muéstrame mis gastos\"\n" +
                "• \"¿En qué gasto más?\"",
                firstName != null ? firstName : "Usuario"
            );
            telegramService.sendMessage(chatId, welcomeBack);
        } else {
            // User not linked
            sendNotLinkedMessage(chatId, firstName);
        }
    }
    
    /**
     * Sends a message to users who haven't linked their account yet.
     */
    private void sendNotLinkedMessage(Long chatId, String firstName) {
        String notLinkedMsg = String.format(
            "¡Hola %s! 👋\n\n" +
            "Soy tu asistente financiero personal.\n\n" +
            "⚠️ *Para usar el bot, primero debes vincular tu cuenta:*\n\n" +
            "1️⃣ Ingresa a tu cuenta en el dashboard web\n" +
            "2️⃣ Ve a Configuración → Conectar Telegram\n" +
            "3️⃣ Haz clic en el botón y abre el link\n\n" +
            "Una vez vinculado, podrás:\n" +
            "💰 Registrar ingresos y gastos\n" +
            "📊 Ver tu balance y resúmenes\n" +
            "🎤 Enviar notas de voz\n" +
            "📏 Establecer límites de gasto",
            firstName != null ? firstName : "Usuario"
        );
        telegramService.sendMessage(chatId, notLinkedMsg);
    }

    /**
     * Extracts text from a message, handling both text messages and voice/audio messages.
     * For voice messages, transcribes the audio using Whisper.
     */
    @SuppressWarnings("unchecked")
    private String extractTextFromMessage(HashMap<String, Object> message, Long chatId) {
        // Check for text message
        String text = (String) message.get("text");
        if (text != null && !text.isEmpty()) {
            return text;
        }
        
        // Check for voice message (voice notes)
        HashMap<String, Object> voice = (HashMap<String, Object>) message.get("voice");
        if (voice != null) {
            return processVoiceMessage(voice, chatId, "nota de voz");
        }
        
        // Check for audio message (audio files)
        HashMap<String, Object> audio = (HashMap<String, Object>) message.get("audio");
        if (audio != null) {
            return processVoiceMessage(audio, chatId, "audio");
        }
        
        // Check for video note (circular video messages)
        HashMap<String, Object> videoNote = (HashMap<String, Object>) message.get("video_note");
        if (videoNote != null) {
            return processVoiceMessage(videoNote, chatId, "video nota");
        }
        
        return null;
    }

    /**
     * Processes a voice/audio message by transcribing it.
     */
    private String processVoiceMessage(HashMap<String, Object> audioObject, Long chatId, String type) {
        String fileId = (String) audioObject.get("file_id");
        Integer duration = (Integer) audioObject.get("duration");
        
        System.out.println("🎤 Recibido " + type + " (duración: " + duration + "s, file_id: " + fileId + ")");
        
        // Notify user that we're processing the audio
        telegramService.sendMessage(chatId, "🎧 Procesando tu " + type + "...");
        
        try {
            String transcription = audioTranscriptionService.transcribeAudio(fileId);
            
            if (transcription == null || transcription.isEmpty()) {
                telegramService.sendMessage(chatId, "❌ No pude entender el audio. Por favor intenta de nuevo o escribe tu mensaje.");
                return null;
            }
            
            // Show the user what we understood
            telegramService.sendMessage(chatId, "📝 Entendí: \"" + transcription + "\"");
            
            return transcription;
        } catch (Exception e) {
            System.err.println("Error transcribing audio: " + e.getMessage());
            telegramService.sendMessage(chatId, "❌ Hubo un error procesando el audio. Por favor intenta de nuevo.");
            return null;
        }
    }
}
