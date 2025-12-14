package com.avaricia.sb_service.assistant.exception;

/**
 * Exception thrown when audio transcription fails.
 * This can happen due to:
 * - Invalid audio file format
 * - Telegram API errors when downloading the file
 * - OpenAI Whisper API errors
 * - Network connectivity issues
 */
public class TranscriptionException extends AssistantException {

    private static final String DEFAULT_USER_MESSAGE = 
        "🎤 No pude transcribir el audio. Por favor, envía un mensaje de texto o intenta con otro audio.";
    private static final String ERROR_CODE = "TRANSCRIPTION_ERROR";

    public TranscriptionException(String message) {
        super(message, DEFAULT_USER_MESSAGE, ERROR_CODE);
    }

    public TranscriptionException(String message, String userFriendlyMessage) {
        super(message, userFriendlyMessage, ERROR_CODE);
    }

    public TranscriptionException(String message, Throwable cause) {
        super(message, DEFAULT_USER_MESSAGE, cause);
    }

    public TranscriptionException(String message, String userFriendlyMessage, Throwable cause) {
        super(message, userFriendlyMessage, cause);
    }

    /**
     * Creates an exception for when the Telegram file download fails.
     */
    public static TranscriptionException telegramDownloadFailed(String fileId, Throwable cause) {
        return new TranscriptionException(
            "Failed to download audio file from Telegram. FileId: " + fileId,
            "🎤 No pude descargar el audio de Telegram. Por favor, intenta enviarlo de nuevo.",
            cause
        );
    }

    /**
     * Creates an exception for when the Whisper API call fails.
     */
    public static TranscriptionException whisperApiFailed(Throwable cause) {
        return new TranscriptionException(
            "Whisper API transcription failed: " + cause.getMessage(),
            "🎤 El servicio de transcripción no está disponible. Por favor, envía un mensaje de texto.",
            cause
        );
    }

    /**
     * Creates an exception for invalid audio format.
     */
    public static TranscriptionException invalidAudioFormat(String format) {
        return new TranscriptionException(
            "Invalid audio format: " + format,
            "🎤 El formato de audio no es compatible. Por favor, envía una nota de voz o un archivo de audio común."
        );
    }
}
