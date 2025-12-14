package com.avaricia.sb_service.assistant.exception;

/**
 * Exception thrown when intent classification fails.
 * This can happen due to:
 * - OpenAI API errors or rate limiting
 * - Invalid or unrecognizable user input
 * - JSON parsing errors from the AI response
 * - Network connectivity issues
 */
public class IntentClassificationException extends AssistantException {

    private static final String DEFAULT_USER_MESSAGE = 
        "🤔 No pude entender tu mensaje. ¿Podrías reformularlo?";
    private static final String ERROR_CODE = "INTENT_CLASSIFICATION_ERROR";

    private final String originalMessage;

    public IntentClassificationException(String message, String originalUserMessage) {
        super(message, DEFAULT_USER_MESSAGE, ERROR_CODE);
        this.originalMessage = originalUserMessage;
    }

    public IntentClassificationException(String message, String userFriendlyMessage, String originalUserMessage) {
        super(message, userFriendlyMessage, ERROR_CODE);
        this.originalMessage = originalUserMessage;
    }

    public IntentClassificationException(String message, String originalUserMessage, Throwable cause) {
        super(message, DEFAULT_USER_MESSAGE, cause);
        this.originalMessage = originalUserMessage;
    }

    /**
     * Returns the original user message that caused the classification failure.
     */
    public String getOriginalMessage() {
        return originalMessage;
    }

    /**
     * Creates an exception for when the OpenAI API call fails.
     */
    public static IntentClassificationException openAiFailed(String userMessage, Throwable cause) {
        return new IntentClassificationException(
            "OpenAI API call failed: " + cause.getMessage(),
            "🤖 El servicio de IA no está disponible temporalmente. Por favor, intenta en unos momentos.",
            userMessage
        );
    }

    /**
     * Creates an exception for when the AI response cannot be parsed.
     */
    public static IntentClassificationException invalidAiResponse(String userMessage, String aiResponse) {
        return new IntentClassificationException(
            "Failed to parse AI response: " + aiResponse,
            "🤔 Tuve problemas procesando tu solicitud. ¿Podrías intentar de otra forma?",
            userMessage
        );
    }

    /**
     * Creates an exception for when the user input is empty or invalid.
     */
    public static IntentClassificationException emptyInput() {
        return new IntentClassificationException(
            "Empty or null user input received",
            "📝 Por favor, escribe o dicta un mensaje para que pueda ayudarte.",
            ""
        );
    }

    /**
     * Creates an exception for rate limiting.
     */
    public static IntentClassificationException rateLimited(String userMessage) {
        return new IntentClassificationException(
            "OpenAI API rate limit exceeded",
            "⏳ Has enviado muchos mensajes. Por favor, espera un momento antes de continuar.",
            userMessage
        );
    }
}
