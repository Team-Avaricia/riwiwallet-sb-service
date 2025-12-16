package com.avaricia.sb_service.assistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.avaricia.sb_service.assistant.dto.ConfirmationIntent;
import com.avaricia.sb_service.assistant.dto.IntentResult;
import com.avaricia.sb_service.assistant.dto.PendingAction;
import com.avaricia.sb_service.assistant.dto.PendingBatchAction;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Service for managing pending actions that require user confirmation.
 * Used for high-value transactions (> 3,000,000 COP) to prevent accidental entries.
 * 
 * Features:
 * - Store pending actions per Telegram user (single or batch)
 * - 60 second timeout for confirmations
 * - Automatic cleanup of expired actions
 * - Support for batch confirmations when multiple high-value transactions are sent together
 * - Hybrid confirmation detection: fast regex + AI fallback for ambiguous messages
 */
@Service
public class ConfirmationService {

    private final ChatClient chatClient;

    private static final Logger log = LoggerFactory.getLogger(ConfirmationService.class);

    /**
     * Threshold amount that requires confirmation (3,000,000 COP)
     */
    public static final double CONFIRMATION_THRESHOLD = 3_000_000.0;

    // ==================== CONFIRMATION KEYWORDS ====================
    
    /**
     * Exact words that indicate confirmation (case insensitive)
     */
    private static final Set<String> CONFIRMATION_WORDS = Set.of(
        "sí", "si", "confirmar", "confirmo", "yes", "ok", "dale", "hazlo", 
        "adelante", "listo", "va", "claro", "por supuesto", "afirmativo",
        "correcto", "exacto", "perfecto", "bueno", "bien", "procede"
    );

