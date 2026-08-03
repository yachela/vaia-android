# Auditoría de Validación de Datos en Formularios - VAIA Android

**Fecha**: 2026-07-19  
**Alcance**: `vaia-android/app/src/main/java/com/vaia/presentation/ui/`  
**Archivos analizados**: 12 screens + 2 utils + 1 viewmodel

---

## Resumen Ejecutivo

La validación de datos en formularios está **dispersa, inconsistente y incompleta**. Se encuentra principalmente en los `confirmButton` de los dialogs (capa UI), sin centralización ni tests. Faltan validaciones críticas: números negativos, rangos, fechas consistentes con el viaje, sanitización de inputs, y política de contraseñas.

---

## Hallazgos por Pantalla

### 1. ExpensesScreen.kt → CreateExpenseDialog (líneas 379-519)

**Campos validados actualmente**:
- `description`: `isBlank()` → error "description_required_error"
- `amount`: `replace(",", ".").toDoubleOrNull()` → null = error "amount_numeric_error"
- `date`: `normalizeDateForApi(date)` → null = error "date_valid_error"

**❌ Faltantes críticos**:
| Campo | Validación faltante | Riesgo |
|-------|---------------------|--------|
| `amount` | Negativos, > 999999.99, > 2 decimales, notación científica | Inyección de gastos negativos = ingreso falso; overflow DB |
| `date` | Fuera de rango `[trip.startDate, trip.endDate]` | Gasto en fecha inválida rompe reportes/presupuesto |
| `description` | Longitud máx, sanitización HTML/JS | XSS persistente si se muestra sin escape |
| `category` | Valor en lista permitida (dropdown lo limita pero no valida server-side) | Inconsistencia datos |

**Código problemático** (líneas 498-510):
```kotlin
val amountValue = amount.replace(",", ".").toDoubleOrNull()
localError = when {
    description.isBlank() -> descriptionRequiredError
    amountValue == null -> amountNumericError      // Genérico, no diferencia vacío vs letras vs negativos
    normalizedDate == null -> dateValidError
    else -> null
}
```

---

### 2. ActivitiesScreen.kt → CreateActivityDialog (líneas 1151-1312)

**Campos validados actualmente**:
- `title`: `isBlank()` → "title_required_error"
- `date`: `normalizeDateForApi()` → null = "date_valid_error"
- `time`: `normalizeTimeForApi()` → null = "time_format_error" (solo si no blank)
- `cost`: `replace(",", ".").toDoubleOrNull()` → null = "amount_numeric_error"

**❌ Faltantes críticos**:
| Campo | Validación faltante |
|-------|---------------------|
| `cost` | Negativos, máximos, decimales (igual que amount) |
| `date` | Rango viaje (startDate-endDate) |
| `time` | Formato estricto HH:mm, rango 00:00-23:59 |
| `title` | Longitud máx, caracteres permitidos, sanitización |
| `description` | Longitud máx, sanitización |
| `location` | Longitud máx, sanitización |

**Nota**: `cost` permite string vacío → 0.0 (línea 1285), pero no valida negativos.

---

### 3. LoginScreen.kt (líneas 40-197) & RegisterScreen.kt (líneas 42-301)

**Validaciones actuales**:
```kotlin
// Login
validateEmail() -> android.util.Patterns.EMAIL_ADDRESS  // Permisivo RFC 5322
password.length < 8 -> "mín 8 chars"

// Register
name.length < 2 -> "mín 2 chars"
validateEmail() -> mismo
password.length < 8 -> "mín 8 chars"
password != confirmPassword -> "no coinciden"
```

**❌ Faltantes críticos (OWASP ASVS V2/V3)**:
| Campo | Validación faltante | Referencia |
|-------|---------------------|------------|
| `email` | Longitud ≤ 254 (RFC 5321), dominio válido (MX), bloquear disposable | ASVS 2.1.3 |
| `password` | Min 12 chars, 1 mayúscula, 1 minúscula, 1 número, 1 símbolo, check HIBP | ASVS 2.1.1, 2.1.2 |
| `name` | Solo letras/espacios/guiones, 2-50 chars, sin caracteres de control | ASVS 5.1.1 |

---

### 4. VaiaDatePickerField.kt (líneas 36-106)

**Comportamiento actual**:
- Usa `Material3 DatePickerDialog` nativo
- Formato salida: `yyyy-MM-dd` UTC via `SimpleDateFormat`
- No validación de rango, solo UI picker

**❌ Faltantes**:
- `minDate` / `maxDate` configurables (para limitar a rango viaje)
- Validación post-selección en ViewModel/UI

---

### 5. DateTimeUiUtils.kt (líneas 30-85)

**Funciones**: `normalizeDateForApi()`, `normalizeTimeForApi()`, `formatDateForDisplay()`, `formatTimeForDisplay()`

**Fortalezas**:
- `isLenient = false` → parsing estricto
- Múltiples patrones backend soportados
- Timezone UTC explícito

**❌ Limitaciones**:
- `normalizeDateForApi` devuelve `null` si no matchea → error genérico en UI
- No valida lógica de negocio (rango viaje, futuro/pasado)
- `normalizeTimeForApi` acepta epoch millis/seconds → vector de confusión

---

### 6. PackingListScreen.kt → AddItemDialog (líneas 390-450)

