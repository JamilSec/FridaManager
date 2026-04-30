package com.j41k.fridamanager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Architecture
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.j41k.fridamanager.net.FridaDownloader
import com.j41k.fridamanager.ui.components.BlueprintGrid
import com.j41k.fridamanager.ui.theme.*
import com.j41k.fridamanager.viewmodel.FridaViewModel

@Composable
fun RemoteScreen(viewModel: FridaViewModel) {
    val release = viewModel.remoteRelease
    val isChecking = viewModel.isCheckingRemote
    val arch = viewModel.deviceArchitecture()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 110.dp)
    ) {
        SectionHeader(
            title = "LANZAMIENTOS · GITHUB",
            subtitle = "frida/frida — última release oficial",
            actionIcon = if (isChecking) null else Icons.Outlined.Refresh,
            actionTint = AccentPrimary,
            isLoading = isChecking,
//            onAction = { viewModel.checkRemoteRelease() }
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (release != null) {
            ReleaseCard(release = release, arch = arch, viewModel = viewModel)
        } else if (!isChecking) {
            EmptyState()
        } else {
            LoadingPlaceholder()
        }
    }
}

@Composable
private fun ReleaseCard(
    release: FridaDownloader.FridaRelease,
    arch: String,
    viewModel: FridaViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(SurfaceLow, SurfaceBase)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        AccentPrimary.copy(alpha = 0.4f),
                        AccentCyan.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {

            // Header con badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AccentPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.CloudDownload,
                        contentDescription = null,
                        tint = AccentPrimaryHi,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RELEASE OFICIAL",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentPrimaryHi,
                        fontSize = 10.sp,
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Frida ${release.version}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info grid
            Row(modifier = Modifier.fillMaxWidth()) {
                MetaCell(
                    icon = Icons.Outlined.Architecture,
                    label = "ARCH",
                    value = arch.uppercase(),
                    valueMono = true,
                    modifier = Modifier.weight(1f)
                )
                MetaCell(
                    icon = Icons.Outlined.FolderZip,
                    label = "SIZE",
                    value = formatBytes(release.sizeBytes),
                    valueMono = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                MetaCell(
                    icon = Icons.Outlined.CalendarMonth,
                    label = "FECHA",
                    value = formatPublishedDate(release.publishedAt),
                    valueMono = false,
                    modifier = Modifier.weight(1f)
                )
                MetaCell(
                    icon = Icons.Outlined.Description,
                    label = "TAG",
                    value = release.version,
                    valueMono = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Changelog colapsable con mejor estilo
            ChangelogSection(changelog = release.changelog)

            Spacer(modifier = Modifier.height(24.dp))

            // CTA
            Button(
                onClick = { viewModel.downloadLatestFrida() },
                enabled = !viewModel.isDownloading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPrimary,
                    contentColor = TextOnAccent
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    Icons.Outlined.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "DESCARGAR AHORA",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}

@Composable
private fun ChangelogSection(changelog: String?) {
    var expanded by remember { mutableStateOf(false) }
    val hasContent = !changelog.isNullOrBlank()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceBase.copy(alpha = 0.55f))
            .border(1.dp, SurfaceDivider, RoundedCornerShape(12.dp))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = hasContent) { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "NOTAS DE LA VERSIÓN",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp,
                    modifier = Modifier.weight(1f)
                )
                if (hasContent) {
                    Icon(
                        if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded && hasContent,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 14.dp)
                ) {
                    Text(
                        text = changelog?.take(2400) ?: "",
                        style = MonoBodySmall,
                        color = TextSecondary,
                        lineHeight = 17.sp,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaCell(
    icon: ImageVector,
    label: String,
    value: String,
    valueMono: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(end = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                fontSize = 9.sp,
                letterSpacing = 1.1.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = if (valueMono) MonoBodyMedium else MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceLow)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.CloudDownload,
                contentDescription = null,
                tint = TextTertiary.copy(alpha = 0.5f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "SIN CONEXIÓN AL REPOSITORIO",
                style = MaterialTheme.typography.labelMedium,
                color = TextTertiary,
                letterSpacing = 1.5.sp,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Comprueba tu red e intenta de nuevo",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun LoadingPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceLow)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = AccentPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "CONSULTANDO API DE GITHUB…",
                style = MaterialTheme.typography.labelMedium,
                color = TextTertiary,
                letterSpacing = 1.4.sp,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionIcon: ImageVector? = null,
    actionTint: Color = AccentPrimary,
    isLoading: Boolean = false,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = if (subtitle != null) 24.dp else 16.dp)
                .background(actionTint, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                fontSize = 11.sp,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.Black
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = actionTint
            )
        } else if (actionIcon != null && onAction != null) {
            IconButton(
                onClick = onAction,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceMid.copy(alpha = 0.4f))
                    .border(1.dp, SurfaceBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    actionIcon,
                    contentDescription = null,
                    tint = actionTint.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "—"
    val units = arrayOf("B", "KB", "MB", "GB")
    var b = bytes.toDouble()
    var i = 0
    while (b >= 1024 && i < units.size - 1) {
        b /= 1024
        i++
    }
    return String.format("%.1f %s", b, units[i])
}

private fun formatPublishedDate(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return try {
        // Formato GitHub: "2024-09-26T15:34:08Z"
        val parser = java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            java.util.Locale.US
        ).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        val formatter = java.text.SimpleDateFormat(
            "dd MMM yyyy",
            java.util.Locale("es", "ES")
        )
        val date = parser.parse(iso) ?: return iso
        formatter.format(date)
    } catch (e: Exception) {
        iso
    }
}
