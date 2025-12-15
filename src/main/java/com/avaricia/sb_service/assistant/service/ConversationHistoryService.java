package com.avaricia.sb_service.assistant.service;

import com.avaricia.sb_service.assistant.entity.ConversationMessageEntity;
import com.avaricia.sb_service.assistant.repository.ConversationMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Service for managing conversation history per user.
 * Stores the last N messages for context in AI conversations.
 * 
 * UPDATED: Now persists to PostgreSQL database for:
 * - Persistence across restarts
 * - Scalability with multiple instances
 * - Analytics and debugging capabilities
 * 
 * Still uses in-memory cache for fast access with DB as source of truth.
 */
@Service
public class ConversationHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ConversationHistoryService.class);

    private static final int MAX_MESSAGES = 10;
    private static final int CONVERSATION_TIMEOUT_MINUTES = 30;

    private final ConversationMessageRepository messageRepository;
    
    // In-memory cache for fast access
    private final Map<Long, List<ConversationMessage>> conversationCache = new HashMap<>();
    private final Map<Long, LocalDateTime> lastActivity = new HashMap<>();

    public ConversationHistoryService(ConversationMessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Transactional
    public void addUserMessage(Long telegramId, String message) {
        addMessage(telegramId, "user", message);
    }

    @Transactional
    public void addAssistantMessage(Long telegramId, String message) {
        addMessage(telegramId, "assistant", message);
    }

    public List<ConversationMessage> getHistory(Long telegramId) {
        LocalDateTime lastTime = lastActivity.get(telegramId);
        if (lastTime != null && lastTime.plusMinutes(CONVERSATION_TIMEOUT_MINUTES).isBefore(LocalDateTime.now())) {
            clearHistory(telegramId);
            return new ArrayList<>();
        }
        
        // Check cache first
        List<ConversationMessage> cached = conversationCache.get(telegramId);
        if (cached != null && !cached.isEmpty()) {
            return new ArrayList<>(cached);
        }
        
        // Load from database
        return loadFromDatabase(telegramId);
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

    @Transactional
    public void clearHistory(Long telegramId) {
        conversationCache.remove(telegramId);
        lastActivity.remove(telegramId);
        
        try {
            messageRepository.deleteByTelegramId(telegramId);
            log.info("🧹 Conversation history cleared for Telegram ID: {}", telegramId);
        } catch (Exception e) {
            log.error("❌ Error clearing conversation history: {}", e.getMessage());
        }
    }

    @Transactional
    private void addMessage(Long telegramId, String role, String content) {
        LocalDateTime lastTime = lastActivity.get(telegramId);
        if (lastTime != null && lastTime.plusMinutes(CONVERSATION_TIMEOUT_MINUTES).isBefore(LocalDateTime.now())) {
            clearHistory(telegramId);
        }
        
        // Create and save to database
        ConversationMessageEntity entity = role.equals("user") 
            ? ConversationMessageEntity.userMessage(telegramId, content)
            : ConversationMessageEntity.assistantMessage(telegramId, content);
        
        try {
            messageRepository.save(entity);
            log.debug("💾 Saved message to DB: {} - {}", role, telegramId);
        } catch (Exception e) {
            log.error("❌ Error saving message to DB: {}", e.getMessage());
        }
        
        // Update cache
        List<ConversationMessage> cache = conversationCache.computeIfAbsent(
            telegramId, 
            k -> new ArrayList<>()
        );
        
        cache.add(new ConversationMessage(role, content, LocalDateTime.now()));
        
        // Trim cache to max size
        while (cache.size() > MAX_MESSAGES) {
            cache.remove(0);
        }
        
        lastActivity.put(telegramId, LocalDateTime.now());
        
        // Trim database messages if too many
        trimDatabaseMessages(telegramId);
    }

    private List<ConversationMessage> loadFromDatabase(Long telegramId) {
        try {
            OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(CONVERSATION_TIMEOUT_MINUTES);
            List<ConversationMessageEntity> entities = 
                messageRepository.findByTelegramIdAndCreatedAtAfterOrderByCreatedAtAsc(telegramId, cutoff);
            
            // Only take the last MAX_MESSAGES
            if (entities.size() > MAX_MESSAGES) {
                entities = entities.subList(entities.size() - MAX_MESSAGES, entities.size());
            }
            
            List<ConversationMessage> messages = new ArrayList<>();
            for (ConversationMessageEntity entity : entities) {
                messages.add(new ConversationMessage(
                    entity.getRole(),
                    entity.getContent(),
                    entity.getCreatedAt().toLocalDateTime()
                ));
            }
            
            // Update cache
            conversationCache.put(telegramId, messages);
            if (!messages.isEmpty()) {
                lastActivity.put(telegramId, LocalDateTime.now());
            }
            
            log.debug("📥 Loaded {} messages from DB for Telegram ID: {}", messages.size(), telegramId);
            return messages;
            
        } catch (Exception e) {
            log.error("❌ Error loading messages from DB: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Transactional
    private void trimDatabaseMessages(Long telegramId) {
        try {
            long count = messageRepository.countByTelegramId(telegramId);
            if (count > MAX_MESSAGES * 2) {  // Keep some buffer
                int toDelete = (int) (count - MAX_MESSAGES);
                messageRepository.deleteOldestMessages(telegramId, toDelete);
                log.debug("🗑️ Trimmed {} old messages for Telegram ID: {}", toDelete, telegramId);
            }
        } catch (Exception e) {
            log.warn("⚠️ Error trimming messages: {}", e.getMessage());
        }
    }

    public int getHistorySize(Long telegramId) {
        List<ConversationMessage> cached = conversationCache.get(telegramId);
        if (cached != null) {
            return cached.size();
        }
        return (int) messageRepository.countByTelegramId(telegramId);
    }

    /**
     * Cleanup old messages from all users.
     * Should be called periodically (e.g., scheduled task).
     */
    @Transactional
    public int cleanupOldMessages() {
        try {
            OffsetDateTime cutoff = OffsetDateTime.now().minusHours(24);
            int deleted = messageRepository.deleteByCreatedAtBefore(cutoff);
            if (deleted > 0) {
                log.info("🧹 Cleaned up {} old messages from database", deleted);
            }
            return deleted;
        } catch (Exception e) {
            log.error("❌ Error cleaning up old messages: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Message data class for internal use.
     */
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
