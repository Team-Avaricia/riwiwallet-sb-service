# Test 20 - Casos Edge (Edge Cases)

## 📅 Información del Test
- **Fecha**: 2025-12-07
- **Hora**: 18:17 - 18:23
- **Modo**: Producción (API real)
- **Versión**: 1.0-SNAPSHOT

---

## 📊 Resumen de Resultados

| # | Caso | Estado | Notas |
|---|------|--------|-------|
| 20.1 | Mensaje vacío | ✅ PASÓ | Respuesta amigable |
| 20.2 | Texto sin sentido | ✅ PASÓ | Manejado como question |
| 20.3 | "gasté" sin monto | ❌ FALLÓ | Error 400 del API |
| 20.4 | "50k" sin contexto | ⚠️ PARCIAL | Muestra "null" en categoría |
| 20.5 | Monto negativo | ✅ PASÓ | Convertido a positivo |
| 20.6 | Monto cero | ❌ FALLÓ | Error 400 del API |
| 20.7 | Monto extremo | ⚠️ PARCIAL | Registró sin confirmación |
| 20.8 | Solo emojis | ✅ PASÓ | Manejado como question |
| 20.9 | Mensaje largo | ✅ PASÓ | 7/7 operaciones detectadas |
| 20.10 | Fechas conflictivas | ❌ FALLÓ | Registró 2 operaciones |
| 20.11-14 | Contexto | ✅ PASÓ | Excelente manejo |

**Resultado Global**: **9/14 pasaron** (64%)

---

## 🧪 Detalle de Cada Caso

### 20.1 - Mensaje Vacío ("")
**Entrada**: `""`
**Salida**: 
```
¡Hola! ¿En qué puedo ayudarte hoy con tus finanzas?
```
**Estado**: ✅ **PASÓ**
**Análisis**: El asistente maneja correctamente mensajes vacíos con una respuesta amigable de bienvenida.

---

### 20.2 - Texto Sin Sentido ("asdfghjkl")
**Entrada**: `adsafadsad`
**Salida**: 
```
Parece que hubo un error en tu mensaje. ¿En qué puedo ayudarte con tus finanzas hoy?
```
**Estado**: ✅ **PASÓ**
**Análisis**: El asistente reconoce que no entiende y pide aclaración de forma amigable.

---

### 20.3 - "Gasté" Sin Monto
**Entrada**: `gaste`
**Salida**: 
```
❌ No pude registrar la transacción. 400 Bad Request on POST request for "http://localhost:5203/api/Transaction": "{"type":"https://tools.ietf.org/html/rfc9110#section-15.5.1","title":"One or more validation errors occurred.","status":400,"errors":{"request":"The request field is required.","$.amount":"The JSON value could not be converted to System.Decimal...
```
**Estado**: ❌ **FALLÓ**
**Problema**: El sistema intentó crear una transacción sin monto, enviando una solicitud inválida al API.
**Solución Requerida**: Validar que el monto exista y sea válido ANTES de llamar al API. Si no hay monto, pedir más información.

---

### 20.4 - Solo Monto Sin Contexto ("50k")
**Entrada**: `50k`
**Salida**: 
```
🤔 Sobre gastar $50.000 en null:

📊 No tienes un límite configurado para null.

💡 Consejos antes de gastar:
• ¿Es una necesidad o un gusto?
• ¿Afecta tus metas de ahorro?
• ¿Tienes un fondo de emergencia?

Si decides hacerlo, dime: "Gasté $50.000 en null"
```
**Estado**: ⚠️ **PARCIAL**
**Problema**: Muestra "null" en lugar de pedir aclaración sobre la categoría.
**Solución Requerida**: Reemplazar "null" por texto genérico o pedir al usuario especificar en qué gastó.

---

### 20.5 - Monto Negativo
**Entrada**: `gaste -50000`
**Salida**: 
```
💸 Gasto registrado!
• Monto: $50.000
• Categoría: Otros
• Descripción: Gasto registrado
```
**Estado**: ✅ **PASÓ**
**Análisis**: El monto negativo se convirtió correctamente a positivo ($50,000).

---

### 20.6 - Monto Cero
**Entrada**: `gaste 0 pesos`
**Salida**: 
```
❌ No pude registrar la transacción. 400 Bad Request... "Amount must be greater than 0"
```
**Estado**: ❌ **FALLÓ**
**Problema**: El sistema intentó enviar un monto de 0 al API en lugar de validar localmente.
**Solución Requerida**: Validar que amount > 0 antes de llamar al API.

---

### 20.7 - Monto Extremadamente Alto
**Entrada**: `Gasté 999999999999`
**Salida**: 
```
💸 Gasto registrado!
• Monto: $999.999.999.999
• Categoría: Otros
• Descripción: Gasto registrado
```
**Estado**: ⚠️ **PARCIAL**
**Problema**: Registró casi un billón de pesos sin pedir confirmación.
**Mejora Sugerida**: Para montos mayores a cierto umbral (ej: $100M), pedir confirmación al usuario.

---

### 20.8 - Solo Emojis
**Entrada**: `🎉🎊🎁`
**Salida**: 
```
¡Felicidades! 🎉 ¿Hay algo específico que te gustaría hacer o preguntar sobre tus finanzas? Estoy aquí para ayudarte.
```
**Estado**: ✅ **PASÓ**
**Análisis**: El asistente interpreta los emojis de celebración y responde de forma contextual y amigable.

---

