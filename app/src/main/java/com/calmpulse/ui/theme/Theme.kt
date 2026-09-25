package com.calmpulse.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = SageGreen,
    onPrimary = SurfaceCard,
    primaryContainer = MistBlue,
    onPrimaryContainer = TextPrimary,
    secondary = SoftLavender,
    onSecondary = TextPrimary,
    background = CalmingBackground,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = MistBlue,
    onSurfaceVariant = TextSecondary,
    outline = SoftLavender.copy(alpha = 0.5f)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkSageGreen,
    onPrimary = DarkBackground,
    primaryContainer = DarkMistBlue,
    onPrimaryContainer = DarkTextPrimary,
    secondary = DarkSoftLavender,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurfaceCard,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkMistBlue
)

val CalmPulseAccentPalettes = listOf(
    Pair("Verde Oficial", WhatsAppGreen),
    Pair("Verde Sálvia", Color(0xFF7A9A85)),
    Pair("Lavanda", Color(0xFF9B8AC4)),
    Pair("Azul Nórdico", Color(0xFF007AFF))
)

@Composable
fun CalmPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    oledDark: Boolean = false,
    accentColor: androidx.compose.ui.graphics.Color = WhatsAppGreen,
    content: @Composable () -> Unit
) {
    val lightColors = lightColorScheme(
        primary = accentColor,
        onPrimary = SurfaceCard,
        primaryContainer = MistBlue,
        onPrimaryContainer = TextPrimary,
        secondary = SoftLavender,
        onSecondary = TextPrimary,
        background = CalmingBackground,
        onBackground = TextPrimary,
        surface = SurfaceCard,
        onSurface = TextPrimary,
        surfaceVariant = MistBlue,
        onSurfaceVariant = TextSecondary,
        outline = SoftLavender.copy(alpha = 0.5f)
    )

    val darkBg = if (oledDark) androidx.compose.ui.graphics.Color(0xFF000000) else DarkBackground
    val darkCard = if (oledDark) androidx.compose.ui.graphics.Color(0xFF0E1317) else DarkSurfaceCard

    val darkColors = darkColorScheme(
        primary = accentColor,
        onPrimary = darkBg,
        primaryContainer = DarkMistBlue,
        onPrimaryContainer = DarkTextPrimary,
        secondary = DarkSoftLavender,
        onSecondary = darkBg,
        background = darkBg,
        onBackground = DarkTextPrimary,
        surface = darkCard,
        onSurface = DarkTextPrimary,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkTextSecondary,
        outline = DarkMistBlue
    )

    val colorScheme = if (darkTheme) darkColors else lightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bgColor = colorScheme.background.toArgb()
            window.statusBarColor = bgColor
            window.navigationBarColor = bgColor
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
