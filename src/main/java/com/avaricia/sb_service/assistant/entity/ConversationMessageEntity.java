package com.avaricia.sb_service.assistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing a single message in a conversation.
 * Maps to the "ConversationMessages" table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "\"ConversationMessages\"")
public class ConversationMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "\"Id\"")
    private UUID id;

    @Column(name = "\"TelegramId\"", nullable = false)
    private Long telegramId;

    @Column(name = "\"Role\"", nullable = false, length = 20)
    private String role;  // 'user' or 'assistant'

    @Column(name = "\"Content\"", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "\"CreatedAt\"", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    /**
     * Factory method to create a user message.
     */
    public static ConversationMessageEntity userMessage(Long telegramId, String content) {
        return ConversationMessageEntity.builder()
            .telegramId(telegramId)
            .role("user")
            .content(content)
            .createdAt(OffsetDateTime.now())
            .build();
    }

    /**
     * Factory method to create an assistant message.
     */
    public static ConversationMessageEntity assistantMessage(Long telegramId, String content) {
        return ConversationMessageEntity.builder()
            .telegramId(telegramId)
            .role("assistant")
            .content(content)
            .createdAt(OffsetDateTime.now())
            .build();
    }
}
