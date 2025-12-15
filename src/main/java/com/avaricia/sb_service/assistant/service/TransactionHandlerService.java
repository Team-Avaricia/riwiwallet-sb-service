package com.avaricia.sb_service.assistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.avaricia.sb_service.assistant.dto.IntentResult;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for handling transaction operations.
 * Manages CRUD operations for user transactions (income/expenses).
 */
@Service
public class TransactionHandlerService {

    private static final Logger log = LoggerFactory.getLogger(TransactionHandlerService.class);

    private final CoreApiService coreApi;
    private final MockCoreApiService mockCoreApi;
    private final ResponseFormatterService formatter;
    private final ConfirmationService confirmationService;
    private final boolean useMock;

    public TransactionHandlerService(
            CoreApiService coreApi,
            MockCoreApiService mockCoreApi,
            ResponseFormatterService formatter,
            ConfirmationService confirmationService,
            @Value("${ms.core.use-mock:false}") boolean useMock) {
        this.coreApi = coreApi;
        this.mockCoreApi = mockCoreApi;
        this.formatter = formatter;
        this.confirmationService = confirmationService;
        this.useMock = useMock;
    }

    /**
     * Handles transaction creation (expense or income).
     * Includes default values for category and description when not provided.
     * Validates that amount is valid before calling the API.
     * 
     * For amounts > $1,000,000, requires confirmation before proceeding.
     * 
     * @param userId The system user ID
     * @param intent The intent with transaction details
     * @param type "Expense" or "Income"
     * @param telegramId The Telegram user ID (for confirmation tracking)
     * @return Response message or confirmation request
     */
    public String handleCreateTransaction(String userId, IntentResult intent, String type, Long telegramId) {
        // Validate that amount exists and is greater than 0
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
        if (amount > 100_000_000_000.0) {
            log.warn("⚠️ Extremely high amount detected: {} for user {}", amount, userId);
        }
        
        // Set default category if not provided
        String category = intent.getCategory();
        if (category == null || category.isEmpty()) {
            category = "Otros";
        }
        intent.setCategory(category);
        
        // Set default description if not provided
        String description = intent.getDescription();
        if (description == null || description.isEmpty()) {
            description = "Expense".equals(type) ? "Gasto registrado" : "Ingreso registrado";
        }
        intent.setDescription(description);
        
        // Check if amount requires confirmation (> 1,000,000)
        if (confirmationService.requiresConfirmation(amount)) {
            log.info("⚠️ High-value transaction detected: ${} - requesting confirmation", String.format("%,.0f", amount));
            String actionType = "Expense".equals(type) ? "create_expense" : "create_income";
            return confirmationService.createPendingAction(actionType, intent, userId, telegramId);
        }
        
        // Execute transaction directly (amount <= 1,000,000)
        return executeTransaction(userId, intent, type);
    }

    /**
     * Handles transaction creation without confirmation (for backwards compatibility).
     * Delegates to the main method with null telegramId (no confirmation support).
     */
    public String handleCreateTransaction(String userId, IntentResult intent, String type) {
        return handleCreateTransaction(userId, intent, type, null);
    }

    /**
     * Executes a transaction after confirmation or for amounts that don't require confirmation.
     * This is the actual transaction creation logic.
     */
    public String executeTransaction(String userId, IntentResult intent, String type) {
        Double amount = intent.getAmount();
        String category = intent.getCategory();
        String description = intent.getDescription();

        log.debug("💾 Executing transaction: {} ${} for user {}", type, String.format("%,.0f", amount), userId);

        Map<String, Object> result = useMock
            ? mockCoreApi.createTransaction(userId, amount, type, category, description)
            : coreApi.createTransaction(userId, amount, type, category, description);
        
        if (result.containsKey("error")) {
            log.error("❌ Transaction failed for user {}: {}", userId, result.get("error"));
            return "❌ No pude registrar la transacción. " + result.get("error");
        }
        
        String emoji = "Expense".equals(type) ? "💸" : "💰";
        String typeText = "Expense".equals(type) ? "Gasto" : "Ingreso";
        String modeIndicator = formatter.getMockIndicator(useMock);
        
        // Add extra info for high-value transactions
        String highValueNote = "";
        if (amount > ConfirmationService.CONFIRMATION_THRESHOLD) {
            highValueNote = "\n\n✅ _Transacción de alto valor confirmada_";
        }
        
        // Show balance in mock mode
        String balanceInfo = "";
        if (useMock) {
            Double balance = mockCoreApi.getBalance(userId);
            balanceInfo = String.format("\n\n💳 Saldo actual: $%,.0f", balance);
        }

        log.info("✅ Transaction created: {} ${} in {} for user {}", type, String.format("%,.0f", amount), category, userId);
        
        return String.format("%s %s registrado!\n• Monto: $%,.0f\n• Categoría: %s\n• Descripción: %s%s%s%s",
            emoji,
            typeText,
            amount,
            category,
            description,
            highValueNote,
            balanceInfo,
            modeIndicator
        );
    }