**Validaciones actuales**:
- `itemName.isNotBlank()` → habilita botón

**❌ Faltantes**:
- Longitud máx nombre
- Sanitización
- Categoría en lista permitida (dropdown limita UI pero no valida)

---

### 7. DocumentsScreen.kt → UploadDocumentDialog (líneas 243-348)

**Validaciones actuales**:
- Ninguna en cliente (solo `selectedCategory` opcional, `description` opcional)
- Validación delegada 100% a backend

**❌ Faltantes**:
- Tipo MIME / extensión permitida
- Tamaño máx archivo (antes de upload)
- Sanitización `description`, `fileName`

---

## Patrones Sistémicos Problemáticos

### 1. Validación en Capa UI (Anti-pattern)
```kotlin
// En CreateExpenseDialog.confirmButton.onClick
val amountValue = amount.replace(",", ".").toDoubleOrNull()
localError = when {
    amountValue == null -> amountNumericError  // UI decide lógica de negocio
}
```
**Problema**: Lógica de negocio en Composable → no testeable unitariamente, duplicada en cada dialog.

### 2. Errores Genéricos
```kotlin
amountValue == null -> amountNumericError  // No diferencia: vacío, "abc", "-50", "1.234"
```
**Problema**: UX pobre + debugging difícil + no guía al usuario.

### 3. Sin Validación Cross-Field
Ningún formulario valida `date` contra `trip.startDate` / `trip.endDate`. El viaje está en ViewModel pero no se pasa al dialog.

### 4. Sin Sanitización de Inputs Libres
Campos `description`, `title`, `location`, `name` aceptan `<script>`, SQL, caracteres de control.

### 5. Parsing Decimal Locale-Dependent
```kotlin
amount.replace(",", ".").toDoubleOrNull()
```
Falla en locales donde `.` es separador de miles (ej. `1.234,56` DE/ES). Debe usar `NumberFormat.getInstance(Locale.US)`.

---

## Matriz de Riesgo (OWASP ASVS / CWE)

| CWE | Hallazgo | Severidad | Archivos afectados |
|-----|----------|-----------|-------------------|
| CWE-20 | Validación insuficiente de input numérico | **Alta** | Expenses, Activities |
| CWE-79 | Falta sanitización XSS en campos texto | **Alta** | Todos los dialogs |
| CWE-1188 | Validación solo en cliente (insecure) | **Media** | Todos |
| CWE-522 | Contraseñas sin política de fuerza | **Alta** | Auth |
| CWE-20 | Fechas sin rango lógico | **Media** | Expenses, Activities |
| CWE-1284 | Parsing numérico locale-dependent | **Media** | Expenses, Activities |

---

## Recomendaciones Priorizadas

### 🔴 Crítico (Hacer ya)
1. **Centralizar validadores** en `presentation/ui/common/Validators.kt` con tests unitarios
2. **Mover validaciones a ViewModel/UseCase** — UI solo renderiza errores
3. **Validar `amount`/`cost`**: rango [0.01, 999999.99], 2 decimales máx, sin notación científica
4. **Validar fechas contra viaje** — pasar `tripStartDate`/`tripEndDate` a dialogs
5. **Política de contraseñas** (min 12, complejidad, HIBP check en backend)

### 🟡 Alto (Próximo sprint)
6. **Sanitización HTML/JS** en todos los campos texto libre (OWASP Java HTML Sanitizer o similar)
7. **Errores específicos por campo** (no genéricos): "El monto no puede ser negativo", "Máximo 2 decimales"
8. **VaiaDatePickerField** → aceptar `minDate`/`maxDate` params
9. **Parsing decimal locale-safe** usando `NumberFormat(Locale.US)`

### 🟢 Medio (Deuda técnica)
10. Tests de validación: boundary values, property-based testing
11. Input masks en UI (ej. `##,##` para amount, `##:##` para time)
12. Validación server-side documentada y consistente con cliente (contract tests)

---

## Archivos a Crear/Modificar

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| Crear | `presentation/ui/common/Validators.kt` | Validadores puros, testables, reutilizables |
| Crear | `presentation/ui/common/ValidationResult.kt` | Sealed class `Valid / Invalid(errors: Map<Field, String>)` |
| Modificar | `ExpensesViewModel.createExpense()` | Validar antes de llamar repo |
| Modificar | `ActivitiesViewModel.createActivity()` | Validar antes de llamar repo |
| Modificar | `AuthViewModel.register()` | Política de contraseña |
| Modificar | `CreateExpenseDialog` | Usar `Validators`, mostrar errores por campo |
| Modificar | `CreateActivityDialog` | Usar `Validators`, pasar rango viaje |
| Modificar | `VaiaDatePickerField` | Params `minDate`, `maxDate` |
| Test | `ValidatorsTest.kt` | > 90% cobertura validadores |

---

## Referencias
- [OWASP ASVS v4.0.3 - V2 Authentication](https://github.com/OWASP/ASVS)
- [OWASP ASVS v4.0.3 - V5 Input Validation](https://github.com/OWASP/ASVS)
- [CWE-20: Improper Input Validation](https://cwe.mitre.org/data/definitions/20.html)
- [CWE-79: Cross-site Scripting](https://cwe.mitre.org/data/definitions/79.html)
- [Android Input Validation Best Practices](https://developer.android.com/training/articles/security-tips#InputValidation)