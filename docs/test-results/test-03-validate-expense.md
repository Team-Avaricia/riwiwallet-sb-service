# Test 3: validate_expense (Validación de Gastos)

> **Fecha:** 5 de Diciembre, 2025  
> **Intent:** `validate_expense`  
> **Resultado:** ⚠️ 8/10 PASSED (80%)

---

## 📊 Resultados por Caso

| # | Estado | Monto | Categoría | Notas |
|---|--------|-------|-----------|-------|
| 1 | ✅ | $50.000 | Ropa | Correcto |
| 2 | ✅ | $200.000 | Entretenimiento | Fiesta → Entretenimiento ✓ |
| 3 | ✅ | $2.000.000 | Tecnología | Celular → Tecnología ✓ |
| 4 | ⚠️ | $100.000 | **null** | ❌ Debería ser "Otros" |
| 5 | ✅ | $80.000 | Entretenimiento | Correcto |
| 6 | ✅ | $500.000 | Entretenimiento | Default correcto |
| 7 | ✅ | $150.000 | Entretenimiento | "Algo" → Default correcto |
| 8 | ✅ | $300.000 | Entretenimiento | Default correcto |
| 9 | ⚠️ | $1.000.000 | Entretenimiento | Vacaciones → debería ser "Viajes" |
| 10 | ✅ | $250.000 | Entretenimiento | "Algo" → Default correcto |

---

## 💬 Conversación Completa

### Caso 1 ✅
**Usuario:**
```
Puedo gastar 50k en ropa?
```

**Asistente:**
```
🤔 Sobre gastar $50.000 en Ropa:

📊 No tienes un límite configurado para Ropa.

💡 Consejos antes de gastar:
• ¿Es una necesidad o un gusto?
• ¿Afecta tus metas de ahorro?
• ¿Tienes un fondo de emergencia?

Si decides hacerlo, dime: "Gasté $50.000 en Ropa"
```

**Análisis:** ✅ Correcto - Identifica categoría "Ropa" correctamente, proporciona consejo financiero sin registrar.

---

### Caso 2 ✅
**Usuario:**
```
¿Me alcanza para una fiesta de 200k?
```

**Asistente:**
```
🤔 Sobre gastar $200.000 en Entretenimiento:

📊 No tienes un límite configurado para Entretenimiento.

💡 Consejos antes de gastar:
• ¿Es una necesidad o un gusto?
• ¿Afecta tus metas de ahorro?
• ¿Tienes un fondo de emergencia?

Si decides hacerlo, dime: "Gasté $200.000 en fiesta"
```

**Análisis:** ✅ Correcto - "Fiesta" clasificada como Entretenimiento correctamente.

---

### Caso 3 ✅
**Usuario:**
```
¿Debería comprar un celular de 2M?
```

**Asistente:**
```
🤔 Sobre gastar $2.000.000 en Tecnología:

📊 No tienes un límite configurado para Tecnología.

💡 Consejos antes de gastar:
• ¿Es una necesidad o un gusto?
• ¿Afecta tus metas de ahorro?
• ¿Tienes un fondo de emergencia?

Si decides hacerlo, dime: "Gasté $2.000.000 en celular"
```

**Análisis:** ✅ Correcto - "Celular" clasificado como Tecnología correctamente.

---

### Caso 4 ⚠️ ISSUE
**Usuario:**
```
¿Es buena idea gastar 100k en eso?
```

**Asistente:**
```
🤔 Sobre gastar $100.000 en null:

📊 No tienes un límite configurado para null.

💡 Consejos antes de gastar:
• ¿Es una necesidad o un gusto?
• ¿Afecta tus metas de ahorro?
• ¿Tienes un fondo de emergencia?

Si decides hacerlo, dime: "Gasté $100.000 en gasto"
```

**Análisis:** ⚠️ **ISSUE** - La categoría se muestra como "null" en lugar de "Otros". El pronombre "eso" no tiene contexto, pero debería defaultear a una categoría válida.

**Causa raíz:** El sistema no maneja correctamente cuando no puede extraer una categoría específica.

---

### Caso 5 ✅
**Usuario:**
```
¿Qué opinas si gasto 80k en entretenimiento?
```

**Asistente:**
```
🤔 Sobre gastar $80.000 en Entretenimiento:

📊 No tienes un límite configurado para Entretenimiento.

💡 Consejos antes de gastar:
• ¿Es una necesidad o un gusto?
• ¿Afecta tus metas de ahorro?
• ¿Tienes un fondo de emergencia?

Si decides hacerlo, dime: "Gasté $80.000 en gasto en entretenimiento"
```

