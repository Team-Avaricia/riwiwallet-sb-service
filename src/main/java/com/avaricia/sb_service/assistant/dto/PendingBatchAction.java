package com.avaricia.sb_service.assistant.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a batch of pending actions that requires user confirmation.
 * Used when multiple transactions are sent in a single message and at least
 * one exceeds the confirmation threshold.
 */
public class PendingBatchAction {

    private final List<BatchItem> items;      // List of transactions to confirm
    private final String userId;               // System user ID
    private final Long telegramId;             // Telegram user ID
    private final LocalDateTime createdAt;     // When the pending action was created
    private final LocalDateTime expiresAt;     // When it expires (60 seconds)

    public PendingBatchAction(String userId, Long telegramId) {
        this.items = new ArrayList<>();
        this.userId = userId;
        this.telegramId = telegramId;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = createdAt.plusSeconds(60);
    }

    /**
     * Adds a transaction to the batch.
     */
    public void addItem(String type, IntentResult intent) {
        items.add(new BatchItem(type, intent));
    }

    public List<BatchItem> getItems() {
        return items;
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
     * Returns the total number of items in the batch.
     */
    public int size() {
        return items.size();
    }

    /**
     * Calculates total expenses in the batch.
     */
    public double getTotalExpenses() {
        return items.stream()
            .filter(item -> "Expense".equals(item.getType()))
            .mapToDouble(item -> item.getIntent().getAmount() != null ? item.getIntent().getAmount() : 0)
            .sum();
    }

    /**
     * Calculates total income in the batch.
     */
    public double getTotalIncome() {
        return items.stream()
            .filter(item -> "Income".equals(item.getType()))
            .mapToDouble(item -> item.getIntent().getAmount() != null ? item.getIntent().getAmount() : 0)
            .sum();
    }

    /**
     * Returns a formatted string showing all transactions for confirmation.
     */
    public String getConfirmationMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ *Confirmación requerida*\n\n");
        sb.append("Vas a registrar *").append(items.size()).append(" operaciones*");
        sb.append(", incluyendo transacciones de alto valor:\n\n");

        int num = 1;
        for (BatchItem item : items) {
            String emoji = "Expense".equals(item.getType()) ? "💸" : "💰";
            String typeText = "Expense".equals(item.getType()) ? "Gasto" : "Ingreso";
            Double amount = item.getIntent().getAmount();
            String description = item.getIntent().getDescription() != null 
                ? item.getIntent().getDescription() 
                : item.getIntent().getCategory();
            
            sb.append(String.format("%d. %s %s de *$%,.0f*", num++, emoji, typeText, amount));
            if (description != null && !description.isEmpty()) {
                sb.append(" - ").append(description);
            }
            sb.append("\n");
        }

        sb.append("\n📊 *Totales:*\n");
        double totalExpenses = getTotalExpenses();
        double totalIncome = getTotalIncome();
        if (totalExpenses > 0) {
            sb.append(String.format("• 💸 Gastos: $%,.0f\n", totalExpenses));
        }
        if (totalIncome > 0) {
            sb.append(String.format("• 💰 Ingresos: $%,.0f\n", totalIncome));
        }

        sb.append("\n¿Confirmas *todas* estas operaciones?\n\n");
        sb.append("Responde *Sí* para registrar todas\n");
        sb.append("Responde *No* para cancelar todas\n\n");
        sb.append("⏱️ _Esta confirmación expira en 60 segundos_");

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("PendingBatchAction{items=%d, user=%s, expires=%s}",
            items.size(), userId, expiresAt);
    }

    /**
     * Represents a single item in the batch.
     */
    public static class BatchItem {
        private final String type;        // "Expense" or "Income"
        private final IntentResult intent;

        public BatchItem(String type, IntentResult intent) {
            this.type = type;
            this.intent = intent;
        }

        public String getType() {
            return type;
        }

        public IntentResult getIntent() {
            return intent;
        }
    }
}
