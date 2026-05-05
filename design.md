# VAIA Design System

## Estándar de Diseño

Este documento define el sistema de diseño para la app VAIA Android, basado en Material Design 3 (Material You).

---

## Colores

### Paleta Principal (Blue VAIA)

| Token | Hex | Uso |
|-------|-----|-----|
| `BluePrimary` | `#1565C0` | Primary button, links, highlights |
| `BlueLight` | `#42A5F5` | Badges, iconos activos |
| `BlueDeep` | `#0D47A1` | Texto sobre backgrounds claros |
| `BlueBackground` | `#F4F7FF` | Fondo principal |
| `BlueSurface` | `#FFFFFF` | Superficies/cards |
| `BlueAccent` | `#1E88E5` | Estados activos |

### Semantic Colors

| Token | Hex | Uso |
|-------|-----|-----|
| `ErrorRed` | `#BA1A1A` | Errores, validación |
| `SuccessGreen` | `#4CAF50` | Estados exitoso |
| `WarningAmber` | `#FFA726` | Warnings |

### Text Colors

| Token | Hex | Uso |
|-------|-----|-----|
| `InkBlack` | `#171A1D` | Texto principal |
| `InkMuted` | `#596066` | Texto secundario |

---

## Tipografía

Sistema baseado en Material 3 Typography:

| Style | Size | Weight | LineHeight |
|-------|------|--------|----------|
| `displayLarge` | 57sp | Normal | 64sp |
| `displayMedium` | 45sp | Normal | 52sp |
| `headlineLarge` | 32sp | Normal | 40sp |
| `headlineMedium` | 28sp | Normal | 36sp |
| `titleLarge` | 22sp | Normal | 28sp |
| `titleMedium` | 16sp | Medium | 24sp |
| `bodyLarge` | 16sp | Normal | 24sp |
| `bodyMedium` | 14sp | Normal | 20sp |
| `labelLarge` | 14sp | Medium | 20sp |
| `labelMedium` | 12sp | Medium | 16sp |

---

## Shapes

RadiosCustom VAIA (diferentes al MD3 estándar):

| Token | Radius |
|-------|--------|
| `extraSmall` | 8dp |
| `small` | 12dp |
| `medium` | 20dp |
| `large` | 28dp |
| `extraLarge` | 32dp |

> **Nota**: Estos valores son intencionalmente más grandes que MD3 (4/8/12/16/28dp) para un look más friendly.

---

## Componentes

### Buttons

| Componente | MD3 Equivalent | Uso |
|------------|---------------|-----|
| `WaypathButton(primary=true)` | `Button` | Primary action |
| `WaypathButton(primary=false)` | `FilledTonalButton` | Secondary action |

### Cards

| Componente | MD3 Equivalent | Uso |
|------------|---------------|-----|
| `WaypathCard` | `Card` | Tarjetas con elevation |

### Badges

| Componente | MD3 Equivalent | Uso |
|------------|---------------|-----|
| `WaypathBadge` | `AssistChip` | Badges de estado |

### Navigation

| Componente | MD3 Equivalent | Uso |
|------------|---------------|-----|
| `AppQuickBar` | `NavigationBar` | Bottom navigation |

---

## Dynamic Color

- **Activado por defecto**: `dynamicColor = true` en `VaiaTheme`
- **Soporte**: Android 12+ (API 31)
- **Fallback**: `LightColorScheme` / `DarkColorScheme` en versiones anteriores

---

## Accesibilidad

### Requisitos

- Todos los `Icon` deben tener `contentDescription`
- Minimum touch target: 48dp
- Contrastes WCAG AA compliant (ver `Color.kt` lines 64-76)

### Content Descriptions Requeridas

```
Home icon → "Inicio"
Explore icon → "Explorar"
List icon → "Viajes"
Person icon → "Perfil"
```

---

## Dark Mode

- Soporte completo incluido
- Detección automática via `isSystemInDarkTheme()`
- Manual toggle disponible en ProfileScreen

---

## Futuras Migraciones

### Pendientes

1. **Navigation Compose**: Migrar de callbacks lambda a NavHost
2. **MD3 Dialogs**: Estandarizar AlertDialogs
3. **FAB**: Agregar en pantallas que tenga acción principal

### Nice to Have

1. Migrar `WaypathButton` a `Button` MD3 estándar
2. Migrar `WaypathCard` a `Card` MD3
3. Considerar `AssistChip` en lugar de `WaypathBadge`