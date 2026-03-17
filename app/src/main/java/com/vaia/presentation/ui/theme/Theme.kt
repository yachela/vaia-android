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

// Formas con curvas pronunciadas (20-30dp) para el estilo cálido
val VaiaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),  // Curvas pronunciadas para cards principales
    large = RoundedCornerShape(28.dp),   // Más redondeado para elementos grandes
    extraLarge = RoundedCornerShape(32.dp) // Muy redondeado para elementos destacados
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenLight,
    onPrimary = Color.White,
    primaryContainer = GreenPrimary,
    onPrimaryContainer = Color.White,
    secondary = GreenAccent,
    onSecondary = InkBlack,
    secondaryContainer = Color(0xFF1B5E20),
    onSecondaryContainer = Color.White,
    tertiary = GreenLight,
    onTertiary = InkBlack,
    tertiaryContainer = InkMuted,
    onTertiaryContainer = SurfaceWhite,
    error = ErrorRed,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Color(0xFF13161C),
    onBackground = SurfaceWhite,
    surface = Color(0xFF1B2028),
    onSurface = SurfaceWhite,
    surfaceVariant = Color(0xFF222733),
    onSurfaceVariant = LineSoft,
    outline = LineSoft
)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = MintSecondary,
    onPrimaryContainer = GreenPrimary,
    secondary = GreenAccent,
    onSecondary = Color.White,
    secondaryContainer = MintSecondary,
    onSecondaryContainer = GreenPrimary,
    tertiary = AccessibleGreen,
    onTertiary = SurfaceWhite,
    tertiaryContainer = SurfaceWhite,
    onTertiaryContainer = InkMuted,
    error = ErrorRed,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = GreenBackground,
    onBackground = InkBlack,
    surface = GreenSurface,
    onSurface = InkBlack,
    surfaceVariant = SurfaceSoft,
    onSurfaceVariant = InkMuted,
    outline = LineSoft
)

@Composable
fun VaiaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Desactivamos colores dinámicos para mantener la paleta cálida consistente
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = if (darkTheme) Color(0xFF13161C).toArgb() else GreenBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = VaiaShapes,  // Usamos nuestras formas con curvas pronunciadas
        content = content
    )
}
