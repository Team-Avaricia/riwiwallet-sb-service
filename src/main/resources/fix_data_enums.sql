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

-- 2. RecurringTransactions
-- TransactionType
UPDATE public."RecurringTransactions" SET "Type" = 'Income' WHERE "Type" = 'INCOME';
UPDATE public."RecurringTransactions" SET "Type" = 'Expense' WHERE "Type" = 'EXPENSE';
-- RecurrenceFrequency
UPDATE public."RecurringTransactions" SET "Frequency" = 'Daily' WHERE "Frequency" = 'DAILY';
UPDATE public."RecurringTransactions" SET "Frequency" = 'Weekly' WHERE "Frequency" = 'WEEKLY';
UPDATE public."RecurringTransactions" SET "Frequency" = 'Monthly' WHERE "Frequency" = 'MONTHLY';
UPDATE public."RecurringTransactions" SET "Frequency" = 'Yearly' WHERE "Frequency" = 'YEARLY';

-- 3. FinancialRules
-- RuleType
UPDATE public."FinancialRules" SET "Type" = 'MonthlyBudget' WHERE "Type" = 'BUDGET';
UPDATE public."FinancialRules" SET "Type" = 'SavingsGoal' WHERE "Type" = 'SAVING_GOAL';
UPDATE public."FinancialRules" SET "Type" = 'CategoryLimit' WHERE "Type" = 'ALERT';
-- RulePeriod
UPDATE public."FinancialRules" SET "Period" = 'Daily' WHERE "Period" = 'DAILY';
UPDATE public."FinancialRules" SET "Period" = 'Weekly' WHERE "Period" = 'WEEKLY';
UPDATE public."FinancialRules" SET "Period" = 'Monthly' WHERE "Period" = 'MONTHLY';
UPDATE public."FinancialRules" SET "Period" = 'Yearly' WHERE "Period" = 'YEARLY';
