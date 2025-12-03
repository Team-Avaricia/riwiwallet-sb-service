package com.avaricia.sb_service.assistant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for mapping telegram_id/phone_number to system userId.
 * Uses a combination of in-memory cache and Core API calls.
 */
@Service
public class UserMappingService {
    
    private final CoreApiService coreApi;
    private final boolean useMock;
    
    private final Map<Long, String> telegramToUserIdCache = new ConcurrentHashMap<>();
    private final Map<String, String> phoneToUserIdCache = new ConcurrentHashMap<>();

    public UserMappingService(
            CoreApiService coreApi,
            @Value("${ms.core.use-mock:false}") boolean useMock) {
        this.coreApi = coreApi;
        this.useMock = useMock;
    }
    
    public String getUserId(Long telegramId) {
        return getUserId(telegramId, null, null);
    }

    public String getUserId(Long telegramId, String username, String firstName) {
        if (telegramToUserIdCache.containsKey(telegramId)) {
            return telegramToUserIdCache.get(telegramId);
        }
        
        if (useMock) {
            String newUserId = UUID.randomUUID().toString();
            telegramToUserIdCache.put(telegramId, newUserId);
            System.out.println("🧪 [MOCK] New user created - Telegram ID: " + telegramId + " -> User ID: " + newUserId);
            return newUserId;
        }
        
        Map<String, Object> userResult = coreApi.getUserByTelegramId(telegramId);
        
        System.out.println("🔍 API Response for TelegramId " + telegramId + ": " + userResult);
        
        if (!userResult.containsKey("error") && (userResult.containsKey("id") || userResult.containsKey("Id"))) {
            String userId = extractUserId(userResult);
            if (userId != null) {
                telegramToUserIdCache.put(telegramId, userId);
                System.out.println("🔗 User found by TelegramId - Telegram: " + telegramId + " -> User ID: " + userId);
                return userId;
            }
        }
        
        System.out.println("⚠️ User not linked - Telegram ID: " + telegramId);
        return null;
    }
    
    public LinkResult linkTelegramAccount(String linkCode, Long telegramId, 
                                           String username, String firstName) {
        if (useMock) {
            String mockUserId = UUID.randomUUID().toString();
            telegramToUserIdCache.put(telegramId, mockUserId);
            System.out.println("🧪 [MOCK] Telegram linked - Code: " + linkCode + " -> User ID: " + mockUserId);
            return new LinkResult(true, "¡Cuenta vinculada exitosamente!", mockUserId, "Usuario Mock");
        }
        
        Map<String, Object> result = coreApi.linkTelegram(linkCode, telegramId, username, firstName);
        
        if (result.containsKey("error")) {
            String error = (String) result.get("error");
            System.err.println("❌ Link failed: " + error);
            return new LinkResult(false, translateError(error), null, null);
        }
        
        Boolean success = (Boolean) result.get("success");
        if (success != null && success) {
            String userId = extractUserId(result);
            String userName = (String) result.get("userName");
            
            if (userId != null) {
                telegramToUserIdCache.put(telegramId, userId);
            }
            
            String message = result.containsKey("message") 
                ? (String) result.get("message") 
                : "¡Cuenta vinculada exitosamente!";
            
            System.out.println("✅ Telegram linked - ID: " + telegramId + " -> User: " + userName);
            return new LinkResult(true, message, userId, userName);
        }
        
        String errorMsg = result.containsKey("error") 
            ? (String) result.get("error") 
            : "Error desconocido al vincular";
        return new LinkResult(false, translateError(errorMsg), null, null);
    }
    
    private String translateError(String error) {
        if (error == null) return "Error desconocido";
        
        if (error.contains("Invalid") || error.contains("invalid")) {
            return "❌ El código de vinculación es inválido.";
        }
        if (error.contains("expired") || error.contains("Expired")) {
            return "⏰ El código de vinculación ha expirado. Por favor genera uno nuevo desde el dashboard.";
        }
        if (error.contains("already used") || error.contains("Used")) {
            return "🔄 Este código ya fue utilizado. Por favor genera uno nuevo.";
        }
        if (error.contains("already linked")) {
            return "🔗 Esta cuenta de Telegram ya está vinculada a otro usuario.";
        }
        
        return "❌ " + error;
    }
    
    public static class LinkResult {
        private final boolean success;
        private final String message;
        private final String userId;
        private final String userName;
        
        public LinkResult(boolean success, String message, String userId, String userName) {
            this.success = success;
            this.message = message;
            this.userId = userId;
            this.userName = userName;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getUserId() { return userId; }
        public String getUserName() { return userName; }
    }
    
    private String extractUserId(Map<String, Object> result) {
        String[] possibleFields = {"id", "Id", "userId", "UserId", "ID"};
        
        for (String field : possibleFields) {
            if (result.containsKey(field)) {
                Object id = result.get(field);
                if (id != null) {
                    return id.toString();
                }
            }
        }
        return null;
    }

    public String getUserIdByPhone(String phoneNumber, String name) {
        if (phoneToUserIdCache.containsKey(phoneNumber)) {
            return phoneToUserIdCache.get(phoneNumber);
        }
        
        if (useMock) {
            String newUserId = UUID.randomUUID().toString();
            phoneToUserIdCache.put(phoneNumber, newUserId);
            System.out.println("🧪 [MOCK] New user created - Phone: " + phoneNumber + " -> User ID: " + newUserId);
            return newUserId;
        }
        
        Map<String, Object> userResult = coreApi.getUserByPhone(phoneNumber);
        
        if (!userResult.containsKey("error") && userResult.containsKey("id")) {
            String userId = (String) userResult.get("id");
            phoneToUserIdCache.put(phoneNumber, userId);
            System.out.println("📱 User found by phone - Phone: " + phoneNumber + " -> User ID: " + userId);
            return userId;
        }
        
        String userId = createNewUser(
            name != null ? name : "Usuario WhatsApp",
            null,
            phoneNumber
        );
        
        phoneToUserIdCache.put(phoneNumber, userId);
        return userId;
    }

    private String createNewUser(String name, String email, String phoneNumber) {
        Map<String, Object> createResult = coreApi.createUser(name, email, phoneNumber, 0.0);
        
        if (createResult.containsKey("error")) {
            String fallbackId = UUID.randomUUID().toString();
            System.err.println("⚠️ Failed to create user in Core API, using fallback ID: " + fallbackId);
            return fallbackId;
        }
        
        String userId = (String) createResult.get("id");
        if (userId == null) {
            userId = createResult.containsKey("userId") ? (String) createResult.get("userId") : UUID.randomUUID().toString();
        }
        
        System.out.println("✅ New user created in Core API - Name: " + name + " -> User ID: " + userId);
        return userId;
    }
    
    public void registerUser(Long telegramId, String userId) {
        telegramToUserIdCache.put(telegramId, userId);
        System.out.println("📝 User registered - Telegram ID: " + telegramId + " -> User ID: " + userId);
    }
    
    public boolean isUserRegistered(Long telegramId) {
        return telegramToUserIdCache.containsKey(telegramId);
    }

    public Long getTelegramId(String userId) {
        return telegramToUserIdCache.entrySet().stream()
                .filter(entry -> entry.getValue().equals(userId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public void clearCache() {
        telegramToUserIdCache.clear();
        phoneToUserIdCache.clear();
        System.out.println("🧹 User cache cleared");
    }
}
