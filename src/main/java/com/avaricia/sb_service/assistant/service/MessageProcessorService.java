package com.avaricia.sb_service.assistant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.avaricia.sb_service.assistant.dto.IntentResult;

import java.util.List;
import java.util.Map;

/**
 * Service that processes user messages and coordinates actions.
 * Acts as the main orchestrator between intent classification and specialized handlers.
 * 
 * REFACTORED: Now delegates to specialized services:
 * - TransactionHandlerService: Handles all transaction operations
 * - RuleHandlerService: Handles financial rules (limits/budgets)
 * - QueryHandlerService: Handles balance and summary queries
 * - ResponseFormatterService: Handles response formatting utilities
 */
@Service
public class MessageProcessorService {

    private final IntentClassifierService intentClassifier;
    private final UserMappingService userMapping;
    private final ConversationHistoryService conversationHistory;
    
    // Specialized handlers
    private final TransactionHandlerService transactionHandler;
    private final RuleHandlerService ruleHandler;
    private final QueryHandlerService queryHandler;
    private final ResponseFormatterService formatter;
    
    // API services (needed for validate_expense which is complex)
    private final CoreApiService coreApi;
    private final MockCoreApiService mockCoreApi;
    private final boolean useMock;

    public MessageProcessorService(
            IntentClassifierService intentClassifier,
            UserMappingService userMapping,
            ConversationHistoryService conversationHistory,
            TransactionHandlerService transactionHandler,
            RuleHandlerService ruleHandler,
            QueryHandlerService queryHandler,
            ResponseFormatterService formatter,
            CoreApiService coreApi,
            MockCoreApiService mockCoreApi,
            @Value("${ms.core.use-mock:false}") boolean useMock) {
        this.intentClassifier = intentClassifier;
        this.userMapping = userMapping;
        this.conversationHistory = conversationHistory;
        this.transactionHandler = transactionHandler;
        this.ruleHandler = ruleHandler;
        this.queryHandler = queryHandler;
        this.formatter = formatter;
        this.coreApi = coreApi;
        this.mockCoreApi = mockCoreApi;
        this.useMock = useMock;
        
        if (useMock) {
            System.out.println("⚠️ MOCK MODE ENABLED - Using MockCoreApiService instead of real API");
        }
    }

    /**
     * Processes a user message and returns the response.
     * Supports multiple operations in a single message.
     * 
     * @param telegramId The Telegram user ID
     * @param message The message sent by the user
     * @return The response message to send back to the user
     */
    public String processMessage(Long telegramId, String message) {
        // 1. Get or create the userId for this Telegram user
        String userId = userMapping.getUserId(telegramId);
        System.out.println("📨 Processing message from Telegram ID: " + telegramId + " (User ID: " + userId + ")");
        
        // 2. Classify the message intent(s) WITH conversation context
        List<IntentResult> intents = intentClassifier.classifyIntent(message, telegramId);
        System.out.println("🎯 Detected " + intents.size() + " intent(s): " + intents);
        
        // 3. Save user message to history
        conversationHistory.addUserMessage(telegramId, message);
        
        // 4. Execute the corresponding action(s)
        String response;
        String mainIntent = intents.get(0).getIntent();
        if (intents.size() == 1) {
            // Single operation
            response = executeIntent(userId, intents.get(0));
        } else {
            // Multiple operations - execute each and combine responses
            response = executeMultipleIntents(userId, intents);
        }
        
        // 5. Humanize the response using AI (for data-rich responses)
        response = humanizeIfNeeded(response, message, mainIntent);
        
        // 6. Save assistant response to history
        conversationHistory.addAssistantMessage(telegramId, response);
        
        System.out.println("💬 Conversation history size: " + conversationHistory.getHistorySize(telegramId) + " messages");
        
        return response;
    }
    
    /**
     * Humanizes the response if it's a data-rich response that could benefit from a more conversational tone.
     */
    private String humanizeIfNeeded(String response, String userQuery, String intent) {
        List<String> humanizeIntents = List.of(
            "get_balance", 
            "get_summary", 
            "list_transactions", 
            "list_transactions_by_range",
            "list_transactions_by_date",
            "search_transactions",
            "list_rules"
        );
        
        String queryLower = userQuery != null ? userQuery.toLowerCase() : "";
        boolean isFilteredQuery = queryLower.contains("ingreso") || 
                                   queryLower.contains("gané") || 
                                   queryLower.contains("ganancia") ||
                                   queryLower.contains("recibí") ||
                                   (queryLower.contains("cuánto") && queryLower.contains("gan"));
        
        // Don't humanize filtered queries to avoid mixing data
        if (isFilteredQuery && ("list_transactions_by_range".equals(intent) || 
                                "list_transactions".equals(intent))) {
            System.out.println("⏭️ Skipping humanization for filtered query: " + userQuery);
            return response;
        }
        
        // Only humanize for specific intents and non-error responses
        if (intent != null && humanizeIntents.contains(intent) && !response.startsWith("❌") && response.length() > 50) {
            try {
                return intentClassifier.humanizeResponse(response, userQuery, intent);
            } catch (Exception e) {
                System.err.println("⚠️ Humanization failed, using original response: " + e.getMessage());
                return response;
            }
        }
        
        return response;
    }
    
