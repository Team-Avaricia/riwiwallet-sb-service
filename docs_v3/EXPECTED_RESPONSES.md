# 📋 Expected Bot Responses Reference

> Detailed expected responses for each type of intent

---

## Intent: `create_expense`

### Input Examples:
- "Gasté 50k en comida"
- "Pagué 30000 de taxi"
- "Compré ropa por 80k"

### Expected Response Format:
```
💸 Gasto registrado!
• Monto: $50,000
• Categoría: Comida
• Descripción: [extracted from message]
```

### With High-Value (> $1,000,000):
```
⚠️ *Confirma esta transacción de alto valor:*

💸 Gasto: $2,000,000
• Categoría: Tecnología
• Descripción: televisor

Este monto supera $1,000,000.
¿Deseas confirmar? (Responde "Sí" o "No")

⏰ Esta confirmación expira en 5 minutos.
```

---

## Intent: `create_income`

### Input Examples:
- "Recibí mi sueldo de 2M"
- "Me pagaron 500k por trabajo"
- "Gané 100k"

### Expected Response Format:
```
💰 Ingreso registrado!
• Monto: $2,000,000
• Categoría: Salario
• Descripción: sueldo
```

---

## Intent: `get_balance`

### Input Examples:
- "¿Cuánto dinero tengo?"
- "¿Cuál es mi saldo?"
- "Mi balance"

### Expected Response Format:
```
💰 Tu situación financiera:

📈 Ingresos totales: $5,000,000
📉 Gastos totales: $1,200,000

💵 Saldo actual: $3,800,000
```

---

## Intent: `list_transactions`

### Input Examples:
- "Muéstrame mis transacciones"
- "Dame mis gastos"
- "Mis ingresos"

### Expected Response Format:
```
📋 *Tus transacciones:*

💸 $50,000 - almuerzo (Comida) - 14/12/2024
💸 $30,000 - taxi (Transporte) - 14/12/2024
💰 $2,000,000 - sueldo (Salario) - 13/12/2024
...

📊 *Resumen:*
• Total: 15 transacciones
• 💰 Ingresos: $5,000,000
• 💸 Gastos: $1,200,000
• 📈 Balance: $3,800,000
```

---

## Intent: `list_transactions_by_date`

### Input Examples:
- "¿Cuánto gasté ayer?"
- "Transacciones del 15 de noviembre"
- "¿Qué compré hoy?"

### Expected Response Format:
```
📅 *Transacciones del 14/12/2024:*

💸 $50,000 - almuerzo (Comida)
💸 $30,000 - taxi (Transporte)

💵 *Total del día:* $80,000
```

---

## Intent: `list_transactions_by_range`

### Input Examples:
- "¿Cuánto gasté esta semana?"
- "Ingresos de noviembre"
- "Gastos del 1 al 15"

### Expected Response Format:
```
📆 *Transacciones del 07/12/2024 al 14/12/2024:*

💸 $50,000 - almuerzo (Comida) - 14/12
💸 $30,000 - taxi (Transporte) - 13/12
💰 $500,000 - freelance (Freelance) - 10/12

... y 5 transacciones más

📊 *Resumen:*
• Transacciones: 8
• 💰 Ingresos: $500,000
• 💸 Gastos: $280,000
• 📈 Balance: $220,000
```

---

## Intent: `search_transactions`

### Input Examples:
- "¿Cuánto pago por Netflix?"
- "Busca mis gastos de Uber"
- "Gastos de categoría Transporte"

### Expected Response Format:
```
🔍 *Resultados para "Netflix":*

💸 $30,000 - Netflix (Entretenimiento) - 01/12/2024
💸 $30,000 - Netflix (Entretenimiento) - 01/11/2024

📊 *Total en "Netflix":* $60,000 (2 transacciones)
```

---

## Intent: `get_summary`

### Input Examples:
- "¿En qué gasto más?"
- "Resumen de gastos"
- "Distribución de gastos"

### Expected Response Format:
```
📊 *Distribución de tus gastos:*

🍔 Comida: $450,000 (38%)
🚗 Transporte: $280,000 (23%)
🎬 Entretenimiento: $200,000 (17%)
💊 Salud: $150,000 (13%)
📦 Otros: $120,000 (10%)

💰 Total gastado: $1,200,000
```

---

## Intent: `create_rule`

### Input Examples:
- "Pon un límite de 500k para comida"
- "Quiero gastar máximo 200k en entretenimiento"
- "Presupuesto mensual de 1M"

