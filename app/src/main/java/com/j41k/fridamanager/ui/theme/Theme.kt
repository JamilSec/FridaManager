package com.j41k.fridamanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * La app es intencionadamente solo-oscura (herramienta de análisis usada junto a
 * terminales/IDEs). Las barras del sistema se configuran en MainActivity con
 * enableEdgeToEdge(SystemBarStyle.dark) para que los iconos sean claros aunque
 * el sistema esté en modo claro.
 */
private val ProColorScheme = darkColorScheme(
    primary             = AccentPrimary,
    onPrimary           = TextOnAccent,
    primaryContainer    = AccentPrimaryLo,
    onPrimaryContainer  = TextPrimary,

    secondary           = AccentCyan,
    onSecondary         = TextOnAccent,
    secondaryContainer  = AccentPrimary.copy(alpha = 0.18f),
    onSecondaryContainer = AccentPrimaryHi,

    tertiary            = StatusOnline,
    onTertiary          = TextOnAccent,

    background          = SurfaceBase,
    onBackground        = TextPrimary,
    surface             = SurfaceBase,
    onSurface           = TextPrimary,
    surfaceVariant      = SurfaceMid,
    onSurfaceVariant    = TextSecondary,
    surfaceContainerLowest = SurfaceBase,
    surfaceContainerLow = SurfaceLow,
    surfaceContainer    = SurfaceLow,
    surfaceContainerHigh = SurfaceMid,
    surfaceContainerHighest = SurfaceHigh,
    inverseSurface      = TextPrimary,
    inverseOnSurface    = SurfaceBase,
    inversePrimary      = AccentPrimaryLo,

    outline             = SurfaceBorder,
    outlineVariant      = SurfaceDivider,

    error               = StatusCritical,
    onError             = TextOnAccent,
    errorContainer      = StatusCritical.copy(alpha = 0.15f),
    onErrorContainer    = StatusCriticalHi
)

@Composable
fun FridaManagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ProColorScheme,
        typography = Typography,
        content = content
    )
}
