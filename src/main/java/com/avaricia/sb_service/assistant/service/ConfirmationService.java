package com.avaricia.sb_service.assistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.avaricia.sb_service.assistant.dto.IntentResult;
import com.avaricia.sb_service.assistant.dto.PendingAction;
import com.avaricia.sb_service.assistant.dto.PendingBatchAction;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing pending actions that require user confirmation.
 * Used for high-value transactions (> 3,000,000 COP) to prevent accidental entries.
 * 
 * Features:
 * - Store pending actions per Telegram user (single or batch)
 * - 60 second timeout for confirmations
 * - Automatic cleanup of expired actions
 * - Support for batch confirmations when multiple high-value transactions are sent together
 */
@Service
public class ConfirmationService {

    private static final Logger log = LoggerFactory.getLogger(ConfirmationService.class);

    /**
     * Threshold amount that requires confirmation (3,000,000 COP)
     */
    public static final double CONFIRMATION_THRESHOLD = 3_000_000.0;

    /**
     * Map of telegramId -> pending single action
     */
    private final Map<Long, PendingAction> pendingActions = new ConcurrentHashMap<>();

    /**
     * Map of telegramId -> pending batch action
     */
    private final Map<Long, PendingBatchAction> pendingBatchActions = new ConcurrentHashMap<>();

    /**
     * Checks if an amount requires confirmation.
     * 
     * @param amount The transaction amount
     * @return true if amount > CONFIRMATION_THRESHOLD
     */
    public boolean requiresConfirmation(Double amount) {
        return amount != null && amount > CONFIRMATION_THRESHOLD;
    }

    /**
     * Checks if a list of intents contains any high-value transaction.
     * 
     * @param intents List of intents to check
     * @return true if any intent has amount > CONFIRMATION_THRESHOLD
     */
    public boolean batchRequiresConfirmation(List<IntentResult> intents) {
        if (intents == null || intents.isEmpty()) {
            return false;
        }
        return intents.stream()
            .anyMatch(intent -> requiresConfirmation(intent.getAmount()));
    }

    // ==================== SINGLE ACTION METHODS ====================

    /**
     * Creates a pending action for confirmation.
     * 
     * @param actionType "create_expense" or "create_income"
     * @param intent The intent with transaction details
     * @param userId The system user ID
     * @param telegramId The Telegram user ID
     * @return The confirmation message to send to the user
     */
    public String createPendingAction(String actionType, IntentResult intent, String userId, Long telegramId) {
        log.info("⏳ Creating pending action for user {} (Telegram: {}): {} of ${}",
            userId, telegramId, actionType, String.format("%,.0f", intent.getAmount()));

        // Remove any existing pending actions for this user
        clearAllPendingActions(telegramId);

        // Create new pending action
        PendingAction pendingAction = new PendingAction(actionType, intent, userId, telegramId);
        pendingActions.put(telegramId, pendingAction);

        log.debug("📝 Pending action created: {}", pendingAction);

        return pendingAction.getConfirmationMessage();
    }

    /**
     * Checks if a user has a pending single action awaiting confirmation.
     * 
     * @param telegramId The Telegram user ID
     * @return true if user has a non-expired pending action
     */
    public boolean hasPendingAction(Long telegramId) {
        PendingAction action = pendingActions.get(telegramId);
        if (action == null) {
            return false;
        }

        if (action.isExpired()) {
            log.debug("⏰ Pending action expired for user: {}", telegramId);
            pendingActions.remove(telegramId);
            return false;
        }

        return true;
    }

    /**
     * Gets the pending action for a user.
     * 
     * @param telegramId The Telegram user ID
     * @return Optional containing the pending action
     */
    public Optional<PendingAction> getPendingAction(Long telegramId) {
        PendingAction action = pendingActions.get(telegramId);
        if (action == null || action.isExpired()) {
            if (action != null && action.isExpired()) {
                log.debug("⏰ Cleaning up expired pending action for user: {}", telegramId);
                pendingActions.remove(telegramId);
            }
            return Optional.empty();
        }
        return Optional.of(action);
    }