    /**
     * Creates a transaction silently (minimal response for batch operations).
     */
    public String handleCreateTransactionSilent(String userId, IntentResult intent, String type) {
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
     * Handles listing user transactions with optional type filter.
     */
    @SuppressWarnings("unchecked")
    public String handleListTransactions(String userId, IntentResult intent) {
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
            result = coreApi.getTransactions(userId, filterType);
        }
        
        if (result.containsKey("error")) {
            return "❌ No pude obtener las transacciones. " + result.get("error");
        }
        
        List<Map<String, Object>> transactions = (List<Map<String, Object>>) result.get("data");
        
        if (transactions == null || transactions.isEmpty()) {
            if (filterType != null) {
                String typeText = "Income".equals(filterType) ? "ingresos" : "gastos";
                return "📋 No tienes " + typeText + " registrados." + formatter.getMockIndicator(useMock);
            }
            return "📋 No tienes transacciones registradas aún." + formatter.getMockIndicator(useMock);
        }
        
        String title = filterType == null ? "Tus transacciones" :
            ("Income".equals(filterType) ? "Tus ingresos" : "Tus gastos");
        
        StringBuilder sb = new StringBuilder("📋 *" + title + ":*\n\n");
        int count = 0;
        int maxToShow = 15;
        double totalIncome = 0;
        double totalExpense = 0;
        
        for (Map<String, Object> tx : transactions) {
            String type = (String) tx.get("type");
            double amt = ((Number) tx.get("amount")).doubleValue();
            
            if ("Income".equals(type)) totalIncome += amt;
            else totalExpense += amt;
            
            if (count >= maxToShow) continue;
            
            String emoji = "Expense".equals(type) ? "💸" : "💰";
            Object amountObj = tx.get("amount");
            String categoryTx = (String) tx.get("category");
            String descriptionTx = (String) tx.get("description");
            String dateStr = extractDateFromTransaction(tx);
            
            String descText = descriptionTx != null && !descriptionTx.isEmpty() ? descriptionTx : categoryTx;
            sb.append(String.format("%s $%,.0f - %s (%s) - %s\n", 
                emoji, ((Number) amountObj).doubleValue(), descText, categoryTx, dateStr));
            count++;
        }
        
        if (transactions.size() > maxToShow) {
            sb.append(String.format("\n... y %d transacciones más\n", transactions.size() - maxToShow));
        }
        
        // Add summary based on filter type
        if (filterType == null) {
            sb.append(String.format("\n📊 *Resumen:*\n• Total: %d transacciones\n• 💰 Ingresos: $%,.0f\n• 💸 Gastos: $%,.0f\n• 📈 Balance: $%,.0f", 
                transactions.size(), totalIncome, totalExpense, totalIncome - totalExpense));
        } else if ("Income".equals(filterType)) {
            sb.append(String.format("\n📊 *Total ingresos:* $%,.0f (%d transacciones)", 
                totalIncome, transactions.size()));
        } else {
            sb.append(String.format("\n📊 *Total gastos:* $%,.0f (%d transacciones)", 
                totalExpense, transactions.size()));
        }
        
        if (useMock) {
            sb.append("\n🧪 _[Modo prueba]_");
        }
        
        return sb.toString();
    }

