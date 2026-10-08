package com.j41k.fridamanager.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.j41k.fridamanager.ui.theme.*
import com.j41k.fridamanager.viewmodel.FridaViewModel

/**
 * Acción principal anclada sobre la navegación inferior (zona del pulgar).
 * Cuando está deshabilitada explica el motivo en lugar de dejar al usuario adivinando.
 */
@Composable
fun ServiceActionBar(viewModel: FridaViewModel, modifier: Modifier = Modifier) {
    val isRunning = viewModel.isFridaRunning
    val isBusy = viewModel.isTransitioning
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val blockedReason = when {
        isRunning -> null
        !viewModel.isRooted -> "Se requiere acceso root para iniciar el servicio"
        viewModel.selectedBinary == null -> "Selecciona o descarga un binario para continuar"
        !viewModel.isNetworkConfigValid -> "Configuración de red no válida: revisa Ajustes"
        else -> null
    }
    val enabled = !isBusy && (isRunning || (viewModel.isRooted && viewModel.selectedBinary != null))

    val toggle = {
        haptic.performHapticFeedback(if (isRunning) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
        viewModel.toggleFrida()
    }

    // Permiso de notificaciones (Android 13+) pedido en contexto, la primera vez
    // que se inicia el servicio: sin él no se ve la notificación del foreground service.
    var askedNotifications by rememberSaveable { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { toggle() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceBase)
    ) {
        HorizontalDivider(color = SurfaceDivider)
        AnimatedVisibility(visible = blockedReason != null) {
            Text(
                text = blockedReason.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.sm)
            )
        }
        Button(
            onClick = {
                val needsPermission = !isRunning && !askedNotifications &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                if (needsPermission) {
                    askedNotifications = true
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    toggle()
                }
            },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                .height(Sizes.buttonHeight),
            shape = Shapes.control,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) StatusCritical else AccentPrimary,
                contentColor = TextOnAccent,
                disabledContainerColor = SurfaceHigh,
                disabledContentColor = TextDisabled
            )
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Sizes.iconMd),
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current
                )
                Spacer(Modifier.width(Spacing.md))
                Text(
                    if (isRunning) "Iniciando…" else "Deteniendo…",
                    style = MaterialTheme.typography.labelLarge
                )
            } else {
                Icon(
                    imageVector = if (isRunning) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(Sizes.iconLg)
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = if (isRunning) "Detener servicio" else "Iniciar servicio",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/** Diálogo bloqueante indeterminado mientras se repaqueta/instala la copia oculta. */
@Composable
fun HideProgressDialog() {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(shape = Shapes.container, color = SurfaceMid, contentColor = TextPrimary) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.xl),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    Modifier.size(Sizes.iconLg),
                    strokeWidth = 2.dp,
                    color = AccentPrimaryHi
                )
                Spacer(Modifier.width(Spacing.lg))
                Column {
                    Text("Ocultando la app", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(
                        "Repaquetando, firmando e instalando…",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Progreso de descarga como diálogo modal: bloquea la interacción de verdad,
 * respeta insets y el botón atrás no lo cierra a mitad de instalación.
 */
@Composable
fun DownloadDialog(status: String, progress: Float) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = Shapes.container,
            color = SurfaceMid,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.xl)
            ) {
                Text("Instalando frida-server", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = status.ifEmpty { "Preparando…" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.height(Spacing.lg))
                if (progress <= 0f || progress >= 1f) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = AccentPrimaryHi,
                        trackColor = SurfaceHigh
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = AccentPrimaryHi,
                        trackColor = SurfaceHigh
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Text("${(progress * 100).toInt()} %", style = MonoCaption, color = TextTertiary)
                    }
                }
            }
        }
    }
}
