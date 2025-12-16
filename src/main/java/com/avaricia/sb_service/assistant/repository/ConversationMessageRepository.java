package com.avaricia.sb_service.assistant.repository;

import com.avaricia.sb_service.assistant.entity.ConversationMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for conversation message persistence.
 */
@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessageEntity, UUID> {

    /**
     * Find the most recent messages for a Telegram user, ordered by creation date.
     * 
     * @param telegramId The Telegram user ID
     * @param limit Maximum number of messages to return
     * @return List of messages ordered by creation date (oldest first for context)
     */
    @Query(value = """
        SELECT * FROM "ConversationMessages" 
        WHERE "TelegramId" = :telegramId 
        ORDER BY "CreatedAt" DESC 
        LIMIT :limit
        """, nativeQuery = true)
    List<ConversationMessageEntity> findRecentByTelegramId(
        @Param("telegramId") Long telegramId, 
        @Param("limit") int limit
    );

    /**
     * Find all messages for a Telegram user ordered by creation date.
     */
    List<ConversationMessageEntity> findByTelegramIdOrderByCreatedAtAsc(Long telegramId);

    /**
     * Find messages created after a certain time.
     */
    List<ConversationMessageEntity> findByTelegramIdAndCreatedAtAfterOrderByCreatedAtAsc(
        Long telegramId, 
        OffsetDateTime after
    );

    /**
     * Delete all messages for a Telegram user.
     */
    @Modifying
    @Query("DELETE FROM ConversationMessageEntity m WHERE m.telegramId = :telegramId")
    void deleteByTelegramId(@Param("telegramId") Long telegramId);

    /**
     * Delete messages older than a certain time.
     */
    @Modifying
    @Query("DELETE FROM ConversationMessageEntity m WHERE m.createdAt < :before")
    int deleteByCreatedAtBefore(@Param("before") OffsetDateTime before);

    /**
     * Count messages for a Telegram user.
     */
    long countByTelegramId(Long telegramId);

    /**
     * Delete oldest messages when count exceeds limit.
     */
    @Modifying
    @Query(value = """
        DELETE FROM "ConversationMessages" 
        WHERE "Id" IN (
            SELECT "Id" FROM "ConversationMessages" 
            WHERE "TelegramId" = :telegramId 
            ORDER BY "CreatedAt" ASC 
            LIMIT :count
        )
        """, nativeQuery = true)
    void deleteOldestMessages(@Param("telegramId") Long telegramId, @Param("count") int count);
}
