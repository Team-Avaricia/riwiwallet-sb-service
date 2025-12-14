package com.avaricia.sb_service.assistant.exception;

/**
 * Exception thrown when there's an error with database operations
 * in the assistant module (conversation history, confirmations, etc.)
 */
public class PersistenceException extends AssistantException {

    private final String operation;
    private final String entity;

    public PersistenceException(String message, String operation, String entity) {
        super(
            message,
            "❌ Error al guardar los datos. Tu mensaje fue procesado pero no se guardó el historial.",
            "PERSISTENCE_ERROR"
        );
        this.operation = operation;
        this.entity = entity;
    }

    public PersistenceException(String message, String operation, String entity, Throwable cause) {
        super(
            message,
            "❌ Error al guardar los datos. Tu mensaje fue procesado pero no se guardó el historial.",
            "PERSISTENCE_ERROR",
            cause
        );
        this.operation = operation;
        this.entity = entity;
    }

    public String getOperation() {
        return operation;
    }

    public String getEntity() {
        return entity;
    }

    // Factory methods for common scenarios

    public static PersistenceException saveMessageFailed(Long telegramId, Throwable cause) {
        return new PersistenceException(
            String.format("Failed to save conversation message for Telegram ID: %d", telegramId),
            "SAVE",
            "ConversationMessage",
            cause
        );
    }

    public static PersistenceException loadHistoryFailed(Long telegramId, Throwable cause) {
        return new PersistenceException(
            String.format("Failed to load conversation history for Telegram ID: %d", telegramId),
            "LOAD",
            "ConversationMessage",
            cause
        );
    }

    public static PersistenceException saveConfirmationFailed(Long telegramId, Throwable cause) {
        return new PersistenceException(
            String.format("Failed to save pending confirmation for Telegram ID: %d", telegramId),
            "SAVE",
            "PendingConfirmation",
            cause
        );
    }

    public static PersistenceException loadConfirmationFailed(Long telegramId, Throwable cause) {
        return new PersistenceException(
            String.format("Failed to load pending confirmation for Telegram ID: %d", telegramId),
            "LOAD",
            "PendingConfirmation",
            cause
        );
    }
}
