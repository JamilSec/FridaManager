package com.j41k.fridamanager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.j41k.fridamanager.data.FridaFile
import com.j41k.fridamanager.ui.theme.*

@Composable
fun BinaryList(
    binaries: List<FridaFile>,
    selectedBinary: FridaFile?,
    onSelect: (FridaFile) -> Unit,
    modifier: Modifier = Modifier
) {
    if (binaries.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceLow.copy(alpha = 0.5f))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SurfaceMid),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Memory,
                        contentDescription = null,
                        tint = TextTertiary.copy(alpha = 0.4f),
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "DEPÓSITO VACÍO",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "No se encontraron binarios en /data/local/tmp.\nVe a la pestaña REMOTOS para adquirir uno.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    lineHeight = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            items(binaries) { binary ->
                val isSelected = selectedBinary == binary
                BinaryItem(
                    binary = binary,
                    isSelected = isSelected,
                    onClick = { onSelect(binary) }
                )
            }
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

    val (cleanName, archTag) = parseBinary(binary.name)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = if (isSelected) AccentPrimary.copy(alpha = 0.6f) else SurfaceBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono simplificado
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) AccentPrimary.copy(alpha = 0.12f)
                        else SurfaceHigh.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Memory,
                    contentDescription = null,
                    tint = if (isSelected) AccentPrimaryHi else TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = cleanName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    if (archTag != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = archTag,
                            style = MonoCaption,
                            color = AccentCyan.copy(alpha = 0.8f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = binary.path,
                    style = MonoCaption,
                    color = TextTertiary,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = AccentPrimaryHi,
                    modifier = Modifier.size(22.dp)
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
