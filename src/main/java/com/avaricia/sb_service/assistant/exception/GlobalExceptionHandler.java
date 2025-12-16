package com.avaricia.sb_service.assistant.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the Assistant module.
 * Provides centralized exception handling with proper logging and user-friendly responses.
 */
@ControllerAdvice(basePackages = "com.avaricia.sb_service.assistant")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles all AssistantException and its subclasses.
     */
    @ExceptionHandler(AssistantException.class)
    public ResponseEntity<Map<String, Object>> handleAssistantException(AssistantException ex) {
        log.error("🔴 AssistantException [{}]: {}", ex.getErrorCode(), ex.getMessage(), ex);
        
        Map<String, Object> response = buildErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            ex.getUserFriendlyMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handles TranscriptionException specifically for audio-related errors.
     */
    @ExceptionHandler(TranscriptionException.class)
    public ResponseEntity<Map<String, Object>> handleTranscriptionException(TranscriptionException ex) {
        log.error("🎤 TranscriptionException: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = buildErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            ex.getUserFriendlyMessage(),
            HttpStatus.BAD_REQUEST
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles IntentClassificationException for AI-related errors.
     */
    @ExceptionHandler(IntentClassificationException.class)
    public ResponseEntity<Map<String, Object>> handleIntentClassificationException(IntentClassificationException ex) {
        log.error("🤖 IntentClassificationException: {} | Original message: '{}'", 
            ex.getMessage(), ex.getOriginalMessage(), ex);
        
        Map<String, Object> response = buildErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            ex.getUserFriendlyMessage(),
            HttpStatus.UNPROCESSABLE_ENTITY
        );
        response.put("originalMessage", ex.getOriginalMessage());
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    /**
     * Handles CoreApiException for backend service communication errors.
     */
    @ExceptionHandler(CoreApiException.class)
    public ResponseEntity<Map<String, Object>> handleCoreApiException(CoreApiException ex) {
        log.error("🔌 CoreApiException [{} {}]: {}", 
            ex.getHttpMethod(), ex.getEndpoint(), ex.getMessage(), ex);
        
        HttpStatus status = ex.getHttpStatus() != null ? ex.getHttpStatus() : HttpStatus.BAD_GATEWAY;
        
        Map<String, Object> response = buildErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            ex.getUserFriendlyMessage(),
            status
        );
        response.put("endpoint", ex.getEndpoint());
        response.put("method", ex.getHttpMethod());
        
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Handles UserNotLinkedException for unlinked Telegram users.
     */
    @ExceptionHandler(UserNotLinkedException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotLinkedException(UserNotLinkedException ex) {
        log.warn("👤 UserNotLinkedException: Telegram ID {} not linked", ex.getTelegramId());
        
        Map<String, Object> response = buildErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            ex.getUserFriendlyMessage(),
            HttpStatus.UNAUTHORIZED
        );
        response.put("telegramId", ex.getTelegramId());
        response.put("requiresLinking", true);
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handles PersistenceException for database-related errors.
     */
    @ExceptionHandler(PersistenceException.class)
    public ResponseEntity<Map<String, Object>> handlePersistenceException(PersistenceException ex) {
        log.error("💾 PersistenceException [{} {}]: {}", 
            ex.getOperation(), ex.getEntity(), ex.getMessage(), ex);
        
        Map<String, Object> response = buildErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            ex.getUserFriendlyMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR
        );
        response.put("operation", ex.getOperation());
        response.put("entity", ex.getEntity());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handles ConfirmationException for confirmation-related errors.
     */
    @ExceptionHandler(ConfirmationException.class)
    public ResponseEntity<Map<String, Object>> handleConfirmationException(ConfirmationException ex) {
        log.warn("⚠️ ConfirmationException [{}]: {} for Telegram ID: {}", 
            ex.getConfirmationState(), ex.getMessage(), ex.getTelegramId());
        
        HttpStatus status = switch (ex.getConfirmationState()) {
            case "EXPIRED" -> HttpStatus.GONE;
            case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "ALREADY_PROCESSED" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        
        Map<String, Object> response = buildErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            ex.getUserFriendlyMessage(),
            status
        );
        response.put("telegramId", ex.getTelegramId());
        response.put("confirmationState", ex.getConfirmationState());
        
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Handles HTTP client errors (4xx from external services).
     */
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<Map<String, Object>> handleHttpClientError(HttpClientErrorException ex) {
        log.error("📡 HTTP Client Error: {} - {}", ex.getStatusCode(), ex.getMessage());
        
        String userMessage = switch (ex.getStatusCode().value()) {
            case 400 -> "❌ Los datos enviados no son válidos.";
            case 401 -> "🔐 Error de autenticación.";
            case 403 -> "🚫 No tienes permiso para realizar esta acción.";
            case 404 -> "🔍 No encontré lo que buscabas.";
            case 429 -> "⏳ Demasiadas solicitudes. Espera un momento.";
            default -> "❌ Error al procesar tu solicitud.";
        };
        
        Map<String, Object> response = buildErrorResponse(
            "HTTP_CLIENT_ERROR",
            ex.getMessage(),
            userMessage,
            HttpStatus.valueOf(ex.getStatusCode().value())
        );
        
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    /**
     * Handles HTTP server errors (5xx from external services).
     */
    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<Map<String, Object>> handleHttpServerError(HttpServerErrorException ex) {
        log.error("🔥 HTTP Server Error: {} - {}", ex.getStatusCode(), ex.getMessage());
        
        Map<String, Object> response = buildErrorResponse(
            "HTTP_SERVER_ERROR",
            ex.getMessage(),
            "🔧 El servidor está experimentando problemas. Intenta más tarde.",
            HttpStatus.BAD_GATEWAY
        );
        
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

    /**
     * Handles connection/network errors.
     */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handleResourceAccessException(ResourceAccessException ex) {
        log.error("🌐 Network Error: {}", ex.getMessage());
        
        Map<String, Object> response = buildErrorResponse(
            "NETWORK_ERROR",
            ex.getMessage(),
            "🌐 Error de conexión. Verifica tu conexión a internet e intenta de nuevo.",
            HttpStatus.SERVICE_UNAVAILABLE
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handles all other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("💥 Unexpected Exception: {}", ex.getMessage(), ex);
        
        Map<String, Object> response = buildErrorResponse(
            "UNEXPECTED_ERROR",
            ex.getMessage(),
            "❌ Ocurrió un error inesperado. Por favor, intenta de nuevo.",
            HttpStatus.INTERNAL_SERVER_ERROR
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Builds a standardized error response map.
     */
    private Map<String, Object> buildErrorResponse(String errorCode, String technicalMessage, 
                                                    String userMessage, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", errorCode);
        response.put("message", userMessage);
        response.put("technicalDetails", technicalMessage);
        response.put("status", status.value());
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }
}
