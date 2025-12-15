# ⚡ Quick Test Flow - RiwiWallet Bot

> Copy and paste these messages IN ORDER to the Telegram bot

---

## 🚀 Quick Smoke Test (5 minutes)

Send these messages one by one:

```
1.  Hola
2.  ¿Qué puedes hacer?
3.  Gasté 50k en comida
4.  Recibí 100k de trabajo
5.  ¿Cuánto dinero tengo?
6.  Muéstrame mis transacciones
7.  Elimina el último gasto
8.  Gasté 2M en tecnología
9.  Sí
10. ¿Cuánto me queda?
```

### Expected Results:

| # | Check |
|---|-------|
| 1 | ✅ Greeting response |
| 2 | ✅ Lists capabilities |
| 3 | ✅ Gasto $50,000 Comida registered |
| 4 | ✅ Ingreso $100,000 registered |
| 5 | ✅ Shows balance |
| 6 | ✅ Shows transaction list |
| 7 | ✅ Deletes last transaction |
| 8 | ⚠️ Asks for confirmation ($2M > threshold) |
| 9 | ✅ Transaction confirmed |
| 10 | ✅ Shows updated balance |

---

## 📝 Full Feature Test (15 minutes)

### Part 1: Setup & Registration

```
Hola
Gasté 25k en almuerzo
Gasté 15k en taxi
Gasté 30k en café
Recibí mi sueldo de 3M
Me pagaron 200k de freelance
```

### Part 2: Queries

```
¿Cuánto dinero tengo?
Muéstrame mis gastos
Dame mis ingresos
¿En qué gasto más?
¿Cuánto gasté hoy?
¿Cuánto gasté esta semana?
```

### Part 3: Rules & Validation

```
Pon un límite de 500k para comida mensual
Pon un límite de 200k para transporte
¿Cuáles son mis límites?
¿Puedo gastar 100k en comida?
¿Me alcanza para una cena de 80k?
```

### Part 4: High-Value Confirmation

```
Gasté 1.5M en un televisor
Sí
Recibí 5M de bonificación
Confirmo
```

### Part 5: Multiple Operations

```
Gasté 10k en snacks y 20k en bebidas
Pagué 50k de Uber y recibí 100k de propina
```

### Part 6: Deletion

```
Gasté 1 peso en prueba
Elimina el último gasto
```

### Part 7: Edge Cases

```
Gasté
asdfghjkl
¿Es posible transferir 100k?
```

---

## 🔁 Reset Test Data

To start fresh:

1. Go to the database
2. Delete transactions for your user ID
3. Delete conversation messages for your Telegram ID
4. Delete pending confirmations

Or create a new test user.

---

## ✅ All Tests Passed Checklist

- [ ] Bot responds to greetings
- [ ] Expenses are registered correctly
- [ ] Incomes are registered correctly
- [ ] Balance calculation is accurate
- [ ] Transactions list works
- [ ] High-value confirmation works
- [ ] Confirmation can be cancelled
- [ ] Rules are created successfully
- [ ] Rule progress is shown
- [ ] Expense validation gives advice (no registration)
- [ ] Multiple operations work
- [ ] Delete last transaction works
- [ ] Edge cases don't crash the bot
- [ ] Context is remembered between messages

---

*Quick test guide - December 2024*
