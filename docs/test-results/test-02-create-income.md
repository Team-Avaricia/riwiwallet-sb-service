# Test 2: create_income (Registro de Ingresos)

> **Fecha:** 5 de Diciembre, 2025  
> **Intent:** `create_income`  
> **Resultado:** ✅ 10/10 PASSED (100%)

---

## 📊 Resultados por Caso

| # | Estado | Monto | Categoría |
|---|--------|-------|-----------|
| 1 | ✅ | $2.500.000 | Salario |
| 2 | ✅ | $500.000 | Freelance |
| 3 | ✅ | $100.000 | Otros |
| 4 | ✅ | $50.000 | Otros |
| 5 | ✅ | $800.000 | Freelance |
| 6 | ✅ | $1.000.000 | Salario |
| 7 | ✅ | $200.000 | Otros |
| 8 | ✅ | $150.000 | Otros |
| 9 | ✅ | $3.000.000 | Salario |
| 10 | ✅ | $75.000 | Otros |

---

## 💬 Conversación Completa

### Caso 1
**Usuario:**
```
Me pagaron el sueldo de 2.5M
```

**Asistente:**
```
✅ Ingreso registrado correctamente

📝 Detalles:
• Monto: $2.500.000
• Categoría: Salario
• Descripción: sueldo

💰 ¡Sigue así! Puedes ver tu balance con "¿Cuánto tengo disponible?"
```

---

### Caso 2
**Usuario:**
```
Recibí 500k por un trabajo freelance
```

**Asistente:**
```
✅ Ingreso registrado correctamente

📝 Detalles:
• Monto: $500.000
• Categoría: Freelance
• Descripción: trabajo freelance

💰 ¡Sigue así! Puedes ver tu balance con "¿Cuánto tengo disponible?"
```

---

### Caso 3
**Usuario:**
```
Me regalaron 100mil
```

**Asistente:**
```
✅ Ingreso registrado correctamente

📝 Detalles:
• Monto: $100.000
• Categoría: Otros
• Descripción: regalo

💰 ¡Sigue así! Puedes ver tu balance con "¿Cuánto tengo disponible?"
```

---

### Caso 4
**Usuario:**
```
Me dieron 50 lucas por ayudar en algo
```

**Asistente:**
```
✅ Ingreso registrado correctamente

📝 Detalles:
• Monto: $50.000
• Categoría: Otros
• Descripción: ayuda

💰 ¡Sigue así! Puedes ver tu balance con "¿Cuánto tengo disponible?"
```

---

### Caso 5
**Usuario:**
```
Cobré un proyecto de 800k
```

**Asistente:**
```
✅ Ingreso registrado correctamente

📝 Detalles:
• Monto: $800.000
• Categoría: Freelance
• Descripción: proyecto

💰 ¡Sigue así! Puedes ver tu balance con "¿Cuánto tengo disponible?"
```

---

### Caso 6
**Usuario:**
```
Entró mi quincena de 1M
```

**Asistente:**
```
✅ Ingreso registrado correctamente

📝 Detalles:
• Monto: $1.000.000
• Categoría: Salario
• Descripción: quincena

💰 ¡Sigue así! Puedes ver tu balance con "¿Cuánto tengo disponible?"
```

---

### Caso 7
**Usuario:**
```
Vendí algo por 200mil
```

**Asistente:**
```
✅ Ingreso registrado correctamente

📝 Detalles:
• Monto: $200.000
• Categoría: Otros
• Descripción: venta

💰 ¡Sigue así! Puedes ver tu balance con "¿Cuánto tengo disponible?"
```

---

### Caso 8
**Usuario:**
```
Me depositaron 150k de un reembolso
```

**Asistente:**
```
✅ Ingreso registrado correctamente

📝 Detalles:
• Monto: $150.000
• Categoría: Otros
• Descripción: reembolso

💰 ¡Sigue así! Puedes ver tu balance con "¿Cuánto tengo disponible?"
```

---

### Caso 9
**Usuario:**
```
Recibí la nómina de 3M
```

**Asistente:**
```
✅ Ingreso registrado correctamente

📝 Detalles:
• Monto: $3.000.000
• Categoría: Salario
• Descripción: nómina

💰 ¡Sigue así! Puedes ver tu balance con "¿Cuánto tengo disponible?"
```

---

### Caso 10
**Usuario:**
```
Gané 75mil en una apuesta
```

**Asistente:**
```
✅ Ingreso registrado correctamente

📝 Detalles:
• Monto: $75.000
• Categoría: Otros
• Descripción: apuesta

💰 ¡Sigue así! Puedes ver tu balance con "¿Cuánto tengo disponible?"
```

---

## ✅ Conclusiones

- **Extracción de montos:** 100% precisa (incluyendo formatos: "2.5M", "500k", "100mil", "50 lucas", "1M", "3M")
- **Clasificación de categorías:** 100% correcta
  - Salario: sueldo, quincena, nómina
  - Freelance: trabajo freelance, proyecto
  - Otros: regalo, ayuda, venta, reembolso, apuesta
- **Extracción de descripción:** 100% correcta
- **Formato de respuesta:** Consistente y profesional
