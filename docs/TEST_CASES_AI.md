# 🧪 Casos de Prueba - Asistente Financiero IA

Este documento contiene todos los casos de prueba para verificar el correcto funcionamiento del asistente financiero de RiwiWallet.

---

## 📋 Índice

1. [Registro de Gastos](#1-registro-de-gastos-create_expense)
2. [Registro de Ingresos](#2-registro-de-ingresos-create_income)
3. [Validación de Gastos (Consultas)](#3-validación-de-gastos-validate_expense)
4. [Gastos Recurrentes](#4-gastos-recurrentes-create_recurring_expense)
5. [Ingresos Recurrentes](#5-ingresos-recurrentes-create_recurring_income)
6. [Listar Transacciones](#6-listar-transacciones-list_transactions)
7. [Transacciones por Fecha](#7-transacciones-por-fecha-list_transactions_by_date)
8. [Transacciones por Rango](#8-transacciones-por-rango-list_transactions_by_range)
9. [Búsqueda de Transacciones](#9-búsqueda-de-transacciones-search_transactions)
10. [Balance](#10-balance-get_balance)
11. [Resumen por Categoría](#11-resumen-por-categoría-get_summary)
12. [Flujo de Caja](#12-flujo-de-caja-get_cashflow)
13. [Transacciones Recurrentes](#13-transacciones-recurrentes-list_recurring)
14. [Eliminar Recurrentes](#14-eliminar-recurrentes-delete_recurring)
15. [Eliminar Transacciones](#15-eliminar-transacciones-delete_transaction)
16. [Reglas Financieras](#16-reglas-financieras-create_rule--list_rules)
17. [Preguntas Generales](#17-preguntas-generales-question)
18. [Operaciones Múltiples](#18-operaciones-múltiples)
19. [Notas de Voz](#19-notas-de-voz)
20. [Casos Edge](#20-casos-edge)

---

## 1. Registro de Gastos (`create_expense`)

### ✅ Casos Válidos

| # | Mensaje | Resultado Esperado |
|---|---------|-------------------|
| 1.1 | "Gasté 50k en comida" | Gasto registrado: $50,000 - Comida |
| 1.2 | "Compré un almuerzo de 15000" | Gasto registrado: $15,000 - Comida |
| 1.3 | "Pagué 120k de arriendo" | Gasto registrado: $120,000 - Arriendo |
| 1.4 | "Me gasté 30 mil en taxi" | Gasto registrado: $30,000 - Transporte |
| 1.5 | "Gasté 2M en un celular nuevo" | Gasto registrado: $2,000,000 - Tecnología |
| 1.6 | "Compré ropa por 80000 pesos" | Gasto registrado: $80,000 - Ropa |
| 1.7 | "Pagué la luz, fueron 45k" | Gasto registrado: $45,000 - Servicios |
| 1.8 | "Gasté 25.000 en una gaseosa" | Gasto registrado: $25,000 - Comida |
| 1.9 | "Pagué Netflix 50k" | Gasto registrado: $50,000 - Entretenimiento |
| 1.10 | "Compré medicinas por 35000" | Gasto registrado: $35,000 - Salud |

### 🔢 Variaciones de Formato Numérico

| # | Mensaje | Monto Esperado |
|---|---------|----------------|
| 1.11 | "Gasté 50k" | $50,000 |
| 1.12 | "Gasté 50K" | $50,000 |
| 1.13 | "Gasté 50mil" | $50,000 |
| 1.14 | "Gasté 50 mil" | $50,000 |
| 1.15 | "Gasté 2M" | $2,000,000 |
| 1.16 | "Gasté 2 millones" | $2,000,000 |
| 1.17 | "Gasté 50.000" | $50,000 |
| 1.18 | "Gasté 50,000" | $50,000 |
| 1.19 | "Gasté cincuenta mil" | $50,000 |
| 1.20 | "Gasté un palo" | $1,000,000 |

---

## 2. Registro de Ingresos (`create_income`)

### ✅ Casos Válidos

| # | Mensaje | Resultado Esperado |
|---|---------|-------------------|
| 2.1 | "Recibí mi sueldo de 2M" | Ingreso registrado: $2,000,000 - Salario |
| 2.2 | "Me pagaron 500k por un trabajo" | Ingreso registrado: $500,000 - Freelance |
| 2.3 | "Gané 100k en una apuesta" | Ingreso registrado: $100,000 - Otros |
| 2.4 | "Me transfirieron 300000" | Ingreso registrado: $300,000 |
| 2.5 | "Vendí mi celular por 800k" | Ingreso registrado: $800,000 - Otros |
| 2.6 | "Me llegó la quincena, 1.5M" | Ingreso registrado: $1,500,000 - Salario |
| 2.7 | "Me regalaron 50k" | Ingreso registrado: $50,000 - Regalos |
| 2.8 | "Recibí dividendos de 200k" | Ingreso registrado: $200,000 - Inversiones |
| 2.9 | "Me consigné mi papá 100mil" | Ingreso registrado: $100,000 - Regalos |
| 2.10 | "Gané 5 millones en la lotería" | Ingreso registrado: $5,000,000 - Otros |

---

## 3. Validación de Gastos (`validate_expense`)

> ⚠️ **CRÍTICO**: Estas son CONSULTAS, NO deben registrar ningún gasto

### ✅ Casos de Consulta (NO registrar)

| # | Mensaje | Resultado Esperado |
|---|---------|-------------------|
| 3.1 | "¿Puedo gastar 50k en ropa?" | Validación/consejo, NO registrar |
| 3.2 | "¿Me alcanza para una fiesta de 200k?" | Validación/consejo, NO registrar |
| 3.3 | "¿Debería comprar un celular de 2M?" | Validación/consejo, NO registrar |
| 3.4 | "¿Es buena idea gastar 100k en eso?" | Validación/consejo, NO registrar |
| 3.5 | "¿Qué opinas si gasto 80k en entretenimiento?" | Validación/consejo, NO registrar |
| 3.6 | "Estoy pensando en gastar 500k" | Validación/consejo, NO registrar |
| 3.7 | "¿Será que me compro algo de 150k?" | Validación/consejo, NO registrar |
| 3.8 | "Quiero saber si puedo gastar 300mil" | Validación/consejo, NO registrar |
| 3.9 | "¿Crees que está bien gastar 1M en vacaciones?" | Validación/consejo, NO registrar |
| 3.10 | "Me gustaría comprar algo de 250k, ¿qué dices?" | Validación/consejo, NO registrar |

---

## 4. Gastos Recurrentes (`create_recurring_expense`)

### ✅ Casos Válidos

| # | Mensaje | Resultado Esperado |
|---|---------|-------------------|
| 4.1 | "Pago Netflix cada mes, 50k" | Gasto recurrente: $50,000 - Monthly |
| 4.2 | "El arriendo son 800k mensuales" | Gasto recurrente: $800,000 - Monthly |
| 4.3 | "Pago gimnasio semanal, 30k" | Gasto recurrente: $30,000 - Weekly |
| 4.4 | "Spotify me cobra 15k al mes" | Gasto recurrente: $15,000 - Monthly |
| 4.5 | "Tengo que pagar seguro de 200k cada año" | Gasto recurrente: $200,000 - Yearly |
| 4.6 | "Los servicios son 150k mensuales" | Gasto recurrente: $150,000 - Monthly |
| 4.7 | "Pago cuota del carro 500k mensual" | Gasto recurrente: $500,000 - Monthly |
| 4.8 | "La hipoteca es de 1.2M cada mes" | Gasto recurrente: $1,200,000 - Monthly |
| 4.9 | "Internet y TV 120k mensuales" | Gasto recurrente: $120,000 - Monthly |
| 4.10 | "Amazon Prime 40k al mes" | Gasto recurrente: $40,000 - Monthly |

### 📅 Con día específico

| # | Mensaje | Resultado Esperado |
|---|---------|-------------------|
| 4.11 | "Pago arriendo el día 5 de cada mes, 800k" | Gasto recurrente: día 5, Monthly |
| 4.12 | "Netflix se cobra el 15 de cada mes" | Gasto recurrente: día 15, Monthly |
| 4.13 | "El primero de cada mes pago servicios" | Gasto recurrente: día 1, Monthly |

---

## 5. Ingresos Recurrentes (`create_recurring_income`)

### ✅ Casos Válidos

| # | Mensaje | Resultado Esperado |
|---|---------|-------------------|
| 5.1 | "Me pagan 2M mensualmente" | Ingreso recurrente: $2,000,000 - Monthly |
| 5.2 | "Recibo 500k cada quincena" | Ingreso recurrente: $500,000 - Monthly |
| 5.3 | "Mi sueldo es de 3M al mes" | Ingreso recurrente: $3,000,000 - Monthly |
| 5.4 | "Gano 50k semanales en freelance" | Ingreso recurrente: $50,000 - Weekly |
| 5.5 | "Recibo arriendo de 800k mensual" | Ingreso recurrente: $800,000 - Monthly |
| 5.6 | "Me llega pensión de 1.5M cada mes" | Ingreso recurrente: $1,500,000 - Monthly |
| 5.7 | "Tengo un ingreso fijo de 400k" | Ingreso recurrente: $400,000 - Monthly |
| 5.8 | "Me pagan cada viernes 200k" | Ingreso recurrente: $200,000 - Weekly |
| 5.9 | "Recibo intereses de 100k mensuales" | Ingreso recurrente: $100,000 - Monthly |
| 5.10 | "El día 15 me pagan 2.5M" | Ingreso recurrente: día 15, $2,500,000 |

---

## 6. Listar Transacciones (`list_transactions`)

### ✅ Casos Válidos

| # | Mensaje | Tipo Esperado |
|---|---------|---------------|
| 6.1 | "Muéstrame mis transacciones" | Todas |
| 6.2 | "Dame mis gastos" | type: "Expense" |
| 6.3 | "Quiero ver mis ingresos" | type: "Income" |
| 6.4 | "¿Qué movimientos tengo?" | Todas |
| 6.5 | "Lista de gastos" | type: "Expense" |
| 6.6 | "Mis últimas compras" | type: "Expense" |
| 6.7 | "¿Qué he gastado?" | type: "Expense" |
| 6.8 | "¿Cuánto me han pagado?" | type: "Income" |
| 6.9 | "Dame mis ganancias" | type: "Income" |
| 6.10 | "Historial de transacciones" | Todas |

---

## 7. Transacciones por Fecha (`list_transactions_by_date`)

### ✅ Casos Válidos

| # | Mensaje | Fecha Esperada |
|---|---------|----------------|
| 7.1 | "¿Cuánto gasté ayer?" | 2025-12-04 |
| 7.2 | "Transacciones de hoy" | 2025-12-05 |
| 7.3 | "¿Qué compré el 15 de noviembre?" | 2025-11-15 |
| 7.4 | "Gastos del 1 de diciembre" | 2025-12-01 |
| 7.5 | "¿Qué hice el lunes?" | Calcular fecha |
| 7.6 | "Movimientos del 20/11" | 2025-11-20 |
| 7.7 | "¿Cuánto gané anteayer?" | 2025-12-03 |
| 7.8 | "Transacciones del viernes pasado" | Calcular fecha |

---

## 8. Transacciones por Rango (`list_transactions_by_range`)

### ✅ Casos Válidos

| # | Mensaje | Rango Esperado |
|---|---------|----------------|
| 8.1 | "¿Cuánto gasté esta semana?" | Últimos 7 días |
| 8.2 | "Gastos de este mes" | Diciembre 2025 |
| 8.3 | "Ingresos de noviembre" | 2025-11-01 a 2025-11-30, type: "Income" |
| 8.4 | "¿Cuánto gané del 1 al 15?" | 2025-12-01 a 2025-12-15, type: "Income" |
| 8.5 | "Transacciones de la semana pasada" | Calcular rango |
| 8.6 | "¿Cuánto he gastado en diciembre?" | 2025-12-01 a hoy, type: "Expense" |
| 8.7 | "Resumen del mes pasado" | Noviembre 2025 |
| 8.8 | "Gastos de los últimos 30 días" | Últimos 30 días |
| 8.9 | "¿Cuánto gasté entre el 10 y el 20 de noviembre?" | 2025-11-10 a 2025-11-20 |
| 8.10 | "Mis ingresos de este año" | 2025-01-01 a hoy |

---

## 9. Búsqueda de Transacciones (`search_transactions`)

### ✅ Casos Válidos

| # | Mensaje | searchQuery/category Esperado |
|---|---------|-------------------------------|
| 9.1 | "¿Cuánto pago por Netflix?" | searchQuery: "Netflix" |
| 9.2 | "Busca mis gastos de Uber" | searchQuery: "Uber" |
| 9.3 | "¿Cuánto he gastado en Spotify?" | searchQuery: "Spotify" |
| 9.4 | "Dame los gastos de categoría Comida" | category: "Comida" |
| 9.5 | "Transacciones relacionadas con Amazon" | searchQuery: "Amazon" |
| 9.6 | "¿Cuánto llevo en gasolina?" | searchQuery: "gasolina" |
| 9.7 | "Busca pagos de luz" | searchQuery: "luz" |
| 9.8 | "¿Cuánto he gastado en restaurantes?" | searchQuery: "restaurantes" |
| 9.9 | "Gastos de la categoría Transporte" | category: "Transporte" |
| 9.10 | "¿Cuánto llevo en medicinas?" | searchQuery: "medicinas" |

---

## 10. Balance (`get_balance`)

### ✅ Casos Válidos

| # | Mensaje |
|---|---------|
| 10.1 | "¿Cuánto dinero tengo?" |
| 10.2 | "¿Cuál es mi saldo?" |
| 10.3 | "¿Cuánto me queda?" |
| 10.4 | "Mi balance" |
| 10.5 | "¿Cómo estoy de plata?" |
| 10.6 | "¿Cuánta plata tengo?" |
| 10.7 | "Estado de cuenta" |
| 10.8 | "¿Tengo dinero?" |
| 10.9 | "Saldo actual" |
| 10.10 | "¿Cuánto hay en mi cuenta?" |

---

## 11. Resumen por Categoría (`get_summary`)

### ✅ Casos Válidos

| # | Mensaje |
|---|---------|
| 11.1 | "¿En qué gasto más?" |
| 11.2 | "Dame un resumen de mis gastos" |
| 11.3 | "¿Cuánto gasto en comida?" |
| 11.4 | "Análisis de gastos" |
| 11.5 | "¿Cuáles son mis mayores gastos?" |
| 11.6 | "Distribución de gastos" |
| 11.7 | "¿Dónde se va mi dinero?" |
| 11.8 | "Resumen por categoría" |
| 11.9 | "¿En qué categoría gasto más?" |
| 11.10 | "Desglose de mis gastos" |

---

## 12. Flujo de Caja (`get_cashflow`)

### ✅ Casos Válidos

| # | Mensaje |
|---|---------|
| 12.1 | "¿Cuánto me queda libre cada mes?" |
| 12.2 | "¿Cuáles son mis gastos fijos?" |
| 12.3 | "Flujo de caja" |
| 12.4 | "¿Cuánto dinero libre tengo?" |
| 12.5 | "Ingresos vs gastos fijos" |
| 12.6 | "¿Cuánto puedo ahorrar al mes?" |
| 12.7 | "Mi capacidad de ahorro" |
| 12.8 | "Balance mensual" |
| 12.9 | "¿Qué me sobra cada mes?" |
| 12.10 | "Análisis de flujo de caja" |

---

## 13. Transacciones Recurrentes (`list_recurring`)

### ✅ Casos Válidos

| # | Mensaje |
|---|---------|
| 13.1 | "¿Cuáles son mis pagos fijos?" |
| 13.2 | "Muéstrame mis ingresos recurrentes" |
| 13.3 | "Mis gastos fijos" |
| 13.4 | "Lista de pagos automáticos" |
| 13.5 | "¿Qué pagos tengo mensuales?" |
| 13.6 | "Mis suscripciones" |
| 13.7 | "Transacciones recurrentes" |
| 13.8 | "¿Qué se cobra automáticamente?" |
| 13.9 | "Mis compromisos mensuales" |
| 13.10 | "Pagos programados" |

---

## 14. Eliminar Recurrentes (`delete_recurring`)

### ✅ Casos Válidos

| # | Mensaje |
|---|---------|
| 14.1 | "Cancela el pago de Netflix" |
| 14.2 | "Ya no tengo gimnasio" |
| 14.3 | "Elimina ese ingreso fijo" |
| 14.4 | "Quita el pago de Spotify" |
| 14.5 | "Borra la suscripción de Amazon" |
| 14.6 | "Ya no pago arriendo" |
| 14.7 | "Cancela mi suscripción mensual" |
| 14.8 | "Elimina el pago del carro" |
| 14.9 | "Quita ese gasto recurrente" |
| 14.10 | "Ya no tengo ese ingreso fijo" |

---

## 15. Eliminar Transacciones (`delete_transaction`)

### ✅ Casos Válidos

| # | Mensaje |
|---|---------|
| 15.1 | "Elimina el último gasto" |
| 15.2 | "Borra esa transacción" |
| 15.3 | "Cancela mi última compra" |
| 15.4 | "Quita el último movimiento" |
| 15.5 | "Me equivoqué, borra eso" |
| 15.6 | "Elimina la última transacción" |
| 15.7 | "Deshaz el último registro" |
| 15.8 | "Borra lo que acabo de poner" |

---

## 16. Reglas Financieras (`create_rule` & `list_rules`)

### ✅ Crear Reglas

| # | Mensaje | Resultado Esperado |
|---|---------|-------------------|
| 16.1 | "Pon un límite de 500k para comida" | Regla: Comida - $500,000 |
| 16.2 | "Quiero gastar máximo 200k en entretenimiento" | Regla: Entretenimiento - $200,000 |
| 16.3 | "Límite mensual de 1M en gastos" | Regla: General - $1,000,000 |
| 16.4 | "No quiero gastar más de 300k en transporte" | Regla: Transporte - $300,000 |
| 16.5 | "Presupuesto de 150k para ropa" | Regla: Ropa - $150,000 |

### ✅ Listar Reglas

| # | Mensaje |
|---|---------|
| 16.6 | "¿Cuáles son mis límites?" |
| 16.7 | "Muéstrame mis reglas" |
| 16.8 | "Mis presupuestos" |
| 16.9 | "¿Qué límites tengo?" |
| 16.10 | "Ver mis reglas financieras" |

---

## 17. Preguntas Generales (`question`)

### ✅ Saludos y Conversación

| # | Mensaje |
|---|---------|
| 17.1 | "Hola" |
| 17.2 | "Buenos días" |
| 17.3 | "Gracias" |
| 17.4 | "¿Qué puedes hacer?" |
| 17.5 | "Ayuda" |

### ✅ Consejos Financieros

| # | Mensaje |
|---|---------|
| 17.6 | "¿Cómo puedo ahorrar más?" |
| 17.7 | "Dame consejos para mis finanzas" |
| 17.8 | "¿Qué puedo hacer para gastar menos?" |
| 17.9 | "¿Cómo organizo mejor mi dinero?" |
| 17.10 | "Tips de ahorro" |
| 17.11 | "¿Debería invertir mi dinero?" |
| 17.12 | "¿Cómo hago un presupuesto?" |
| 17.13 | "¿Es bueno tener tarjeta de crédito?" |

---

## 18. Operaciones Múltiples

### ✅ Casos con Múltiples Operaciones

| # | Mensaje | Operaciones Esperadas |
|---|---------|----------------------|
| 18.1 | "Gasté 10k en gaseosa y gané 50k en una apuesta" | 2: create_expense + create_income |
| 18.2 | "Compré almuerzo por 15k y pagué taxi 8k" | 2: create_expense + create_expense |
| 18.3 | "Recibí sueldo de 2M y pagué arriendo de 800k" | 2: create_income + create_expense |
| 18.4 | "Gasté 20k en café, 30k en almuerzo y 15k en snacks" | 3: create_expense x3 |
| 18.5 | "Me pagaron 500k del trabajo y vendí algo por 200k" | 2: create_income x2 |

---

## 19. Notas de Voz

> Enviar audios por Telegram diciendo estos mensajes

### ✅ Casos de Audio

| # | Audio | Resultado Esperado |
|---|-------|-------------------|
| 19.1 | "Gasté cincuenta mil pesos en comida" | Transcripción + create_expense |
| 19.2 | "Cuánto dinero tengo" | Transcripción + get_balance |
| 19.3 | "Me pagaron dos millones" | Transcripción + create_income |
| 19.4 | "Muéstrame mis gastos de esta semana" | Transcripción + list_transactions_by_range |
| 19.5 | "Hola, necesito registrar un gasto de treinta mil" | Transcripción + create_expense |

---

## 20. Casos Edge

### ⚠️ Casos Límite y Errores

| # | Mensaje | Comportamiento Esperado |
|---|---------|------------------------|
| 20.1 | "" (mensaje vacío) | Ignorar o mensaje de ayuda |
| 20.2 | "asdfghjkl" | Intent: question, respuesta amigable |
| 20.3 | "Gasté" (sin monto) | Pedir más información |
| 20.4 | "50k" (sin contexto) | Pedir aclaración |
| 20.5 | "Gasté -50000" (monto negativo) | Manejar como $50,000 |
| 20.6 | "Gasté 0 pesos" | Ignorar o pedir confirmación |
| 20.7 | "Gasté 999999999999" (monto extremo) | Registrar o pedir confirmación |
| 20.8 | "🎉🎊🎁" (solo emojis) | Intent: question |
| 20.9 | Mensaje muy largo (>1000 caracteres) | Procesar normalmente |
| 20.10 | "Gasté 50k ayer en comida del 15 de noviembre" (fechas conflictivas) | Usar la más específica |

### 🔄 Contexto de Conversación

| # | Secuencia | Comportamiento Esperado |
|---|-----------|------------------------|
| 20.11 | "Gasté 50k" → "¿En qué?" | Mantener contexto, pedir categoría |
| 20.12 | "Mis gastos" → "¿De qué días?" | Usar contexto anterior |
| 20.13 | "Elimina eso" (sin transacción previa) | Pedir aclaración |
| 20.14 | "¿Y los ingresos?" (después de ver gastos) | Mostrar ingresos |

---

## 📊 Resumen de Intents

| Intent | Cuenta de Casos |
|--------|-----------------|
| `create_expense` | 20 |
| `create_income` | 10 |
| `validate_expense` | 10 |
| `create_recurring_expense` | 13 |
| `create_recurring_income` | 10 |
| `list_transactions` | 10 |
| `list_transactions_by_date` | 8 |
| `list_transactions_by_range` | 10 |
| `search_transactions` | 10 |
| `get_balance` | 10 |
| `get_summary` | 10 |
| `get_cashflow` | 10 |
| `list_recurring` | 10 |
| `delete_recurring` | 10 |
| `delete_transaction` | 8 |
| `create_rule` | 5 |
| `list_rules` | 5 |
| `question` | 13 |
| Múltiples operaciones | 5 |
| Notas de voz | 5 |
| Casos edge | 14 |

**Total: ~180 casos de prueba**

---

## 🚀 Cómo Ejecutar las Pruebas

### Telegram Bot
1. Buscar el bot en Telegram
2. Vincular cuenta con `/start LINK_<code>`
3. Enviar cada mensaje de prueba
4. Verificar respuesta esperada

### Mock Mode
```bash
# Activar modo mock
export MS_CORE_USE_MOCK=true
./mvnw spring-boot:run
```

### Verificar Logs
```bash
# Ver clasificación de intents
tail -f logs/application.log | grep "Intent"
```

---

## 📝 Notas

- Todos los montos usan pesos colombianos (COP)
- Las fechas se calculan desde la fecha actual
- El modo mock almacena datos en memoria
- Las notas de voz requieren conexión a OpenAI Whisper
