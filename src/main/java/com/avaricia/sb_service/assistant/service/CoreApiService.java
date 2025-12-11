package com.avaricia.sb_service.assistant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for communicating with the Core MS (.NET service).
 * Handles all HTTP requests to the financial management API.
 */
@Service
public class CoreApiService {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public CoreApiService(@Value("${ms.core.base-url}") String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
        this.objectMapper = new ObjectMapper();
    }

    // ==================== USER ENDPOINTS ====================

    public Map<String, Object> getUserByTelegramId(Long telegramId) {
        String url = baseUrl + "/api/User/telegram/" + telegramId;
        return getRequest(url);
    }

    public Map<String, Object> linkTelegram(String linkCode, Long telegramId, 
                                             String telegramUsername, String telegramFirstName) {
        String url = baseUrl + "/api/User/link-telegram";
        
        Map<String, Object> body = new HashMap<>();
        body.put("linkCode", linkCode);
        body.put("telegramId", telegramId);
        body.put("telegramUsername", telegramUsername != null ? telegramUsername : "");
        body.put("telegramFirstName", telegramFirstName != null ? telegramFirstName : "");
        
        return postRequest(url, body);
    }

    public Map<String, Object> createUser(String name, String email, String phoneNumber, Double initialBalance) {
        String url = baseUrl + "/api/User";
        
        String userEmail = email;
        if (userEmail == null || userEmail.isEmpty()) {
            String uniqueId = phoneNumber != null ? phoneNumber.replaceAll("[^0-9]", "") : String.valueOf(System.currentTimeMillis());
            userEmail = "user_" + uniqueId + "@avaricia.app";
        }
        
        Map<String, Object> body = new HashMap<>();
        body.put("name", name != null ? name : "Usuario");
        body.put("email", userEmail);
        body.put("phoneNumber", phoneNumber);
        body.put("initialBalance", initialBalance != null ? initialBalance : 0.0);
        
        return postRequest(url, body);
    }

    public Map<String, Object> getUserById(String userId) {
        String url = baseUrl + "/api/User/" + userId;
        return getRequest(url);
    }

    public Map<String, Object> getUserByEmail(String email) {
        String url = baseUrl + "/api/User/email/" + email;
        return getRequest(url);
    }

    public Map<String, Object> getUserByPhone(String phoneNumber) {
        String url = baseUrl + "/api/User/phone/" + phoneNumber;
        return getRequest(url);
    }

    /**
     * Get user's Telegram ID from their user ID.
     * Used for sending notifications to users.
     * @param userId The user's system ID
     * @return Map containing telegramId field if user has linked Telegram
     */
    public Map<String, Object> getTelegramIdByUserId(String userId) {
        String url = baseUrl + "/api/User/" + userId + "/telegram";
        return getRequest(url);
    }

    // ==================== SPENDING VALIDATION ENDPOINTS ====================

    public Map<String, Object> validateExpense(String userId, Double amount, String category, String description) {
        String url = baseUrl + "/api/SpendingValidation/validate";
        
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("amount", amount);
        body.put("category", category);
        body.put("description", description);
        
        return postRequest(url, body);
    }

    // ==================== TRANSACTION ENDPOINTS ====================

    public Map<String, Object> createTransaction(String userId, Double amount, String type, 
                                                   String category, String description, String source) {
        String url = baseUrl + "/api/Transaction";
        
        // Use category as description if description is null or empty
        String finalDescription = (description != null && !description.isEmpty()) 
            ? description 
            : category;
        
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("amount", amount);
        body.put("type", type);
        body.put("category", category);
        body.put("description", finalDescription);
        body.put("source", source);
        
        return postRequest(url, body);
    }

    public Map<String, Object> createTransaction(String userId, Double amount, String type, 
                                                   String category, String description) {
        return createTransaction(userId, amount, type, category, description, "Telegram");
    }

    public Map<String, Object> getTransactions(String userId) {
        String url = baseUrl + "/api/Transaction/user/" + userId;
        return getRequest(url);
    }

    /**
     * Get transactions with optional type filter.
     * @param userId The user ID
     * @param type Optional filter: "Income" or "Expense". Null returns all transactions.
     * @return Map containing the transactions data
     */
    public Map<String, Object> getTransactions(String userId, String type) {
        String url = baseUrl + "/api/Transaction/user/" + userId;
        if (type != null && !type.isEmpty()) {
            url += "?type=" + type;
        }
        return getRequest(url);
    }

    public Map<String, Object> getTransactionById(String transactionId) {
        String url = baseUrl + "/api/Transaction/" + transactionId;
        return getRequest(url);
    }

    public Map<String, Object> deleteTransaction(String transactionId) {
        String url = baseUrl + "/api/Transaction/" + transactionId;
        return deleteRequest(url);
    }

    // ==================== FINANCIAL RULE ENDPOINTS ====================

    public Map<String, Object> createRule(String userId, String type, String category, 
                                           Double amountLimit, String period) {
        String url = baseUrl + "/api/FinancialRule";
        
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("type", type);
        body.put("category", category);
        body.put("amountLimit", amountLimit);
        body.put("period", period);
        
        return postRequest(url, body);
    }

    public Map<String, Object> getRules(String userId) {
        String url = baseUrl + "/api/FinancialRule/user/" + userId;
        return getRequest(url);
    }

