package com.avaricia.sb_service.assistant.dto;

import com.avaricia.sb_service.assistant.dto.IntentResult;

import java.time.LocalDateTime;

/**
 * Represents a pending action that requires user confirmation.
 * Used for high-value transactions (> 1,000,000) to prevent accidental entries.
 */
public class PendingAction {

    private final String actionType;      // "create_expense" or "create_income"
    private final IntentResult intent;     // Original intent with all details
    private final String userId;           // System user ID
    private final Long telegramId;         // Telegram user ID
    private final LocalDateTime createdAt; // When the pending action was created
    private final LocalDateTime expiresAt; // When it expires (60 seconds)

    public PendingAction(String actionType, IntentResult intent, String userId, Long telegramId) {
        this.actionType = actionType;
        this.intent = intent;
        this.userId = userId;
        this.telegramId = telegramId;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = createdAt.plusSeconds(60);
    }

    public String getActionType() {
        return actionType;
    }

    public IntentResult getIntent() {
        return intent;
    }

    public String getUserId() {
        return userId;
    }

    public Long getTelegramId() {
        return telegramId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    /**
     * Checks if this pending action has expired.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Returns a formatted string showing transaction details for confirmation.
     */
    public String getConfirmationMessage() {
        String emoji = "create_expense".equals(actionType) ? "💸" : "💰";
        String typeText = "create_expense".equals(actionType) ? "gasto" : "ingreso";
        Double amount = intent.getAmount();
        String category = intent.getCategory() != null ? intent.getCategory() : "General";
        String description = intent.getDescription() != null ? intent.getDescription() : category;

        return String.format(
            "⚠️ *Confirmación requerida*\n\n" +
            "Vas a registrar un %s de alto valor:\n\n" +
            "%s *$%,.0f*\n" +
            "• Categoría: %s\n" +
            "• Descripción: %s\n\n" +
            "¿Confirmas esta operación?\n\n" +
            "Responde *Sí* o *Confirmar* para continuar\n" +
            "Responde *No* o *Cancelar* para anular\n\n" +
            "⏱️ _Esta confirmación expira en 60 segundos_",
            typeText, emoji, amount, category, description
        );
    }

    @Override
    public String toString() {
        return String.format("PendingAction{type=%s, amount=%,.0f, user=%s, expires=%s}",
            actionType, intent.getAmount(), userId, expiresAt);
    }
}
