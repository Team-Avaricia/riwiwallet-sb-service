package com.avaricia.sb_service.assistant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.avaricia.sb_service.assistant.dto.IntentResult;

import java.util.List;
import java.util.Map;

/**
 * Service responsible for handling balance and summary queries.
 * Manages financial overview, balance checks, and summary reports.
 */
@Service
public class QueryHandlerService {

    private final CoreApiService coreApi;
    private final MockCoreApiService mockCoreApi;
    private final ResponseFormatterService formatter;
    private final boolean useMock;

    public QueryHandlerService(
            CoreApiService coreApi,
            MockCoreApiService mockCoreApi,
            ResponseFormatterService formatter,
            @Value("${ms.core.use-mock:false}") boolean useMock) {
        this.coreApi = coreApi;
        this.mockCoreApi = mockCoreApi;
        this.formatter = formatter;
        this.useMock = useMock;
    }

    /**
     * Handles getting user's current balance.
     */
    public String handleGetBalance(String userId) {
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
     * Provides conversational and contextual responses.
     */
    @SuppressWarnings("unchecked")
    public String handleGetSummary(String userId, IntentResult intent) {
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
            String topCatEmoji = formatter.getCategoryEmoji(topCatName);
            
            // Conversational intro based on the top category percentage
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
            
            // Balance summary line
            sb.append("💰 Ingresos: $").append(String.format("%,.0f", totalIncome));
            sb.append(" | 💸 Gastos: $").append(String.format("%,.0f", totalExpenses));
            sb.append(" | 💵 Saldo: *$").append(String.format("%,.0f", currentBalance)).append("*\n\n");
            
            sb.append("📉 *Desglose completo:*\n");
            
            for (Map<String, Object> cat : categories) {
                String categoryName = (String) cat.get("category");
                Double amount = ((Number) cat.get("totalAmount")).doubleValue();
                Double percentage = cat.get("percentage") != null ? ((Number) cat.get("percentage")).doubleValue() : 0.0;
                String emoji = formatter.getCategoryEmoji(categoryName);
                
                String bar = formatter.generateProgressBar(percentage);
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
}
