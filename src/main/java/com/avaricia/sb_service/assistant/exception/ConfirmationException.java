package com.avaricia.sb_service.assistant.exception;

/**
 * Exception thrown when there's an error with confirmation operations
 * (expired, invalid state, etc.)
 */
public class ConfirmationException extends AssistantException {

    private final Long telegramId;
    private final String confirmationState;

    public ConfirmationException(String message, String userFriendlyMessage, Long telegramId, String state) {
        super(message, userFriendlyMessage, "CONFIRMATION_ERROR");
        this.telegramId = telegramId;
        this.confirmationState = state;
    }

    public ConfirmationException(String message, String userFriendlyMessage, Long telegramId, String state, Throwable cause) {
        super(message, userFriendlyMessage, "CONFIRMATION_ERROR", cause);
        this.telegramId = telegramId;
        this.confirmationState = state;
    }

    public Long getTelegramId() {
        return telegramId;
    }

    public String getConfirmationState() {
        return confirmationState;
    }

    // Factory methods for common scenarios

    public static ConfirmationException expired(Long telegramId) {
        return new ConfirmationException(
            String.format("Confirmation expired for Telegram ID: %d", telegramId),
            "⏰ La confirmación ha expirado. Si aún quieres registrar la transacción, envía el mensaje nuevamente.",
            telegramId,
            "EXPIRED"
        );
    }

    public static ConfirmationException notFound(Long telegramId) {
        return new ConfirmationException(
            String.format("No pending confirmation found for Telegram ID: %d", telegramId),
            "🤔 No tienes ninguna operación pendiente de confirmar.",
            telegramId,
            "NOT_FOUND"
        );
    }

    public static ConfirmationException alreadyProcessed(Long telegramId, String result) {
        return new ConfirmationException(
            String.format("Confirmation already processed for Telegram ID: %d (result: %s)", telegramId, result),
            "ℹ️ Esta operación ya fue procesada anteriormente.",
            telegramId,
            "ALREADY_PROCESSED"
        );
    }

    public static ConfirmationException invalidData(Long telegramId, Throwable cause) {
        return new ConfirmationException(
            String.format("Invalid confirmation data for Telegram ID: %d", telegramId),
            "❌ Error al procesar la confirmación. Intenta registrar la transacción nuevamente.",
            telegramId,
            "INVALID_DATA",
            cause
        );
    }
}
