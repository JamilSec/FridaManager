package com.j41k.fridamanager.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ProColorScheme = darkColorScheme(
    primary             = AccentPrimary,
    onPrimary           = TextOnAccent,
    primaryContainer    = AccentPrimaryLo,
    onPrimaryContainer  = TextPrimary,

    secondary           = AccentCyan,
    onSecondary         = TextOnAccent,
    secondaryContainer  = AccentCyanLo,
    onSecondaryContainer = TextPrimary,

    tertiary            = StatusOnline,
    onTertiary          = TextOnAccent,

    background          = SurfaceBase,
    onBackground        = TextPrimary,
    surface             = SurfaceLow,
    onSurface           = TextPrimary,
    surfaceVariant      = SurfaceMid,
    onSurfaceVariant    = TextSecondary,

    outline             = SurfaceBorder,
    outlineVariant      = SurfaceDivider,

    error               = StatusCritical,
    onError             = TextOnAccent,
    errorContainer      = StatusCritical.copy(alpha = 0.15f),
    onErrorContainer    = StatusCriticalHi
)

@Composable
fun FridaManagerTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = ProColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
