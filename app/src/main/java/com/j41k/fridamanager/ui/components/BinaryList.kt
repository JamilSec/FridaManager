package com.j41k.fridamanager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.j41k.fridamanager.data.FridaFile
import com.j41k.fridamanager.ui.theme.*

/**
 * Elementos de la lista de binarios, para componer dentro del LazyColumn de la
 * pantalla (evita listas anidadas y mantiene una sola superficie de scroll).
 */
fun LazyListScope.binaryItems(
    binaries: List<FridaFile>,
    selectedBinary: FridaFile?,
    onSelect: (FridaFile) -> Unit,
    onDownload: () -> Unit
) {
    if (binaries.isEmpty()) {
        item(key = "binaries_empty", contentType = "empty") {
            EmptyState(
                icon = Icons.Outlined.Memory,
                title = "No hay binarios instalados",
                message = "No se encontró frida-server en /data/local/tmp. Descarga la última versión o súbelo con adb push.",
                actionLabel = "Descargar frida-server",
                actionIcon = Icons.Outlined.CloudDownload,
                onAction = onDownload
            )
        }
    } else {
        items(binaries, key = { it.path }, contentType = { "binary" }) { binary ->
            BinaryItem(
                binary = binary,
                isSelected = selectedBinary?.path == binary.path,
                onClick = { onSelect(binary) }
            )
        }
    }
}

@Composable
private fun BinaryItem(
    binary: FridaFile,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) SurfaceMid else SurfaceLow,
        label = "bin_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) AccentPrimary.copy(alpha = 0.6f) else SurfaceBorder,
        label = "bin_border"
    )
    val (cleanName, archTag) = parseBinary(binary.name)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.control)
            .background(bgColor)
            .border(1.dp, borderColor, Shapes.control)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Memory,
            contentDescription = null,
            tint = if (isSelected) AccentPrimaryHi else TextTertiary,
            modifier = Modifier.size(Sizes.iconLg)
        )
        Spacer(Modifier.width(Spacing.lg))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = cleanName,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (archTag != null) {
                    Spacer(Modifier.width(Spacing.sm))
                    Chip(text = archTag, color = AccentCyan, mono = true)
                }
            }
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = binary.path,
                style = MonoCaption,
                color = TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        // Espacio reservado para que el contenido no salte al seleccionar.
        Box(Modifier.size(Sizes.iconLg)) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = AccentPrimaryHi
                )
            }
        }
    }
}

/** Devuelve (nombre limpio, etiqueta de arquitectura). */
private fun parseBinary(rawName: String): Pair<String, String?> {
    val arch = when {
        rawName.contains("android-arm64") -> "ARM64"
        rawName.contains("android-arm") -> "ARM"
        rawName.contains("android-x86_64") -> "X86_64"
        rawName.contains("android-x86") -> "X86"
        else -> null
    }
    val clean = rawName
        .replace("-android-arm64", "")
        .replace("-android-arm", "")
        .replace("-android-x86_64", "")
        .replace("-android-x86", "")
        .replace("frida-server-", "frida ")
        .replace("frida-server", "frida")
    return clean to arch
}
