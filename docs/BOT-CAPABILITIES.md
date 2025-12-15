# 🤖 RiwiWallet Financial Assistant - Bot Capabilities & Limitations

> **Version:** 1.0  
> **Last Updated:** December 2025  
> **Service:** sb-service (Spring Boot + OpenAI)

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Capabilities](#capabilities)
   - [Transaction Management](#1-transaction-management)
   - [Queries & Reports](#2-queries--reports)
   - [Financial Rules](#3-financial-rules)
   - [Expense Validation](#4-expense-validation)
   - [General Assistance](#5-general-assistance)
3. [Limitations](#limitations)
4. [Supported Categories](#supported-categories)
5. [Example Commands](#example-commands)
6. [Technical Details](#technical-details)

---

## Overview

The **RiwiWallet Financial Assistant** is an AI-powered chatbot that helps users manage their personal finances through conversational interactions via Telegram. It uses OpenAI's GPT model to understand user intent and execute financial operations.

### Key Features:
- 💬 **Natural Language Processing** - Understands casual, conversational Spanish
- 🎙️ **Voice Message Support** - Transcribes audio messages using OpenAI Whisper
- 📊 **Context-Aware Responses** - Remembers conversation history for follow-up questions
- 🔔 **Smart Notifications** - Alerts when approaching budget limits

---

## Capabilities

### 1. Transaction Management

| Intent | Description | Example Commands |
|--------|-------------|------------------|
| `create_expense` | Register a new expense | "Gasté 50k en comida", "Pagué 30000 de taxi", "Compré almuerzo por 15k" |
| `create_income` | Register a new income | "Recibí mi sueldo de 2M", "Me pagaron 500k", "Gané 100k en freelance" |
| `delete_transaction` | Delete the last transaction | "Elimina el último gasto", "Borra esa transacción", "Me equivoqué, quita eso" |
| `list_transactions` | List all transactions | "Muéstrame mis transacciones", "Dame mis gastos", "¿Cuáles son mis ingresos?" |

**Multi-operation Support:**
The bot can process multiple transactions in a single message:
- "Gasté 10k en gaseosa y 50k en almuerzo" → Registers 2 expenses
- "Pagué 30k de taxi y recibí 100k de freelance" → Registers 1 expense + 1 income

---

### 2. Queries & Reports

| Intent | Description | Example Commands |
|--------|-------------|------------------|
| `get_balance` | Get current balance | "¿Cuánto dinero tengo?", "¿Cuál es mi saldo?", "¿Cuánto me queda?" |
| `get_summary` | Get expense breakdown by category | "¿En qué gasto más?", "Dame un resumen", "¿Cuánto gasto en comida?" |
| `list_transactions_by_date` | Get transactions for a specific date | "¿Cuánto gasté ayer?", "¿Qué compré el 15 de noviembre?", "Gastos de hoy" |
| `list_transactions_by_range` | Get transactions for a date range | "¿Cuánto gasté esta semana?", "Gastos de los últimos 30 días", "Mis ingresos de noviembre" |
| `search_transactions` | Search by description/category | "¿Cuánto pago por Netflix?", "Busca mis gastos de Uber", "Dame los gastos de categoría Otros" |

**Smart Date Parsing:**
- "ayer" → Yesterday's date
- "esta semana" → Last 7 days
- "este mes" → Current month
- "del 1 al 15" → 1st to 15th of current month
- "noviembre" → Full November dates

---

### 3. Financial Rules

| Intent | Description | Example Commands |
|--------|-------------|------------------|
| `create_rule` | Create a budget limit | "Pon un límite de 500k en comida", "Límite mensual de 200k en entretenimiento", "Quiero gastar máximo 1M al mes" |
| `list_rules` | View all configured rules | "¿Cuáles son mis límites?", "Muéstrame mis reglas", "¿Qué presupuestos tengo?" |

**Supported Periods:**
- `Weekly` - Semanal  
- `Biweekly` - Quincenal
- `Monthly` - Mensual (default)
- `Yearly` - Anual

---

### 4. Expense Validation

| Intent | Description | Example Commands |
|--------|-------------|------------------|
| `validate_expense` | Ask for spending advice (does NOT register) | "¿Puedo gastar 100k en ropa?", "¿Me alcanza para una fiesta de 200k?", "¿Debería comprar esto por 50k?" |

**What it does:**
- ✅ Checks if user has a budget rule for the category
- ✅ Calculates remaining budget for the period
- ✅ Shows percentage of budget used
- ✅ Provides personalized recommendation
- ❌ Does NOT register any transaction

---

### 5. General Assistance

| Intent | Description | Example Commands |
|--------|-------------|------------------|
| `question` | General questions, greetings, financial advice | "Hola", "¿Cómo ahorro dinero?", "Dame consejos", "¿Qué puedes hacer?" |

**Bot Capabilities Response:**
When user asks "qué puedes hacer", "ayuda", "help":

```
¡Soy tu Asistente Financiero personal! 🤖💰

Puedo ayudarte a organizar tus finanzas con todo esto:

📝 Registro de Movimientos:
• Registrar gastos: 'Gasté 50k en comida'
• Registrar ingresos: 'Me pagaron 2M'

🔎 Consultas y Reportes:
• Ver saldo: '¿Cuánto dinero tengo?'
• Ver movimientos: 'Gastos de esta semana'
• Buscar: '¿Cuánto gasto en Uber?'
• Resúmenes: '¿En qué gasto más?'

⚙️ Control y Alertas:
• Presupuestos: 'Límite de 500k en comida'
• Consejos: '¿Debería comprar esto?'

¡Solo escríbeme o mándame una nota de voz! 🎙️
```

---

## Limitations

### ❌ What the Bot CANNOT Do

| Category | Limitation | What to Say Instead |
|----------|------------|---------------------|
| **Payments** | Cannot make real payments or transfers | "Registra que transferí 100k a Juan" |
| **Bank Access** | Cannot connect to real bank accounts | N/A |
| **Purchases** | Cannot buy products or subscriptions | "Registra mi pago de Netflix de 30k" |
| **Cancellations** | Cannot cancel services or subscriptions | N/A - Do it directly on the service |
| **Investments** | Cannot invest money (stocks, crypto, funds) | "Registra que invertí 500k en Bitcoin" |
| **Loans** | Cannot request or manage loans | N/A |
| **Third-party Info** | Cannot access other people's accounts | N/A |
| **Bank Statements** | Cannot view real bank statements | "Muéstrame mis transacciones" |
| **Edit Transactions** | Cannot modify existing transactions | "Elimina la última" then re-register |
| **Market Predictions** | Cannot predict market or investments | N/A |
| **Professional Advice** | Cannot provide legal/tax/accounting advice | Consult a professional |

### Example Bot Responses to Limitations

**"Transfiere 100k a Juan"**
```
🚫 Lo siento, no puedo realizar transferencias ni mover dinero real.

Soy un *asistente de registro* - te ayudo a llevar el control de tus finanzas, 
pero no tengo acceso a tus cuentas bancarias.

✅ Lo que sí puedo hacer:
• Registrar que hiciste una transferencia: 'Transferí 100k a Juan'
• Consultar tu historial de movimientos
• Ayudarte con presupuestos

¿Te ayudo con algo de esto?
```

**"Paga mi Netflix"**
```
🚫 No puedo realizar pagos ni acceder a servicios externos.

Soy un asistente de *control financiero*, no una plataforma de pagos.

✅ Lo que sí puedo hacer:
• Registrar tu pago: 'Pagué Netflix 30k'
• Recordarte cuánto gastas en Netflix
• Buscar tus pagos de Netflix

¿Quieres que registre un pago de Netflix?
```

---

## Supported Categories

### Expense Categories

| Category | Keywords | Emoji |
|----------|----------|-------|
| Comida | almuerzo, desayuno, cena, restaurante, café, pizza, hamburguesa | 🍔 |
| Transporte | taxi, Uber, bus, gasolina, parqueadero, vuelo | 🚗 |
| Entretenimiento | cine, Netflix, Spotify, juegos, conciertos, vacaciones | 🎬 |
| Salud | medicinas, doctor, farmacia, hospital, dentista | 💊 |
| Educación | cursos, libros, universidad, clases | 📚 |
| Hogar | muebles, decoración, reparaciones | 🏠 |
| Ropa | ropa, zapatos, accesorios | 👕 |
| Tecnología | celular, computadora, laptop, tablet | 📱 |
| Servicios | internet, luz, agua, gas, teléfono | 💡 |
| Arriendo | pago de arriendo, alquiler | 🏠 |
| Vivienda | hipoteca, crédito hipotecario | 🏠 |
| Otros | Default for unrecognized | 📦 |

### Income Categories

| Category | Keywords | Emoji |
|----------|----------|-------|
| Salario | sueldo, nómina, pago mensual, quincena | 💼 |
| Freelance | trabajo independiente, honorarios, proyectos | 💻 |
| Inversiones | intereses, rendimientos, dividendos, arriendo recibido | 📈 |
| Regalos | presentes, donaciones, herencia, bonos | 🎁 |

---

## Example Commands

### Complete Interaction Examples

**Registering Expenses:**
```
User: Gasté 50000 en almuerzo con amigos
Bot: 💸 Gasto registrado!
     • Monto: $50,000
     • Categoría: Comida
     • Descripción: almuerzo con amigos
```

**Checking Balance:**
```
User: ¿Cuánto dinero tengo?
Bot: 💰 Tu situación financiera:
     
     📈 Ingresos totales: $5,000,000
     📉 Gastos totales: $1,200,000
     
     💵 Saldo actual: $3,800,000
```

**Creating a Budget:**
```
User: Quiero un límite de 500k en comida al mes
Bot: 📏 ¡Regla creada!
     
     • 📂 Categoría: Comida
     • 💰 Límite: $500,000
     • 📅 Período: Mensual
     
     💡 Te avisaré cuando te acerques al límite.
```

**Asking Before Spending:**
```
User: ¿Puedo gastar 300k en ropa?
Bot: 🤔 Sobre gastar $300,000 en Ropa:
     
     📏 Tu presupuesto mensual para Ropa:
     • Límite: $400,000
     • Ya gastaste: $150,000 (38%)
     • Disponible: $250,000
     
     ⚠️ Este gasto excedería tu presupuesto disponible.
     
     💡 Recomendación: Solo te quedan $250,000 disponibles.
```

---

## Technical Details

### Architecture

```
User (Telegram) 
    ↓
TelegramController
    ↓
MessageProcessorService (Orchestrator)
    ├── IntentClassifierService (OpenAI GPT)
    ├── TransactionHandlerService
    ├── RuleHandlerService
    ├── QueryHandlerService
    └── ResponseFormatterService
    ↓
CoreApiService (.NET Backend)
    ↓
PostgreSQL Database
```

### Conversation Context

- **History Size:** Last 10 messages per user
- **Session Timeout:** 30 minutes of inactivity
- **Storage:** In-memory (ConcurrentHashMap) with thread-safe operations

### Number Formats Understood

| Format | Interpreted As |
|--------|---------------|
| 50k, 50K | $50,000 |
| 2M, 2m | $2,000,000 |
| 50 mil | $50,000 |
| 50.000 | $50,000 |
| 50,000 | $50,000 |
| 2 millones | $2,000,000 |

---

## API Reference

For detailed API endpoints and integration, see:
- [API Documentation](./API-DOCUMENTATION.md)
- [Technical Document](./TECHNICAL_DOCUMENT.md)

---

*This documentation is auto-generated based on the IntentClassifierService prompt configuration.*
