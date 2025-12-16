package com.avaricia.sb_service.assistant.exception;

/**
 * Exception thrown when a user is not linked to the system.
 * This happens when a Telegram user tries to use the bot
 * without having linked their account first.
 */
public class UserNotLinkedException extends AssistantException {

    private static final String DEFAULT_USER_MESSAGE = 
        "👋 ¡Hola! Parece que no has vinculado tu cuenta aún.\n\n" +
        "Para usar el bot, primero debes:\n" +
        "1️⃣ Iniciar sesión en la aplicación web\n" +
        "2️⃣ Ir a Configuración → Vincular Telegram\n" +
        "3️⃣ Copiar el código y enviármelo aquí\n\n" +
        "¿Necesitas ayuda? Escribe /ayuda";
    private static final String ERROR_CODE = "USER_NOT_LINKED";

    private final Long telegramId;

    public UserNotLinkedException(Long telegramId) {
        super("User with Telegram ID " + telegramId + " is not linked to any account", DEFAULT_USER_MESSAGE, ERROR_CODE);
        this.telegramId = telegramId;
    }

    public UserNotLinkedException(Long telegramId, String customMessage) {
        super("User with Telegram ID " + telegramId + " is not linked to any account", customMessage, ERROR_CODE);
        this.telegramId = telegramId;
    }

    public Long getTelegramId() {
        return telegramId;
    }
}
