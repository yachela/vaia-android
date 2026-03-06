package com.vaia.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta clara/clean inspirada en la referencia (crema + oliva + verde profundo)
val SkyBackground = Color(0xFFF5F2E8)
val MintPrimary = Color(0xFFB7C95A)
val MintSecondary = Color(0xFFEFF4DC)
val SunAccent = Color(0xFFB5CC3A)
val SurfaceWhite = Color(0xFFFFFEFA)
val SurfaceSoft = Color(0xFFF3F0E6)
val InkBlack = Color(0xFF171A1D)
val InkMuted = Color(0xFF596066)
val LineSoft = Color(0xFFE4E0D2)

// Compat aliases usados en pantallas existentes
val BeigeBackground = SkyBackground
val CreamLight = SurfaceWhite
val CreamMedium = SurfaceSoft
val SandLight = MintSecondary
val SandMedium = LineSoft
val SalmonOrange = SunAccent
val CoralLight = MintPrimary
val Peach = MintSecondary
val WarmBrown = InkMuted
val CharcoalDark = InkBlack
val CharcoalMedium = InkMuted
val CharcoalLight = LineSoft

val SuccessGreen = MintPrimary
val WarningAmber = SunAccent
val ErrorRed = Color(0xFFBA1A1A)
val ErrorContainer = Color(0xFFFFDAD6)
val OnError = Color(0xFFFFFFFF)
val OnErrorContainer = Color(0xFF410002)

val GlassWhite = Color(0x80FFFFFF)
val GlassDark = Color(0x40000000)

// Accessible accent for text/icons on light surfaces (WCAG AA ≥4.5:1 on SkyBackground)
val AccessibleGreen = Color(0xFF3A5700)
