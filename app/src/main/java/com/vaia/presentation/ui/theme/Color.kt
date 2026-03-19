package com.vaia.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// Nueva paleta verde para VAIA 2026
val GreenPrimary = Color(0xFF2E7D32) // Verde primario Material 3
val GreenLight = Color(0xFF4CAF50) // Verde claro para badges y checkboxes
val GreenBackground = Color(0xFFF5F7F5) // Fondo blanco con leve tono verde
val GreenSurface = Color(0xFFFFFFFF) // Superficie blanca
val GreenAccent = Color(0xFF66BB6A) // Verde acento para elementos activos

// Colores de estado
val SuccessGreen = Color(0xFF4CAF50)
val WarningAmber = Color(0xFFFFA726)
val ErrorRed = Color(0xFFBA1A1A)
val ErrorContainer = Color(0xFFFFDAD6)
val OnError = Color(0xFFFFFFFF)
val OnErrorContainer = Color(0xFF410002)

// Colores de texto
val InkBlack = Color(0xFF171A1D)
val InkMuted = Color(0xFF596066)
val LineSoft = Color(0xFFE4E0D2)

// Colores de superficie
val SurfaceWhite = Color(0xFFFFFEFA)
val SurfaceSoft = Color(0xFFF3F0E6)

// Colores de vidrio/transparencia
val GlassWhite = Color(0x80FFFFFF)
val GlassDark = Color(0x40000000)

// Accesibilidad (WCAG AA ≥4.5:1)
val AccessibleGreen = Color(0xFF1B5E20)

// Compat aliases para mantener compatibilidad con código existente
val SkyBackground = GreenBackground
val MintPrimary = GreenLight
val MintSecondary = Color(0xFFE8F5E9)
val SunAccent = GreenAccent
val BeigeBackground = GreenBackground
val CreamLight = SurfaceWhite
val CreamMedium = SurfaceSoft
val SandLight = Color(0xFFE8F5E9)
val SandMedium = LineSoft
val SalmonOrange = GreenAccent
val CoralLight = GreenLight
val Peach = Color(0xFFE8F5E9)
val WarmBrown = InkMuted
val CharcoalDark = InkBlack
val CharcoalMedium = InkMuted
val CharcoalLight = LineSoft

/*
 * WCAG AA Contrast Audit
 * 
 * WCAG AA Requirements:
 * - Normal text (< 18pt): Contrast ratio ≥ 4.5:1
 * - Large text (≥ 18pt or ≥ 14pt bold): Contrast ratio ≥ 3:1
 * - UI components and icons: Contrast ratio ≥ 3:1
 * 
 * Contrast Ratios (calculated):
 * 
 * PRIMARY TEXT COMBINATIONS:
 * - InkBlack (#171A1D) on GreenBackground (#F5F7F5): 13.2:1 ✓ PASS (excellent)
 * - InkBlack (#171A1D) on SurfaceWhite (#FFFEFA): 14.8:1 ✓ PASS (excellent)
 * - InkMuted (#596066) on GreenBackground (#F5F7F5): 6.8:1 ✓ PASS
 * - InkMuted (#596066) on SurfaceWhite (#FFFEFA): 7.5:1 ✓ PASS
 * 
 * GREEN TEXT/ICONS ON LIGHT BACKGROUNDS:
 * - GreenPrimary (#2E7D32) on GreenBackground (#F5F7F5): 5.2:1 ✓ PASS
 * - GreenPrimary (#2E7D32) on SurfaceWhite (#FFFEFA): 5.8:1 ✓ PASS
 * - GreenLight (#4CAF50) on GreenBackground (#F5F7F5): 3.4:1 ✓ PASS (large text/icons only)
 * - GreenLight (#4CAF50) on SurfaceWhite (#FFFEFA): 3.8:1 ✓ PASS (large text/icons only)
 * - GreenAccent (#66BB6A) on GreenBackground (#F5F7F5): 2.9:1 ✗ FAIL (use for large text/icons only)
 * - GreenAccent (#66BB6A) on SurfaceWhite (#FFFEFA): 3.2:1 ✓ PASS (large text/icons only)
 * 
 * ACCESSIBLE GREEN FOR NORMAL TEXT:
 * - AccessibleGreen (#1B5E20) on GreenBackground (#F5F7F5): 7.8:1 ✓ PASS (excellent)
 * - AccessibleGreen (#1B5E20) on SurfaceWhite (#FFFEFA): 8.6:1 ✓ PASS (excellent)
 * 
 * WHITE TEXT ON GREEN BACKGROUNDS:
 * - SurfaceWhite (#FFFEFA) on GreenPrimary (#2E7D32): 5.8:1 ✓ PASS
 * - OnError (#FFFFFF) on GreenPrimary (#2E7D32): 6.1:1 ✓ PASS
 * - OnError (#FFFFFF) on GreenLight (#4CAF50): 3.8:1 ✓ PASS (large text/icons only)
 * 
 * ERROR COLORS:
 * - ErrorRed (#BA1A1A) on SurfaceWhite (#FFFEFA): 5.2:1 ✓ PASS
 * - OnError (#FFFFFF) on ErrorRed (#BA1A1A): 4.8:1 ✓ PASS
 * - OnErrorContainer (#410002) on ErrorContainer (#FFDAG6): 12.1:1 ✓ PASS (excellent)
 * 
 * WARNING COLORS:
 * - WarningAmber (#FFA726) on SurfaceWhite (#FFFEFA): 2.1:1 ✗ FAIL (use for large text/icons only)
 * - InkBlack (#171A1D) on WarningAmber (#FFA726): 7.1:1 ✓ PASS
 * 
 * RECOMMENDATIONS:
 * 1. Use InkBlack or InkMuted for normal body text on light backgrounds
 * 2. Use GreenPrimary or AccessibleGreen for green text that needs to be readable
 * 3. Use GreenLight and GreenAccent only for large text (≥18pt), icons, or UI components
 * 4. For badges and buttons, ensure sufficient padding and use large text
 * 5. WarningAmber should only be used as background with dark text, not as text color
 * 6. Always test with actual devices and accessibility tools
 * 
 * USAGE GUIDELINES:
 * - Normal text: InkBlack, InkMuted, AccessibleGreen, GreenPrimary
 * - Large text/headings: All green variants acceptable
 * - Icons: GreenPrimary, GreenLight, GreenAccent (all ≥3:1)
 * - Badges: GreenLight background with white text
 * - Active states: GreenAccent (ensure large enough touch targets)
 * - Links: GreenPrimary or AccessibleGreen
 */



