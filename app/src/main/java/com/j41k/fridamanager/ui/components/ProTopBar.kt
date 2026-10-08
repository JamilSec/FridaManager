package com.j41k.fridamanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.j41k.fridamanager.ui.theme.*

/**
 *   [logo] Frida Manager           [● Activo] [i]
 *          v1.2.0
 */
@Composable
fun ProTopBar(
    versionLabel: String,
    isLive: Boolean,
    onInfoClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceBase)
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(start = Spacing.lg, end = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppLogo(size = 28.dp, activeAccent = isLive)
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Frida Manager",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = versionLabel,
                    style = MonoCaption,
                    color = TextTertiary
                )
            }
            ServiceStatusPill(isLive = isLive)
            IconButton(onClick = onInfoClick) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = "Guía y créditos",
                    tint = TextSecondary,
                    modifier = Modifier.size(Sizes.iconLg)
                )
            }
        }
        HorizontalDivider(color = SurfaceDivider)
    }
}

/** Estado del servicio siempre visible, también fuera de la pestaña Servidor. */
@Composable
private fun ServiceStatusPill(isLive: Boolean) {
    val color = if (isLive) StatusOnline else StatusOffline
    val label = if (isLive) "Activo" else "Inactivo"
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), CircleShape)
            .padding(horizontal = 10.dp, vertical = Spacing.xs)
            .semantics {
                contentDescription = "Servicio $label"
                liveRegion = LiveRegionMode.Polite
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isLive) StatusOnlineHi else TextSecondary
        )
    }
}
