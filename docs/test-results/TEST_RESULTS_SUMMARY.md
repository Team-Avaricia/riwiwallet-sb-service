# 📊 Resultados de Pruebas de IA - Asistente Financiero

> **Fecha de ejecución:** 5 de Diciembre, 2025  
> **Proyecto:** sb-service (RiwiWallet Assistant)  
> **Modelo IA:** OpenAI GPT-4o-mini

---

## 📈 Resumen General

| Test | Categoría | Casos | Pasados | Parciales | Fallidos | Score |
|------|-----------|-------|---------|-----------|----------|-------|
| Test 1 | create_expense | 10 | 10 | 0 | 0 | **100%** ✅ |
| Test 2 | create_income | 10 | 10 | 0 | 0 | **100%** ✅ |
| Test 3 | validate_expense | 10 | 8 | 2 | 0 | **80%** ⚠️ |
| Test 4 | create_recurring_expense | 12 | 9 | 2 | 1 | **75%** ⚠️ |
| Test 5 | create_recurring_income | 10 | 5 | 5 | 0 | **75%** ⚠️ |
| Test 6 | list_transactions | 10 | 10 | 0 | 0 | **100%** ✅ |
| **TOTAL** | - | **62** | **52** | **9** | **1** | **87%** |

---

## 🔍 Estado de Tests

- ✅ **Completados:** Tests 1, 2, 3, 4, 5, 6
- ⏳ **Pendientes:** Tests 7-20 (~120 casos restantes)

---

## 📁 Archivos de Detalle

- [Test 1 - create_expense](./test-01-create-expense.md)
- [Test 2 - create_income](./test-02-create-income.md)
- [Test 3 - validate_expense](./test-03-validate-expense.md)
- [Test 4 - create_recurring_expense](./test-04-recurring-transactions.md)
- [Test 5 - create_recurring_income](./test-05-recurring-income.md)
- [Test 6 - list_transactions](./test-06-list-transactions.md)

---

## 🐛 Issues Identificados

### Issue #1 - Categoría "null" (Test 3, Caso 4)
- **Input:** "¿Es buena idea gastar 100k en eso?"
- **Problema:** Categoría extraída como `null` en lugar de "Otros"
- **Impacto:** Mensaje muestra "null" al usuario
- **Estado:** Pendiente de corrección

### Issue #2 - Categoría "vacaciones" (Test 3, Caso 9)
- **Input:** "¿Crees que está bien gastar 1M en vacaciones?"
- **Problema:** Clasificado como "Entretenimiento" en lugar de "Viajes"
- **Impacto:** Menor - funcionalidad correcta pero categoría subóptima
- **Estado:** Mejora sugerida

### Issue #3 - Monto faltante en recurrentes (Test 4, Caso 12) ✅ FIXED
- **Input:** "Netflix se cobra el 15 de cada mes"
- **Problema:** Sistema intentaba crear transacción sin monto (400 Bad Request)
- **Solución:** Validación + pregunta de clarificación al usuario
- **Estado:** ✅ Corregido

### Issue #4 - Categorías subóptimas (Test 4, Casos 7-9) ✅ FIXED
- **Inputs:** "cuota del carro", "hipoteca", "internet y TV"
- **Problema:** Clasificados como "Otros" en lugar de categorías específicas
- **Solución:** Añadidos mappings específicos + categoría "Vivienda"
- **Estado:** ✅ Corregido

### Issue #5 - Frecuencia Biweekly no detectada (Test 5, Casos 3, 9) ✅ FIXED
- **Inputs:** "quincena", "cada dos semanas"
- **Problema:** Clasificados como "Monthly" en lugar de "Biweekly"
- **Solución:** Agregada frecuencia "Biweekly" con mapeos específicos
- **Estado:** ✅ Corregido

### Issue #6 - Categorías de ingresos pasivos (Test 5, Casos 6-8) ✅ FIXED
- **Inputs:** "pensión", "intereses", "arriendo recibido"
- **Problema:** Clasificados como "Otros" en lugar de "Inversiones"
- **Solución:** Nueva sección de categorías específicas para ingresos
- **Estado:** ✅ Corregido

---

## 📝 Notas

- Todos los tests de **registro de transacciones** (Test 1, 2) funcionan al 100%
- La **clasificación de intents** es precisa en todos los casos
- La **extracción de montos** es 100% correcta cuando el usuario lo especifica
- El **formato de respuestas** es consistente y profesional
- **Detección de frecuencias** (Mensual, Semanal, Anual, día específico) funciona al 100%
