package com.j41k.fridamanager.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
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

/** Icono + texto: el estado nunca se comunica solo con color. */
private fun ReqStatus.icon(): ImageVector = when (this) {
    ReqStatus.OK   -> Icons.Outlined.CheckCircle
    ReqStatus.WARN -> Icons.Outlined.WarningAmber
    ReqStatus.FAIL -> Icons.Outlined.ErrorOutline
}

private fun ReqStatus.label(): String = when (this) {
    ReqStatus.OK   -> "Cumplido"
    ReqStatus.WARN -> "Recomendado"
    ReqStatus.FAIL -> "Pendiente"
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
//  CARD
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

    val openDevSettings: () -> Unit = {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    val items = remember(
        isRooted, isDeveloperModeEnabled, isUsbDebuggingEnabled,
        isSelinuxPermissive, hasBinarySelected, isBinaryCompatible
    ) {
        listOf(
            Requirement(
                icon = Icons.Outlined.Shield,
                label = "Acceso root",
                hint = "Concede permisos de superusuario a la app desde tu gestor de root",
                status = if (isRooted) ReqStatus.OK else ReqStatus.FAIL
            ),
            Requirement(
                icon = Icons.Outlined.Code,
                label = "Opciones de desarrollador",
                hint = "Actívalas en Ajustes del sistema › Información del teléfono",
                status = if (isDeveloperModeEnabled) ReqStatus.OK else ReqStatus.FAIL,
                actionLabel = "Abrir",
                onAction = openDevSettings
            ),
            Requirement(
                icon = Icons.Outlined.Usb,
                label = "Depuración USB",
                hint = "Necesaria para conectar Frida desde el ordenador vía ADB",
                status = if (isUsbDebuggingEnabled) ReqStatus.OK else ReqStatus.FAIL,
                actionLabel = "Activar",
                onAction = openDevSettings
            ),
            Requirement(
                icon = Icons.Outlined.Security,
                label = "SELinux permisivo",
                hint = "Evita fallos de inyección en Samsung y dispositivos recientes",
                status = if (isSelinuxPermissive) ReqStatus.OK else ReqStatus.WARN,
                actionLabel = if (!isSelinuxPermissive) "Ajustes" else null,
                onAction = if (!isSelinuxPermissive) onNavigateToSettings else null
            ),
            Requirement(
                icon = Icons.Outlined.Storage,
                label = "Binario seleccionado",
                hint = "Elige un frida-server de la lista o descárgalo",
                status = if (hasBinarySelected) ReqStatus.OK else ReqStatus.FAIL
            ),
            Requirement(
                icon = Icons.Outlined.Memory,
                label = "Arquitectura compatible",
                hint = "El binario debe coincidir con la CPU del dispositivo",
                status = if (isBinaryCompatible) ReqStatus.OK else ReqStatus.WARN
            )
        )
    }

    val failCount = items.count { it.status == ReqStatus.FAIL }
    val warnCount = items.count { it.status == ReqStatus.WARN }
    val okCount   = items.size - failCount - warnCount

    val summaryStatus = when {
        failCount > 0 -> ReqStatus.FAIL
        warnCount > 0 -> ReqStatus.WARN
        else          -> ReqStatus.OK
    }
    val summaryColor = summaryStatus.color()

    // Se expande automáticamente cuando aparecen problemas; el usuario puede plegarlo.
    var expanded by rememberSaveable { mutableStateOf(failCount > 0) }
    LaunchedEffect(failCount) {
        if (failCount > 0) expanded = true
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .panel(if (summaryStatus == ReqStatus.OK) SurfaceBorder else summaryColor.copy(alpha = 0.35f))
    ) {
        // Cabecera (toda la fila es el objetivo táctil)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(
                    role = Role.Button,
                    onClickLabel = if (expanded) "Ocultar requisitos" else "Mostrar requisitos"
                ) { expanded = !expanded }
                .semantics {
                    stateDescription = if (expanded) "Expandido" else "Contraído"
                }
                .padding(horizontal = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                summaryStatus.icon(),
                contentDescription = null,
                tint = summaryColor,
                modifier = Modifier.size(Sizes.iconMd)
            )
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Requisitos del entorno",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = when {
                        failCount > 0 -> "$okCount de ${items.size} cumplidos · $failCount pendiente${if (failCount > 1) "s" else ""}"
                        warnCount > 0 -> "Listo · $warnCount recomendación${if (warnCount > 1) "es" else ""}"
                        else          -> "Todo listo"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (failCount > 0) summaryColor else TextSecondary
                )
            }
            StatusDots(items)
            Spacer(Modifier.width(Spacing.sm))
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(Sizes.iconMd)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(220), expandFrom = Alignment.Top) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(180), shrinkTowards = Alignment.Top) + fadeOut(tween(120))
        ) {
            Column {
                HorizontalDivider(color = SurfaceDivider)
                items.forEachIndexed { index, item ->
                    ReqRow(item = item)
                    if (index < items.lastIndex) {
                        HorizontalDivider(
                            color = SurfaceDivider,
                            modifier = Modifier.padding(start = 48.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Resumen visual compacto; decorativo para lectores de pantalla (el texto ya lo describe). */
@Composable
private fun StatusDots(items: List<Requirement>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.clearAndSetSemantics {}
    ) {
        items.forEach { item ->
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(item.status.color())
            )
        }
    }
}

@Composable
private fun ReqRow(item: Requirement) {
    val color = item.status.color()
    val isOk  = item.status == ReqStatus.OK

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.touchTarget)
            .padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.sm, bottom = Spacing.sm)
            .semantics(mergeDescendants = true) {
                contentDescription = "${item.label}: ${item.status.label()}"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.status.icon(),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(Sizes.iconMd)
        )
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isOk) TextSecondary else TextPrimary
            )
            if (!isOk) {
                Text(
                    text = item.hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }
        if (!isOk && item.onAction != null && item.actionLabel != null) {
            TextButton(
                onClick = item.onAction,
                colors = ButtonDefaults.textButtonColors(contentColor = AccentPrimaryHi)
            ) {
                Text(item.actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Spacer(Modifier.width(Spacing.sm))
        }
    }
}