    /**
     * Confirms and removes the pending single action.
     * 
     * @param telegramId The Telegram user ID
     * @return The pending action if it existed and wasn't expired
     */
    public Optional<PendingAction> confirmAction(Long telegramId) {
        PendingAction action = pendingActions.remove(telegramId);

        if (action == null) {
            log.debug("❓ No pending action to confirm for user: {}", telegramId);
            return Optional.empty();
        }

        if (action.isExpired()) {
            log.info("⏰ Attempted to confirm expired action for user: {}", telegramId);
            return Optional.empty();
        }

        log.info("✅ Action confirmed for user {}: {} of ${}",
            telegramId, action.getActionType(), String.format("%,.0f", action.getIntent().getAmount()));

        return Optional.of(action);
    }

    // ==================== BATCH ACTION METHODS ====================

    /**
     * Creates a pending batch action for confirmation.
     * 
     * @param intents List of intents to include in the batch
     * @param userId The system user ID
     * @param telegramId The Telegram user ID
     * @return The confirmation message to send to the user
     */
    public String createPendingBatchAction(List<IntentResult> intents, String userId, Long telegramId) {
        log.info("⏳ Creating pending batch action for user {} (Telegram: {}): {} operations",
            userId, telegramId, intents.size());

        // Remove any existing pending actions for this user
        clearAllPendingActions(telegramId);

        // Create new pending batch action
        PendingBatchAction batchAction = new PendingBatchAction(userId, telegramId);
        
        for (IntentResult intent : intents) {
            String intentType = intent.getIntent();
            String type = "create_expense".equals(intentType) ? "Expense" : "Income";
            batchAction.addItem(type, intent);
        }

        pendingBatchActions.put(telegramId, batchAction);

        log.debug("📝 Pending batch action created: {}", batchAction);

        return batchAction.getConfirmationMessage();
    }

    /**
     * Checks if a user has a pending batch action awaiting confirmation.
     * 
     * @param telegramId The Telegram user ID
     * @return true if user has a non-expired pending batch action
     */
    public boolean hasPendingBatchAction(Long telegramId) {
        PendingBatchAction action = pendingBatchActions.get(telegramId);
        if (action == null) {
            return false;
        }

        if (action.isExpired()) {
            log.debug("⏰ Pending batch action expired for user: {}", telegramId);
            pendingBatchActions.remove(telegramId);
            return false;
        }

        return true;
    }

    /**
     * Gets the pending batch action for a user.
     * 
     * @param telegramId The Telegram user ID
     * @return Optional containing the pending batch action
     */
    public Optional<PendingBatchAction> getPendingBatchAction(Long telegramId) {
        PendingBatchAction action = pendingBatchActions.get(telegramId);
        if (action == null || action.isExpired()) {
            if (action != null && action.isExpired()) {
                log.debug("⏰ Cleaning up expired pending batch action for user: {}", telegramId);
                pendingBatchActions.remove(telegramId);
            }
            return Optional.empty();
        }
        return Optional.of(action);
    }

    /**
     * Confirms and removes the pending batch action.
     * 
     * @param telegramId The Telegram user ID
     * @return The pending batch action if it existed and wasn't expired
     */
    public Optional<PendingBatchAction> confirmBatchAction(Long telegramId) {
        PendingBatchAction action = pendingBatchActions.remove(telegramId);

        if (action == null) {
            log.debug("❓ No pending batch action to confirm for user: {}", telegramId);
            return Optional.empty();
        }

        if (action.isExpired()) {
            log.info("⏰ Attempted to confirm expired batch action for user: {}", telegramId);
            return Optional.empty();
        }

        log.info("✅ Batch action confirmed for user {}: {} operations, ${} total",
            telegramId, action.size(), 
            String.format("%,.0f", action.getTotalExpenses() + action.getTotalIncome()));

        return Optional.of(action);
    }

