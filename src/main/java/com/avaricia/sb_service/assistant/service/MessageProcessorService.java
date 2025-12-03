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
        if (intents.size() == 1) {
            // Single operation
            response = executeIntent(userId, intents.get(0));
        } else {
            // Multiple operations - execute each and combine responses
            response = executeMultipleIntents(userId, intents);
        }
        
        // 5. Save assistant response to history
        conversationHistory.addAssistantMessage(telegramId, response);
        
        System.out.println("💬 Conversation history size: " + conversationHistory.getHistorySize(telegramId) + " messages");
        
        return response;
    }
    
    /**
     * Executes multiple intents and combines the responses.
     */
    private String executeMultipleIntents(String userId, List<IntentResult> intents) {
        StringBuilder combinedResponse = new StringBuilder();
        
        // Use the response from the first intent (which should summarize all operations)
        String summaryResponse = intents.get(0).getResponse();
        if (summaryResponse != null && !summaryResponse.isEmpty()) {
            combinedResponse.append(summaryResponse).append("\n\n");
        }
        
        int successCount = 0;
        int failCount = 0;
        
        for (int i = 0; i < intents.size(); i++) {
            IntentResult intent = intents.get(i);
            System.out.println("🔄 Executing operation " + (i + 1) + "/" + intents.size() + ": " + intent.getIntent());
            
            try {
                String result = executeIntentSilent(userId, intent);
                if (result.startsWith("❌")) {
                    failCount++;
                    combinedResponse.append("❌ Op ").append(i + 1).append(": ").append(result).append("\n");
                } else {
                    successCount++;
                }
            } catch (Exception e) {
                failCount++;
                combinedResponse.append("❌ Error en operación ").append(i + 1).append(": ").append(e.getMessage()).append("\n");
            }
        }
        
        // Add summary at the end
        if (failCount == 0) {
            combinedResponse.append("\n✅ ").append(successCount).append(" operación(es) completada(s) exitosamente.");
        } else {
            combinedResponse.append("\n⚠️ ").append(successCount).append(" exitosa(s), ").append(failCount).append(" fallida(s).");
        }
        
        return combinedResponse.toString().trim();
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
            case "create_recurring_income":
                return handleCreateRecurringTransactionSilent(userId, intent, "Income");
            case "create_recurring_expense":
                return handleCreateRecurringTransactionSilent(userId, intent, "Expense");
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
     * Creates a recurring transaction silently (minimal response for batch operations).
     */
    private String handleCreateRecurringTransactionSilent(String userId, IntentResult intent, String type) {
        String frequency = intent.getFrequency() != null ? intent.getFrequency() : "Monthly";
        Integer dayOfMonth = intent.getDayOfMonth();
        
        Map<String, Object> api = useMock 
            ? mockCoreApi.createRecurringTransaction(userId, intent.getAmount(), type, 
                intent.getCategory(), intent.getDescription(), frequency, dayOfMonth)
            : coreApi.createRecurringTransaction(userId, intent.getAmount(), type, 
                intent.getCategory(), intent.getDescription(), frequency, dayOfMonth);
        
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
                    
                case "create_recurring_income":
                    return handleCreateRecurringTransaction(userId, intent, "Income");
                    
                case "create_recurring_expense":
                    return handleCreateRecurringTransaction(userId, intent, "Expense");
                    
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
                    
                case "get_cashflow":
                    return handleGetCashflow(userId);
                    
                case "list_recurring":
                    return handleListRecurring(userId);
                    
                case "delete_recurring":
                    return handleDeleteRecurring(userId, intent);
                    
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
        response.append("🤔 *Sobre gastar $").append(String.format("%,.0f", intent.getAmount()));
        response.append(" en ").append(intent.getCategory()).append(":*\n\n");
        
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
            response.append("📊 No tienes un límite configurado para ").append(intent.getCategory()).append(".\n\n");
            response.append("💡 *Consejos antes de gastar:*\n");
            response.append("• ¿Es una necesidad o un gusto?\n");
            response.append("• ¿Afecta tus metas de ahorro?\n");
            response.append("• ¿Tienes un fondo de emergencia?\n\n");
            response.append("Si decides hacerlo, dime: \"Gasté $").append(String.format("%,.0f", intent.getAmount()));
            response.append(" en ").append(intent.getDescription() != null ? intent.getDescription() : intent.getCategory()).append("\"");
        }
        
        String modeIndicator = useMock ? "\n\n🧪 _[Modo prueba - No se registró ningún gasto]_" : "";
        
        return response.toString() + modeIndicator;
    }

    /**
     * Handles transaction creation (expense or income).
     */
    private String handleCreateTransaction(String userId, IntentResult intent, String type) {
        Map<String, Object> result = useMock
            ? mockCoreApi.createTransaction(userId, intent.getAmount(), type, intent.getCategory(), intent.getDescription())
            : coreApi.createTransaction(userId, intent.getAmount(), type, intent.getCategory(), intent.getDescription());
        
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
            intent.getAmount(),
            intent.getCategory(),
            intent.getDescription(),
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
        
        String title = filterType == null ? "Tus últimas transacciones" :
            ("Income".equals(filterType) ? "Tus últimos ingresos" : "Tus últimos gastos");
        
        StringBuilder sb = new StringBuilder("📋 *" + title + ":*\n\n");
        int count = 0;
        
        for (Map<String, Object> tx : transactions) {
            if (count >= 5) break; // Show only the last 5
            
            String type = (String) tx.get("type");
            String emoji = "Expense".equals(type) ? "💸" : "💰";
            Object amount = tx.get("amount");
            String category = (String) tx.get("category");
            String description = (String) tx.get("description");
            // Try both 'createdAt' (from Brahiam's API) and 'date' (fallback)
            String dateStr = extractDateFromTransaction(tx);
            
            // Format: 💸 $10,000 - gaseosa (Otros) - 02/12/2025
            String descText = description != null && !description.isEmpty() ? description : category;
            sb.append(String.format("%s $%,.0f - %s (%s) - %s\n", 
                emoji, ((Number) amount).doubleValue(), descText, category, dateStr));
            count++;
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
        
        Object amount = lastTx.get("amount");
        String category = (String) lastTx.get("category");
        String modeIndicator = useMock ? "\n\n🧪 _[Modo prueba]_" : "";
        
        return String.format("🗑️ Transacción eliminada!\n• Monto: $%s\n• Categoría: %s\n\nTu saldo ha sido actualizado.%s", 
            amount, category, modeIndicator);
    }

    private String handleCreateRule(String userId, IntentResult intent) {
        Map<String, Object> result = useMock
            ? mockCoreApi.createRule(
                userId,
                "MonthlyBudget",
                intent.getCategory(),
                intent.getAmount(),
                intent.getPeriod() != null ? intent.getPeriod() : "Monthly"
            )
            : coreApi.createRule(
                userId,
                "MonthlyBudget",
                intent.getCategory(),
                intent.getAmount(),
                intent.getPeriod() != null ? intent.getPeriod() : "Monthly"
            );
        
        if (result.containsKey("error")) {
            return "❌ No pude crear la regla. " + result.get("error");
        }
        
        String periodText = translatePeriod(intent.getPeriod());
        String modeIndicator = useMock ? "\n\n🧪 _[Modo prueba]_" : "";
        
        return String.format("📏 Regla creada!\n• Categoría: %s\n• Límite: $%,.0f\n• Período: %s%s",
            intent.getCategory(),
            intent.getAmount(),
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
            String category = (String) rule.get("category");
            Object amountLimit = rule.get("amountLimit");
            String period = (String) rule.get("period");
            String periodText = translatePeriod(period);
            
            sb.append(String.format("• %s: $%s (%s)\n", category, amountLimit, periodText));
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

    // ==================== NEW HANDLERS FOR BRAHIAM'S ENDPOINTS ====================

    /**
     * Handles creating recurring transactions (income or expense).
     */
    private String handleCreateRecurringTransaction(String userId, IntentResult intent, String type) {
        if (useMock) {
            String emoji = "Income".equals(type) ? "💰" : "💸";
            String typeText = "Income".equals(type) ? "Ingreso" : "Gasto";
            String freqText = translatePeriod(intent.getFrequency());
            String dayInfo = intent.getDayOfMonth() != null ? " (día " + intent.getDayOfMonth() + ")" : "";
            
            return String.format("%s %s recurrente registrado!\n• Monto: $%,.0f %s%s\n• Categoría: %s\n• Descripción: %s\n\n🧪 _[Modo prueba]_",
                emoji, typeText, intent.getAmount(), freqText, dayInfo, intent.getCategory(), intent.getDescription());
        }
        
        Map<String, Object> result = coreApi.createRecurringTransaction(
            userId, 
            intent.getAmount(), 
            type, 
            intent.getCategory(), 
            intent.getDescription(),
            intent.getFrequency() != null ? intent.getFrequency() : "Monthly",
            intent.getDayOfMonth()
        );
        
        if (result.containsKey("error")) {
            return "❌ No pude registrar la transacción recurrente. " + result.get("error");
        }
        
        String emoji = "Income".equals(type) ? "💰" : "💸";
        String typeText = "Income".equals(type) ? "Ingreso" : "Gasto";
        String freqText = translatePeriod(intent.getFrequency());
        String dayInfo = intent.getDayOfMonth() != null ? " (día " + intent.getDayOfMonth() + ")" : "";
        
        return String.format("%s ¡%s recurrente creado!\n• Monto: $%,.0f\n• Frecuencia: %s%s\n• Categoría: %s\n• Descripción: %s\n\nSe registrará automáticamente cada período.",
            emoji, typeText, intent.getAmount(), freqText, dayInfo, intent.getCategory(), intent.getDescription());
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
            Object amount = tx.get("amount");
            String category = (String) tx.get("category");
            String description = (String) tx.get("description");
            
            sb.append(String.format("%s $%s - %s (%s)\n", emoji, amount, category, description));
        }
        
        sb.append(String.format("\n💵 *Total del día:* $%,.0f", totalAmount));
        
        return sb.toString();
    }

    /**
     * Handles getting transactions for a date range.
     */
    @SuppressWarnings("unchecked")
    private String handleListTransactionsByRange(String userId, IntentResult intent) {
        if (useMock) {
            return "📆 Función disponible solo con el API real.\n\n🧪 _[Modo prueba]_";
        }
        
        String startDate = intent.getStartDate();
        String endDate = intent.getEndDate();
        
        if (startDate == null || endDate == null) {
            return "❌ No pude determinar el período. Por favor especifica: \"¿Cuánto gasté del 1 al 15 de noviembre?\"";
        }
        
        Map<String, Object> result = coreApi.getTransactionsByRange(userId, startDate, endDate);
        
        if (result.containsKey("error")) {
            return "❌ No pude obtener las transacciones. " + result.get("error");
        }
        
        List<Map<String, Object>> transactions = (List<Map<String, Object>>) result.get("data");
        Double totalAmount = result.get("totalAmount") != null ? ((Number) result.get("totalAmount")).doubleValue() : 0.0;
        Integer count = result.get("count") != null ? ((Number) result.get("count")).intValue() : 0;
        
        if (transactions == null || transactions.isEmpty()) {
            return String.format("📆 No tienes transacciones entre %s y %s", formatDate(startDate), formatDate(endDate));
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📆 *Transacciones del %s al %s:*\n\n", formatDate(startDate), formatDate(endDate)));
        
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
            String category = (String) tx.get("category");
            String description = (String) tx.get("description");
            // Use extractDateFromTransaction to handle both createdAt and date fields
            String dateStr = extractDateFromTransaction(tx);
            
            // Format: 💸 $10,000 - gaseosa (Otros) - 02/12/2025
            String descText = description != null && !description.isEmpty() ? description : category;
            sb.append(String.format("%s $%,.0f - %s (%s) - %s\n", 
                emoji, amt, descText, category, dateStr));
            shown++;
        }
        
        if (transactions.size() > 10) {
            sb.append(String.format("\n... y %d transacciones más\n", transactions.size() - 10));
        }
        
        sb.append(String.format("\n📊 *Resumen:*\n• Transacciones: %d\n• 💰 Ingresos: $%,.0f\n• 💸 Gastos: $%,.0f\n• 📈 Balance: $%,.0f", 
            transactions.size(), totalIncome, totalExpense, totalIncome - totalExpense));
        
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
            Object amount = tx.get("amount");
            String description = (String) tx.get("description");
            String dateStr = extractDateFromTransaction(tx);
            
            sb.append(String.format("%s $%s - %s %s\n", emoji, amount, description, dateStr));
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
     * Handles getting expense summary by category.
     */
    @SuppressWarnings("unchecked")
    private String handleGetSummary(String userId, IntentResult intent) {
        if (useMock) {
            return "📊 Función disponible solo con el API real.\n\n🧪 _[Modo prueba]_";
        }
        
        Map<String, Object> result;
        String startDate = intent.getStartDate();
        String endDate = intent.getEndDate();
        
        // If dates provided, use them; otherwise call without dates (returns all history)
        if (startDate != null && endDate != null) {
            result = coreApi.getTransactionSummaryByCategory(userId, startDate, endDate);
        } else {
            result = coreApi.getTransactionSummaryByCategory(userId);
        }
        
        if (result.containsKey("error")) {
            return "❌ No pude obtener el resumen. " + result.get("error");
        }
        
        List<Map<String, Object>> categories = (List<Map<String, Object>>) result.get("data");
        Double grandTotal = result.get("grandTotal") != null ? ((Number) result.get("grandTotal")).doubleValue() : 0.0;
        
        if (categories == null || categories.isEmpty()) {
            return "📊 No tienes gastos registrados para mostrar un resumen.";
        }
        
        StringBuilder sb = new StringBuilder("📊 *Resumen de gastos por categoría:*\n\n");
        
        for (Map<String, Object> cat : categories) {
            String category = (String) cat.get("category");
            Double totalAmount = ((Number) cat.get("totalAmount")).doubleValue();
            Double percentage = cat.get("percentage") != null ? ((Number) cat.get("percentage")).doubleValue() : 0.0;
            
            String bar = generateProgressBar(percentage);
            sb.append(String.format("• %s: $%,.0f (%s %.1f%%)\n", category, totalAmount, bar, percentage));
        }
        
        sb.append(String.format("\n💵 *Total:* $%,.0f", grandTotal));
        
        return sb.toString();
    }

    /**
     * Handles getting cashflow (recurring income vs expenses).
     */
    @SuppressWarnings("unchecked")
    private String handleGetCashflow(String userId) {
        if (useMock) {
            return "💵 Función disponible solo con el API real.\n\n🧪 _[Modo prueba]_";
        }
        
        Map<String, Object> result = coreApi.getCashflow(userId);
        
        if (result.containsKey("error")) {
            return "❌ No pude obtener el flujo de caja. " + result.get("error");
        }
        
        Double monthlyIncome = result.get("totalMonthlyIncome") != null ? ((Number) result.get("totalMonthlyIncome")).doubleValue() : 0.0;
        Double monthlyExpenses = result.get("totalMonthlyExpenses") != null ? ((Number) result.get("totalMonthlyExpenses")).doubleValue() : 0.0;
        Double netCashflow = result.get("netMonthlyCashflow") != null ? ((Number) result.get("netMonthlyCashflow")).doubleValue() : 0.0;
        
        StringBuilder sb = new StringBuilder("💵 *Tu flujo de caja mensual:*\n\n");
        sb.append(String.format("📈 Ingresos fijos: $%,.0f\n", monthlyIncome));
        sb.append(String.format("📉 Gastos fijos: $%,.0f\n", monthlyExpenses));
        sb.append(String.format("\n💰 *Dinero libre mensual:* $%,.0f", netCashflow));
        
        if (netCashflow > 0) {
            sb.append("\n\n✅ ¡Excelente! Tienes un flujo positivo.");
        } else if (netCashflow < 0) {
            sb.append("\n\n⚠️ Cuidado: tus gastos fijos superan tus ingresos fijos.");
        }
        
        return sb.toString();
    }

    /**
     * Handles listing recurring transactions.
     */
    @SuppressWarnings("unchecked")
    private String handleListRecurring(String userId) {
        if (useMock) {
            return "🔄 Función disponible solo con el API real.\n\n🧪 _[Modo prueba]_";
        }
        
        Map<String, Object> result = coreApi.getRecurringTransactions(userId);
        
        if (result.containsKey("error")) {
            return "❌ No pude obtener las transacciones recurrentes. " + result.get("error");
        }
        
        List<Map<String, Object>> recurring = (List<Map<String, Object>>) result.get("data");
        
        if (recurring == null || recurring.isEmpty()) {
            return "🔄 No tienes transacciones recurrentes configuradas.\n\n💡 Puedes crear una diciendo: \"Me pagan 2M cada mes\" o \"Pago Netflix mensualmente\"";
        }
        
        StringBuilder sb = new StringBuilder("🔄 *Tus transacciones recurrentes:*\n\n");
        
        for (Map<String, Object> rec : recurring) {
            String type = (String) rec.get("type");
            String emoji = "Income".equals(type) ? "💰" : "💸";
            Double amount = ((Number) rec.get("amount")).doubleValue();
            String category = (String) rec.get("category");
            String frequency = translatePeriod((String) rec.get("frequency"));
            Boolean isActive = (Boolean) rec.get("isActive");
            String status = isActive != null && isActive ? "" : " ⏸️";
            
            sb.append(String.format("%s $%,.0f - %s (%s)%s\n", emoji, amount, category, frequency, status));
        }
        
        return sb.toString();
    }

    /**
     * Handles deleting a recurring transaction.
     */
    @SuppressWarnings("unchecked")
    private String handleDeleteRecurring(String userId, IntentResult intent) {
        if (useMock) {
            return "🔄 Función disponible solo con el API real.\n\n🧪 _[Modo prueba]_";
        }
        
        // First get recurring transactions to find the one to delete
        Map<String, Object> result = coreApi.getRecurringTransactions(userId);
        
        if (result.containsKey("error")) {
            return "❌ No pude obtener las transacciones recurrentes. " + result.get("error");
        }
        
        List<Map<String, Object>> recurring = (List<Map<String, Object>>) result.get("data");
        
        if (recurring == null || recurring.isEmpty()) {
            return "🔄 No tienes transacciones recurrentes para eliminar.";
        }
        
        // Try to find by description or category
        String searchTerm = intent.getDescription() != null ? intent.getDescription() : intent.getCategory();
        Map<String, Object> toDelete = null;
        
        if (searchTerm != null) {
            for (Map<String, Object> rec : recurring) {
                String desc = (String) rec.get("description");
                String cat = (String) rec.get("category");
                if ((desc != null && desc.toLowerCase().contains(searchTerm.toLowerCase())) ||
                    (cat != null && cat.toLowerCase().contains(searchTerm.toLowerCase()))) {
                    toDelete = rec;
                    break;
                }
            }
        }
        
        // If not found by search, delete the last one
        if (toDelete == null) {
            toDelete = recurring.get(0);
        }
        
        String recId = (String) toDelete.get("id");
        Map<String, Object> deleteResult = coreApi.deleteRecurringTransaction(recId);
        
        if (deleteResult.containsKey("error")) {
            return "❌ No pude eliminar la transacción recurrente. " + deleteResult.get("error");
        }
        
        String category = (String) toDelete.get("category");
        Double amount = ((Number) toDelete.get("amount")).doubleValue();
        
        return String.format("✅ Transacción recurrente eliminada!\n• %s: $%,.0f", category, amount);
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
