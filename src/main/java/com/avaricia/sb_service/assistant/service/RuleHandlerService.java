package com.avaricia.sb_service.assistant.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.avaricia.sb_service.assistant.dto.IntentResult;

import java.util.List;
import java.util.Map;

/**
 * Service responsible for handling financial rules (limits/budgets).
 * Manages CRUD operations for user financial rules.
 */
@Service
public class RuleHandlerService {

    private final CoreApiService coreApi;
    private final MockCoreApiService mockCoreApi;
    private final ResponseFormatterService formatter;
    private final boolean useMock;

    public RuleHandlerService(
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
     * Handles creating or updating a financial rule.
     */
    @SuppressWarnings("unchecked")
    public String handleCreateRule(String userId, IntentResult intent) {
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
        
        // Check for existing rule with same category and period
        boolean isUpdate = false;
        String existingRuleId = null;
        Double oldAmount = null;
        
        if (!useMock) {
            Map<String, Object> rulesResult = coreApi.getRules(userId);
            if (!rulesResult.containsKey("error") && rulesResult.containsKey("data")) {
                List<Map<String, Object>> rules = (List<Map<String, Object>>) rulesResult.get("data");
                for (Map<String, Object> rule : rules) {
                    String ruleCategory = (String) rule.get("category");
                    String rulePeriod = (String) rule.get("period");
                    
                    // Check if same category and period (case insensitive)
                    if (ruleCategory != null && rulePeriod != null &&
                        ruleCategory.equalsIgnoreCase(category) && 
                        rulePeriod.equalsIgnoreCase(period)) {
                        existingRuleId = (String) rule.get("id");
                        oldAmount = rule.get("amountLimit") != null ? 
                            ((Number) rule.get("amountLimit")).doubleValue() : null;
                        isUpdate = true;
                        break;
                    }
                }
            }
            
            // Delete existing rule if found (to update)
            if (existingRuleId != null) {
                Map<String, Object> deleteResult = coreApi.deleteRule(existingRuleId);
                if (deleteResult.containsKey("error")) {
                    System.err.println("⚠️ Could not delete existing rule: " + deleteResult.get("error"));
                } else {
                    System.out.println("🔄 Deleted existing rule " + existingRuleId + " for update");
                }
            }
        }
        
        // Create the new rule
        Map<String, Object> result = useMock
            ? mockCoreApi.createRule(userId, "CategoryBudget", category, amount, period)
            : coreApi.createRule(userId, "CategoryBudget", category, amount, period);
        
        if (result.containsKey("error")) {
            return "❌ No pude crear la regla. " + result.get("error");
        }
        
        String periodText = formatter.translatePeriod(period);
        String modeIndicator = formatter.getMockIndicator(useMock);
        String categoryText = "General".equals(category) ? "Todos los gastos" : category;
        
        // Different message for update vs create
        if (isUpdate && oldAmount != null) {
            return String.format("📏 ¡Regla actualizada!\n\n• 📂 Categoría: %s\n• 💰 Límite anterior: $%,.0f\n• 💰 Nuevo límite: $%,.0f\n• 📅 Período: %s\n\n💡 Te avisaré cuando te acerques al límite.%s",
                categoryText,
                oldAmount,
                amount,
                periodText,
                modeIndicator
            );
        }
        
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
    public String handleListRules(String userId) {
        Map<String, Object> result = useMock
            ? mockCoreApi.getRules(userId)
            : coreApi.getRules(userId);
        
        if (result.containsKey("error")) {
            return "❌ No pude obtener las reglas. " + result.get("error");
        }
        
        List<Map<String, Object>> rules = (List<Map<String, Object>>) result.get("data");
        
        if (rules == null || rules.isEmpty()) {
            return "📏 No tienes reglas financieras configuradas." + formatter.getMockIndicator(useMock);
        }
        
        StringBuilder sb = new StringBuilder("📏 *Tus reglas financieras:*\n\n");
        
        for (Map<String, Object> rule : rules) {
            String categoryRule = (String) rule.get("category");
            Object amountLimit = rule.get("amountLimit");
            String periodRule = (String) rule.get("period");
            String periodText = formatter.translatePeriod(periodRule);
            
            sb.append(String.format("• %s: $%s (%s)\n", categoryRule, amountLimit, periodText));
        }
        
        if (useMock) {
            sb.append("\n🧪 _[Modo prueba]_");
        }
        
        return sb.toString();
    }

    /**
     * Gets rules for a user (for internal use by other services).
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRulesForUser(String userId) {
        Map<String, Object> result = useMock
            ? mockCoreApi.getRules(userId)
            : coreApi.getRules(userId);
        
        if (result.containsKey("error") || !result.containsKey("data")) {
            return List.of();
        }
        
        return (List<Map<String, Object>>) result.get("data");
    }
}
