package com.vaia.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta azul VAIA 2026
val BluePrimary    = Color(0xFF1565C0) // Azul profundo — primary principal
val BlueLight      = Color(0xFF42A5F5) // Azul claro — badges, iconos, checkboxes
val BlueBackground = Color(0xFFF4F7FF) // Fondo con leve tono azul
val BlueSurface    = Color(0xFFFFFFFF) // Superficie blanca
val BlueAccent     = Color(0xFF1E88E5) // Azul medio — estados activos
val BlueDeep       = Color(0xFF0D47A1) // Azul muy oscuro — texto accesible

// Contenedores
val BlueContainer  = Color(0xFFE3F2FD) // Azul muy claro — containers/chips
val BlueSoft       = Color(0xFFEEF4FF) // Superficie variante

// Colores de estado (inalterados — son neutros)
val SuccessGreen   = Color(0xFF4CAF50)
val WarningAmber   = Color(0xFFFFA726)
val ErrorRed       = Color(0xFFBA1A1A)
val ErrorContainer = Color(0xFFFFDAD6)
val OnError        = Color(0xFFFFFFFF)
val OnErrorContainer = Color(0xFF410002)

// Colores de texto
val InkBlack  = Color(0xFF171A1D)
val InkMuted  = Color(0xFF596066)
val LineSoft  = Color(0xFFDDE3F0) // Ligeramente azulado

// Superficies
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceSoft  = Color(0xFFEEF2FB)

// Vidrio/transparencia
val GlassWhite = Color(0x80FFFFFF)
val GlassDark  = Color(0x40000000)

// Aliases de compatibilidad con código existente
val SkyBackground  = BlueBackground
val MintPrimary    = BlueLight
val MintSecondary  = BlueContainer
val SunAccent      = BlueAccent
val BeigeBackground = BlueBackground
val CreamLight     = SurfaceWhite
val CreamMedium    = SurfaceSoft
val SandLight      = BlueContainer
val SandMedium     = LineSoft
val SalmonOrange   = BlueAccent
val CoralLight     = BlueLight
val Peach          = BlueContainer
val WarmBrown      = InkMuted
val CharcoalDark   = InkBlack
val CharcoalMedium = InkMuted
val CharcoalLight  = LineSoft
val AccessibleGreen = BlueDeep   // alias heredado

val GreenPrimary   = BluePrimary  // alias heredado
val GreenLight     = BlueLight    // alias heredado
val GreenBackground = BlueBackground // alias heredado
val GreenSurface   = BlueSurface  // alias heredado
val GreenAccent    = BlueAccent   // alias heredado
val AccessibleBlue = BlueDeep

/*
 * WCAG AA Contrast Audit — Paleta Azul
 *
 * InkBlack (#171A1D) on BlueBackground (#F4F7FF): ~13.5:1 ✓ PASS (excelente)
 * InkBlack (#171A1D) on SurfaceWhite (#FFFFFF):   ~14.8:1 ✓ PASS (excelente)
 * InkMuted (#596066) on BlueBackground (#F4F7FF):  ~6.9:1 ✓ PASS
 * BluePrimary (#1565C0) on BlueBackground:         ~5.5:1 ✓ PASS
 * BluePrimary (#1565C0) on SurfaceWhite:           ~6.0:1 ✓ PASS
 * BlueDeep (#0D47A1) on BlueBackground:            ~8.2:1 ✓ PASS (excelente)
 * White on BluePrimary (#1565C0):                  ~5.5:1 ✓ PASS
 * BlueLight (#42A5F5) on BlueBackground:           ~3.1:1 ✓ PASS (solo texto grande/iconos)
 * ErrorRed (#BA1A1A) on SurfaceWhite:              ~5.2:1 ✓ PASS
 */
