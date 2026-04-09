package com.vaia.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// ── Gold VAIA ──────────────────────────────────────────
val GoldPrimary     = Color(0xFFD6B35B) // dorado principal
val GoldLight       = Color(0xFFE8C96A) // dorado claro
val GoldDeep        = Color(0xFFB8943A) // dorado oscuro (para texto sobre claro)
val GoldSoft        = Color(0xFF2A2418) // contenedor dorado dark mode
val GoldContainer   = Color(0xFFFDF3DC) // contenedor dorado light mode

// ── Dark surfaces ──────────────────────────────────────
val NightBase       = Color(0xFF0F0F0D) // fondo principal dark
val NightSurface    = Color(0xFF1A1A16) // superficie dark
val NightCard       = Color(0xFF232320) // tarjetas dark
val NightBorder     = Color(0xFF2E2E29) // bordes dark

// ── Light surfaces ─────────────────────────────────────
val WarmWhite       = Color(0xFFFAFAF7) // fondo principal light
val WarmSurface     = Color(0xFFFFFFFF) // superficie light
val WarmCard        = Color(0xFFF5F4EF) // tarjetas light
val WarmBorder      = Color(0xFFE8E6DC) // bordes light

// ── Text ───────────────────────────────────────────────
val InkPrimary      = Color(0xFFFAFAF7) // texto dark mode
val InkSecondary    = Color(0xFF9A9888) // texto muted dark
val InkDark         = Color(0xFF1A1A16) // texto light mode
val InkMuted        = Color(0xFF6B6858) // texto muted light

// ── Semantic ───────────────────────────────────────────
val SuccessGreen    = Color(0xFF4CAF50)
val WarningAmber    = Color(0xFFFFA726)
val ErrorRed        = Color(0xFFCF6679)
val ErrorContainer  = Color(0xFF3B1219)
val OnError         = Color(0xFFFFFFFF)
val OnErrorContainer = Color(0xFFFFB3BC)

// ── Misc ───────────────────────────────────────────────
val Transparent     = Color(0x00000000)

// ── Aliases legacy (para no romper código existente) ───
val BluePrimary     = GoldPrimary
val BlueLight       = GoldLight
val BlueBackground  = WarmWhite
val BlueSurface     = WarmSurface
val BlueAccent      = GoldDeep
val BlueDeep        = GoldDeep
val BlueContainer   = GoldContainer
val BlueSoft        = WarmCard
val SkyBackground   = WarmWhite
val MintPrimary     = GoldLight
val MintSecondary   = GoldContainer
val SunAccent       = GoldPrimary
val AccessibleGreen = GoldDeep
val SurfaceWhite    = WarmSurface
val SurfaceSoft     = WarmCard
val LineSoft        = WarmBorder
val GlassWhite      = Color(0x80FFFFFF)
val GlassDark       = Color(0x40000000)
val InkBlack        = InkDark
val GreenPrimary    = SuccessGreen
val SalmonOrange    = Color(0xFFE07B5C) // acento cálido VAIA