    /**
     * Executes multiple intents and combines the responses.
     * Builds a detailed response listing all operations.
     */
    private String executeMultipleIntents(String userId, List<IntentResult> intents) {
        StringBuilder combinedResponse = new StringBuilder();
        
        // Count valid operations (those with amount > 0)
        int validOperationCount = 0;
        for (IntentResult intent : intents) {
            Double amount = intent.getAmount();
            if (amount != null && amount > 0) {
                validOperationCount++;
            }
        }
        
        combinedResponse.append("📝 *Registrando ").append(validOperationCount).append(" operaciones:*\n\n");
        
        // List all valid operations before executing
        int opNumber = 0;
        for (IntentResult intent : intents) {
            String intentType = intent.getIntent();
            String type = "create_expense".equals(intentType) ? "Expense" : "Income";
            String emoji = formatter.getOperationEmoji(type);
            String typeText = formatter.getOperationTypeText(type);
            Double amount = intent.getAmount();
            String description = intent.getDescription() != null ? intent.getDescription() : intent.getCategory();
            
            if (amount != null && amount > 0) {
                opNumber++;
                combinedResponse.append(String.format("%d. %s %s de $%,.0f", opNumber, emoji, typeText, amount));
                if (description != null && !description.isEmpty()) {
                    combinedResponse.append(" - ").append(description);
                }
                combinedResponse.append("\n");
            }
        }
        combinedResponse.append("\n");
        
        // Execute all operations
        int successCount = 0;
        int failCount = 0;
        StringBuilder errors = new StringBuilder();
        
        for (int i = 0; i < intents.size(); i++) {
            IntentResult intent = intents.get(i);
            System.out.println("🔄 Executing operation " + (i + 1) + "/" + intents.size() + ": " + intent.getIntent());
            
            try {
                String result = executeIntentSilent(userId, intent);
                if (result.startsWith("❌")) {
                    failCount++;
                    errors.append("❌ Op ").append(i + 1).append(": ").append(result).append("\n");
                } else {
                    successCount++;
                }
            } catch (Exception e) {
                failCount++;
                errors.append("❌ Error en operación ").append(i + 1).append(": ").append(e.getMessage()).append("\n");
            }
        }
        
        // Add errors if any
        if (errors.length() > 0) {
            combinedResponse.append(errors);
        }
        
        // Add summary at the end
        if (failCount == 0) {
            combinedResponse.append("✅ ¡").append(successCount).append(" operación(es) registrada(s) exitosamente!");
        } else {
            combinedResponse.append("⚠️ ").append(successCount).append(" exitosa(s), ").append(failCount).append(" fallida(s).");
        }
        
        return combinedResponse.toString().trim();
    }
    
    /**
     * Executes an intent without adding the full response message (for batch processing).
     */
    private String executeIntentSilent(String userId, IntentResult intent) {
        switch (intent.getIntent()) {
            case "create_expense":
                return transactionHandler.handleCreateTransactionSilent(userId, intent, "Expense");
            case "create_income":
                return transactionHandler.handleCreateTransactionSilent(userId, intent, "Income");
            default:
                return executeIntent(userId, intent);
        }
    }

