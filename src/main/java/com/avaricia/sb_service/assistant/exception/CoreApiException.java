package com.avaricia.sb_service.assistant.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when communication with the Core API (.NET service) fails.
 * This can happen due to:
 * - Network connectivity issues
 * - Core service being unavailable
 * - Authentication failures (invalid API key)
 * - Invalid request data
 * - Core service internal errors
 */
public class CoreApiException extends AssistantException {

    private static final String DEFAULT_USER_MESSAGE = 
        "❌ No pude conectar con el servidor. Por favor, intenta más tarde.";
    private static final String ERROR_CODE = "CORE_API_ERROR";

    private final String endpoint;
    private final String httpMethod;
    private final HttpStatus httpStatus;

    public CoreApiException(String message, String endpoint, String httpMethod) {
        super(message, DEFAULT_USER_MESSAGE, ERROR_CODE);
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
        this.httpStatus = null;
    }

    public CoreApiException(String message, String endpoint, String httpMethod, HttpStatus httpStatus) {
        super(message, DEFAULT_USER_MESSAGE, ERROR_CODE);
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
        this.httpStatus = httpStatus;
    }

    public CoreApiException(String message, String userFriendlyMessage, String endpoint, String httpMethod) {
        super(message, userFriendlyMessage, ERROR_CODE);
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
        this.httpStatus = null;
    }

    public CoreApiException(String message, String endpoint, String httpMethod, Throwable cause) {
        super(message, DEFAULT_USER_MESSAGE, cause);
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
        this.httpStatus = null;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /**
     * Creates an exception for when the Core service is unreachable.
     */
    public static CoreApiException serviceUnavailable(String endpoint, String method, Throwable cause) {
        return new CoreApiException(
            "Core API service is unavailable: " + cause.getMessage(),
            "🔌 El servicio no está disponible en este momento. Por favor, intenta más tarde.",
            endpoint,
            method
        );
    }

    /**
     * Creates an exception for authentication failures.
     */
    public static CoreApiException authenticationFailed(String endpoint, String method) {
        return new CoreApiException(
            "Core API authentication failed - invalid API key",
            "🔐 Error de autenticación con el servidor. Contacta al soporte.",
            endpoint,
            method
        );
    }

    /**
     * Creates an exception for user not found errors.
     */
    public static CoreApiException userNotFound(String userId) {
        return new CoreApiException(
            "User not found in Core API: " + userId,
            "👤 No encontré tu cuenta. ¿Ya vinculaste tu Telegram con la aplicación?",
            "/api/User/" + userId,
            "GET"
        );
    }

    /**
     * Creates an exception for transaction creation failures.
     */
    public static CoreApiException transactionFailed(String userId, String error) {
        return new CoreApiException(
            "Failed to create transaction for user " + userId + ": " + error,
            "❌ No pude registrar la transacción. " + error,
            "/api/Transaction",
            "POST"
        );
    }

    /**
     * Creates an exception for rule creation failures.
     */
    public static CoreApiException ruleFailed(String userId, String error) {
        return new CoreApiException(
            "Failed to create/update rule for user " + userId + ": " + error,
            "❌ No pude crear la regla financiera. " + error,
            "/api/FinancialRule",
            "POST"
        );
    }

    /**
     * Creates an exception for invalid request data.
     */
    public static CoreApiException badRequest(String endpoint, String method, String details) {
        return new CoreApiException(
            "Bad request to Core API: " + details,
            "❌ Los datos enviados no son válidos. " + details,
            endpoint,
            method
        );
    }
}
