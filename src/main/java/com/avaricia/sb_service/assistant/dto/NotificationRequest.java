package com.avaricia.sb_service.assistant.dto;

/**
 * DTO for incoming notification requests from external services (Dashboard, etc.)
 */
public class NotificationRequest {
    
    private String userId;
    private String message;
    
    public NotificationRequest() {}
    
    public NotificationRequest(String userId, String message) {
        this.userId = userId;
        this.message = message;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
