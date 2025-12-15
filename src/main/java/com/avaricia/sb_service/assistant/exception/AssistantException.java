package com.avaricia.sb_service.assistant.exception;

/**
 * Base exception class for all Assistant module exceptions.
 * Provides common functionality for custom exceptions.
 */
public class AssistantException extends RuntimeException {

    private final String userFriendlyMessage;
    private final String errorCode;

    public AssistantException(String message) {
        super(message);
        this.userFriendlyMessage = "❌ Ocurrió un error procesando tu solicitud. Por favor, intenta de nuevo.";
        this.errorCode = "ASSISTANT_ERROR";
    }

    public AssistantException(String message, String userFriendlyMessage) {
        super(message);
        this.userFriendlyMessage = userFriendlyMessage;
        this.errorCode = "ASSISTANT_ERROR";
    }

    public AssistantException(String message, String userFriendlyMessage, String errorCode) {
        super(message);
        this.userFriendlyMessage = userFriendlyMessage;
        this.errorCode = errorCode;
    }

    public AssistantException(String message, Throwable cause) {
        super(message, cause);
        this.userFriendlyMessage = "❌ Ocurrió un error procesando tu solicitud. Por favor, intenta de nuevo.";
        this.errorCode = "ASSISTANT_ERROR";
    }

    public AssistantException(String message, String userFriendlyMessage, Throwable cause) {
        super(message, cause);
        this.userFriendlyMessage = userFriendlyMessage;
        this.errorCode = "ASSISTANT_ERROR";
    }

    public AssistantException(String message, String userFriendlyMessage, String errorCode, Throwable cause) {
        super(message, cause);
        this.userFriendlyMessage = userFriendlyMessage;
        this.errorCode = errorCode;
    }

    /**
     * Returns a user-friendly message suitable for displaying to end users.
     * This message should be localized and not contain technical details.
     */
    public String getUserFriendlyMessage() {
        return userFriendlyMessage;
    }

    /**
     * Returns an error code for categorizing and tracking errors.
     */
    public String getErrorCode() {
        return errorCode;
    }
}