### 20.9 - Mensaje Muy Largo (>1000 caracteres)
**Entrada**: Mensaje de 1,247 caracteres con 7 operaciones financieras
**Salida**: 
```
📝 Registrando 7 operaciones:

1. 💸 Gasto de $185.000 - mercado en Éxito
2. 💸 Gasto de $120.000 - tanqueo en Terpel
3. 💸 Gasto de $65.000 - almuerzo en restaurante japonés
4. 💰 Ingreso de $800.000 - bono de productividad
5. 💸 Gasto de $55.000 - pago de factura de celular Claro
6. 💸 Gasto de $200.000 - préstamo a mi hermano
7. 💰 Ingreso de $150.000 - reembolso de Amazon

✅ ¡7 operación(es) registrada(s) exitosamente!
```
**Estado**: ✅ **PASÓ** 🌟
**Análisis**: ¡Excelente! El asistente:
- Procesó el mensaje completo sin truncamiento
- Detectó las 7 operaciones correctamente
- Clasificó 5 gastos y 2 ingresos
- Extrajo montos precisos
- Generó descripciones contextuales (Éxito, Terpel, restaurante japonés, etc.)

---

### 20.10 - Fechas Conflictivas
**Entrada**: `Gasté 50k ayer en comida del 15 de noviembre`
**Salida**: 
```
📝 Registrando 2 operaciones:

1. 💸 Gasto de $50.000 - comida

✅ ¡2 operación(es) registrada(s) exitosamente!
```
**Estado**: ❌ **FALLÓ**
**Problemas**:
1. Dice "2 operaciones" pero solo lista 1
2. Debería ser solo 1 operación
3. No usó ninguna de las fechas mencionadas
**Solución Requerida**: Mejorar el parsing de fechas conflictivas y usar la más específica (15 de noviembre).

---

### 20.11-20.14 - Contexto de Conversación

#### "gaste 50k" (después de contexto)
**Salida**: 
```
💸 Gasto registrado!
• Monto: $50.000
• Categoría: Comida
• Descripción: gasto de comida
```
**Estado**: ✅ **PASÓ**
**Análisis**: Usó el contexto previo para inferir la categoría "Comida".

---

#### "mis gastos"
**Salida**: Lista completa de 34 gastos con total de $1,000,004,551,499
**Estado**: ✅ **PASÓ**
**Análisis**: Muestra todos los gastos con formato humanizado y total.

---

#### "elimina eso" (sin transacción previa mencionada)
**Salida**: 
```
✅ ¡Listo! Eliminé tu último gasto:

💸 $50.000
• Descripción: gasto de comida
• Categoría: Comida

📝 Tu saldo ha sido restaurado.
```
**Estado**: ✅ **PASÓ**
**Análisis**: Entendió que "eso" se refiere al último gasto y lo eliminó correctamente.

---

#### "y los ingresos?" (después de ver gastos)
**Salida**: Lista de 21 ingresos con total de $19,300,000
**Estado**: ✅ **PASÓ**
**Análisis**: Excelente manejo del contexto. Entendió que el usuario quiere ver ingresos después de haber visto gastos.

---

## 🐛 Bugs Identificados

### Bug 1: Sin validación de monto (CRÍTICO)
**Casos afectados**: 20.3, 20.6
**Descripción**: El sistema intenta crear transacciones sin validar que el monto exista y sea mayor a 0.
**Impacto**: Errores 400 del API expuestos al usuario.
**Prioridad**: 🔴 Alta

### Bug 2: Categoría "null" visible
**Caso afectado**: 20.4
**Descripción**: Cuando no hay categoría, se muestra "null" al usuario.
**Impacto**: UX pobre, confusión del usuario.
**Prioridad**: 🟡 Media

### Bug 3: Conteo incorrecto de operaciones
**Caso afectado**: 20.10
**Descripción**: Dice "2 operaciones" pero solo registra 1.
**Impacto**: Confusión del usuario.
**Prioridad**: 🟡 Media

### Bug 4: Fechas conflictivas no manejadas
**Caso afectado**: 20.10
**Descripción**: Cuando hay múltiples fechas, no usa ninguna.
**Impacto**: Transacciones con fecha incorrecta.
**Prioridad**: 🟡 Media

---

## ✅ Puntos Positivos

1. **Mensaje largo (20.9)**: Excelente procesamiento de 7 operaciones en un solo mensaje
2. **Contexto conversacional**: Mantiene contexto para "elimina eso" y "y los ingresos?"
3. **Emojis**: Maneja mensajes con solo emojis de forma natural
4. **Monto negativo**: Convierte correctamente a positivo
5. **Texto sin sentido**: Respuesta amigable sin errores
6. **Humanización**: Las respuestas son naturales y conversacionales

---

## 📋 Acciones Requeridas

| # | Acción | Prioridad | Estado |
|---|--------|-----------|--------|
| 1 | Validar monto > 0 antes de llamar API | 🔴 Alta | Pendiente |
| 2 | Reemplazar "null" por texto descriptivo | 🟡 Media | Pendiente |
| 3 | Corregir conteo de operaciones múltiples | 🟡 Media | Pendiente |
| 4 | Mejorar manejo de fechas conflictivas | 🟡 Media | Pendiente |
| 5 | Pedir confirmación para montos extremos | 🟢 Baja | Opcional |

---

## 📈 Métricas

- **Casos que pasaron**: 9/14 (64%)
- **Casos con fallas críticas**: 2 (Bug de validación)
- **Casos parciales**: 2
- **Tiempo de respuesta promedio**: ~2-3 segundos