    /**
     * Handles getting transactions for a specific date.
     */
    @SuppressWarnings("unchecked")
    public String handleListTransactionsByDate(String userId, IntentResult intent) {
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
            return String.format("📅 No tienes transacciones registradas el %s", formatter.formatDate(date));
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📅 *Transacciones del %s:*\n\n", formatter.formatDate(date)));
        
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
     * Supports type filtering (Income/Expense) based on intent.
     */
    @SuppressWarnings("unchecked")
    public String handleListTransactionsByRange(String userId, IntentResult intent) {
        if (useMock) {
            return "📆 Función disponible solo con el API real.\n\n🧪 _[Modo prueba]_";
        }
        
        String startDate = intent.getStartDate();
        String endDate = intent.getEndDate();
        String filterType = intent.getType();
        
        if (startDate == null || endDate == null) {
            return "❌ No pude determinar el período. Por favor especifica: \"¿Cuánto gasté del 1 al 15 de noviembre?\"";
        }
        
        Map<String, Object> result = coreApi.getTransactionsByRange(userId, startDate, endDate, filterType);
        
        if (result.containsKey("error")) {
            return "❌ No pude obtener las transacciones. " + result.get("error");
        }
        
        List<Map<String, Object>> transactions = (List<Map<String, Object>>) result.get("data");
        
        if (transactions == null || transactions.isEmpty()) {
            String typeText = filterType == null ? "transacciones" : 
                ("Income".equals(filterType) ? "ingresos" : "gastos");
            return String.format("📆 No tienes %s entre %s y %s", typeText, formatter.formatDate(startDate), formatter.formatDate(endDate));
        }

        String title = filterType == null ? "Transacciones" :
            ("Income".equals(filterType) ? "Ingresos" : "Gastos");
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📆 *%s del %s al %s:*\n\n", title, formatter.formatDate(startDate), formatter.formatDate(endDate)));
        
        int shown = 0;
        double totalIncome = 0;
        double totalExpense = 0;
        
        for (Map<String, Object> tx : transactions) {
            String type = (String) tx.get("type");
            double amt = ((Number) tx.get("amount")).doubleValue();
            
            if ("Income".equals(type)) totalIncome += amt;
            else totalExpense += amt;
            
            if (shown >= 10) continue;
            
            String emoji = "Expense".equals(type) ? "💸" : "💰";
            String categoryTx = (String) tx.get("category");
            String descriptionTx = (String) tx.get("description");
            String dateStr = extractDateFromTransaction(tx);
            
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
            sb.append(String.format("\n📊 *Resumen:*\n• Transacciones: %d\n• 💰 Ingresos: $%,.0f\n• 💸 Gastos: $%,.0f\n• 📈 Balance: $%,.0f", 
                transactions.size(), totalIncome, totalExpense, totalIncome - totalExpense));
        } else if ("Income".equals(filterType)) {
            sb.append(String.format("\n📊 *Total ingresos:* $%,.0f (%d transacciones)", 
                totalIncome, transactions.size()));
        } else {
            sb.append(String.format("\n📊 *Total gastos:* $%,.0f (%d transacciones)", 
                totalExpense, transactions.size()));
        }
        
        return sb.toString();
    }

    /**
     * Handles searching transactions by description or category.
     */
    @SuppressWarnings("unchecked")
    public String handleSearchTransactions(String userId, IntentResult intent) {
        if (useMock) {
            return "🔍 Función disponible solo con el API real.\n\n🧪 _[Modo prueba]_";
        }
        
        // Check both searchQuery and category - use whichever is available
        String query = intent.getSearchQuery();
        String category = intent.getCategory();
        String searchTerm = null;
        boolean isCategory = false;
        
        if (query != null && !query.isEmpty()) {
            searchTerm = query;
        } else if (category != null && !category.isEmpty()) {
            searchTerm = category;
            isCategory = true;
        }
        
        if (searchTerm == null || searchTerm.isEmpty()) {
            return "❌ No pude determinar qué buscar. Por favor especifica: \"¿Cuánto he pagado de Netflix?\" o \"Gastos de categoría Comida\"";
        }
        
        Map<String, Object> result = coreApi.searchTransactions(userId, searchTerm);
        
        if (result.containsKey("error")) {
            return "❌ No pude buscar las transacciones. " + result.get("error");
        }
        
        List<Map<String, Object>> transactions = (List<Map<String, Object>>) result.get("data");
        Double totalAmount = result.get("totalAmount") != null ? ((Number) result.get("totalAmount")).doubleValue() : 0.0;
        Integer count = result.get("count") != null ? ((Number) result.get("count")).intValue() : 0;
        
        if (transactions == null || transactions.isEmpty()) {
            String errorMsg = isCategory 
                ? String.format("🔍 No encontré transacciones en la categoría \"%s\"", searchTerm)
                : String.format("🔍 No encontré transacciones relacionadas con \"%s\"", searchTerm);
            return errorMsg;
        }
        
        StringBuilder sb = new StringBuilder();
        String title = isCategory 
            ? String.format("🔍 *Gastos en categoría \"%s\":*\n\n", searchTerm)
            : String.format("🔍 *Resultados para \"%s\":*\n\n", searchTerm);
        sb.append(title);
        
        for (Map<String, Object> tx : transactions) {
            String type = (String) tx.get("type");
            String emoji = "Expense".equals(type) ? "💸" : "💰";
            Object amountObj = tx.get("amount");
            String descriptionTx = (String) tx.get("description");
            String dateStr = extractDateFromTransaction(tx);
            
            sb.append(String.format("%s $%s - %s %s\n", emoji, amountObj, descriptionTx, dateStr));
        }
        
        String totalLabel = isCategory 
            ? String.format("\n\n📊 *Total en categoría \"%s\":* $%,.0f (%d transacciones)", searchTerm, totalAmount, count)
            : String.format("\n\n📊 *Total en \"%s\":* $%,.0f (%d transacciones)", searchTerm, totalAmount, count);
        sb.append(totalLabel);
        
        return sb.toString();
    }

    /**
     * Handles deleting the last transaction.
     */
    @SuppressWarnings("unchecked")
    public String handleDeleteTransaction(String userId) {
        // First get transactions to find the last one
        Map<String, Object> result = useMock
            ? mockCoreApi.getTransactions(userId)
            : coreApi.getTransactions(userId);
        
        if (result.containsKey("error")) {
            return "❌ No pude obtener las transacciones. " + result.get("error");
        }
        
        List<Map<String, Object>> transactions = (List<Map<String, Object>>) result.get("data");
        
        if (transactions == null || transactions.isEmpty()) {
            return "📋 No tienes transacciones para eliminar." + formatter.getMockIndicator(useMock);
        }
        
        // Get the last transaction (first in the list - sorted by createdAt desc)
        Map<String, Object> lastTx = transactions.get(0);
        String txId = (String) lastTx.get("id");
        
        // Delete the transaction
        Map<String, Object> deleteResult = useMock
            ? mockCoreApi.deleteTransaction(txId)
            : coreApi.deleteTransaction(txId);
        
        if (deleteResult.containsKey("error")) {
            return "❌ No pude eliminar la transacción. " + deleteResult.get("error");
        }
        
        // Build a user-friendly response
        String type = (String) lastTx.get("type");
        String emoji = "Income".equals(type) ? "💰" : "💸";
        Double amountDeleted = lastTx.get("amount") != null ? ((Number) lastTx.get("amount")).doubleValue() : 0.0;
        String descriptionDeleted = (String) lastTx.get("description");
        String categoryDeleted = (String) lastTx.get("category");
        String typeText = "Income".equals(type) ? "ingreso" : "gasto";
        String modeIndicator = formatter.getMockIndicator(useMock);
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("✅ ¡Listo! Eliminé tu último %s:\n\n", typeText));
        sb.append(String.format("%s *$%,.0f*\n", emoji, amountDeleted));
        
        if (descriptionDeleted != null && !descriptionDeleted.isEmpty()) {
            sb.append(String.format("• Descripción: %s\n", descriptionDeleted));
        }
        sb.append(String.format("• Categoría: %s\n", categoryDeleted));
        sb.append("\n📝 Tu saldo ha sido restaurado.");
        sb.append(modeIndicator);
        
        return sb.toString();
    }

