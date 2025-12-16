package com.avaricia.sb_service.assistant.dto;

/**
 * DTO for notification response
 */
public class NotificationResponse {
    
    private boolean success;
    private String message;
    private String telegramId;
    
    public NotificationResponse() {}
    
    public NotificationResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public NotificationResponse(boolean success, String message, String telegramId) {
        this.success = success;
        this.message = message;
        this.telegramId = telegramId;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getTelegramId() {
        return telegramId;
    }
    
    public void setTelegramId(String telegramId) {
        this.telegramId = telegramId;
    }
}
