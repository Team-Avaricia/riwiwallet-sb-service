package com.avaricia.sb_service.assistant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.avaricia.sb_service.assistant.dto.IntentResult;

import java.util.List;
import java.util.Map;

/**
 * Service that processes user messages and coordinates actions.
 * Acts as the main orchestrator between intent classification and API calls.
 * Now includes conversation history for context-aware responses.
 */
@Service
public class MessageProcessorService {

    private final IntentClassifierService intentClassifier;
    private final CoreApiService coreApi;
    private final MockCoreApiService mockCoreApi;
    private final UserMappingService userMapping;
    private final ConversationHistoryService conversationHistory;
    private final boolean useMock;

    public MessageProcessorService(
            IntentClassifierService intentClassifier,
            CoreApiService coreApi,
            MockCoreApiService mockCoreApi,
            UserMappingService userMapping,
            ConversationHistoryService conversationHistory,
            @Value("${ms.core.use-mock:false}") boolean useMock) {
        this.intentClassifier = intentClassifier;
        this.coreApi = coreApi;
        this.mockCoreApi = mockCoreApi;
        this.userMapping = userMapping;
        this.conversationHistory = conversationHistory;
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
        // List of intents that benefit from humanization
        List<String> humanizeIntents = List.of(
            "get_balance", 
            "get_summary", 
            "list_transactions", 
            "list_transactions_by_range",
            "list_transactions_by_date",
            "search_transactions",
            "list_rules"
        );
        
        // Only humanize for specific intents and non-error responses
        if (humanizeIntents.contains(intent) && !response.startsWith("❌") && response.length() > 50) {
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
     * Now builds a detailed response listing ALL operations.
     */
    private String executeMultipleIntents(String userId, List<IntentResult> intents) {
        StringBuilder combinedResponse = new StringBuilder();
        
        // First, count valid operations (those with amount > 0)
        int validOperationCount = 0;
        for (IntentResult intent : intents) {
            Double amount = intent.getAmount();
            if (amount != null && amount > 0) {
                validOperationCount++;
            }
        }
        
        // Build header with correct count
        combinedResponse.append("📝 *Registrando ").append(validOperationCount).append(" operaciones:*\n\n");
        
        // List all valid operations before executing
        int opNumber = 0;
        for (IntentResult intent : intents) {
            String emoji = getOperationEmoji(intent);
            String typeText = getOperationTypeText(intent);
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
     * Gets the emoji for an operation based on intent type.
     */
    private String getOperationEmoji(IntentResult intent) {
        String intentType = intent.getIntent();
        if (intentType == null) return "📋";
        
        return switch (intentType) {
            case "create_expense" -> "💸";
            case "create_income" -> "💰";
            default -> "📋";
        };
    }
    
    /**
     * Gets the type text for an operation.
     */
    private String getOperationTypeText(IntentResult intent) {
        String intentType = intent.getIntent();
        if (intentType == null) return "Operación";
        
        return switch (intentType) {
            case "create_expense" -> "Gasto";
            case "create_income" -> "Ingreso";
            default -> "Operación";
        };
    }
    
    /**
     * Executes an intent without adding the full response message (for batch processing).
     */
    private String executeIntentSilent(String userId, IntentResult intent) {
        // For create operations, just execute and return minimal response
        switch (intent.getIntent()) {
            case "create_expense":
                return handleCreateTransactionSilent(userId, intent, "Expense");
            case "create_income":
                return handleCreateTransactionSilent(userId, intent, "Income");
            default:
                return executeIntent(userId, intent);
        }
    }
    
    /**
     * Creates a transaction silently (minimal response for batch operations).
     */
    private String handleCreateTransactionSilent(String userId, IntentResult intent, String type) {
        Map<String, Object> api = useMock ? mockCoreApi.createTransaction(userId, intent.getAmount(), 
                type, intent.getCategory(), intent.getDescription())
            : coreApi.createTransaction(userId, intent.getAmount(), 
                type, intent.getCategory(), intent.getDescription());
        
        if (api.containsKey("error")) {
            return "❌ " + api.get("error");
        }
        return "✅";
    }

    /**
     * Executes the action based on the classified intent.
     */
    private String executeIntent(String userId, IntentResult intent) {
        try {
            switch (intent.getIntent()) {
                case "validate_expense":
                    return handleValidateExpense(userId, intent);
                    
                case "create_expense":
                    return handleCreateTransaction(userId, intent, "Expense");
                    
                case "create_income":
                    return handleCreateTransaction(userId, intent, "Income");
                    
                case "list_transactions":
                    return handleListTransactions(userId, intent);
                    
                case "list_transactions_by_date":
                    return handleListTransactionsByDate(userId, intent);
                    
                case "list_transactions_by_range":
                    return handleListTransactionsByRange(userId, intent);
                    
                case "search_transactions":
                    return handleSearchTransactions(userId, intent);
                    
                case "get_balance":
                    return handleGetBalance(userId);
                    
                case "get_summary":
                    return handleGetSummary(userId, intent);
                    
                case "delete_transaction":
                    return handleDeleteTransaction(userId);
                    
                case "create_rule":
                    return handleCreateRule(userId, intent);
                    
                case "list_rules":
                    return handleListRules(userId);
                    
                case "question":
                default:
                    return intent.getResponse() != null ? intent.getResponse() : 
                           "¡Hola! Soy tu asistente financiero. ¿En qué puedo ayudarte?";
            }
        } catch (Exception e) {
            System.err.println("Error executing intent: " + e.getMessage());
            return "Lo siento, hubo un error procesando tu solicitud. Por favor intenta de nuevo.";
        }
    }

    /**
     * Handles expense validation/consultation requests.
     * This ONLY provides advice - it does NOT register any transaction.
     * The user is just ASKING if they can spend, not confirming they spent.
     */
    private String handleValidateExpense(String userId, IntentResult intent) {
        // Get user's transaction history to provide context-aware advice
        Map<String, Object> transactionsResult = useMock 
            ? mockCoreApi.getTransactions(userId)
            : coreApi.getTransactions(userId);
        
        // Get user's rules to check limits
        Map<String, Object> rulesResult = useMock 
            ? mockCoreApi.getRules(userId)
            : coreApi.getRules(userId);
        
        // Build a helpful response based on the question
        StringBuilder response = new StringBuilder();
        
        // Handle null/missing amount
        Double amount = intent.getAmount();
        if (amount == null || amount <= 0) {
            return "🤔 ¿Cuánto estás pensando gastar? Por favor especifica el monto.\n\n" +
                   "💡 Ejemplo: \"¿Puedo gastar 50000 en ropa?\"";
        }
        
        // Handle null/missing category - replace with descriptive text
        String category = intent.getCategory();
        if (category == null || category.isEmpty() || category.equalsIgnoreCase("null")) {
            category = "eso";
        }
        
        response.append("🤔 *Sobre gastar $").append(String.format("%,.0f", amount));
        response.append(" en ").append(category).append(":*\n\n");
        
        // Check if user has rules for this category
        boolean hasRule = false;
        Double categoryLimit = null;
        
        if (!rulesResult.containsKey("error") && rulesResult.containsKey("data")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rules = (List<Map<String, Object>>) rulesResult.get("data");
            for (Map<String, Object> rule : rules) {
                String ruleCategory = (String) rule.get("category");
                if (ruleCategory != null && ruleCategory.equalsIgnoreCase(intent.getCategory())) {
                    hasRule = true;
                    categoryLimit = ((Number) rule.get("amountLimit")).doubleValue();
                    break;
                }
            }
        }
        
        if (hasRule && categoryLimit != null) {
            if (intent.getAmount() > categoryLimit) {
                response.append("⚠️ Tienes un límite de $").append(String.format("%,.0f", categoryLimit));
                response.append(" para ").append(intent.getCategory()).append(".\n");
                response.append("Este gasto excedería tu límite.\n\n");
                response.append("💡 *Recomendación:* Considera si realmente lo necesitas o busca una alternativa más económica.");
            } else {
                response.append("✅ Está dentro de tu presupuesto de $").append(String.format("%,.0f", categoryLimit));
                response.append(" para ").append(intent.getCategory()).append(".\n\n");
                response.append("💡 Si decides hacerlo, dime: \"Gasté $").append(String.format("%,.0f", intent.getAmount()));
                response.append(" en ").append(intent.getDescription() != null ? intent.getDescription() : intent.getCategory()).append("\"");
            }
        } else {
            // No specific rule, give general advice
            response.append("📊 No tienes un límite configurado para ").append(category).append(".\n\n");
            response.append("💡 *Consejos antes de gastar:*\n");
            response.append("• ¿Es una necesidad o un gusto?\n");
            response.append("• ¿Afecta tus metas de ahorro?\n");
            response.append("• ¿Tienes un fondo de emergencia?\n\n");
            String whatToSay = intent.getDescription() != null ? intent.getDescription() : category;
            if (whatToSay.equals("eso")) {
                whatToSay = "[categoría]";
            }
            response.append("Si decides hacerlo, dime: \"Gasté $").append(String.format("%,.0f", amount));
            response.append(" en ").append(whatToSay).append("\"");
        }
        
        String modeIndicator = useMock ? "\n\n🧪 _[Modo prueba - No se registró ningún gasto]_" : "";
        
        return response.toString() + modeIndicator;
    }

    /**
     * Handles transaction creation (expense or income).
     * Now includes default values for category and description when not provided.
     * Also validates that amount is valid before calling the API.
     */
    private String handleCreateTransaction(String userId, IntentResult intent, String type) {
        // VALIDATION: Check that amount exists and is greater than 0
        Double amount = intent.getAmount();
        if (amount == null) {
            String typeText = "Expense".equals(type) ? "gasto" : "ingreso";
            return "🤔 ¿Cuánto fue el " + typeText + "? Por favor dime el monto.\n\n" +
                   "💡 Ejemplo: \"Gasté 50000 en comida\" o \"Recibí 100k\"";
        }
        
        if (amount <= 0) {
            return "🤔 El monto debe ser mayor a $0. ¿Cuánto fue realmente?";
        }
        
        // Warning for extremely high amounts (>100 billion - likely typo)
        // Allow the transaction but log a warning
        if (amount > 100_000_000_000.0) {
            System.out.println("⚠️ WARNING: Extremely high amount detected: " + amount);
        }
        
        // Set default category if not provided
        String category = intent.getCategory();
        if (category == null || category.isEmpty()) {
            category = "Otros";
        }
        
        // Set default description if not provided
        String description = intent.getDescription();
        if (description == null || description.isEmpty()) {
            description = "Expense".equals(type) ? "Gasto registrado" : "Ingreso registrado";
        }
        
        Map<String, Object> result = useMock
            ? mockCoreApi.createTransaction(userId, amount, type, category, description)
            : coreApi.createTransaction(userId, amount, type, category, description);
        
        if (result.containsKey("error")) {
            return "❌ No pude registrar la transacción. " + result.get("error");
        }
        
        String emoji = "Expense".equals(type) ? "💸" : "💰";
        String typeText = "Expense".equals(type) ? "Gasto" : "Ingreso";
        String modeIndicator = useMock ? "\n\n🧪 _[Modo prueba]_" : "";
        
        // Show balance in mock mode
        String balanceInfo = "";
        if (useMock) {
            Double balance = mockCoreApi.getBalance(userId);
            balanceInfo = String.format("\n\n💳 Saldo actual: $%,.0f", balance);
        }
        
        return String.format("%s %s registrado!\n• Monto: $%,.0f\n• Categoría: %s\n• Descripción: %s%s%s",
            emoji,
            typeText,
            amount,
            category,
            description,
            balanceInfo,
            modeIndicator
        );
    }

    /**
     * Handles listing user transactions with optional type filter.
     * Now uses the API filter parameter instead of filtering in memory.
     */
    @SuppressWarnings("unchecked")
    private String handleListTransactions(String userId, IntentResult intent) {
        String filterType = intent.getType();
        
        // Use API filter if not in mock mode and type is specified
        Map<String, Object> result;
        if (useMock) {
            result = mockCoreApi.getTransactions(userId);
            // Filter in memory for mock mode
            if (filterType != null && result.containsKey("data")) {
                List<Map<String, Object>> allTx = (List<Map<String, Object>>) result.get("data");
                if (allTx != null) {
                    List<Map<String, Object>> filtered = allTx.stream()
                        .filter(tx -> filterType.equals(tx.get("type")))
                        .toList();
                    result.put("data", filtered);
                }
            }
        } else {
            // Use API filter parameter (implemented by Brahiam)
            result = coreApi.getTransactions(userId, filterType);
        }
        
        if (result.containsKey("error")) {
            return "❌ No pude obtener las transacciones. " + result.get("error");
        }
        
        List<Map<String, Object>> transactions = (List<Map<String, Object>>) result.get("data");
        
        if (transactions == null || transactions.isEmpty()) {
            if (filterType != null) {
                String typeText = "Income".equals(filterType) ? "ingresos" : "gastos";
                return "📋 No tienes " + typeText + " registrados." + (useMock ? "\n\n🧪 _[Modo prueba]_" : "");
            }
            return "📋 No tienes transacciones registradas aún." + (useMock ? "\n\n🧪 _[Modo prueba]_" : "");
        }
        
        String title = filterType == null ? "Tus transacciones" :
            ("Income".equals(filterType) ? "Tus ingresos" : "Tus gastos");
        
        StringBuilder sb = new StringBuilder("📋 *" + title + ":*\n\n");
        int count = 0;
        int maxToShow = 15; // Show up to 15 transactions
        double totalIncome = 0;
        double totalExpense = 0;
        
        for (Map<String, Object> tx : transactions) {
            String type = (String) tx.get("type");
            double amt = ((Number) tx.get("amount")).doubleValue();
            
            // Count totals for all transactions
            if ("Income".equals(type)) totalIncome += amt;
            else totalExpense += amt;
            
            if (count >= maxToShow) continue; // Count all but only show maxToShow
            
            String emoji = "Expense".equals(type) ? "💸" : "💰";
            Object amountObj = tx.get("amount");
            String categoryTx = (String) tx.get("category");
            String descriptionTx = (String) tx.get("description");
            // Try both 'createdAt' (from Brahiam's API) and 'date' (fallback)
            String dateStr = extractDateFromTransaction(tx);
            
            // Format: 💸 $10,000 - gaseosa (Otros) - 02/12/2025
            String descText = descriptionTx != null && !descriptionTx.isEmpty() ? descriptionTx : categoryTx;
            sb.append(String.format("%s $%,.0f - %s (%s) - %s\n", 
                emoji, ((Number) amountObj).doubleValue(), descText, categoryTx, dateStr));
            count++;
        }
        
        // Show how many more if truncated
        if (transactions.size() > maxToShow) {
            sb.append(String.format("\n... y %d transacciones más\n", transactions.size() - maxToShow));
        }
        
        // Add summary - different format based on filter type
        if (filterType == null) {
            // All transactions - show full summary with balance
            sb.append(String.format("\n📊 *Resumen:*\n• Total: %d transacciones\n• 💰 Ingresos: $%,.0f\n• 💸 Gastos: $%,.0f\n• 📈 Balance: $%,.0f", 
                transactions.size(), totalIncome, totalExpense, totalIncome - totalExpense));
        } else if ("Income".equals(filterType)) {
            // Only income - show just income total
            sb.append(String.format("\n📊 *Total ingresos:* $%,.0f (%d transacciones)", 
                totalIncome, transactions.size()));
        } else {
            // Only expenses - show just expense total
            sb.append(String.format("\n📊 *Total gastos:* $%,.0f (%d transacciones)", 
                totalExpense, transactions.size()));
        }
        
        if (useMock) {
            sb.append("\n🧪 _[Modo prueba]_");
        }
        
        return sb.toString();
    }
    
    /**
     * Extracts and formats the date from a transaction.
     * Handles both 'createdAt' (from Brahiam's API) and 'date' field names.
     */
    private String extractDateFromTransaction(Map<String, Object> tx) {
        // Try createdAt first (Brahiam's API uses this)
        Object dateObj = tx.get("createdAt");
        if (dateObj == null) {
            // Fallback to date field
            dateObj = tx.get("date");
        }
        if (dateObj == null) {
            return "";
        }
        return formatDateFromApi(dateObj.toString());
    }

    /**
     * Handles deleting the last transaction.
     * Improved with better response formatting.
     */
    @SuppressWarnings("unchecked")
    private String handleDeleteTransaction(String userId) {
        // First get transactions to find the last one
        Map<String, Object> result = useMock
            ? mockCoreApi.getTransactions(userId)
            : coreApi.getTransactions(userId);
        
        if (result.containsKey("error")) {
            return "❌ No pude obtener las transacciones. " + result.get("error");
        }
        
        List<Map<String, Object>> transactions = (List<Map<String, Object>>) result.get("data");
        
        if (transactions == null || transactions.isEmpty()) {
            return "📋 No tienes transacciones para eliminar." + (useMock ? "\n\n🧪 _[Modo prueba]_" : "");
        }
        
        // Get the last transaction
        Map<String, Object> lastTx = transactions.get(0);
        String txId = (String) lastTx.get("id");
        
        // Delete the transaction
        Map<String, Object> deleteResult = useMock
            ? mockCoreApi.deleteTransaction(txId)
            : coreApi.deleteTransaction(txId);
        
        if (deleteResult.containsKey("error")) {
            return "❌ No pude eliminar la transacción. " + deleteResult.get("error");
        }
        
        // Build a better response
        String type = (String) lastTx.get("type");
        String emoji = "Income".equals(type) ? "💰" : "💸";
        Double amountDeleted = lastTx.get("amount") != null ? ((Number) lastTx.get("amount")).doubleValue() : 0.0;
        String descriptionDeleted = (String) lastTx.get("description");
        String categoryDeleted = (String) lastTx.get("category");
        String typeText = "Income".equals(type) ? "ingreso" : "gasto";
        String modeIndicator = useMock ? "\n\n🧪 _[Modo prueba]_" : "";
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("✅ ¡Listo! Eliminé tu último %s:\n\n", typeText));
        sb.append(String.format("%s *$%,.0f*\n", emoji, amountDeleted));
        
        // Always show description if available
        if (descriptionDeleted != null && !descriptionDeleted.isEmpty()) {
            sb.append(String.format("• Descripción: %s\n", descriptionDeleted));
        }
        sb.append(String.format("• Categoría: %s\n", categoryDeleted));
        sb.append("\n📝 Tu saldo ha sido restaurado.");
        sb.append(modeIndicator);
        
        return sb.toString();
    }

    private String handleCreateRule(String userId, IntentResult intent) {
        // Handle category - default to "General" if not specified
        String category = intent.getCategory();
        if (category == null || category.isEmpty() || 
            category.equalsIgnoreCase("gastos") || 
            category.equalsIgnoreCase("todos") ||
            category.equalsIgnoreCase("general")) {
            category = "General";
        }
        
        // Validate amount
        Double amount = intent.getAmount();
        if (amount == null || amount <= 0) {
            return "❌ Por favor especifica un monto válido para el límite. Ejemplo: \"Límite de 500k en comida\"";
        }
        
        String period = intent.getPeriod() != null ? intent.getPeriod() : "Monthly";
        
        Map<String, Object> result = useMock
            ? mockCoreApi.createRule(userId, "MonthlyBudget", category, amount, period)
            : coreApi.createRule(userId, "MonthlyBudget", category, amount, period);
        
        if (result.containsKey("error")) {
            return "❌ No pude crear la regla. " + result.get("error");
        }
        
        String periodText = translatePeriod(period);
        String modeIndicator = useMock ? "\n\n🧪 _[Modo prueba]_" : "";
        String categoryText = "General".equals(category) ? "Todos los gastos" : category;
        
        return String.format("📏 ¡Regla creada!\n\n• 📂 Categoría: %s\n• 💰 Límite: $%,.0f\n• 📅 Período: %s\n\n💡 Te avisaré cuando te acerques al límite.%s",
            categoryText,
            amount,
            periodText,
            modeIndicator
        );
    }

    /**
     * Handles listing user financial rules.
     */
    @SuppressWarnings("unchecked")
    private String handleListRules(String userId) {
        Map<String, Object> result = useMock
            ? mockCoreApi.getRules(userId)
            : coreApi.getRules(userId);
        
        if (result.containsKey("error")) {
            return "❌ No pude obtener las reglas. " + result.get("error");
        }
        
        List<Map<String, Object>> rules = (List<Map<String, Object>>) result.get("data");
        
        if (rules == null || rules.isEmpty()) {
            return "📏 No tienes reglas financieras configuradas." + (useMock ? "\n\n🧪 _[Modo prueba]_" : "");
        }
        
        StringBuilder sb = new StringBuilder("📏 *Tus reglas financieras:*\n\n");
        
        for (Map<String, Object> rule : rules) {
            String categoryRule = (String) rule.get("category");
            Object amountLimit = rule.get("amountLimit");
            String periodRule = (String) rule.get("period");
            String periodText = translatePeriod(periodRule);
            
            sb.append(String.format("• %s: $%s (%s)\n", categoryRule, amountLimit, periodText));
        }
        
        if (useMock) {
            sb.append("\n🧪 _[Modo prueba]_");
        }
        
        return sb.toString();
    }

    /**
     * Translates period values from English to Spanish.
     */
    private String translatePeriod(String period) {
        if (period == null) return "Mensual";
        
        switch (period.toLowerCase()) {
            case "monthly": return "Mensual";
            case "weekly": return "Semanal";
            case "daily": return "Diario";
            case "yearly": return "Anual";
            default: return period;
        }
    }

    /**
     * Handles getting transactions for a specific date.
     */
    @SuppressWarnings("unchecked")
    private String handleListTransactionsByDate(String userId, IntentResult intent) {
        if (useMock) {
            return "📅 Función disponible solo con el API real.\n\n🧪 _[Modo prueba]_";
        }
        
        String date = intent.getStartDate();
        if (date == null) {
            return "❌ No pude determinar la fecha. Por favor especifica: \"¿Cuánto gasté el 15 de noviembre?\"";
        }
        
        Map<String, Object> result = coreApi.getTransactionsByDate(userId, date);
        
        if (result.containsKey("error")) {
            return "❌ No pude obtener las transacciones. " + result.get("error");
        }
        
        List<Map<String, Object>> transactions = (List<Map<String, Object>>) result.get("data");
        Double totalAmount = result.get("totalAmount") != null ? ((Number) result.get("totalAmount")).doubleValue() : 0.0;
        
        if (transactions == null || transactions.isEmpty()) {
            return String.format("📅 No tienes transacciones registradas el %s", formatDate(date));
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📅 *Transacciones del %s:*\n\n", formatDate(date)));
        
        for (Map<String, Object> tx : transactions) {
            String type = (String) tx.get("type");
            String emoji = "Expense".equals(type) ? "💸" : "💰";
            Object amountObj = tx.get("amount");
            String categoryTx = (String) tx.get("category");
            String descriptionTx = (String) tx.get("description");
            
            sb.append(String.format("%s $%s - %s (%s)\n", emoji, amountObj, categoryTx, descriptionTx));
        }
        
        sb.append(String.format("\n💵 *Total del día:* $%,.0f", totalAmount));
        
        return sb.toString();
    }

    /**
     * Handles getting transactions for a date range.
     * Now supports type filtering (Income/Expense) based on intent.
     */
    @SuppressWarnings("unchecked")
    private String handleListTransactionsByRange(String userId, IntentResult intent) {
        if (useMock) {
            return "📆 Función disponible solo con el API real.\n\n🧪 _[Modo prueba]_";
        }
        
        String startDate = intent.getStartDate();
        String endDate = intent.getEndDate();
        String filterType = intent.getType(); // Get the type filter from intent
        
        if (startDate == null || endDate == null) {
            return "❌ No pude determinar el período. Por favor especifica: \"¿Cuánto gasté del 1 al 15 de noviembre?\"";
        }
        
        // Call API with type filter if specified (more efficient - filtering at DB level)
        Map<String, Object> result = coreApi.getTransactionsByRange(userId, startDate, endDate, filterType);
        
        if (result.containsKey("error")) {
            return "❌ No pude obtener las transacciones. " + result.get("error");
        }
        
        List<Map<String, Object>> transactions = (List<Map<String, Object>>) result.get("data");
        
        if (transactions == null || transactions.isEmpty()) {
            String typeText = filterType == null ? "transacciones" : 
                ("Income".equals(filterType) ? "ingresos" : "gastos");
            return String.format("📆 No tienes %s entre %s y %s", typeText, formatDate(startDate), formatDate(endDate));
        }
        // Build title based on filter
        String title = filterType == null ? "Transacciones" :
            ("Income".equals(filterType) ? "Ingresos" : "Gastos");
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📆 *%s del %s al %s:*\n\n", title, formatDate(startDate), formatDate(endDate)));
        
        int shown = 0;
        double totalIncome = 0;
        double totalExpense = 0;
        
        for (Map<String, Object> tx : transactions) {
            String type = (String) tx.get("type");
            double amt = ((Number) tx.get("amount")).doubleValue();
            
            if ("Income".equals(type)) totalIncome += amt;
            else totalExpense += amt;
            
            if (shown >= 10) continue; // Count all but only show 10
            
            String emoji = "Expense".equals(type) ? "💸" : "💰";
            String categoryTx = (String) tx.get("category");
            String descriptionTx = (String) tx.get("description");
            // Use extractDateFromTransaction to handle both createdAt and date fields
            String dateStr = extractDateFromTransaction(tx);
            
            // Format: 💸 $10,000 - gaseosa (Otros) - 02/12/2025
            String descText = descriptionTx != null && !descriptionTx.isEmpty() ? descriptionTx : categoryTx;
            sb.append(String.format("%s $%,.0f - %s (%s) - %s\n", 
                emoji, amt, descText, categoryTx, dateStr));
            shown++;
        }
        
        if (transactions.size() > 10) {
            sb.append(String.format("\n... y %d transacciones más\n", transactions.size() - 10));
        }
        
        // Add summary based on filter type
        if (filterType == null) {
            // All transactions - show full summary with balance
            sb.append(String.format("\n📊 *Resumen:*\n• Transacciones: %d\n• 💰 Ingresos: $%,.0f\n• 💸 Gastos: $%,.0f\n• 📈 Balance: $%,.0f", 
                transactions.size(), totalIncome, totalExpense, totalIncome - totalExpense));
        } else if ("Income".equals(filterType)) {
            // Only income - show just income total
            sb.append(String.format("\n📊 *Total ingresos:* $%,.0f (%d transacciones)", 
                totalIncome, transactions.size()));
        } else {
            // Only expenses - show just expense total
            sb.append(String.format("\n📊 *Total gastos:* $%,.0f (%d transacciones)", 
                totalExpense, transactions.size()));
        }
        
        return sb.toString();
    }
    
    /**
     * Formats a date string from API (ISO format) to display format.
     */
    private String formatDateFromApi(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return "";
        try {
            // Handle ISO format: 2025-12-02T00:00:00Z or 2025-12-02
            String datePart = isoDate.contains("T") ? isoDate.substring(0, 10) : isoDate;
            String[] parts = datePart.split("-");
            if (parts.length >= 3) {
                return parts[2] + "/" + parts[1] + "/" + parts[0]; // DD/MM/YYYY
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return isoDate;
    }

    /**
     * Handles searching transactions by description.
     */
    @SuppressWarnings("unchecked")
    private String handleSearchTransactions(String userId, IntentResult intent) {
        if (useMock) {
            return "🔍 Función disponible solo con el API real.\n\n🧪 _[Modo prueba]_";
        }
        
        String query = intent.getSearchQuery();
        if (query == null || query.isEmpty()) {
            return "❌ No pude determinar qué buscar. Por favor especifica: \"¿Cuánto he pagado de Netflix?\"";
        }
        
        Map<String, Object> result = coreApi.searchTransactions(userId, query);
        
        if (result.containsKey("error")) {
            return "❌ No pude buscar las transacciones. " + result.get("error");
        }
        
        List<Map<String, Object>> transactions = (List<Map<String, Object>>) result.get("data");
        Double totalAmount = result.get("totalAmount") != null ? ((Number) result.get("totalAmount")).doubleValue() : 0.0;
        Integer count = result.get("count") != null ? ((Number) result.get("count")).intValue() : 0;
        
        if (transactions == null || transactions.isEmpty()) {
            return String.format("🔍 No encontré transacciones relacionadas con \"%s\"", query);
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🔍 *Resultados para \"%s\":*\n\n", query));
        
        for (Map<String, Object> tx : transactions) {
            String type = (String) tx.get("type");
            String emoji = "Expense".equals(type) ? "💸" : "💰";
            Object amountObj = tx.get("amount");
            String descriptionTx = (String) tx.get("description");
            String dateStr = extractDateFromTransaction(tx);
            
            sb.append(String.format("%s $%s - %s %s\n", emoji, amountObj, descriptionTx, dateStr));
        }
        
        sb.append(String.format("\n\n📊 *Total en \"%s\":* $%,.0f (%d transacciones)", query, totalAmount, count));
        
        return sb.toString();
    }

    /**
     * Handles getting user's current balance.
     */
    private String handleGetBalance(String userId) {
        if (useMock) {
            Double balance = mockCoreApi.getBalance(userId);
            return String.format("💰 *Tu saldo actual:* $%,.0f\n\n🧪 _[Modo prueba]_", balance);
        }
        
        Map<String, Object> result = coreApi.getUserBalance(userId);
        
        if (result.containsKey("error")) {
            return "❌ No pude obtener tu saldo. " + result.get("error");
        }
        
        Double totalIncome = result.get("totalIncome") != null ? ((Number) result.get("totalIncome")).doubleValue() : 0.0;
        Double totalExpenses = result.get("totalExpenses") != null ? ((Number) result.get("totalExpenses")).doubleValue() : 0.0;
        Double currentBalance = result.get("currentBalance") != null ? ((Number) result.get("currentBalance")).doubleValue() : 0.0;
        
        StringBuilder sb = new StringBuilder();
        sb.append("💰 *Tu situación financiera:*\n\n");
        sb.append(String.format("📈 Ingresos totales: $%,.0f\n", totalIncome));
        sb.append(String.format("📉 Gastos totales: $%,.0f\n", totalExpenses));
        sb.append(String.format("\n💵 *Saldo actual:* $%,.0f", currentBalance));
        
        return sb.toString();
    }

    /**
     * Handles getting complete financial summary including income and expenses.
     * Now with more conversational and direct responses.
     */
    @SuppressWarnings("unchecked")
    private String handleGetSummary(String userId, IntentResult intent) {
        if (useMock) {
            return "📊 Función disponible solo con el API real.\n\n🧪 _[Modo prueba]_";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // 1. Get balance info (total income and expenses)
        Map<String, Object> balanceResult = coreApi.getUserBalance(userId);
        Double totalIncome = 0.0;
        Double totalExpenses = 0.0;
        Double currentBalance = 0.0;
        
        if (!balanceResult.containsKey("error")) {
            totalIncome = balanceResult.get("totalIncome") != null ? ((Number) balanceResult.get("totalIncome")).doubleValue() : 0.0;
            totalExpenses = balanceResult.get("totalExpenses") != null ? ((Number) balanceResult.get("totalExpenses")).doubleValue() : 0.0;
            currentBalance = balanceResult.get("currentBalance") != null ? ((Number) balanceResult.get("currentBalance")).doubleValue() : 0.0;
        }
        
        // 2. Get expenses by category
        Map<String, Object> result;
        String startDate = intent.getStartDate();
        String endDate = intent.getEndDate();
        
        if (startDate != null && endDate != null) {
            result = coreApi.getTransactionSummaryByCategory(userId, startDate, endDate);
        } else {
            result = coreApi.getTransactionSummaryByCategory(userId);
        }
        
        if (result.containsKey("error")) {
            sb.append("📊 *Tu situación financiera:*\n\n");
            sb.append("💰 *Ingresos totales:* $").append(String.format("%,.0f", totalIncome)).append("\n");
            sb.append("💸 *Gastos totales:* $").append(String.format("%,.0f", totalExpenses)).append("\n");
            sb.append("💵 *Saldo actual:* $").append(String.format("%,.0f", currentBalance)).append("\n\n");
            return sb.toString() + "❌ No pude obtener el desglose por categoría.";
        }
        
        List<Map<String, Object>> categories = (List<Map<String, Object>>) result.get("data");
        
        if (categories != null && !categories.isEmpty()) {
            // Get the top category for conversational intro
            Map<String, Object> topCategory = categories.get(0);
            String topCatName = (String) topCategory.get("category");
            Double topCatAmount = ((Number) topCategory.get("totalAmount")).doubleValue();
            Double topCatPercentage = topCategory.get("percentage") != null ? ((Number) topCategory.get("percentage")).doubleValue() : 0.0;
            String topCatEmoji = getCategoryEmoji(topCatName);
            
            // Conversational intro based on the top category
            if (topCatPercentage > 50) {
                sb.append(String.format("¡Tu mayor gasto está en *%s*! %s Con $%,.0f (%.0f%%), representa la mayor parte de tus gastos.\n\n", 
                    topCatName, topCatEmoji, topCatAmount, topCatPercentage));
            } else if (topCatPercentage > 30) {
                sb.append(String.format("*%s* %s es donde más gastas, con $%,.0f (%.0f%%) de tus gastos totales.\n\n", 
                    topCatName, topCatEmoji, topCatAmount, topCatPercentage));
            } else {
                sb.append(String.format("Tus gastos están bastante distribuidos. *%s* %s lidera con $%,.0f (%.0f%%).\n\n", 
                    topCatName, topCatEmoji, topCatAmount, topCatPercentage));
            }
            
            // Balance summary
            sb.append("💰 Ingresos: $").append(String.format("%,.0f", totalIncome));
            sb.append(" | 💸 Gastos: $").append(String.format("%,.0f", totalExpenses));
            sb.append(" | 💵 Saldo: *$").append(String.format("%,.0f", currentBalance)).append("*\n\n");
            
            sb.append("📉 *Desglose completo:*\n");
            
            for (Map<String, Object> cat : categories) {
                String categoryName = (String) cat.get("category");
                Double amount = ((Number) cat.get("totalAmount")).doubleValue();
                Double percentage = cat.get("percentage") != null ? ((Number) cat.get("percentage")).doubleValue() : 0.0;
                String emoji = getCategoryEmoji(categoryName);
                
                String bar = generateProgressBar(percentage);
                sb.append(String.format("• %s %s: $%,.0f (%s %.1f%%)\n", emoji, categoryName, amount, bar, percentage));
            }
            
            // Add helpful tip based on spending pattern
            if (topCatPercentage > 50) {
                sb.append(String.format("\n💡 *Tip:* Considera revisar tus gastos en %s, ya que representan más de la mitad de tu presupuesto.", topCatName));
            }
        } else {
            sb.append("📊 *Tu situación financiera:*\n\n");
            sb.append("💰 *Ingresos totales:* $").append(String.format("%,.0f", totalIncome)).append("\n");
            sb.append("💸 *Gastos totales:* $").append(String.format("%,.0f", totalExpenses)).append("\n");
            sb.append("💵 *Saldo actual:* $").append(String.format("%,.0f", currentBalance)).append("\n\n");
            sb.append("📋 No tienes gastos registrados aún. ¡Empieza a registrar para ver tu desglose!");
        }
        
        return sb.toString();
    }
    
    /**
     * Gets emoji for a category to make responses more visual.
     */
    private String getCategoryEmoji(String category) {
        if (category == null) return "📦";
        return switch (category.toLowerCase()) {
            case "comida" -> "🍔";
            case "transporte" -> "🚗";
            case "entretenimiento" -> "🎬";
            case "salud" -> "💊";
            case "educación" -> "📚";
            case "hogar" -> "🏠";
            case "ropa" -> "👕";
            case "tecnología" -> "📱";
            case "servicios" -> "💡";
            case "arriendo", "vivienda" -> "🏠";
            case "salario" -> "💼";
            case "freelance" -> "💻";
            case "inversiones" -> "📈";
            case "regalos" -> "🎁";
            default -> "📦";
        };
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Formats a date string to a more readable format.
     */
    private String formatDate(String isoDate) {
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(isoDate);
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return date.format(formatter);
        } catch (Exception e) {
            return isoDate;
        }
    }

    /**
     * Generates a simple progress bar for percentages.
     */
    private String generateProgressBar(double percentage) {
        int filled = (int) Math.min(percentage / 10, 10);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < filled; i++) bar.append("█");
        for (int i = filled; i < 10; i++) bar.append("░");
        return bar.toString();
    }
}
