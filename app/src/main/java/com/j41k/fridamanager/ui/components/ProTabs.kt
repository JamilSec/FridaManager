package com.j41k.fridamanager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.j41k.fridamanager.ui.theme.*

data class ProTabItem(
    val label: String,
    val icon: ImageVector,
    val badge: Int? = null
)

@Composable
fun ProTabs(
    tabs: List<ProTabItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceLow.copy(alpha = 0.92f))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tabs.forEachIndexed { index, item ->
            ProTab(
                item = item,
                isSelected = index == selectedIndex,
                onClick = {
                    if (index != selectedIndex) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    }
                    onSelected(index)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProTab(
    item: ProTabItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) SurfaceHigh else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tab_bg"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isSelected) AccentPrimaryHi else TextSecondary,
        label = "tab_label"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) AccentPrimaryHi else TextTertiary,
        label = "tab_icon"
    )

    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .let {
                if (isSelected) {
                    it.border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                AccentPrimary.copy(alpha = 0.5f),
                                AccentCyan.copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                } else it
            }
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true, color = AccentPrimary),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = labelColor,
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    letterSpacing = 1.1.sp,
                    maxLines = 1
                )
                if (item.badge != null && item.badge > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    TabBadge(value = item.badge, isSelected = isSelected)
                }
            }
        }
    }
}

@Composable
private fun TabBadge(value: Int, isSelected: Boolean) {
    val display = if (value > 99) "99+" else value.toString()
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (isSelected) AccentPrimary.copy(alpha = 0.25f)
                else SurfaceHigh
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = display,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) AccentPrimaryHi else TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Conjunto de tabs canónicos de la app — útil para no repetir definiciones. */
object FridaTabs {
    val Local    = ProTabItem("LOCAL",    Icons.Outlined.Storage)
    val Remote   = ProTabItem("REMOTOS",  Icons.Outlined.CloudDownload)
    val Logs     = ProTabItem("LOGS",     Icons.Outlined.Terminal)
    val Settings = ProTabItem("AJUSTES",  Icons.Outlined.Tune)
}
