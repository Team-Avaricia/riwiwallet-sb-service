package com.avaricia.sb_service.assistant.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing conversation history per user.
 * Stores the last N messages for context in AI conversations.
 */
@Service
public class ConversationHistoryService {

    private static final int MAX_MESSAGES = 10;
    private static final int CONVERSATION_TIMEOUT_MINUTES = 30;

    private final Map<Long, List<ConversationMessage>> conversationHistory = new ConcurrentHashMap<>();
    private final Map<Long, LocalDateTime> lastActivity = new ConcurrentHashMap<>();

    public void addUserMessage(Long telegramId, String message) {
        addMessage(telegramId, "user", message);
    }

    public void addAssistantMessage(Long telegramId, String message) {
        addMessage(telegramId, "assistant", message);
    }

    public List<ConversationMessage> getHistory(Long telegramId) {
        LocalDateTime lastTime = lastActivity.get(telegramId);
        if (lastTime != null && lastTime.plusMinutes(CONVERSATION_TIMEOUT_MINUTES).isBefore(LocalDateTime.now())) {
            clearHistory(telegramId);
            return new ArrayList<>();
        }
        
        return conversationHistory.getOrDefault(telegramId, new ArrayList<>());
    }

    public List<Map<String, String>> getHistoryForOpenAI(Long telegramId) {
        List<ConversationMessage> history = getHistory(telegramId);
        List<Map<String, String>> result = new ArrayList<>();
        
        for (ConversationMessage msg : history) {
            Map<String, String> messageMap = new HashMap<>();
            messageMap.put("role", msg.getRole());
            messageMap.put("content", msg.getContent());
            result.add(messageMap);
        }
        
        return result;
    }

    public String getContextSummary(Long telegramId) {
        List<ConversationMessage> history = getHistory(telegramId);
        
        if (history.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Historial reciente de la conversación:\n");
        
        for (ConversationMessage msg : history) {
            String role = "user".equals(msg.getRole()) ? "Usuario" : "Asistente";
            sb.append(role).append(": ").append(msg.getContent()).append("\n");
        }
        
        return sb.toString();
    }

    public void clearHistory(Long telegramId) {
        conversationHistory.remove(telegramId);
        lastActivity.remove(telegramId);
        System.out.println("🧹 Conversation history cleared for Telegram ID: " + telegramId);
    }

    private void addMessage(Long telegramId, String role, String content) {
        LocalDateTime lastTime = lastActivity.get(telegramId);
        if (lastTime != null && lastTime.plusMinutes(CONVERSATION_TIMEOUT_MINUTES).isBefore(LocalDateTime.now())) {
            clearHistory(telegramId);
        }
        
        List<ConversationMessage> history = conversationHistory.computeIfAbsent(telegramId, k -> new ArrayList<>());
        
        history.add(new ConversationMessage(role, content, LocalDateTime.now()));
        
        while (history.size() > MAX_MESSAGES) {
            history.remove(0);
        }
        
        lastActivity.put(telegramId, LocalDateTime.now());
    }

    public int getHistorySize(Long telegramId) {
        return conversationHistory.getOrDefault(telegramId, new ArrayList<>()).size();
    }

    public static class ConversationMessage {
        private final String role;
        private final String content;
        private final LocalDateTime timestamp;

        public ConversationMessage(String role, String content, LocalDateTime timestamp) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return role + ": " + content;
        }
    }
}