    /**
     * Executes the action based on the classified intent.
     * Delegates to specialized handler services.
     */
    private String executeIntent(String userId, IntentResult intent) {
        try {
            String intentType = intent.getIntent();
            if (intentType == null) {
                return intent.getResponse() != null ? intent.getResponse() : 
                       "¡Hola! Soy tu asistente financiero. ¿En qué puedo ayudarte?";
            }
            
            return switch (intentType) {
                // Validate expense is special - stays here due to complexity
                case "validate_expense" -> handleValidateExpense(userId, intent);
                
                // Transaction operations - delegated to TransactionHandlerService
                case "create_expense" -> transactionHandler.handleCreateTransaction(userId, intent, "Expense");
                case "create_income" -> transactionHandler.handleCreateTransaction(userId, intent, "Income");
                case "list_transactions" -> transactionHandler.handleListTransactions(userId, intent);
                case "list_transactions_by_date" -> transactionHandler.handleListTransactionsByDate(userId, intent);
                case "list_transactions_by_range" -> transactionHandler.handleListTransactionsByRange(userId, intent);
                case "search_transactions" -> transactionHandler.handleSearchTransactions(userId, intent);
                case "delete_transaction" -> transactionHandler.handleDeleteTransaction(userId);
                
                // Query operations - delegated to QueryHandlerService
                case "get_balance" -> queryHandler.handleGetBalance(userId);
                case "get_summary" -> queryHandler.handleGetSummary(userId, intent);
                
                // Rule operations - delegated to RuleHandlerService
                case "create_rule" -> ruleHandler.handleCreateRule(userId, intent);
                case "list_rules" -> ruleHandler.handleListRules(userId);
                
                // Default/Question - return AI response
                default -> intent.getResponse() != null ? intent.getResponse() : 
                           "¡Hola! Soy tu asistente financiero. ¿En qué puedo ayudarte?";
            };
        } catch (Exception e) {
            System.err.println("Error executing intent: " + e.getMessage());
            return "Lo siento, hubo un error procesando tu solicitud. Por favor intenta de nuevo.";
        }
    }