### Expected Response Format:
```
📏 ¡Regla creada!

• 📂 Categoría: Comida
• 💰 Límite: $500,000
• 📅 Período: Mensual

💡 Te avisaré cuando te acerques al límite.
```

---

## Intent: `list_rules`

### Input Examples:
- "¿Cuáles son mis límites?"
- "Muéstrame mis reglas"
- "Mis presupuestos"

### Expected Response Format:
```
📏 *Tus reglas financieras:*

1. 🍔 Comida
   • Límite: $500,000 mensual
   • Gastado: $350,000 (70%)
   • Disponible: $150,000

2. 🚗 Transporte
   • Límite: $200,000 mensual
   • Gastado: $180,000 (90%) ⚠️
   • Disponible: $20,000
```

---

## Intent: `validate_expense`

### Input Examples:
- "¿Puedo gastar 100k en ropa?"
- "¿Me alcanza para una cena de 80k?"
- "¿Debería comprar esto por 200k?"

### Expected Response Format (Within Budget):
```
🤔 *Sobre gastar $100,000 en Ropa:*

📏 *Tu presupuesto mensual para Ropa:*
• Límite: $300,000
• Ya gastaste: $150,000 (50%)
• Disponible: $150,000

✅ *¡Está dentro de tu presupuesto!*

💡 Después de este gasto aún te quedarían $50,000 disponibles.

Si decides hacerlo, dime: "Gasté $100,000 en Ropa"
```

### Expected Response Format (Exceeds Budget):
```
🤔 *Sobre gastar $200,000 en Entretenimiento:*

📏 *Tu presupuesto mensual para Entretenimiento:*
• Límite: $200,000
• Ya gastaste: $150,000 (75%)
• Disponible: $50,000

⚠️ *Este gasto excedería tu presupuesto disponible.*

💡 *Recomendación:* Solo te quedan $50,000 disponibles. 
Este gasto de $200,000 te dejaría $150,000 por encima del límite.

Podrías:
• Gastar máximo $50,000
• Esperar al próximo período
• Ajustar tu presupuesto si realmente lo necesitas
```

---

## Intent: `delete_transaction`

### Input Examples:
- "Elimina el último gasto"
- "Borra esa transacción"
- "Me equivoqué, quita eso"

### Expected Response Format:
```
✅ ¡Listo! Eliminé tu último gasto:

💸 *$50,000*
• Descripción: almuerzo
• Categoría: Comida

📝 Tu saldo ha sido restaurado.
```

---

## Intent: `question`

### Input Examples:
- "Hola"
- "¿Cómo ahorro dinero?"
- "Gracias"

### Expected Response Format (Greeting):
```
¡Hola! 👋 Soy tu asistente financiero personal.
¿En qué puedo ayudarte hoy?
```

### Expected Response Format (Capabilities):
```
¡Soy tu Asistente Financiero personal! 🤖💰

Puedo ayudarte a organizar tus finanzas con todo esto:

📝 Registro de Movimientos:
• Registrar gastos: "Gasté 50k en comida"
• Registrar ingresos: "Me pagaron 2M"

🔎 Consultas y Reportes:
• Ver saldo: "¿Cuánto dinero tengo?"
• Ver movimientos: "Gastos de esta semana"
• Buscar: "¿Cuánto gasto en Uber?"
• Resúmenes: "¿En qué gasto más?"

⚙️ Control y Alertas:
• Presupuestos: "Límite de 500k en comida"
• Consejos: "¿Debería comprar esto?"

¡Solo escríbeme o mándame una nota de voz! 🎙️
```

---

## Error Responses

### Missing Amount:
```
🤔 ¿Cuánto fue el gasto? Por favor dime el monto.

💡 Ejemplo: "Gasté 50000 en comida" o "Recibí 100k"
```

### Invalid Request:
```
🤔 No entendí bien. ¿Podrías ser más específico?

Puedo ayudarte con:
• Registrar gastos e ingresos
• Consultar tu saldo
• Ver tus transacciones
• Crear presupuestos
```

### Limitation Response:
```
🚫 Lo siento, no puedo realizar transferencias ni mover dinero real.

Soy un *asistente de registro* - te ayudo a llevar el control de tus finanzas, 
pero no tengo acceso a tus cuentas bancarias.

✅ Lo que sí puedo hacer:
• Registrar que hiciste una transferencia: "Transferí 100k a Juan"
• Consultar tu historial de movimientos
• Ayudarte con presupuestos

¿Te ayudo con algo de esto?
```

---

*Response reference - December 2024*
