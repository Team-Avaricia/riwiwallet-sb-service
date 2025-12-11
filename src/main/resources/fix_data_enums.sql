-- ============================================
-- SCRIPT DE CORRECIÓN DE ENUMS (UPPERCASE -> PascalCase)
-- Para compatibilidad con Backend .NET
-- ============================================

-- 1. Transactions
-- TransactionType
UPDATE public."Transactions" SET "Type" = 'Income' WHERE "Type" = 'INCOME';
UPDATE public."Transactions" SET "Type" = 'Expense' WHERE "Type" = 'EXPENSE';
-- TransactionSource
UPDATE public."Transactions" SET "Source" = 'Manual' WHERE "Source" = 'MANUAL';

-- 2. FinancialRules
-- RuleType
UPDATE public."FinancialRules" SET "Type" = 'MonthlyBudget' WHERE "Type" = 'BUDGET';
UPDATE public."FinancialRules" SET "Type" = 'SavingsGoal' WHERE "Type" = 'SAVING_GOAL';
UPDATE public."FinancialRules" SET "Type" = 'CategoryLimit' WHERE "Type" = 'ALERT';
-- RulePeriod
UPDATE public."FinancialRules" SET "Period" = 'Daily' WHERE "Period" = 'DAILY';
UPDATE public."FinancialRules" SET "Period" = 'Weekly' WHERE "Period" = 'WEEKLY';
UPDATE public."FinancialRules" SET "Period" = 'Monthly' WHERE "Period" = 'MONTHLY';
UPDATE public."FinancialRules" SET "Period" = 'Yearly' WHERE "Period" = 'YEARLY';
