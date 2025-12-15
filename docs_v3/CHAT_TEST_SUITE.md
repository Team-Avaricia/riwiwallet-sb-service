# 🧪 Chat Test Suite v3 - RiwiWallet Financial Assistant

> **Version:** 3.0  
> **Date:** December 2024  
> **Purpose:** Step-by-step test scenarios for the Telegram bot

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Test Flow 1: Basic Setup & Greetings](#test-flow-1-basic-setup--greetings)
3. [Test Flow 2: Transaction Registration](#test-flow-2-transaction-registration)
4. [Test Flow 3: High-Value Transaction Confirmation](#test-flow-3-high-value-transaction-confirmation)
5. [Test Flow 4: Queries & Reports](#test-flow-4-queries--reports)
6. [Test Flow 5: Financial Rules](#test-flow-5-financial-rules)
7. [Test Flow 6: Expense Validation](#test-flow-6-expense-validation)
8. [Test Flow 7: Multiple Operations](#test-flow-7-multiple-operations)
9. [Test Flow 8: Delete Operations](#test-flow-8-delete-operations)
10. [Test Flow 9: Edge Cases](#test-flow-9-edge-cases)
11. [Test Flow 10: Conversation Context](#test-flow-10-conversation-context)

---

## Prerequisites

Before starting tests:

1. **Start the .NET Core API** (on port 5003):
   ```bash
   cd Micro-Back-Brahiam
   dotnet run --project src/API/API.csproj
   ```

2. **Start the Spring Boot Service** (on port 8082):
   ```bash
   cd sb-service
   mvn spring-boot:run
   ```

3. **Link your Telegram account** to the bot using a valid LINK code.

4. **Verify both services are healthy**:
   - .NET API: `http://localhost:5003/health`
   - Spring Boot: `http://localhost:8082/actuator/health`

---

## Test Flow 1: Basic Setup & Greetings

**Objective:** Verify bot responds to basic interactions

### Messages to Send (in order):

| Step | Send This Message | Expected Response Contains |
|------|-------------------|---------------------------|
| 1.1 | `Hola` | Greeting, welcoming message |
| 1.2 | `Buenos días` | Greeting response |
| 1.3 | `¿Qué puedes hacer?` | List of capabilities (registrar gastos, ingresos, consultar saldo, etc.) |
| 1.4 | `Ayuda` | Help message with available commands |
| 1.5 | `Gracias` | Friendly acknowledgment |

---

## Test Flow 2: Transaction Registration

**Objective:** Test expense and income registration with various formats

### 2A. Register Expenses

| Step | Send This Message | Expected Response Contains |
|------|-------------------|---------------------------|
| 2.1 | `Gasté 50k en comida` | ✅ Gasto registrado, $50,000, Comida |
| 2.2 | `Compré un almuerzo de 15000` | ✅ Gasto registrado, $15,000 |
| 2.3 | `Pagué 30 mil en taxi` | ✅ Gasto registrado, $30,000, Transporte |
| 2.4 | `Pagué Netflix 25k` | ✅ Gasto registrado, $25,000, Entretenimiento |
| 2.5 | `Compré medicinas por 35000` | ✅ Gasto registrado, $35,000, Salud |

### 2B. Register Incomes

| Step | Send This Message | Expected Response Contains |
|------|-------------------|---------------------------|
| 2.6 | `Recibí mi sueldo de 2M` | ✅ Ingreso registrado, $2,000,000, Salario |
| 2.7 | `Me pagaron 500k por un trabajo` | ✅ Ingreso registrado, $500,000, Freelance |
| 2.8 | `Gané 100k en una apuesta` | ✅ Ingreso registrado, $100,000 |
| 2.9 | `Me regalaron 50k` | ✅ Ingreso registrado, $50,000, Regalos |

### 2C. Number Format Variations

| Step | Send This Message | Expected Amount |
|------|-------------------|-----------------|
| 2.10 | `Gasté 80K en ropa` | $80,000 |
| 2.11 | `Pagué 1.5M de arriendo` | $1,500,000 |
| 2.12 | `Compré algo de 150.000 pesos` | $150,000 |
| 2.13 | `Me cobraron doscientos mil` | $200,000 |

---

## Test Flow 3: High-Value Transaction Confirmation

**Objective:** Verify transactions over $1,000,000 require confirmation

### 3A. Expense Confirmation Flow

| Step | Send This Message | Expected Response |
|------|-------------------|-------------------|
| 3.1 | `Gasté 2M en un televisor` | ⚠️ Confirmation request for $2,000,000 |
| 3.2 | `Sí` or `Confirmar` | ✅ Transaction confirmed and registered |

### 3B. Expense Cancellation Flow

| Step | Send This Message | Expected Response |
|------|-------------------|-------------------|
| 3.3 | `Pagué 5M en muebles` | ⚠️ Confirmation request for $5,000,000 |
| 3.4 | `No` or `Cancelar` | ❌ Transaction cancelled |

### 3C. Income Confirmation Flow

| Step | Send This Message | Expected Response |
|------|-------------------|-------------------|
| 3.5 | `Recibí 3M de bonificación` | ⚠️ Confirmation request for $3,000,000 |
| 3.6 | `Confirmo` | ✅ Income confirmed and registered |

### 3D. Timeout (Skip confirmation)

| Step | Send This Message | Expected Response |
|------|-------------------|-------------------|
| 3.7 | `Gasté 1.5M en vacaciones` | ⚠️ Confirmation request |
| 3.8 | (Wait 5 minutes, then) `Hola` | Confirmation expired, processes new message |

---

## Test Flow 4: Queries & Reports

**Objective:** Test balance, transactions list, and reports

### 4A. Balance Queries

| Step | Send This Message | Expected Response Contains |
|------|-------------------|---------------------------|
| 4.1 | `¿Cuánto dinero tengo?` | Balance total with incomes/expenses |
| 4.2 | `¿Cuál es mi saldo?` | Current balance |
| 4.3 | `¿Cómo estoy de plata?` | Financial status |

### 4B. Transaction Lists

| Step | Send This Message | Expected Response Contains |
|------|-------------------|---------------------------|
| 4.4 | `Muéstrame mis transacciones` | List of all transactions |
| 4.5 | `Dame mis gastos` | Only expenses listed |
| 4.6 | `Quiero ver mis ingresos` | Only incomes listed |

### 4C. Transactions by Date

| Step | Send This Message | Expected Response Contains |
|------|-------------------|---------------------------|
| 4.7 | `¿Cuánto gasté hoy?` | Today's expenses |
| 4.8 | `Transacciones de ayer` | Yesterday's transactions |
| 4.9 | `¿Qué compré el 1 de diciembre?` | Transactions from that date |

### 4D. Transactions by Range

| Step | Send This Message | Expected Response Contains |
|------|-------------------|---------------------------|
| 4.10 | `¿Cuánto gasté esta semana?` | Last 7 days expenses |
| 4.11 | `Gastos de este mes` | Current month expenses |
| 4.12 | `Ingresos de los últimos 30 días` | Last 30 days income |

### 4E. Search Transactions

| Step | Send This Message | Expected Response Contains |
|------|-------------------|---------------------------|
| 4.13 | `¿Cuánto pago por Netflix?` | Netflix-related transactions |
| 4.14 | `Busca mis gastos de Uber` | Uber-related transactions |
| 4.15 | `Gastos de categoría Comida` | Food category summary |

### 4F. Summary

| Step | Send This Message | Expected Response Contains |
|------|-------------------|---------------------------|
| 4.16 | `¿En qué gasto más?` | Expense breakdown by category |
| 4.17 | `Dame un resumen de gastos` | Category-wise summary |
| 4.18 | `¿Dónde se va mi dinero?` | Spending distribution |

---

## Test Flow 5: Financial Rules

**Objective:** Test budget rule creation and listing

### 5A. Create Rules

| Step | Send This Message | Expected Response Contains |
|------|-------------------|---------------------------|
| 5.1 | `Pon un límite de 500k para comida` | ✅ Regla creada: Comida $500,000 |
| 5.2 | `Quiero gastar máximo 200k en entretenimiento mensual` | ✅ Regla: Entretenimiento $200,000 |
| 5.3 | `Límite de 300k en transporte al mes` | ✅ Regla: Transporte $300,000 |
| 5.4 | `Presupuesto semanal de 100k para ropa` | ✅ Regla: Ropa $100,000 Weekly |

### 5B. List Rules

| Step | Send This Message | Expected Response Contains |
|------|-------------------|---------------------------|
| 5.5 | `¿Cuáles son mis límites?` | List of all configured rules |
| 5.6 | `Muéstrame mis reglas` | Budget rules with amounts |
| 5.7 | `Mis presupuestos` | All budgets configured |

---

## Test Flow 6: Expense Validation

**Objective:** Test spending advice WITHOUT registering transactions

### ⚠️ CRITICAL: These should NOT create any transaction

| Step | Send This Message | Expected Response Contains |
|------|-------------------|---------------------------|
| 6.1 | `¿Puedo gastar 50k en ropa?` | Advice about Ropa budget, NO registration |
| 6.2 | `¿Me alcanza para una cena de 80k?` | Budget analysis, recommendation |
| 6.3 | `¿Debería comprar un celular de 500k?` | Spending advice |
| 6.4 | `¿Es buena idea gastar 100k en entretenimiento?` | Budget check against rule |
| 6.5 | `Estoy pensando en gastar 200k en comida` | Analysis of budget impact |
| 6.6 | `¿Qué opinas si gasto 300k?` | Should ask "en qué?" (needs category) |

### Verify NO transactions were created:

| Step | Send This Message | Expected |
|------|-------------------|----------|
| 6.7 | `Muéstrame mis transacciones` | Should NOT include the amounts from 6.1-6.6 |

---

## Test Flow 7: Multiple Operations

**Objective:** Test multiple transactions in a single message

| Step | Send This Message | Expected Response Contains |
|------|-------------------|---------------------------|
| 7.1 | `Gasté 10k en gaseosa y 50k en almuerzo` | 2 expenses registered |
| 7.2 | `Pagué 30k de taxi y recibí 100k de freelance` | 1 expense + 1 income |
| 7.3 | `Compré café por 5k, almuerzo 15k y snacks 8k` | 3 expenses registered |
| 7.4 | `Me pagaron 500k y gasté 80k en Uber` | 1 income + 1 expense |

### With High-Value (Confirmation needed)

| Step | Send This Message | Expected Response |
|------|-------------------|-------------------|
| 7.5 | `Gasté 2M en muebles y 500k en decoración` | Confirmation for batch |
| 7.6 | `Sí` | All transactions confirmed |

---

## Test Flow 8: Delete Operations

**Objective:** Test transaction deletion

| Step | Send This Message | Expected Response Contains |
|------|-------------------|---------------------------|
| 8.1 | `Gasté 999 en prueba` | Transaction registered |
| 8.2 | `Elimina el último gasto` | ✅ Transaction deleted, $999 |
| 8.3 | `Muéstrame mis transacciones` | $999 should NOT appear |
| 8.4 | `Borra la última transacción` | Deletes last transaction |
| 8.5 | `Me equivoqué, quita eso` | Deletes last transaction |

---

## Test Flow 9: Edge Cases

**Objective:** Test bot behavior with unusual inputs

### 9A. Missing Information

| Step | Send This Message | Expected Response |
|------|-------------------|-------------------|
| 9.1 | `Gasté` | Ask for amount |
| 9.2 | `50k` | Ask what was it for |
| 9.3 | `Registra un gasto` | Ask for amount and details |

### 9B. Invalid Inputs

| Step | Send This Message | Expected Response |
|------|-------------------|-------------------|
| 9.4 | `asdfghjkl` | Friendly "I didn't understand" |
| 9.5 | `🎉🎊🎁` | Generic response |
| 9.6 | `Gasté -50000` | Handle as positive or ask |
| 9.7 | `Gasté 0 pesos` | Ask for valid amount |

### 9C. Limitations

| Step | Send This Message | Expected Response |
|------|-------------------|-------------------|
| 9.8 | `Transfiere 100k a Juan` | Cannot make transfers, explain limitation |
| 9.9 | `Paga mi Netflix` | Cannot make payments, suggest alternative |
| 9.10 | `Invierte 500k en Bitcoin` | Cannot invest, explain |

---

## Test Flow 10: Conversation Context

**Objective:** Test conversation memory and context

### 10A. Follow-up Questions

| Step | Send This Message | Expected Response |
|------|-------------------|-------------------|
| 10.1 | `Cuánto gasté ayer` | Shows yesterday's expenses |
| 10.2 | `¿Y hoy?` | Uses context, shows today's expenses |
| 10.3 | `¿En qué categoría?` | Shows breakdown by category |

### 10B. Context Reset

| Step | Send This Message | Expected Response |
|------|-------------------|-------------------|
| 10.4 | `Quiero registrar un gasto` | Ask for details |
| 10.5 | `50k` | May ask what category |
| 10.6 | `comida` | Registers the expense |

### 10C. Persistence Test (After Restart)

1. Note your last transaction
2. Restart the Spring Boot service
3. Resume conversation:

| Step | Send This Message | Expected Response |
|------|-------------------|-------------------|
| 10.7 | `Muéstrame mis transacciones` | Previous transactions visible |
| 10.8 | `¿Qué fue mi último gasto?` | Shows last expense (context persisted) |

---

## 📊 Test Summary Checklist

| Flow | Tests | Status |
|------|-------|--------|
| 1. Basic Setup & Greetings | 5 | ☐ |
| 2. Transaction Registration | 13 | ☐ |
| 3. High-Value Confirmation | 8 | ☐ |
| 4. Queries & Reports | 18 | ☐ |
| 5. Financial Rules | 7 | ☐ |
| 6. Expense Validation | 7 | ☐ |
| 7. Multiple Operations | 6 | ☐ |
| 8. Delete Operations | 5 | ☐ |
| 9. Edge Cases | 10 | ☐ |
| 10. Conversation Context | 8 | ☐ |
| **TOTAL** | **87** | ☐ |

---

## 🔧 Troubleshooting

### Bot Not Responding
1. Check if Spring Boot service is running on 8082
2. Check Telegram bot token in application.properties
3. Verify user is linked with valid code

### Transactions Not Saving
1. Check if .NET API is running on 5003
2. Check database connection
3. Review logs for errors: `tail -f logs/sb-service.log`

### Confirmation Not Working
1. Verify CONFIRMATION_THRESHOLD is 1,000,000
2. Check PendingConfirmation table in database
3. Ensure confirmations haven't expired (5 min timeout)

---

*Last updated: December 2024*
