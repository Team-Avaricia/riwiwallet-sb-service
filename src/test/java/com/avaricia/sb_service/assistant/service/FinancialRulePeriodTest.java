package com.avaricia.sb_service.assistant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Financial Rules Period Calculation.
 * Verifies that budget periods reset correctly based on the configured period type.
 */
class FinancialRulePeriodTest {

    /**
     * Simulates the period calculation logic from TransactionHandlerService.
     * This is extracted for testing purposes.
     * Note: Daily period was removed - not practical for budget tracking.
     */
    private LocalDate calculatePeriodStartDate(String period) {
        LocalDate now = LocalDate.now();
        
        if (period == null) period = "Monthly";
        
        switch (period.toLowerCase()) {
            case "weekly":
                return now.with(DayOfWeek.MONDAY);
            case "biweekly":
                int dayOfMonth = now.getDayOfMonth();
                if (dayOfMonth >= 15) {
                    return now.withDayOfMonth(15);
                } else {
                    return now.withDayOfMonth(1);
                }
            case "yearly":
                return now.withDayOfYear(1);
            case "monthly":
            default:
                return now.withDayOfMonth(1);
        }
    }
    
    /**
     * Simulates calculating total spending in a period.
     */
    private double calculateSpentInPeriod(List<Map<String, Object>> transactions, 
                                          String category, 
                                          LocalDate periodStart) {
        double total = 0;
        for (Map<String, Object> tx : transactions) {
            String txType = (String) tx.get("type");
            String txCategory = (String) tx.get("category");
            LocalDate txDate = (LocalDate) tx.get("date");
            
            if ("Expense".equals(txType) && 
                (category.equalsIgnoreCase(txCategory) || "General".equalsIgnoreCase(category))) {
                if (!txDate.isBefore(periodStart)) {
                    total += ((Number) tx.get("amount")).doubleValue();
                }
            }
        }
        return total;
    }
    
    /**
     * Helper to create a test transaction.
     */
    private Map<String, Object> createTransaction(String type, String category, double amount, LocalDate date) {
        Map<String, Object> tx = new HashMap<>();
        tx.put("type", type);
        tx.put("category", category);
        tx.put("amount", amount);
        tx.put("date", date);
        return tx;
    }

    // ==================== WEEKLY PERIOD TESTS ====================

    @Test
    @DisplayName("Weekly period - Counts from Monday of current week")
    void weeklyPeriod_CountsFromMonday() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate lastSunday = monday.minusDays(1);
        
        List<Map<String, Object>> transactions = Arrays.asList(
            createTransaction("Expense", "Transporte", 50000, lastSunday),  // Should NOT count (previous week)
            createTransaction("Expense", "Transporte", 30000, monday),       // Should count
            createTransaction("Expense", "Transporte", 20000, today)         // Should count
        );
        
        LocalDate periodStart = calculatePeriodStartDate("weekly");
        double spent = calculateSpentInPeriod(transactions, "Transporte", periodStart);
        
