# Test 20.9 - Mensaje Muy Largo (>1000 caracteres)

## 📋 Descripción del Caso
**Objetivo**: Verificar que el asistente puede procesar correctamente mensajes extensos con múltiples detalles, manteniendo la precisión en la extracción de información financiera.

## 🧪 Mensaje de Prueba

### Mensaje Enviado (1,247 caracteres):
```
Hola! Necesito contarte todo lo que pasó hoy con mis finanzas. Primero, en la mañana fui al supermercado Éxito del centro comercial Santafé y gasté aproximadamente 185.000 pesos en mercado para la semana, incluyendo frutas, verduras, carnes, productos de aseo y algunas cosas para el desayuno. Después, alrededor del mediodía, paré en una estación de gasolina Terpel y tanqueé el carro por 120.000 pesos porque el tanque estaba casi vacío. Luego de eso, como tenía hambre, pasé por un restaurante de comida japonesa donde almorcé un combo de sushi con gyozas y una limonada, todo por 65.000 pesos. En la tarde, mi jefe me transfirió el bono de productividad del mes que son 800.000 pesos, así que fue un buen día! También pagué la factura del celular de Claro que eran 55.000 pesos mensuales. Ah, y casi se me olvida, le presté 200.000 pesos a mi hermano que me los devuelve la próxima semana. Por cierto, también recibí el reembolso de un producto que devolví en Amazon por 150.000 pesos. En total fue un día bastante movido financieramente. ¿Puedes registrar todo esto y después decirme cómo quedó mi balance?
```

---

## 📊 Resultado Esperado

### Operaciones que deberían detectarse:

| # | Tipo | Monto | Categoría | Descripción |
|---|------|-------|-----------|-------------|
| 1 | 💸 Gasto | $185,000 | Mercado/Comida | Supermercado Éxito |
| 2 | 💸 Gasto | $120,000 | Transporte | Gasolina Terpel |
| 3 | 💸 Gasto | $65,000 | Comida | Restaurante japonés/sushi |
| 4 | 💰 Ingreso | $800,000 | Salario/Bonos | Bono de productividad |
| 5 | 💸 Gasto | $55,000 | Servicios | Factura Claro |
| 6 | 💸 Gasto | $200,000 | Otros/Préstamo | Préstamo a hermano |
| 7 | 💰 Ingreso | $150,000 | Otros | Reembolso Amazon |

**Total gastos**: $625,000  
**Total ingresos**: $950,000  
**Balance neto del mensaje**: +$325,000

---

## ✅ Resultado Obtenido

### Respuesta del Asistente:
```
[PEGAR AQUÍ LA RESPUESTA REAL DEL BOT]
```

### Intents Detectados:
```
[PEGAR AQUÍ LOS LOGS DE CLASIFICACIÓN]
```

---

## 📝 Análisis

### ¿Se procesó el mensaje completo?
- [ ] Sí, sin truncamiento
- [ ] Parcialmente (se perdió información)
- [ ] No, dio error

### ¿Se detectaron todas las operaciones?
- [ ] 7/7 operaciones detectadas
- [ ] Parcial: __/7 operaciones detectadas
- [ ] Solo 1 operación detectada

### Operaciones faltantes (si aplica):
- 

### ¿Se pidió el balance al final como solicitó?
- [ ] Sí
- [ ] No

### Tiempo de respuesta:
- Tiempo aproximado: __ segundos

---

## 🔍 Observaciones

### Puntos positivos:
1. 

### Puntos a mejorar:
1. 

### Notas adicionales:
- El mensaje incluye variedad de formatos de montos (185.000, 120.000, 65.000, etc.)
- Incluye contexto temporal (mañana, mediodía, tarde)
- Mezcla ingresos y gastos en el mismo mensaje
- Incluye una solicitud adicional al final (ver balance)
- Contiene nombres propios de comercios (Éxito, Terpel, Claro, Amazon)

---

## 📈 Métricas de Evaluación

| Criterio | Puntuación (1-5) | Notas |
|----------|------------------|-------|
| Precisión en montos | /5 | |
| Clasificación de categorías | /5 | |
| Detección de todas las operaciones | /5 | |
| Manejo del contexto | /5 | |
| Tiempo de respuesta | /5 | |
| Claridad de la respuesta | /5 | |

**Puntuación Total**: __/30

---

## 🎯 Veredicto

- [ ] ✅ **PASÓ** - El asistente procesó correctamente el mensaje largo
- [ ] ⚠️ **PARCIAL** - Funciona pero con limitaciones
- [ ] ❌ **FALLÓ** - No pudo procesar el mensaje correctamente

---

## 📅 Información del Test
- **Fecha**: 2025-12-07
- **Modo**: [ ] Producción / [ ] Mock
- **Versión**: 1.0-SNAPSHOT