    /**
     * Patterns that indicate confirmation (compiled for performance)
     * Matches phrases like "si hazlo", "dale pues", "ok registralo", etc.
     */
    private static final Pattern CONFIRMATION_PATTERN = Pattern.compile(
        "^(s[ií]|dale|ok|claro|bueno)\\s+.{0,25}$|" +        // Starts with confirmation word + short phrase
        "quiero\\s+confirmar|" +                              // "quiero confirmar"
        "confirmo\\s+(la|el|esto|eso)|" +                     // "confirmo la/el/esto/eso"
        "reg[ií]stra(lo|me|r)|" +                             // "registralo", "registrame", "registrar"
        "hazlo\\s+(ya|pues|ahora)|" +                         // "hazlo ya/pues/ahora"
        "(est[aá]|todo)\\s+bien",                             // "está bien", "todo bien"
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    /**
     * Exact words that indicate cancellation (case insensitive)
     */
    private static final Set<String> CANCELLATION_WORDS = Set.of(
        "no", "cancelar", "cancelo", "cancel", "anular", "olvídalo", "olvidalo",
        "mejor no", "nope", "nel", "negativo", "para nada", "nunca"
    );

    /**
     * Patterns that indicate cancellation
     */
    private static final Pattern CANCELLATION_PATTERN = Pattern.compile(
        "^no\\s+.{0,20}$|" +                                  // Starts with "no" + short phrase
        "no\\s+(quiero|gracias|lo\\s+hagas)|" +               // "no quiero", "no gracias", "no lo hagas"
        "mejor\\s+no|" +                                      // "mejor no"
        "olv[ií]da(lo|te)|" +                                 // "olvidalo", "olvidate"
        "dej[aá]\\s*(lo|eso)|" +                              // "deja eso", "déjalo"
        "cancela\\s*(lo|eso|r)?",                             // "cancela", "cancelalo"
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    // ==================== STATE MANAGEMENT ====================

    /**
     * Map of telegramId -> pending single action
     */
    private final Map<Long, PendingAction> pendingActions = new ConcurrentHashMap<>();

    /**
     * Map of telegramId -> pending batch action
     */
    private final Map<Long, PendingBatchAction> pendingBatchActions = new ConcurrentHashMap<>();

    // ==================== CONSTRUCTOR ====================

    public ConfirmationService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

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
     * Uses compiled patterns and word sets for efficiency.
     * 
     * @param message The user's message
     * @return true if the message indicates confirmation
     */
    public boolean isConfirmationMessage(String message) {
        if (message == null || message.isBlank()) return false;
        
        String normalized = message.toLowerCase().trim();
        
        // Check exact word matches first (O(1) lookup)
        if (CONFIRMATION_WORDS.contains(normalized)) {
            return true;
        }
        
        // Check pattern matches for phrases
        return CONFIRMATION_PATTERN.matcher(normalized).find();
    }

    /**
     * Checks if a message is a cancellation response.
     * Uses compiled patterns and word sets for efficiency.
     * 
     * @param message The user's message
     * @return true if the message indicates cancellation
     */
    public boolean isCancellationMessage(String message) {
        if (message == null || message.isBlank()) return false;
        
        String normalized = message.toLowerCase().trim();
        
        // Check exact word matches first (O(1) lookup)
        if (CANCELLATION_WORDS.contains(normalized)) {
            return true;
        }
        
        // Check pattern matches for phrases
        return CANCELLATION_PATTERN.matcher(normalized).find();
    }

    // ==================== HYBRID AI CLASSIFICATION ====================

    /**
     * Classifies a message as confirmation, cancellation, or unclear using a hybrid approach.
     * 
     * Strategy:
     * 1. Fast path: Check exact word matches (O(1) with Set)
     * 2. Medium path: Check regex patterns (compiled for performance)
     * 3. Slow path: Use AI to classify ambiguous messages
     * 
     * This approach optimizes for speed in common cases while maintaining
     * flexibility for complex or ambiguous user responses.
     * 
     * @param message The user's message
     * @return ConfirmationIntent indicating CONFIRM, CANCEL, or UNCLEAR
     */
    public ConfirmationIntent classifyConfirmationIntent(String message) {
        if (message == null || message.isBlank()) {
            return ConfirmationIntent.UNCLEAR;
        }
        
        String normalized = message.toLowerCase().trim();
        
        // Fast path: exact word matches
        if (CONFIRMATION_WORDS.contains(normalized)) {
            log.debug("✅ Fast path confirmation: '{}'", message);
            return ConfirmationIntent.CONFIRM;
        }
        if (CANCELLATION_WORDS.contains(normalized)) {
            log.debug("❌ Fast path cancellation: '{}'", message);
            return ConfirmationIntent.CANCEL;
        }
        
        // Medium path: regex pattern matches
        if (CONFIRMATION_PATTERN.matcher(normalized).find()) {
            log.debug("✅ Pattern confirmation: '{}'", message);
            return ConfirmationIntent.CONFIRM;
        }
        if (CANCELLATION_PATTERN.matcher(normalized).find()) {
            log.debug("❌ Pattern cancellation: '{}'", message);
            return ConfirmationIntent.CANCEL;
        }
        
        // Slow path: AI classification for ambiguous messages
        log.debug("🤖 Using AI to classify ambiguous message: '{}'", message);
        return classifyWithAI(message);
    }

    /**
     * Uses OpenAI to classify an ambiguous message as confirmation or cancellation.
     * Only called when fast/medium paths fail to match.
     * 
     * @param message The ambiguous message to classify
     * @return ConfirmationIntent based on AI analysis
     */
    private ConfirmationIntent classifyWithAI(String message) {
        try {
            String prompt = """
                Analiza el siguiente mensaje y determina si el usuario está:
                1. CONFIRMANDO una acción (quiere proceder)
                2. CANCELANDO una acción (no quiere proceder)
                3. UNCLEAR - el mensaje no es una respuesta de confirmación/cancelación
                
                El usuario tiene una transacción financiera pendiente y se le preguntó si quiere confirmarla.
                
                Responde SOLO con una de estas tres palabras: CONFIRM, CANCEL, o UNCLEAR
                
                Mensaje del usuario: "%s"
                
                Tu respuesta (solo una palabra):
                """.formatted(message);
            
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            
            if (response == null || response.isBlank()) {
                log.warn("⚠️ AI returned empty response for message: '{}'", message);
                return ConfirmationIntent.UNCLEAR;
            }
            
            String aiDecision = response.trim().toUpperCase();
            log.debug("🤖 AI classified '{}' as: {}", message, aiDecision);
            
            return switch (aiDecision) {
                case "CONFIRM" -> ConfirmationIntent.CONFIRM;
                case "CANCEL" -> ConfirmationIntent.CANCEL;
                default -> ConfirmationIntent.UNCLEAR;
            };
            
        } catch (Exception e) {
            log.error("❌ Error classifying message with AI: {}", e.getMessage());
            // Fallback to UNCLEAR if AI fails
            return ConfirmationIntent.UNCLEAR;
        }
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
