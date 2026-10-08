package com.j41k.fridamanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.SecurityUpdateWarning
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.j41k.fridamanager.ui.components.SectionHeader
import com.j41k.fridamanager.ui.components.panel
import com.j41k.fridamanager.ui.theme.*
import com.j41k.fridamanager.viewmodel.FridaViewModel

@Composable
fun SettingsScreen(viewModel: FridaViewModel) {
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var showTurboConfirm by rememberSaveable { mutableStateOf(false) }
    var showHideConfirm by rememberSaveable { mutableStateOf(false) }
    var hideName by rememberSaveable { mutableStateOf("") }
    var replaceApp by rememberSaveable { mutableStateOf(true) }
    val isRunning = viewModel.isFridaRunning
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen)
            .padding(top = Spacing.sm, bottom = Spacing.xl)
    ) {
        // ── Actualizaciones ───────────────────────────────────
        SectionHeader(title = "Actualizaciones", subtitle = "Versión instalada y disponibles")
        Spacer(Modifier.height(Spacing.sm))

        Column(Modifier.fillMaxWidth().panel().padding(Spacing.lg)) {
            val update = viewModel.appUpdate
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Frida Manager", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Text(
                        text = when {
                            update != null -> "Disponible ${update.version} · tienes v${viewModel.currentAppVersion}"
                            else -> "Versión v${viewModel.currentAppVersion}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (update != null) AccentPrimaryHi else TextTertiary
                    )
                }
                if (viewModel.isCheckingAppUpdate) {
                    CircularProgressIndicator(Modifier.size(Sizes.iconMd), strokeWidth = 2.dp, color = AccentPrimaryHi)
                } else {
                    TextButton(onClick = { viewModel.checkAppUpdate(userInitiated = true) }) {
                        Text("Buscar", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            if (update != null) {
                Spacer(Modifier.height(Spacing.md))
                Button(
                    onClick = { viewModel.installAppUpdate() },
                    enabled = viewModel.isRooted && !viewModel.isInstallingUpdate,
                    modifier = Modifier.fillMaxWidth().height(Sizes.touchTarget),
                    shape = Shapes.control,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary, contentColor = TextOnAccent)
                ) {
                    if (viewModel.isInstallingUpdate) {
                        CircularProgressIndicator(Modifier.size(Sizes.iconMd), strokeWidth = 2.dp, color = TextOnAccent)
                        Spacer(Modifier.width(Spacing.sm))
                        Text("Instalando…", style = MaterialTheme.typography.labelLarge)
                    } else {
                        Icon(Icons.Outlined.SystemUpdate, contentDescription = null, modifier = Modifier.size(Sizes.iconMd))
                        Spacer(Modifier.width(Spacing.sm))
                        Text("Actualizar a ${update.version}", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))

        // ── Red ───────────────────────────────────────────────
        SectionHeader(title = "Red", subtitle = "Dirección y puerto donde escucha frida-server")
        Spacer(Modifier.height(Spacing.sm))

        val portError = viewModel.portError
        OutlinedTextField(
            value = viewModel.fridaPort,
            onValueChange = { new -> viewModel.fridaPort = new.filter(Char::isDigit).take(5) },
            enabled = !isRunning,
            label = { Text("Puerto") },
            isError = portError != null,
            supportingText = {
                Text(portError ?: if (isRunning) "Detén el servicio para modificar la red" else "Por defecto 27042")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            singleLine = true,
            textStyle = MonoBodyMedium,
            modifier = Modifier.fillMaxWidth(),
            shape = Shapes.control,
            colors = fieldColors()
        )

        Spacer(Modifier.height(Spacing.xs))

        val addressError = viewModel.addressError
        OutlinedTextField(
            value = viewModel.fridaAddress,
            onValueChange = { new -> viewModel.fridaAddress = new.filter { it.isDigit() || it == '.' }.take(15) },
            enabled = !isRunning,
            label = { Text("Dirección de escucha") },
            isError = addressError != null,
            supportingText = if (isRunning && addressError == null) null else {
                { Text(addressError ?: "0.0.0.0 escucha en todas las interfaces") }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            singleLine = true,
            textStyle = MonoBodyMedium,
            modifier = Modifier.fillMaxWidth(),
            shape = Shapes.control,
            colors = fieldColors()
        )

        Spacer(Modifier.height(Spacing.xl))

        // ── Seguridad ─────────────────────────────────────────
        SectionHeader(title = "Seguridad (root)", subtitle = "Ajustes para Samsung y dispositivos recientes")
        Spacer(Modifier.height(Spacing.sm))

        Column(Modifier.fillMaxWidth().panel()) {
            SwitchRow(
                title = "SELinux permisivo automático",
                description = "Cambia SELinux a permisivo al iniciar el servicio. Recomendado.",
                checked = viewModel.autoPermissive,
                onCheckedChange = { viewModel.autoPermissive = it }
            )
            HorizontalDivider(color = SurfaceDivider)
            ActionRow(
                icon = if (viewModel.isSelinuxPermissive) Icons.Outlined.SecurityUpdateWarning else Icons.Outlined.Security,
                iconTint = if (viewModel.isSelinuxPermissive) StatusWarning else TextSecondary,
                title = "Estado de SELinux",
                description = if (viewModel.isSelinuxPermissive)
                    "Permisivo. Algunos Samsung lo revierten: si Frida falla, vuelve a aplicarlo antes de iniciar."
                else "Enforcing (modo por defecto del sistema).",
                actionLabel = if (viewModel.isSelinuxPermissive) "Restaurar" else "Permisivo",
                enabled = viewModel.isRooted,
                onAction = { viewModel.toggleSelinux() }
            )
            HorizontalDivider(color = SurfaceDivider)
            ActionRow(
                icon = Icons.Outlined.FlashOn,
                iconTint = AccentPrimaryHi,
                title = "Ajuste rápido para Samsung",
                description = "Desactiva el pool USAP de Zygote y pone SELinux en permisivo.",
                actionLabel = "Aplicar",
                enabled = viewModel.isRooted,
                onAction = { showTurboConfirm = true }
            )
        }

        Spacer(Modifier.height(Spacing.xl))

        // ── Ocultar la app ────────────────────────────────────
        SectionHeader(title = "Ocultar la app", subtitle = "Reinstalar con un nombre de paquete neutro")
        Spacer(Modifier.height(Spacing.sm))

        if (viewModel.isHiddenInstance) {
            Column(Modifier.fillMaxWidth().panel().padding(Spacing.lg)) {
                Text("Instancia oculta activa", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                Text(
                    "Esta app corre bajo «${viewModel.currentPackage}». Puedes restaurar su " +
                        "identidad original cuando quieras.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
                Spacer(Modifier.height(Spacing.md))
                OutlinedButton(
                    onClick = { viewModel.restoreOriginal() },
                    enabled = viewModel.isRooted && !viewModel.isHiding,
                    modifier = Modifier.fillMaxWidth().height(Sizes.touchTarget),
                    shape = Shapes.control
                ) {
                    Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(Sizes.iconMd))
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Restaurar app original", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(Spacing.md))
        }

        Column(Modifier.fillMaxWidth().panel().padding(Spacing.lg)) {
            val hidden = viewModel.hiddenPackage
            Text(
                text = "Crea una copia de la app con un nombre de paquete distinto para que una app " +
                    "bajo análisis no la detecte por «${viewModel.currentPackage}». " +
                    "Requiere root.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(Spacing.md))
            OutlinedTextField(
                value = hideName,
                onValueChange = { hideName = it },
                enabled = !viewModel.isHiding,
                label = { Text("Nombre de paquete (opcional)") },
                placeholder = { Text("aleatorio si se deja vacío") },
                supportingText = { Text("Ej. com.android.systemservice") },
                singleLine = true,
                textStyle = MonoBodyMedium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.control,
                colors = fieldColors()
            )
            Spacer(Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = replaceApp,
                        role = Role.Switch,
                        enabled = !viewModel.isHiding,
                        onValueChange = { replaceApp = it }
                    )
                    .padding(vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Reemplazar esta app", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Text(
                        if (replaceApp) "Abre la copia y desinstala esta app"
                        else "Instala la copia aparte, conservando esta app",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
                Spacer(Modifier.width(Spacing.lg))
                Switch(
                    checked = replaceApp,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextOnAccent,
                        checkedTrackColor = AccentPrimaryHi,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SurfaceHigh,
                        uncheckedBorderColor = SurfaceBorder
                    )
                )
            }
            Spacer(Modifier.height(Spacing.md))
            Button(
                onClick = { focusManager.clearFocus(); showHideConfirm = true },
                enabled = viewModel.isRooted && !viewModel.isHiding,
                modifier = Modifier.fillMaxWidth().height(Sizes.touchTarget),
                shape = Shapes.control,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary, contentColor = TextOnAccent)
            ) {
                if (viewModel.isHiding) {
                    CircularProgressIndicator(Modifier.size(Sizes.iconMd), strokeWidth = 2.dp, color = TextOnAccent)
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Creando copia…", style = MaterialTheme.typography.labelLarge)
                } else {
                    Icon(Icons.Outlined.VisibilityOff, contentDescription = null, modifier = Modifier.size(Sizes.iconMd))
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Crear copia oculta", style = MaterialTheme.typography.labelLarge)
                }
            }
            if (hidden != null) {
                Spacer(Modifier.height(Spacing.md))
                HorizontalDivider(color = SurfaceDivider)
                Spacer(Modifier.height(Spacing.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Copia instalada", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text(hidden, style = MonoCaption, color = AccentCyan)
                    }
                    TextButton(
                        onClick = { viewModel.restoreHidden() },
                        colors = ButtonDefaults.textButtonColors(contentColor = StatusCriticalHi)
                    ) { Text("Desinstalar", style = MaterialTheme.typography.labelLarge) }
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))

        // ── Mantenimiento ─────────────────────────────────────
        SectionHeader(title = "Mantenimiento")
        Spacer(Modifier.height(Spacing.sm))

        OutlinedButton(
            onClick = { showDeleteConfirm = true },
            enabled = viewModel.selectedBinary != null && !isRunning && !viewModel.isDeleting,
            modifier = Modifier
                .fillMaxWidth()
                .height(Sizes.touchTarget),
            shape = Shapes.control,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusCriticalHi)
        ) {
            if (viewModel.isDeleting) {
                CircularProgressIndicator(Modifier.size(Sizes.iconMd), strokeWidth = 2.dp, color = StatusCriticalHi)
                Spacer(Modifier.width(Spacing.sm))
                Text("Eliminando…", style = MaterialTheme.typography.labelLarge)
            } else {
                Icon(Icons.Outlined.DeleteForever, contentDescription = null, modifier = Modifier.size(Sizes.iconMd))
                Spacer(Modifier.width(Spacing.sm))
                Text("Eliminar binario seleccionado", style = MaterialTheme.typography.labelLarge)
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = when {
                viewModel.selectedBinary == null -> "Selecciona un binario en la pestaña Servidor para habilitar esta opción."
                isRunning -> "Detén el servicio antes de eliminar el binario."
                else -> viewModel.selectedBinary?.name.orEmpty()
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            modifier = Modifier.padding(horizontal = Spacing.lg)
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = StatusCriticalHi) },
            title = { Text("¿Eliminar binario?") },
            text = {
                Text("Se eliminará ${viewModel.selectedBinary?.name} de /data/local/tmp. Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelectedBinary()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = StatusCriticalHi)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            },
            containerColor = SurfaceMid,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    if (showHideConfirm) {
        AlertDialog(
            onDismissRequest = { showHideConfirm = false },
            icon = { Icon(Icons.Outlined.VisibilityOff, contentDescription = null, tint = AccentPrimaryHi) },
            title = { Text(if (replaceApp) "¿Reemplazar la app?" else "¿Crear copia oculta?") },
            text = {
                Text(
                    if (replaceApp)
                        "Se instalará una copia con un nombre de paquete nuevo, se abrirá, y esta " +
                            "app actual se desinstalará. Requiere conceder root al instalador."
                    else
                        "Se instalará una segunda copia con un nombre de paquete distinto, firmada " +
                            "con una clave nueva. La app actual no se toca; podrás desinstalar la " +
                            "copia desde aquí. Requiere conceder root al instalador."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.hideApp(hideName, replaceApp)
                    showHideConfirm = false
                }) { Text(if (replaceApp) "Reemplazar" else "Crear copia") }
            },
            dismissButton = {
                TextButton(onClick = { showHideConfirm = false }) { Text("Cancelar") }
            },
            containerColor = SurfaceMid,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    if (showTurboConfirm) {
        AlertDialog(
            onDismissRequest = { showTurboConfirm = false },
            icon = { Icon(Icons.Outlined.FlashOn, contentDescription = null, tint = AccentPrimaryHi) },
            title = { Text("¿Aplicar ajuste para Samsung?") },
            text = {
                Text(
                    "Se modificarán estos ajustes del sistema:\n\n" +
                        "• SELinux pasa a modo permisivo\n" +
                        "• Se desactiva el pool USAP de Zygote (persiste tras reiniciar)\n" +
                        "• El tiempo de apagado de pantalla pasa a 10 minutos"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.applyTurboFix()
                    showTurboConfirm = false
                }) { Text("Aplicar") }
            },
            dismissButton = {
                TextButton(onClick = { showTurboConfirm = false }) { Text("Cancelar") }
            },
            containerColor = SurfaceMid,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentPrimary,
    unfocusedBorderColor = SurfaceBorder,
    disabledBorderColor = SurfaceDivider,
    focusedLabelColor = AccentPrimaryHi,
    unfocusedLabelColor = TextSecondary,
    disabledLabelColor = TextDisabled,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    disabledTextColor = TextTertiary,
    focusedSupportingTextColor = TextTertiary,
    unfocusedSupportingTextColor = TextTertiary,
    disabledSupportingTextColor = StatusWarning,
    cursorColor = AccentPrimaryHi
)

/** Fila completa conmutable: el objetivo táctil es toda la fila, no solo el Switch. */
@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
        Spacer(Modifier.width(Spacing.lg))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextOnAccent,
                checkedTrackColor = AccentPrimaryHi,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SurfaceHigh,
                uncheckedBorderColor = SurfaceBorder
            )
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    actionLabel: String,
    enabled: Boolean,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.md, bottom = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(Sizes.iconLg))
        Spacer(Modifier.width(Spacing.lg))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
        Spacer(Modifier.width(Spacing.sm))
        TextButton(
            onClick = onAction,
            enabled = enabled,
            colors = ButtonDefaults.textButtonColors(contentColor = AccentPrimaryHi)
        ) { Text(actionLabel, style = MaterialTheme.typography.labelLarge) }
    }
}
