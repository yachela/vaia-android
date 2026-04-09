package com.vaia.presentation.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val VaiaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val DarkColorScheme = darkColorScheme(
    primary              = GoldPrimary,
    onPrimary            = NightBase,
    primaryContainer     = GoldSoft,
    onPrimaryContainer   = GoldLight,
    secondary            = GoldLight,
    onSecondary          = NightBase,
    secondaryContainer   = NightCard,
    onSecondaryContainer = InkPrimary,
    tertiary             = GoldDeep,
    onTertiary           = WarmWhite,
    tertiaryContainer    = NightBorder,
    onTertiaryContainer  = InkSecondary,
    error                = ErrorRed,
    onError              = OnError,
    errorContainer       = ErrorContainer,
    onErrorContainer     = OnErrorContainer,
    background           = NightBase,
    onBackground         = InkPrimary,
    surface              = NightSurface,
    onSurface            = InkPrimary,
    surfaceVariant       = NightCard,
    onSurfaceVariant     = InkSecondary,
    outline              = NightBorder,
    outlineVariant       = Color(0xFF3E3D38),
    scrim                = Color(0x99000000),
    inverseSurface       = WarmCard,
    inverseOnSurface     = InkDark,
    inversePrimary       = GoldDeep,
)

private val LightColorScheme = lightColorScheme(
    primary              = GoldDeep,
    onPrimary            = WarmWhite,
    primaryContainer     = GoldContainer,
    onPrimaryContainer   = GoldDeep,
    secondary            = GoldPrimary,
    onSecondary          = WarmWhite,
    secondaryContainer   = GoldContainer,
    onSecondaryContainer = GoldDeep,
    tertiary             = GoldDeep,
    onTertiary           = WarmWhite,
    tertiaryContainer    = WarmCard,
    onTertiaryContainer  = InkMuted,
    error                = Color(0xFFB00020),
    onError              = WarmWhite,
    errorContainer       = Color(0xFFFFDAD6),
    onErrorContainer     = Color(0xFF410002),
    background           = WarmWhite,
    onBackground         = InkDark,
    surface              = WarmSurface,
    onSurface            = InkDark,
    surfaceVariant       = WarmCard,
    onSurfaceVariant     = InkMuted,
    outline              = WarmBorder,
    outlineVariant       = Color(0xFFDDD8C4),
    scrim                = Color(0x99000000),
    inverseSurface       = NightCard,
    inverseOnSurface     = InkPrimary,
    inversePrimary       = GoldLight,
)

@Composable
fun VaiaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (darkTheme) NightBase.toArgb() else WarmWhite.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = VaiaShapes,
        content     = content
    )
}
