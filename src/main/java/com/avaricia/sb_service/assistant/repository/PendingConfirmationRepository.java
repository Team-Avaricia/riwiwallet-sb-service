package com.avaricia.sb_service.assistant.repository;

import com.avaricia.sb_service.assistant.entity.PendingConfirmationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for pending confirmation persistence.
 */
@Repository
public interface PendingConfirmationRepository extends JpaRepository<PendingConfirmationEntity, UUID> {

    /**
     * Find the active (non-processed, non-expired) pending confirmation for a user.
     */
    @Query("""
        SELECT p FROM PendingConfirmationEntity p 
        WHERE p.telegramId = :telegramId 
          AND p.isProcessed = false 
          AND p.expiresAt > :now
        ORDER BY p.createdAt DESC
        """)
    Optional<PendingConfirmationEntity> findActivePendingConfirmation(
        @Param("telegramId") Long telegramId,
        @Param("now") OffsetDateTime now
    );

    /**
     * Find all pending confirmations for a user.
     */
    List<PendingConfirmationEntity> findByTelegramIdOrderByCreatedAtDesc(Long telegramId);

    /**
     * Check if user has an active pending confirmation.
     */
    @Query("""
        SELECT COUNT(p) > 0 FROM PendingConfirmationEntity p 
        WHERE p.telegramId = :telegramId 
          AND p.isProcessed = false 
          AND p.expiresAt > :now
        """)
    boolean hasActivePendingConfirmation(
        @Param("telegramId") Long telegramId,
        @Param("now") OffsetDateTime now
    );

    /**
     * Mark all pending confirmations for a user as cancelled.
     */
    @Modifying
    @Query("""
        UPDATE PendingConfirmationEntity p 
        SET p.isProcessed = true, 
            p.processedAt = :now, 
            p.processedResult = 'cancelled'
        WHERE p.telegramId = :telegramId 
          AND p.isProcessed = false
        """)
    int cancelPendingConfirmations(
        @Param("telegramId") Long telegramId,
        @Param("now") OffsetDateTime now
    );

    /**
     * Mark expired confirmations as expired.
     */
    @Modifying
    @Query("""
        UPDATE PendingConfirmationEntity p 
        SET p.isProcessed = true, 
            p.processedAt = :now, 
            p.processedResult = 'expired'
        WHERE p.isProcessed = false 
          AND p.expiresAt < :now
        """)
    int markExpiredConfirmations(@Param("now") OffsetDateTime now);

    /**
     * Delete old processed confirmations.
     */
    @Modifying
    @Query("DELETE FROM PendingConfirmationEntity p WHERE p.processedAt < :before")
    int deleteProcessedBefore(@Param("before") OffsetDateTime before);
}