    public Map<String, Object> getRuleById(String ruleId) {
        String url = baseUrl + "/api/FinancialRule/" + ruleId;
        return getRequest(url);
    }

    public Map<String, Object> deleteRule(String ruleId) {
        String url = baseUrl + "/api/FinancialRule/" + ruleId;
        return deleteRequest(url);
    }

    public Map<String, Object> deactivateRule(String ruleId) {
        String url = baseUrl + "/api/FinancialRule/" + ruleId + "/deactivate";
        return patchRequest(url, null);
    }

    // ==================== BALANCE & QUERY ENDPOINTS ====================

    public Map<String, Object> getUserBalance(String userId) {
        String url = baseUrl + "/api/User/" + userId + "/balance";
        return getRequest(url);
    }

    public Map<String, Object> getTransactionsByRange(String userId, String startDate, String endDate) {
        return getTransactionsByRange(userId, startDate, endDate, null);
    }

    /**
     * Get transactions within a date range with optional type filter.
     * @param userId The user ID
     * @param startDate Start date in YYYY-MM-DD format
     * @param endDate End date in YYYY-MM-DD format
     * @param type Optional filter: "Income" or "Expense". Null returns all transactions.
     * @return Map containing the transactions data
     */
    public Map<String, Object> getTransactionsByRange(String userId, String startDate, String endDate, String type) {
        String utcStartDate = convertToUtcFormat(startDate, true);
        String utcEndDate = convertToUtcFormat(endDate, false);
        String url = baseUrl + "/api/Transaction/user/" + userId + "/range?startDate=" + utcStartDate + "&endDate=" + utcEndDate;
        if (type != null && !type.isEmpty()) {
            url += "&type=" + type;
        }
        return getRequest(url);
    }

    public Map<String, Object> getTransactionsByDate(String userId, String date) {
        String utcDate = convertToUtcFormat(date, true);
        String url = baseUrl + "/api/Transaction/user/" + userId + "/date/" + utcDate;
        return getRequest(url);
    }

    public Map<String, Object> searchTransactions(String userId, String query) {
        String url = baseUrl + "/api/Transaction/user/" + userId + "/search?query=" + query;
        return getRequest(url);
    }

    public Map<String, Object> getTransactionSummaryByCategory(String userId) {
        String url = baseUrl + "/api/Transaction/user/" + userId + "/summary/category";
        return getRequest(url);
    }

    public Map<String, Object> getTransactionSummaryByCategory(String userId, String startDate, String endDate) {
        String utcStartDate = convertToUtcFormat(startDate, true);
        String utcEndDate = convertToUtcFormat(endDate, false);
        String url = baseUrl + "/api/Transaction/user/" + userId + "/summary/category?startDate=" + utcStartDate + "&endDate=" + utcEndDate;
        return getRequest(url);
    }

    // ==================== HELPER METHODS ====================

    @SuppressWarnings("unchecked")
    private Map<String, Object> postRequest(String url, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            
            System.out.println("📤 POST " + url);
            System.out.println("   Body: " + body);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            System.out.println("📥 Response: " + response.getStatusCode());
            
            if (response.getBody() != null && !response.getBody().isEmpty()) {
                return objectMapper.readValue(response.getBody(), Map.class);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("status", response.getStatusCode().value());
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ Error in POST " + url + ": " + e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return error;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getRequest(String url) {
        try {
            System.out.println("📤 GET " + url);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            System.out.println("📥 Response: " + response.getStatusCode());
            
            if (response.getBody() != null && !response.getBody().isEmpty()) {
                JsonNode node = objectMapper.readTree(response.getBody());
                if (node.isArray()) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("data", objectMapper.readValue(response.getBody(), java.util.List.class));
                    return result;
                }
                return objectMapper.readValue(response.getBody(), Map.class);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", null);
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ Error in GET " + url + ": " + e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return error;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deleteRequest(String url) {
        try {
            System.out.println("📤 DELETE " + url);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, null, String.class);
            
            System.out.println("📥 Response: " + response.getStatusCode());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("status", response.getStatusCode().value());
            
            if (response.getBody() != null && !response.getBody().isEmpty()) {
                result.putAll(objectMapper.readValue(response.getBody(), Map.class));
            }
            
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ Error in DELETE " + url + ": " + e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return error;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> patchRequest(String url, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            
            System.out.println("📤 PATCH " + url);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PATCH, request, String.class);
            
            System.out.println("📥 Response: " + response.getStatusCode());
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("status", response.getStatusCode().value());
            
            if (response.getBody() != null && !response.getBody().isEmpty()) {
                result.putAll(objectMapper.readValue(response.getBody(), Map.class));
            }
            
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ Error in PATCH " + url + ": " + e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return error;
        }
    }

    private String convertToUtcFormat(String dateStr, boolean isStartOfDay) {
        try {
            if (dateStr == null || dateStr.isEmpty()) {
                return dateStr;
            }
            
            if (dateStr.contains("T")) {
                if (!dateStr.endsWith("Z")) {
                    return dateStr + "Z";
                }
                return dateStr;
            }
            
            if (isStartOfDay) {
                return dateStr + "T00:00:00Z";
            } else {
                return dateStr + "T23:59:59Z";
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error converting date to UTC: " + e.getMessage());
            return dateStr;
        }
    }
}
