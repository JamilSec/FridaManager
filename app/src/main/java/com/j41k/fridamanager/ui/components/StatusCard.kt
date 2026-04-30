package com.j41k.fridamanager.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.SettingsInputComponent
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.j41k.fridamanager.data.FridaShell
import com.j41k.fridamanager.ui.theme.*

@Composable
fun StatusCard(
    isRooted: Boolean,
    fridaStatus: FridaShell.FridaStatus,
    uptime: String,
    heartbeat: List<Float>,
    architecture: String,
    selectedBinaryName: String?,
    modifier: Modifier = Modifier
) {
    val isRunning = fridaStatus.isRunning

    val infiniteTransition = rememberInfiniteTransition(label = "status_anim")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.86f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )
    val coreGlow by infiniteTransition.animateFloat(
        initialValue = 0.45f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core"
    )

    val activeColor = StatusOnline
    val idleColor = StatusOffline.copy(alpha = 0.55f)
    val primaryColor = if (isRunning) activeColor else idleColor

    Column(modifier = modifier.fillMaxWidth()) {

        // ── TARJETA PRINCIPAL ─────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SurfaceLow,
                            SurfaceLow.copy(alpha = 0.85f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = if (isRunning) Brush.horizontalGradient(
                        listOf(
                            StatusOnline.copy(alpha = 0.45f),
                            AccentCyan.copy(alpha = 0.20f)
                        )
                    ) else Brush.horizontalGradient(
                        listOf(SurfaceBorder, SurfaceBorder.copy(alpha = 0.5f))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            // Glow sutil cuando está activo
            if (isRunning) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    StatusOnline.copy(alpha = 0.06f),
                                    Color.Transparent
                                ),
                                center = Offset(0f, 0f),
                                radius = 600f
                            )
                        )
                )
            }

            BlueprintGrid(
                modifier = Modifier.matchParentSize(),
                spacing = 30.dp,
                dotColor = Color.White.copy(alpha = 0.025f)
            )

            Column(modifier = Modifier.padding(20.dp)) {

                // FILA SUPERIOR: label + arch chip + Root
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRunning) StatusOnline.copy(alpha = coreGlow)
                                    else StatusOffline
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ESTADO DEL SISTEMA",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextTertiary,
                            letterSpacing = 1.6.sp,
                            fontSize = 9.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SmallRootBadge(isRooted = isRooted)
                        Spacer(modifier = Modifier.width(8.dp))
                        ArchChip(arch = architecture)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // FILA CENTRAL: estado grande + orbe
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = if (isRunning) "ACTIVO" else "INACTIVO",
                            transitionSpec = {
                                (slideInVertically { it / 2 } + fadeIn()) togetherWith
                                (slideOutVertically { -it / 2 } + fadeOut())
                            },
                            label = "status_text"
                        ) { statusText ->
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.displayMedium,
                                color = primaryColor,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Text(
                            text = if (isRunning)
                                "frida-server :: running"
                            else if (selectedBinaryName != null)
                                "ready :: $selectedBinaryName"
                            else
                                "no binary selected",
                            style = MonoBodySmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }

                    StatusOrb(
                        isRunning = isRunning,
                        pulseScale = pulseScale,
                        sweepAngle = sweepAngle,
                        coreGlow = coreGlow,
                        activeColor = activeColor,
                        size = 48.dp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // SPARKLINE compacta
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceBase.copy(alpha = 0.4f))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Sparkline(
                        values = heartbeat,
                        color = if (isRunning) StatusOnline else AccentPrimary.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // MÉTRICAS compactas 2x2
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCell(
                        icon = Icons.Outlined.Timer,
                        label = "UPTIME",
                        value = if (isRunning) uptime else "—",
                        valueColor = if (isRunning) TextPrimary else TextTertiary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCell(
                        icon = Icons.Outlined.SettingsInputComponent,
                        label = "PORT",
                        value = if (isRunning) fridaStatus.port ?: "27042" else "—",
                        valueColor = if (isRunning) AccentCyan else TextTertiary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCell(
                        icon = Icons.Outlined.Wifi,
                        label = "HOST",
                        value = if (isRunning) fridaStatus.listenAddress ?: "0.0.0.0" else "—",
                        valueColor = if (isRunning) AccentCyan else TextTertiary,
                        modifier = Modifier.weight(1.2f)
                    )
                    MetricCell(
                        icon = Icons.Outlined.Memory,
                        label = "PID",
                        value = if (fridaStatus.pid != null) fridaStatus.pid.toString() else "—",
                        valueColor = if (fridaStatus.pid != null) TextPrimary else TextTertiary,
                        modifier = Modifier.weight(0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallRootBadge(isRooted: Boolean) {
    val accent = if (isRooted) StatusOnline else StatusCritical
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.1f))
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Shield,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isRooted) "ROOT OK" else "NO ROOT",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun ArchChip(arch: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceHigh)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "ARCH · ${arch.uppercase()}",
            style = MonoCaption,
            color = AccentCyan,
            fontSize = 9.sp,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun StatusOrb(
    isRunning: Boolean,
    pulseScale: Float,
    sweepAngle: Float,
    coreGlow: Float,
    activeColor: Color,
    size: Dp
) {
    val idleColor = StatusOffline.copy(alpha = 0.30f)

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val outerRadius = this.size.minDimension / 2 * pulseScale

            if (isRunning) {
                drawCircle(
                    color = activeColor.copy(alpha = 0.10f * coreGlow),
                    radius = outerRadius,
                    center = center
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(Color.Transparent, activeColor.copy(alpha = 0.75f)),
                        center = center
                    ),
                    startAngle = sweepAngle,
                    sweepAngle = 120f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                    topLeft = Offset(
                        center.x - outerRadius * 0.85f,
                        center.y - outerRadius * 0.85f
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        outerRadius * 1.7f,
                        outerRadius * 1.7f
                    )
                )
            }

            drawCircle(
                color = if (isRunning) activeColor.copy(alpha = 0.40f) else idleColor,
                radius = outerRadius * 0.85f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            drawCircle(
                brush = if (isRunning) Brush.radialGradient(
                    colors = listOf(
                        activeColor.copy(alpha = coreGlow * 0.65f),
                        activeColor.copy(alpha = 0.05f)
                    ),
                    center = center
                ) else Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = center
                ),
                radius = outerRadius * 0.55f,
                center = center
            )

            drawCircle(
                color = if (isRunning) activeColor.copy(alpha = coreGlow) else idleColor,
                radius = 4.dp.toPx(),
                center = center
            )
        }
    }
}

@Composable
private fun MetricCell(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                fontSize = 8.sp,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MonoBodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