    /**
     * Handles expense validation/consultation requests.
     * This ONLY provides advice - it does NOT register any transaction.
     * The user is just ASKING if they can spend, not confirming they spent.
     * 
     * Note: This method stays in MessageProcessorService due to its complexity
     * and need to access multiple services (transactions, rules, formatting).
     */
    @SuppressWarnings("unchecked")
    private String handleValidateExpense(String userId, IntentResult intent) {
        // Get user's transaction history
        Map<String, Object> transactionsResult = transactionHandler.getTransactionsForUser(userId);
        
        // Get user's rules
        List<Map<String, Object>> rules = ruleHandler.getRulesForUser(userId);
        
        StringBuilder response = new StringBuilder();
        
        // Handle null/missing amount
        Double amount = intent.getAmount();
        if (amount == null || amount <= 0) {
            return "🤔 ¿Cuánto estás pensando gastar? Por favor especifica el monto.\n\n" +
                   "💡 Ejemplo: \"¿Puedo gastar 50000 en ropa?\"";
        }
        
        // Handle null/missing/ambiguous category
        String category = intent.getCategory();
        String description = intent.getDescription();
        
        boolean isAmbiguousCategory = category == null || category.isEmpty() || 
            category.equalsIgnoreCase("null") ||
            category.equalsIgnoreCase("eso") ||
            category.equalsIgnoreCase("esto") ||
            category.equalsIgnoreCase("aquello") ||
            category.equalsIgnoreCase("Otros");
        
        boolean isAmbiguousDescription = description == null || description.isEmpty() ||
            description.equalsIgnoreCase("esto") ||
            description.equalsIgnoreCase("eso") ||
            description.equalsIgnoreCase("aquello") ||
            description.equalsIgnoreCase("algo");
        
        // If both category and description are ambiguous, ask for clarification
        if (isAmbiguousCategory && isAmbiguousDescription) {
            return String.format(
                "🤔 ¿Gastar $%,.0f en *qué* exactamente?\n\n" +
                "💡 Necesito saber en qué quieres gastar para darte una mejor recomendación.\n\n" +
                "Por ejemplo:\n" +
                "• \"¿Puedo gastar $%,.0f en ropa?\"\n" +
                "• \"¿Me alcanza para una cena de $%,.0f?\"\n" +
                "• \"¿Debería gastar $%,.0f en entretenimiento?\"",
                amount, amount, amount, amount
            );
        }
        
        // Use description as category if category is ambiguous but description is clear
        if (isAmbiguousCategory && !isAmbiguousDescription) {
            category = description;
        }
        
        response.append("🤔 *Sobre gastar $").append(String.format("%,.0f", amount));
        response.append(" en ").append(category).append(":*\n\n");
        
        // Check if user has rules for this category
        boolean hasRule = false;
        Double categoryLimit = null;
        String rulePeriod = null;
        
        for (Map<String, Object> rule : rules) {
            String ruleCategory = (String) rule.get("category");
            if (ruleCategory != null && 
                (ruleCategory.equalsIgnoreCase(category) || ruleCategory.equalsIgnoreCase("General"))) {
                hasRule = true;
                categoryLimit = ((Number) rule.get("amountLimit")).doubleValue();
                rulePeriod = (String) rule.get("period");
                break;
            }
        }
        
        if (hasRule && categoryLimit != null) {
            // User has a budget rule for this category
            double spentInCategory = transactionHandler.calculateSpentInPeriod(userId, category, rulePeriod, transactionsResult);
            double remainingBudget = categoryLimit - spentInCategory;
            double percentUsed = (spentInCategory / categoryLimit) * 100;
            String periodText = formatter.translatePeriod(rulePeriod);
            
            response.append("📏 *Tu presupuesto ").append(periodText.toLowerCase()).append(" para ").append(category).append(":*\n");
            response.append("• Límite: $").append(String.format("%,.0f", categoryLimit)).append("\n");
            response.append("• Ya gastaste: $").append(String.format("%,.0f", spentInCategory));
            response.append(" (").append(String.format("%.0f", percentUsed)).append("%)\n");
            response.append("• Disponible: $").append(String.format("%,.0f", Math.max(0, remainingBudget))).append("\n\n");
            
            if (amount > remainingBudget) {
                // Would exceed the remaining budget
                if (remainingBudget <= 0) {
                    response.append("🚫 *¡Ya agotaste tu presupuesto ").append(periodText.toLowerCase()).append("!*\n\n");
                    response.append("💡 *Recomendación:* Este gasto de $").append(String.format("%,.0f", amount));
                    response.append(" excedería tu límite por $").append(String.format("%,.0f", amount - remainingBudget)).append(".\n");
                    response.append("Considera esperar al próximo período o ajustar tu presupuesto.");
                } else {
                    response.append("⚠️ *Este gasto excedería tu presupuesto disponible.*\n\n");
                    response.append("💡 *Recomendación:* Solo te quedan $").append(String.format("%,.0f", remainingBudget));
                    response.append(" disponibles. Este gasto de $").append(String.format("%,.0f", amount));
                    response.append(" te dejaría $").append(String.format("%,.0f", amount - remainingBudget)).append(" por encima del límite.\n\n");
                    response.append("Podrías:\n");
                    response.append("• Gastar máximo $").append(String.format("%,.0f", remainingBudget)).append("\n");
                    response.append("• Esperar al próximo período\n");
                    response.append("• Ajustar tu presupuesto si realmente lo necesitas");
                }
            } else if (amount > remainingBudget * 0.8) {
                // Would use more than 80% of remaining budget
                response.append("⚡ *Está dentro del presupuesto, pero ajustado.*\n\n");
                response.append("💡 *Recomendación:* Después de este gasto te quedarían solo $");
                response.append(String.format("%,.0f", remainingBudget - amount)).append(" para el resto del período.\n\n");
                String whatToSay = description != null ? description : category;
                response.append("Si decides hacerlo, dime: \"Gasté $").append(String.format("%,.0f", amount));
                response.append(" en ").append(whatToSay).append("\"");
            } else {
                // Comfortable within budget
                response.append("✅ *¡Está dentro de tu presupuesto!*\n\n");
                response.append("💡 Después de este gasto aún te quedarían $");
                response.append(String.format("%,.0f", remainingBudget - amount)).append(" disponibles.\n\n");
                String whatToSay = description != null ? description : category;
                response.append("Si decides hacerlo, dime: \"Gasté $").append(String.format("%,.0f", amount));
                response.append(" en ").append(whatToSay).append("\"");
            }
        } else {
            // No specific rule - provide general advice
            double spentInCategory = transactionHandler.calculateSpentInPeriod(userId, category, "Monthly", transactionsResult);
            
            if (spentInCategory > 0) {
                response.append("📊 *No tienes un límite para ").append(category).append("*, pero este mes ya gastaste $");
                response.append(String.format("%,.0f", spentInCategory)).append(" en esta categoría.\n\n");
                response.append("Con este gasto de $").append(String.format("%,.0f", amount));
                response.append(" llevarías $").append(String.format("%,.0f", spentInCategory + amount)).append(" ").append(category.toLowerCase()).append(".\n\n");
            } else {
                response.append("📊 No tienes un límite configurado para ").append(category).append(".\n\n");
            }
            
            response.append("💡 *Antes de gastar, considera:*\n");
            response.append("• ¿Es una necesidad o un gusto?\n");
            response.append("• ¿Afecta tus metas de ahorro?\n");
            response.append("• ¿Quieres establecer un límite para esta categoría?\n\n");
            
            String whatToSay = description != null ? description : category;
            response.append("Si decides hacerlo, dime: \"Gasté $").append(String.format("%,.0f", amount));
            response.append(" en ").append(whatToSay).append("\"");
        }
        
        String modeIndicator = formatter.getMockIndicator(useMock);
        
        return response.toString() + modeIndicator;
    }
}
