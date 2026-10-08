package com.j41k.fridamanager.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GppBad
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.j41k.fridamanager.data.FridaShell
import com.j41k.fridamanager.ui.theme.*

@Composable
fun StatusCard(
    isRooted: Boolean,
    fridaStatus: FridaShell.FridaStatus,
    uptime: () -> String,
    architecture: String,
    selectedBinaryName: String?,
    modifier: Modifier = Modifier
) {
    val isRunning = fridaStatus.isRunning
    val statusColor by animateColorAsState(
        targetValue = if (isRunning) StatusOnline else TextSecondary,
        label = "status_color"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isRunning) StatusOnline.copy(alpha = 0.4f) else SurfaceBorder,
        label = "status_border"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .panel(borderColor)
            .padding(Spacing.lg)
    ) {
        // Fila superior: etiqueta + root + arquitectura
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Estado del servidor",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.weight(1f)
            )
            RootBadge(isRooted = isRooted)
            Spacer(Modifier.width(Spacing.sm))
            Chip(text = architecture.uppercase(), color = AccentCyan, mono = true)
        }

        Spacer(Modifier.height(Spacing.md))

        // Estado principal
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = isRunning,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                    label = "status_text"
                ) { running ->
                    Text(
                        text = if (running) "Activo" else "Inactivo",
                        style = MaterialTheme.typography.headlineMedium,
                        color = statusColor
                    )
                }
                Text(
                    text = when {
                        isRunning -> "frida-server en ejecución"
                        selectedBinaryName != null -> "Listo para iniciar · $selectedBinaryName"
                        else -> "Ningún binario seleccionado"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(Spacing.md))
            StatusIndicator(isRunning = isRunning)
        }

        Spacer(Modifier.height(Spacing.lg))
        HorizontalDivider(color = SurfaceDivider)
        Spacer(Modifier.height(Spacing.md))

        // Métricas
        Row(modifier = Modifier.fillMaxWidth()) {
            UptimeCell(isRunning = isRunning, uptime = uptime, modifier = Modifier.weight(1f))
            MetricCell(
                label = "PUERTO",
                value = if (isRunning) fridaStatus.port ?: "27042" else "—",
                valueColor = if (isRunning) AccentCyan else TextTertiary,
                modifier = Modifier.weight(0.8f)
            )
            MetricCell(
                label = "HOST",
                value = if (isRunning) fridaStatus.listenAddress ?: "0.0.0.0" else "—",
                valueColor = if (isRunning) AccentCyan else TextTertiary,
                modifier = Modifier.weight(1.2f)
            )
            MetricCell(
                label = "PID",
                value = fridaStatus.pid?.toString() ?: "—",
                valueColor = if (fridaStatus.pid != null) TextPrimary else TextTertiary,
                modifier = Modifier.weight(0.8f)
            )
        }
    }
}

/** Lee el uptime en su propio scope: solo esta celda se recompone cada segundo. */
@Composable
private fun UptimeCell(isRunning: Boolean, uptime: () -> String, modifier: Modifier) {
    MetricCell(
        label = "UPTIME",
        value = if (isRunning) uptime() else "—",
        valueColor = if (isRunning) TextPrimary else TextTertiary,
        modifier = modifier
    )
}

@Composable
private fun RootBadge(isRooted: Boolean) {
    val color = if (isRooted) StatusOnline else StatusCritical
    Row(
        modifier = Modifier
            .clip(Shapes.chip)
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = Spacing.xxs)
            .semantics(mergeDescendants = true) {
                contentDescription = if (isRooted) "Root concedido" else "Sin acceso root"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isRooted) Icons.Outlined.Shield else Icons.Outlined.GppBad,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = if (isRooted) "Root" else "Sin root",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
internal fun Chip(text: String, color: Color, mono: Boolean = false) {
    Text(
        text = text,
        style = if (mono) MonoCaption else MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1,
        modifier = Modifier
            .clip(Shapes.chip)
            .background(SurfaceHigh)
            .border(1.dp, SurfaceBorder, Shapes.chip)
            .padding(horizontal = 6.dp, vertical = Spacing.xxs)
    )
}

/**
 * Indicador de estado. Inactivo: anillo estático. Activo: halo con pulso suave
 * cuyo valor se lee en la fase de dibujo (sin recomposición por frame), y la
 * transición infinita solo existe mientras el servicio está activo.
 */
@Composable
private fun StatusIndicator(isRunning: Boolean) {
    val size = 40.dp
    if (!isRunning) {
        Canvas(Modifier.size(size)) {
            drawCircle(color = TextDisabled, radius = this.size.minDimension / 2 * 0.8f, style = Stroke(1.5.dp.toPx()))
            drawCircle(color = TextDisabled, radius = 4.dp.toPx())
        }
        return
    }
    val pulse = rememberInfiniteTransition(label = "status_pulse").animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2
        drawCircle(color = StatusOnline.copy(alpha = 0.14f * pulse.value), radius = r * pulse.value)
        drawCircle(color = StatusOnline.copy(alpha = 0.5f), radius = r * 0.8f, style = Stroke(1.5.dp.toPx()))
        drawCircle(color = StatusOnline, radius = 5.dp.toPx())
    }
}

@Composable
private fun MetricCell(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(end = Spacing.xs)) {
        Text(text = label, style = OverlineLabel, color = TextTertiary, maxLines = 1)
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            text = value,
            style = MonoBodyMedium,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