    /**
     * Calculates how much the user has spent in a specific category for the given period.
     * Used for budget validation and expense recommendations.
     */
    @SuppressWarnings("unchecked")
    public double calculateSpentInPeriod(String userId, String category, String period, Map<String, Object> transactionsResult) {
        LocalDate now = LocalDate.now();
        LocalDate startDate;
        
        if (period == null) period = "Monthly";
        
        switch (period.toLowerCase()) {
            case "weekly":
                startDate = now.with(DayOfWeek.MONDAY);
                break;
            case "biweekly":
                int dayOfMonth = now.getDayOfMonth();
                if (dayOfMonth >= 15) {
                    startDate = now.withDayOfMonth(15);
                } else {
                    startDate = now.withDayOfMonth(1);
                }
                break;
            case "yearly":
                startDate = now.withDayOfYear(1);
                break;
            case "monthly":
            default:
                startDate = now.withDayOfMonth(1);
                break;
        }
        
        // If we already have transactions, filter them
        if (transactionsResult != null && transactionsResult.containsKey("data")) {
            List<Map<String, Object>> transactions = (List<Map<String, Object>>) transactionsResult.get("data");
            if (transactions == null) {
                transactions = (List<Map<String, Object>>) transactionsResult.get("success");
            }
            if (transactions != null) {
                double total = 0;
                for (Map<String, Object> tx : transactions) {
                    String txType = (String) tx.get("type");
                    String txCategory = (String) tx.get("category");
                    
                    // Only count expenses in matching category (or "General" which matches all)
                    if ("Expense".equalsIgnoreCase(txType) && 
                        (category.equalsIgnoreCase(txCategory) || "General".equalsIgnoreCase(category))) {
                        
                        String createdAt = extractDateFromTransaction(tx);
                        if (createdAt != null && !createdAt.isEmpty()) {
                            try {
                                // Parse DD/MM/YYYY format
                                String[] parts = createdAt.split("/");
                                if (parts.length == 3) {
                                    LocalDate txDate = LocalDate.of(
                                        Integer.parseInt(parts[2]),
                                        Integer.parseInt(parts[1]),
                                        Integer.parseInt(parts[0])
                                    );
                                    if (!txDate.isBefore(startDate)) {
                                        Object amountObj = tx.get("amount");
                                        if (amountObj instanceof Number) {
                                            total += ((Number) amountObj).doubleValue();
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                // Skip transactions with invalid dates
                            }
                        }
                    }
                }
                return total;
            }
        }
        
        // Fallback: fetch transactions for the specific range from API
        if (!useMock) {
            String startDateStr = startDate.toString();
            String endDateStr = now.toString();
            Map<String, Object> rangeResult = coreApi.getTransactionsByRange(userId, startDateStr, endDateStr, "Expense");
            
            if (!rangeResult.containsKey("error") && rangeResult.containsKey("data")) {
                List<Map<String, Object>> transactions = (List<Map<String, Object>>) rangeResult.get("data");
                double total = 0;
                for (Map<String, Object> tx : transactions) {
                    String txCategory = (String) tx.get("category");
                    if (category.equalsIgnoreCase(txCategory) || "General".equalsIgnoreCase(category)) {
                        Object amountObj = tx.get("amount");
                        if (amountObj instanceof Number) {
                            total += ((Number) amountObj).doubleValue();
                        }
                    }
                }
                return total;
            }
        }
        
        return 0;
    }

    /**
     * Gets transactions for a user (for internal use by other services).
     */
    public Map<String, Object> getTransactionsForUser(String userId) {
        return useMock ? mockCoreApi.getTransactions(userId) : coreApi.getTransactions(userId);
    }

    /**
     * Extracts and formats the date from a transaction.
     * Handles both 'createdAt' (API format) and 'date' field names.
     */
    public String extractDateFromTransaction(Map<String, Object> tx) {
        Object dateObj = tx.get("createdAt");
        if (dateObj == null) {
            dateObj = tx.get("date");
        }
        if (dateObj == null) {
            return "";
        }
        return formatter.formatDateFromApi(dateObj.toString());
    }
}