        assertEquals(50000, spent, "Weekly period should count from Monday");
        System.out.println("✅ Weekly period test passed: $" + spent + " counted (since Monday " + monday + ")");
    }

    // ==================== BIWEEKLY PERIOD TESTS ====================

    @Test
    @DisplayName("Biweekly period - First half of month (days 1-14)")
    void biweeklyPeriod_FirstHalf() {
        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        LocalDate day5 = today.withDayOfMonth(5);
        LocalDate day14 = today.withDayOfMonth(14);
        
        // Simulate we are on day 10 (first half)
        List<Map<String, Object>> transactions = Arrays.asList(
            createTransaction("Expense", "Entretenimiento", 100000, firstOfMonth.minusDays(1)), // Previous month - NOT count
            createTransaction("Expense", "Entretenimiento", 50000, day5),                       // Should count
            createTransaction("Expense", "Entretenimiento", 30000, day14)                       // Should count (still first half)
        );
        
        // Force period start to first of month (simulating day 1-14)
        LocalDate periodStart = firstOfMonth;
        double spent = calculateSpentInPeriod(transactions, "Entretenimiento", periodStart);
        
        assertEquals(80000, spent, "Biweekly (first half) should count from day 1");
        System.out.println("✅ Biweekly (first half) test passed: $" + spent + " counted");
    }

    @Test
    @DisplayName("Biweekly period - Second half of month (days 15-end)")
    void biweeklyPeriod_SecondHalf() {
        LocalDate today = LocalDate.now();
        LocalDate day15 = today.withDayOfMonth(15);
        LocalDate day20 = today.withDayOfMonth(20);
        LocalDate day10 = today.withDayOfMonth(10);
        
        List<Map<String, Object>> transactions = Arrays.asList(
            createTransaction("Expense", "Ropa", 200000, day10),  // First half - NOT count in second half period
            createTransaction("Expense", "Ropa", 150000, day15),  // Should count
            createTransaction("Expense", "Ropa", 100000, day20)   // Should count
        );
        
        // Force period start to day 15 (simulating day 15-end)
        LocalDate periodStart = day15;
        double spent = calculateSpentInPeriod(transactions, "Ropa", periodStart);
        
        assertEquals(250000, spent, "Biweekly (second half) should count from day 15");
        System.out.println("✅ Biweekly (second half) test passed: $" + spent + " counted");
    }

    // ==================== MONTHLY PERIOD TESTS ====================

    @Test
    @DisplayName("Monthly period - Counts from first of current month")
    void monthlyPeriod_CountsFromFirstOfMonth() {
        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        LocalDate lastMonth = firstOfMonth.minusDays(1);
        
        List<Map<String, Object>> transactions = Arrays.asList(
            createTransaction("Expense", "Comida", 500000, lastMonth),      // Last month - NOT count
            createTransaction("Expense", "Comida", 100000, firstOfMonth),   // Should count
            createTransaction("Expense", "Comida", 200000, today)           // Should count
        );
        
        LocalDate periodStart = calculatePeriodStartDate("monthly");
        double spent = calculateSpentInPeriod(transactions, "Comida", periodStart);
        
        assertEquals(300000, spent, "Monthly period should count from first of month");
        System.out.println("✅ Monthly period test passed: $" + spent + " counted (since " + firstOfMonth + ")");
    }

    @Test
    @DisplayName("Monthly period - Automatically resets on new month")
    void monthlyPeriod_ResetsOnNewMonth() {
        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        LocalDate lastMonth = firstOfMonth.minusDays(5); // 5 days before first of current month
        
        List<Map<String, Object>> transactions = Arrays.asList(
            createTransaction("Expense", "Servicios", 1000000, lastMonth),  // Last month - NOT count
            createTransaction("Expense", "Servicios", 50000, firstOfMonth)  // Current month - count
        );
        
        LocalDate periodStart = calculatePeriodStartDate("monthly");
        double spent = calculateSpentInPeriod(transactions, "Servicios", periodStart);
        
        // Only the $50,000 from current month should count
        assertEquals(50000, spent, "Monthly should reset - only current month expenses count");
        System.out.println("✅ Monthly reset test passed: Only $" + spent + " counts (last month's $1M ignored)");
    }

    // ==================== YEARLY PERIOD TESTS ====================

    @Test
    @DisplayName("Yearly period - Counts from January 1st of current year")
    void yearlyPeriod_CountsFromJanuary() {
        LocalDate today = LocalDate.now();
        LocalDate jan1 = today.withDayOfYear(1);
        LocalDate lastYear = jan1.minusDays(1);
        
        List<Map<String, Object>> transactions = Arrays.asList(
            createTransaction("Expense", "Tecnología", 2000000, lastYear),  // Last year - NOT count
            createTransaction("Expense", "Tecnología", 500000, jan1),       // Should count
            createTransaction("Expense", "Tecnología", 1000000, today)      // Should count
        );
        
        LocalDate periodStart = calculatePeriodStartDate("yearly");
        double spent = calculateSpentInPeriod(transactions, "Tecnología", periodStart);
        
        assertEquals(1500000, spent, "Yearly period should count from January 1st");
        System.out.println("✅ Yearly period test passed: $" + spent + " counted (since " + jan1 + ")");
    }

    // ==================== GENERAL CATEGORY TESTS ====================

    @Test
    @DisplayName("General category rule - Counts ALL expense categories")
    void generalCategory_CountsAllExpenses() {
        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        
        List<Map<String, Object>> transactions = Arrays.asList(
            createTransaction("Expense", "Comida", 100000, today),
            createTransaction("Expense", "Transporte", 50000, today),
            createTransaction("Expense", "Entretenimiento", 80000, today),
            createTransaction("Income", "Salario", 5000000, today)  // Income - NOT count as expense
        );
        
        LocalDate periodStart = calculatePeriodStartDate("monthly");
        double spent = calculateSpentInPeriod(transactions, "General", periodStart);
        
        assertEquals(230000, spent, "General category should count ALL expense categories");
        System.out.println("✅ General category test passed: $" + spent + " total expenses counted");
    }

    // ==================== BUDGET VALIDATION TESTS ====================

    @Test
    @DisplayName("Budget validation - Under budget scenario")
    void budgetValidation_UnderBudget() {
        double budget = 500000;  // $500k limit
        double spent = 200000;   // Already spent $200k
        double newExpense = 100000;  // Want to spend $100k more
        
        double remaining = budget - spent;
        double afterPurchase = remaining - newExpense;
        
        assertTrue(afterPurchase > 0, "Should be under budget");
        assertEquals(200000, afterPurchase, "Should have $200k remaining after purchase");
        System.out.println("✅ Under budget: Spent $" + spent + ", new $" + newExpense + ", remaining $" + afterPurchase);
    }

    @Test
    @DisplayName("Budget validation - Over budget scenario")
    void budgetValidation_OverBudget() {
        double budget = 500000;  // $500k limit
        double spent = 450000;   // Already spent $450k
        double newExpense = 100000;  // Want to spend $100k more
        
        double remaining = budget - spent;
        double afterPurchase = remaining - newExpense;
        
        assertTrue(afterPurchase < 0, "Should be over budget");
        assertEquals(-50000, afterPurchase, "Should be $50k over budget");
        System.out.println("✅ Over budget: Would exceed by $" + Math.abs(afterPurchase));
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @DisplayName("Edge case - No transactions in period")
    void edgeCase_NoTransactionsInPeriod() {
        LocalDate today = LocalDate.now();
        LocalDate lastMonth = today.minusMonths(1);
        
        List<Map<String, Object>> transactions = Arrays.asList(
            createTransaction("Expense", "Comida", 500000, lastMonth)  // All transactions are old
        );
        
        LocalDate periodStart = calculatePeriodStartDate("monthly");
        double spent = calculateSpentInPeriod(transactions, "Comida", periodStart);
        
        assertEquals(0, spent, "No transactions in current period = $0 spent");
        System.out.println("✅ No transactions test passed: $" + spent + " (budget is 100% available)");
    }

    @Test
    @DisplayName("Edge case - Empty transaction list")
    void edgeCase_EmptyTransactionList() {
        List<Map<String, Object>> transactions = new ArrayList<>();
        
        LocalDate periodStart = calculatePeriodStartDate("monthly");
        double spent = calculateSpentInPeriod(transactions, "Comida", periodStart);
        
        assertEquals(0, spent, "Empty list = $0 spent");
        System.out.println("✅ Empty list test passed: $" + spent);
    }

    @Test
    @DisplayName("Edge case - Null period defaults to Monthly")
    void edgeCase_NullPeriodDefaultsToMonthly() {
        LocalDate periodStart = calculatePeriodStartDate(null);
        LocalDate expectedMonthlyStart = LocalDate.now().withDayOfMonth(1);
        
        assertEquals(expectedMonthlyStart, periodStart, "Null period should default to monthly (first of month)");
        System.out.println("✅ Null period test passed: Defaults to " + periodStart);
    }

    // ==================== SUMMARY TEST ====================

    @Test
    @DisplayName("Full scenario - User with monthly budget rule")
    void fullScenario_MonthlyBudgetRule() {
        System.out.println("\n========== FULL SCENARIO TEST ==========");
        
        // Setup
        String category = "Comida";
        String period = "Monthly";
        double budgetLimit = 500000;  // $500k monthly limit
        
        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        LocalDate lastMonth = firstOfMonth.minusDays(5);
        
        // Simulate user transactions
        List<Map<String, Object>> transactions = Arrays.asList(
            createTransaction("Expense", "Comida", 300000, lastMonth),    // Last month - won't count
            createTransaction("Expense", "Comida", 100000, firstOfMonth), // Current month
            createTransaction("Expense", "Comida", 150000, today),        // Current month
            createTransaction("Expense", "Transporte", 50000, today),     // Different category
            createTransaction("Income", "Salario", 5000000, today)        // Income, not expense
        );
        
        // Calculate
        LocalDate periodStart = calculatePeriodStartDate(period);
        double spent = calculateSpentInPeriod(transactions, category, periodStart);
        double remaining = budgetLimit - spent;
        double percentUsed = (spent / budgetLimit) * 100;
        
        // Output
        System.out.println("📏 Budget Rule: " + category + " - $" + budgetLimit + " " + period);
        System.out.println("📅 Period Start: " + periodStart);
        System.out.println("💸 Spent this period: $" + spent);
        System.out.println("💰 Remaining budget: $" + remaining);
        System.out.println("📊 Percent used: " + percentUsed + "%");
        
        // Assertions
        assertEquals(250000, spent, "Should only count current month's Comida expenses");
        assertEquals(250000, remaining, "Should have $250k remaining");
        assertEquals(50, percentUsed, 0.1, "Should be 50% used");
        
        // Validate a new expense
        double proposedExpense = 300000;
        if (proposedExpense > remaining) {
            System.out.println("⚠️ ALERT: Proposed expense of $" + proposedExpense + 
                             " would exceed budget by $" + (proposedExpense - remaining));
        } else {
            System.out.println("✅ OK: Proposed expense of $" + proposedExpense + 
                             " is within budget. Would leave $" + (remaining - proposedExpense));
        }
        
        System.out.println("========================================\n");
    }
}
