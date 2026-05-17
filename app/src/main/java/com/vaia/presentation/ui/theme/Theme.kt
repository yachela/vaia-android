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
    medium     = RoundedCornerShape(20.dp),
    large      = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val DarkColorScheme = darkColorScheme(
    primary             = Color(0xFF90CAF9), // Blue 200
    onPrimary           = Color(0xFF003063),
    primaryContainer    = Color(0xFF004A9C),
    onPrimaryContainer  = Color(0xFFD3E4FF),
    secondary           = Color(0xFF81D4FA), // Light blue 200
    onSecondary         = Color(0xFF003549),
    secondaryContainer  = Color(0xFF004D64),
    onSecondaryContainer = Color(0xFFB9EAFF),
    tertiary            = SuccessGreen,
    onTertiary          = InkBlack,
    tertiaryContainer   = InkMuted,
    onTertiaryContainer = SurfaceWhite,
    error               = ErrorRed,
    onError             = OnError,
    errorContainer      = ErrorContainer,
    onErrorContainer    = OnErrorContainer,
    background          = Color(0xFF0D1117),
    onBackground        = SurfaceWhite,
    surface             = Color(0xFF161B22),
    onSurface           = SurfaceWhite,
    surfaceVariant      = Color(0xFF1C2333),
    onSurfaceVariant    = LineSoft,
    outline             = LineSoft
)

private val LightColorScheme = lightColorScheme(
    primary              = BluePrimary,
    onPrimary            = Color.White,
    primaryContainer     = BlueContainer,
    onPrimaryContainer   = BlueDeep,
    secondary            = BlueAccent,
    onSecondary          = Color.White,
    secondaryContainer   = BlueContainer,
    onSecondaryContainer = BluePrimary,
    tertiary             = SuccessGreen,
    onTertiary           = SurfaceWhite,
    tertiaryContainer    = SurfaceWhite,
    onTertiaryContainer  = InkMuted,
    error                = ErrorRed,
    onError              = OnError,
    errorContainer       = ErrorContainer,
    onErrorContainer     = OnErrorContainer,
    background           = BlueBackground,
    onBackground         = InkBlack,
    surface              = BlueSurface,
    onSurface            = InkBlack,
    surfaceVariant       = SurfaceSoft,
    onSurfaceVariant     = InkMuted,
    outline              = LineSoft
)

@Composable
fun VaiaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
            window.statusBarColor = if (darkTheme) Color(0xFF0D1117).toArgb() else BlueBackground.toArgb()
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