    // ==================== COMMON METHODS ====================

    /**
     * Checks if a user has ANY pending action (single or batch).
     * 
     * @param telegramId The Telegram user ID
     * @return true if user has any non-expired pending action
     */
    public boolean hasAnyPendingAction(Long telegramId) {
        return hasPendingAction(telegramId) || hasPendingBatchAction(telegramId);
    }

    /**
     * Cancels and removes any pending action (single or batch).
     * 
     * @param telegramId The Telegram user ID
     * @return true if an action was cancelled
     */
    public boolean cancelAction(Long telegramId) {
        boolean hadSingle = pendingActions.remove(telegramId) != null;
        boolean hadBatch = pendingBatchActions.remove(telegramId) != null;

        if (hadSingle || hadBatch) {
            log.info("❌ Action cancelled for user {}", telegramId);
            return true;
        }

        log.debug("❓ No pending action to cancel for user: {}", telegramId);
        return false;
    }

    /**
     * Clears all pending actions for a user.
     */
    private void clearAllPendingActions(Long telegramId) {
        PendingAction existingSingle = pendingActions.remove(telegramId);
        PendingBatchAction existingBatch = pendingBatchActions.remove(telegramId);
        
        if (existingSingle != null) {
            log.debug("🔄 Replaced existing pending action: {}", existingSingle);
        }
        if (existingBatch != null) {
            log.debug("🔄 Replaced existing pending batch action: {}", existingBatch);
        }
    }

    /**
     * Checks if a message is a confirmation response.
     */
    public boolean isConfirmationMessage(String message) {
        if (message == null) return false;
        String normalized = message.toLowerCase().trim();
        return normalized.equals("sí") ||
               normalized.equals("si") ||
               normalized.equals("confirmar") ||
               normalized.equals("confirmo") ||
               normalized.equals("yes") ||
               normalized.equals("ok") ||
               normalized.equals("dale") ||
               normalized.equals("hazlo") ||
               normalized.equals("adelante");
    }

    /**
     * Checks if a message is a cancellation response.
     */
    public boolean isCancellationMessage(String message) {
        if (message == null) return false;
        String normalized = message.toLowerCase().trim();
        return normalized.equals("no") ||
               normalized.equals("cancelar") ||
               normalized.equals("cancelo") ||
               normalized.equals("cancel") ||
               normalized.equals("anular") ||
               normalized.equals("olvídalo") ||
               normalized.equals("olvidalo") ||
               normalized.equals("mejor no");
    }

    /**
     * Gets the expired message for when a pending action times out.
     */
    public String getExpiredMessage() {
        return "⏰ La confirmación ha expirado. Si aún quieres registrar la(s) transacción(es), " +
               "por favor envía el mensaje nuevamente.";
    }

    /**
     * Gets the cancellation message.
     */
    public String getCancellationMessage() {
        return "❌ Operación cancelada. No se registró ninguna transacción.";
    }

    /**
     * Gets the message for when there's nothing to confirm.
     */
    public String getNothingToConfirmMessage() {
        return "🤔 No tienes ninguna operación pendiente de confirmar.";
    }

    /**
     * Cleans up all expired pending actions.
     */
    public void cleanupExpiredActions() {
        int removed = 0;
        
        for (Map.Entry<Long, PendingAction> entry : pendingActions.entrySet()) {
            if (entry.getValue().isExpired()) {
                pendingActions.remove(entry.getKey());
                removed++;
            }
        }
        
        for (Map.Entry<Long, PendingBatchAction> entry : pendingBatchActions.entrySet()) {
            if (entry.getValue().isExpired()) {
                pendingBatchActions.remove(entry.getKey());
                removed++;
            }
        }
        
        if (removed > 0) {
            log.debug("🧹 Cleaned up {} expired pending actions", removed);
        }
    }

    /**
     * Gets the count of pending actions (for monitoring).
     */
    public int getPendingCount() {
        return pendingActions.size() + pendingBatchActions.size();
    }
}
