package com.avaricia.sb_service.assistant.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock service for testing when MS Core is not available.
 * Simulates API responses and stores data in memory.
 */
@Service
public class MockCoreApiService {

    private final Map<String, Map<String, Object>> users = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> userTransactions = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> userRules = new ConcurrentHashMap<>();
    private final Map<String, Double> userBalances = new ConcurrentHashMap<>();

    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public Map<String, Object> createUser(String name, String email, String phoneNumber, Double initialBalance) {
        System.out.println("=== MOCK: Creating User ===");

        String userId = UUID.randomUUID().toString();
        Map<String, Object> user = new HashMap<>();
        user.put("id", userId);
        user.put("name", name);
        user.put("email", email);
        user.put("phoneNumber", phoneNumber);
        user.put("balance", initialBalance != null ? initialBalance : 0.0);
        user.put("createdAt", LocalDateTime.now().format(formatter));

        users.put(userId, user);
        if (phoneNumber != null) {
            users.put("phone:" + phoneNumber, user);
        }
        if (email != null) {
            users.put("email:" + email, user);
        }
        userBalances.put(userId, initialBalance != null ? initialBalance : 1000000.0);

        return user;
    }

    public Map<String, Object> getUserById(String userId) {
        Map<String, Object> user = users.get(userId);
        if (user != null) {
            return user;
        }

        Map<String, Object> error = new HashMap<>();
        error.put("error", "User not found");
        return error;
    }

    public Map<String, Object> getUserByEmail(String email) {
        Map<String, Object> user = users.get("email:" + email);
        if (user != null) {
            return user;
        }

        Map<String, Object> error = new HashMap<>();
        error.put("error", "User not found");
        return error;
    }

    public Map<String, Object> getUserByPhone(String phoneNumber) {
        Map<String, Object> user = users.get("phone:" + phoneNumber);
        if (user != null) {
            return user;
        }

        Map<String, Object> error = new HashMap<>();
        error.put("error", "User not found");
        return error;
    }

    public Map<String, Object> validateExpense(String userId, Double amount, String category, String description) {
        Double balance = userBalances.getOrDefault(userId, 1000000.0);
        boolean isApproved = amount <= balance;

        Map<String, Object> response = new HashMap<>();
        response.put("isApproved", isApproved);
        
        if (isApproved) {
            response.put("verdict", "Aprobado");
            response.put("reason", String.format("Gasto permitido. Saldo después: $%,.0f", balance - amount));
            response.put("remainingBudget", balance - amount);
        } else {
            response.put("verdict", "Rechazado");
            response.put("reason", String.format("Saldo insuficiente. Saldo actual: $%,.0f", balance));
            response.put("remainingBudget", balance);
        }

        return response;
    }

    public Map<String, Object> createTransaction(String userId, Double amount, String type, String category, String description) {
        String transactionId = UUID.randomUUID().toString();
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("id", transactionId);
        transaction.put("userId", userId);
        transaction.put("amount", amount);
        transaction.put("type", type);
        transaction.put("category", category);
        transaction.put("description", description);
        transaction.put("source", "Telegram");
        transaction.put("createdAt", LocalDateTime.now().format(formatter));

        userTransactions.computeIfAbsent(userId, k -> new ArrayList<>()).add(0, transaction);

        Double currentBalance = userBalances.getOrDefault(userId, 1000000.0);
        if ("Expense".equals(type)) {
            userBalances.put(userId, currentBalance - amount);
        } else {
            userBalances.put(userId, currentBalance + amount);
        }

        System.out.println("🧪 [MOCK] Transaction created: " + type + " $" + amount);
        return transaction;
    }

    public Map<String, Object> getTransactions(String userId) {
        List<Map<String, Object>> transactions = userTransactions.getOrDefault(userId, new ArrayList<>());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", transactions);

        return response;
    }

    public Map<String, Object> deleteTransaction(String transactionId) {
        for (Map.Entry<String, List<Map<String, Object>>> entry : userTransactions.entrySet()) {
            List<Map<String, Object>> transactions = entry.getValue();
            for (int i = 0; i < transactions.size(); i++) {
                Map<String, Object> tx = transactions.get(i);
                if (transactionId.equals(tx.get("id"))) {
                    transactions.remove(i);
                    
                    String userId = entry.getKey();
                    Double amount = (Double) tx.get("amount");
                    String type = (String) tx.get("type");
                    Double currentBalance = userBalances.getOrDefault(userId, 0.0);
                    
                    if ("Expense".equals(type)) {
                        userBalances.put(userId, currentBalance + amount);
                    } else {
                        userBalances.put(userId, currentBalance - amount);
                    }
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    return response;
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", "Transaction not found");
        return response;
    }

    public Map<String, Object> createRule(String userId, String type, String category, Double amountLimit, String period) {
        String ruleId = UUID.randomUUID().toString();
        Map<String, Object> rule = new HashMap<>();
        rule.put("id", ruleId);
        rule.put("userId", userId);
        rule.put("type", type);
        rule.put("category", category);
        rule.put("amountLimit", amountLimit);
        rule.put("period", period);
        rule.put("isActive", true);
        rule.put("createdAt", LocalDateTime.now().format(formatter));

        userRules.computeIfAbsent(userId, k -> new ArrayList<>()).add(rule);

        return rule;
    }

    public Map<String, Object> getRules(String userId) {
        List<Map<String, Object>> rules = userRules.getOrDefault(userId, new ArrayList<>());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", rules);

        return response;
    }

    public Map<String, Object> createRecurringTransaction(String userId, Double amount, String type, 
            String category, String description, String frequency, Integer dayOfMonth) {
        
        String recurringId = UUID.randomUUID().toString();
        Map<String, Object> recurring = new HashMap<>();
        recurring.put("id", recurringId);
        recurring.put("userId", userId);
        recurring.put("amount", amount);
        recurring.put("type", type);
        recurring.put("category", category);
        recurring.put("description", description);
        recurring.put("frequency", frequency != null ? frequency : "Monthly");
        recurring.put("dayOfMonth", dayOfMonth != null ? dayOfMonth : 1);
        recurring.put("isActive", true);
        recurring.put("createdAt", LocalDateTime.now().format(formatter));

        return recurring;
    }

    public Double getBalance(String userId) {
        return userBalances.getOrDefault(userId, 1000000.0);
    }

    public void setBalance(String userId, Double balance) {
        userBalances.put(userId, balance);
    }
}