**Análisis:** ✅ Correcto - Categoría explícita identificada correctamente.

---

### Caso 6 ✅
**Usuario:**
```
Estoy pensando en gastar 500k
```

**Asistente:**
```
🤔 Sobre gastar $500.000 en Entretenimiento:

📊 No tienes un límite configurado para Entretenimiento.

💡 Consejos antes de gastar:
• ¿Es una necesidad o un gusto?
• ¿Afecta tus metas de ahorro?
• ¿Tienes un fondo de emergencia?

Si decides hacerlo, dime: "Gasté $500.000 en Entretenimiento"
```

**Análisis:** ✅ Correcto - Sin contexto específico, usa Entretenimiento como default.

---

### Caso 7 ✅
**Usuario:**
```
¿Será que me compro algo de 150k?
```

**Asistente:**
```
🤔 Sobre gastar $150.000 en Entretenimiento:

📊 No tienes un límite configurado para Entretenimiento.

💡 Consejos antes de gastar:
• ¿Es una necesidad o un gusto?
• ¿Afecta tus metas de ahorro?
• ¿Tienes un fondo de emergencia?

Si decides hacerlo, dime: "Gasté $150.000 en compra"
```

**Análisis:** ✅ Correcto - "Algo" no tiene contexto, usa Entretenimiento como default.

---

### Caso 8 ✅
**Usuario:**
```
Quiero saber si puedo gastar 300mil
```

**Asistente:**
```
🤔 Sobre gastar $300.000 en Entretenimiento:

📊 No tienes un límite configurado para Entretenimiento.

💡 Consejos antes de gastar:
• ¿Es una necesidad o un gusto?
• ¿Afecta tus metas de ahorro?
• ¿Tienes un fondo de emergencia?

Si decides hacerlo, dime: "Gasté $300.000 en Entretenimiento"
```

**Análisis:** ✅ Correcto - Sin contexto, usa Entretenimiento como default.

---

### Caso 9 ⚠️ MINOR
**Usuario:**
```
¿Crees que está bien gastar 1M en vacaciones?
```

**Asistente:**
```
🤔 Sobre gastar $1.000.000 en Entretenimiento:

📊 No tienes un límite configurado para Entretenimiento.

💡 Consejos antes de gastar:
• ¿Es una necesidad o un gusto?
• ¿Afecta tus metas de ahorro?
• ¿Tienes un fondo de emergencia?

Si decides hacerlo, dime: "Gasté $1.000.000 en vacaciones"
```

**Análisis:** ⚠️ **MINOR** - Funciona correctamente, pero "vacaciones" podría clasificarse mejor como "Viajes" en lugar de "Entretenimiento" para mayor precisión.

---

### Caso 10 ✅
**Usuario:**
```
Me gustaría comprar algo de 250k, ¿qué dices?
```

**Asistente:**
```
🤔 Sobre gastar $250.000 en Entretenimiento:

📊 No tienes un límite configurado para Entretenimiento.

💡 Consejos antes de gastar:
• ¿Es una necesidad o un gusto?
• ¿Afecta tus metas de ahorro?
• ¿Tienes un fondo de emergencia?

Si decides hacerlo, dime: "Gasté $250.000 en compra"
```

**Análisis:** ✅ Correcto - "Algo" sin contexto, usa Entretenimiento como default.

---

## 🐛 Issues Identificados

### Issue #1: Categoría "null" (Caso 4) - CRÍTICO
- **Input:** "¿Es buena idea gastar 100k en eso?"
- **Comportamiento actual:** Muestra "null" como categoría
- **Comportamiento esperado:** Mostrar "Otros" como categoría default
- **Archivo afectado:** `MessageProcessorService.java` o `IntentClassifierService.java`
- **Prioridad:** Alta

### Issue #2: Categoría "vacaciones" (Caso 9) - MENOR
- **Input:** "¿Crees que está bien gastar 1M en vacaciones?"
- **Comportamiento actual:** Clasifica como "Entretenimiento"
- **Comportamiento sugerido:** Clasificar como "Viajes"
- **Archivo afectado:** `IntentClassifierService.java` (category classification guide)
- **Prioridad:** Baja

---

## ✅ Conclusiones

- **Extracción de montos:** 100% precisa
- **Clasificación de intent:** 100% correcta (todos identificados como validate_expense)
- **Clasificación de categorías:** 80% correcta (8/10)
- **Formato de respuesta:** Consistente y profesional
- **Comportamiento principal:** ✅ No registra transacciones (correcto para validate_expense)
