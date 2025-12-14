package com.avaricia.sb_service.assistant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity representing a pending confirmation for high-value transactions.
 * Maps to the "PendingConfirmations" table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "\"PendingConfirmations\"")
public class PendingConfirmationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "\"Id\"")
    private UUID id;

    @Column(name = "\"TelegramId\"", nullable = false)
    private Long telegramId;

    @Column(name = "\"UserId\"", nullable = false, length = 100)
    private String userId;

    @Column(name = "\"ActionType\"", nullable = false, length = 50)
    private String actionType;  // 'single' or 'batch'

    @Column(name = "\"ActionData\"", nullable = false, columnDefinition = "jsonb")
    private String actionData;  // JSON serialized intent data

    @Column(name = "\"CreatedAt\"", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "\"ExpiresAt\"", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "\"IsProcessed\"", nullable = false)
    private Boolean isProcessed;

    @Column(name = "\"ProcessedAt\"")
    private OffsetDateTime processedAt;

    @Column(name = "\"ProcessedResult\"", length = 20)
    private String processedResult;  // 'confirmed', 'cancelled', 'expired'

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (isProcessed == null) {
            isProcessed = false;
        }
    }

    /**
     * Checks if this confirmation has expired.
     */
    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(expiresAt);
    }

    /**
     * Marks the confirmation as processed with the given result.
     */
    public void markProcessed(String result) {
        this.isProcessed = true;
        this.processedAt = OffsetDateTime.now();
        this.processedResult = result;
    }
}
