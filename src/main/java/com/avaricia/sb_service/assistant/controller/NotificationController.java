package com.avaricia.sb_service.assistant.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.avaricia.sb_service.assistant.dto.NotificationRequest;
import com.avaricia.sb_service.assistant.dto.NotificationResponse;
import com.avaricia.sb_service.assistant.service.TelegramService;
import com.avaricia.sb_service.assistant.service.UserMappingService;

/**
 * Controller for handling notification requests from external services.
 * Allows Dashboard and other services to send messages to users via Telegram.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final TelegramService telegramService;
    private final UserMappingService userMappingService;

    public NotificationController(TelegramService telegramService, UserMappingService userMappingService) {
        this.telegramService = telegramService;
        this.userMappingService = userMappingService;
    }

    /**
     * Send a notification to a user via Telegram.
     * 
     * @param request NotificationRequest containing userId and message
     * @return NotificationResponse with success status and details
     * 
     * Example request:
     * POST /api/notifications/telegram
     * {
     *   "userId": "d532bd25-b813-4c7c-8af8-b3ca08000c52",
     *   "message": "💸 Nueva transacción registrada desde el Dashboard"
     * }
     */
    @PostMapping("/telegram")
    public ResponseEntity<NotificationResponse> sendTelegramNotification(@RequestBody NotificationRequest request) {
        System.out.println("📬 Notification request received for user: " + request.getUserId());
        
        // Validate request
        if (request.getUserId() == null || request.getUserId().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new NotificationResponse(false, "userId is required"));
        }
        
        if (request.getMessage() == null || request.getMessage().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new NotificationResponse(false, "message is required"));
        }
        
        try {
            // Get Telegram ID for the user
            Long telegramId = userMappingService.getTelegramId(request.getUserId());
            
            if (telegramId == null) {
                System.out.println("⚠️ User " + request.getUserId() + " has no linked Telegram account");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new NotificationResponse(false, 
                        "User has no linked Telegram account or has not interacted with the bot yet"));
            }
            
            // Send message via Telegram
            telegramService.sendMessage(telegramId, request.getMessage());
            
            System.out.println("✅ Notification sent to Telegram ID: " + telegramId);
            return ResponseEntity.ok(
                new NotificationResponse(true, "Notification sent successfully", telegramId.toString())
            );
            
        } catch (Exception e) {
            System.err.println("❌ Error sending notification: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new NotificationResponse(false, "Error sending notification: " + e.getMessage()));
        }
    }
}
