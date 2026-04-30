package com.j41k.fridamanager.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.j41k.fridamanager.ui.theme.*

// ─────────────────────────────────────────────────────────────
//  MODELO DE REQUISITO
// ─────────────────────────────────────────────────────────────

enum class ReqStatus { OK, WARN, FAIL }

private fun ReqStatus.color(): Color = when (this) {
    ReqStatus.OK   -> StatusOnline
    ReqStatus.WARN -> StatusWarning
    ReqStatus.FAIL -> StatusCritical
}

private fun ReqStatus.label(): String = when (this) {
    ReqStatus.OK   -> "OK"
    ReqStatus.WARN -> "WARN"
    ReqStatus.FAIL -> "FAIL"
}

data class Requirement(
    val icon: ImageVector,
    val label: String,
    val hint: String,
    val status: ReqStatus,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

// ─────────────────────────────────────────────────────────────
//  CARD PRINCIPAL
// ─────────────────────────────────────────────────────────────

@Composable
fun PrerequisitesCard(
    isRooted: Boolean,
    isDeveloperModeEnabled: Boolean,
    isUsbDebuggingEnabled: Boolean,
    isSelinuxPermissive: Boolean,
    hasBinarySelected: Boolean,
    isBinaryCompatible: Boolean,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val items = remember(
        isRooted, isDeveloperModeEnabled, isUsbDebuggingEnabled,
        isSelinuxPermissive, hasBinarySelected, isBinaryCompatible
    ) {
        buildList {
            add(Requirement(
                icon = Icons.Outlined.Shield,
                label = "ROOT CONCEDIDO",
                hint = "Permisos de superusuario requeridos para todas las operaciones",
                status = if (isRooted) ReqStatus.OK else ReqStatus.FAIL
            ))
            add(Requirement(
                icon = Icons.Outlined.Code,
                label = "MODO DESARROLLADOR",
                hint = "Activa las opciones de desarrollador en Ajustes del sistema",
                status = if (isDeveloperModeEnabled) ReqStatus.OK else ReqStatus.FAIL,
                actionLabel = "Abrir",
                onAction = {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Exception) {
                        context.startActivity(
                            Intent(Settings.ACTION_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            ))
            add(Requirement(
                icon = Icons.Outlined.SettingsInputComponent,
                label = "DEPURACIÓN USB (ADB)",
                hint = "Necesaria para conectar Frida con la PC via ADB",
                status = if (isUsbDebuggingEnabled) ReqStatus.OK else ReqStatus.FAIL,
                actionLabel = "Habilitar",
                onAction = {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Exception) {}
                }
            ))
            add(Requirement(
                icon = Icons.Outlined.Security,
                label = "SELINUX PERMISIVO",
                hint = "Evita fallos de inyección en Samsung y dispositivos modernos",
                status = if (isSelinuxPermissive) ReqStatus.OK else ReqStatus.WARN,
                actionLabel = if (!isSelinuxPermissive) "Ajustes" else null,
                onAction = if (!isSelinuxPermissive) onNavigateToSettings else null
            ))
            add(Requirement(
                icon = Icons.Outlined.Storage,
                label = "BINARIO SELECCIONADO",
                hint = "Elige un frida-server en la pestaña LOCALES",
                status = if (hasBinarySelected) ReqStatus.OK else ReqStatus.FAIL
            ))
            add(Requirement(
                icon = Icons.Outlined.Memory,
                label = "ARQUITECTURA COMPATIBLE",
                hint = "El binario debe coincidir con la CPU del dispositivo",
                status = if (isBinaryCompatible) ReqStatus.OK else ReqStatus.WARN
            ))
        }
    }

    val failCount = items.count { it.status == ReqStatus.FAIL }
    val warnCount = items.count { it.status == ReqStatus.WARN }
    val okCount   = items.count { it.status == ReqStatus.OK }

    val borderColor = when {
        failCount > 0 -> StatusCritical
        warnCount > 0 -> StatusWarning
        else          -> StatusOnline
    }

    // Expansión automática cuando hay problemas
    var expanded by remember { mutableStateOf(failCount > 0 || warnCount > 0) }
    LaunchedEffect(failCount, warnCount) {
        if (failCount > 0 || warnCount > 0) expanded = true
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceLow)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        borderColor.copy(alpha = 0.40f),
                        borderColor.copy(alpha = 0.08f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        // Halo sutil de fondo cuando hay fallos
        if (failCount > 0) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                StatusCritical.copy(alpha = 0.04f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Column(modifier = Modifier.padding(14.dp)) {

            // ── HEADER (siempre visible, tappable) ───────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Dots de estado
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items.forEach { item ->
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(item.status.color())
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ENTORNO · REQUISITOS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontSize = 9.sp,
                        letterSpacing = 1.6.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Contador OK / total
                    Text(
                        text = "$okCount/${items.size}",
                        style = MonoCaption,
                        color = borderColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess
                                      else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // ── DETALLE EXPANDIBLE ────────────────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = tween(220),
                    expandFrom = Alignment.Top
                ) + fadeIn(tween(180)),
                exit = shrinkVertically(
                    animationSpec = tween(180),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(tween(120))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Línea separadora
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(borderColor.copy(alpha = 0.25f), Color.Transparent)
                                )
                            )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    items.forEachIndexed { index, item ->
                        ReqRow(item = item)
                        if (index < items.lastIndex) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(SurfaceDivider)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
//  FILA DE UN REQUISITO
// ─────────────────────────────────────────────────────────────

@Composable
private fun ReqRow(item: Requirement) {
    val color = item.status.color()
    val isOk  = item.status == ReqStatus.OK

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ícono del requisito
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = if (isOk) TextTertiary else color,
            modifier = Modifier.size(14.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Label + descripción (solo si no está OK)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isOk) TextSecondary else TextPrimary,
                fontSize = 9.sp,
                fontWeight = if (isOk) FontWeight.Normal else FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            AnimatedVisibility(visible = !isOk) {
                Text(
                    text = item.hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Botón de acción (solo si hay problema y tiene acción)
        if (!isOk && item.onAction != null && item.actionLabel != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(color.copy(alpha = 0.08f))
                    .border(1.dp, color.copy(alpha = 0.30f), RoundedCornerShape(5.dp))
                    .clickable { item.onAction.invoke() }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = item.actionLabel,
                    color = color,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        // Badge de estado
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(color.copy(alpha = if (isOk) 0.06f else 0.12f))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = item.status.label(),
                color = color,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
