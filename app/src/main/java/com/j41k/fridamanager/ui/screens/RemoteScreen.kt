package com.j41k.fridamanager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.j41k.fridamanager.net.FridaDownloader
import com.j41k.fridamanager.ui.components.EmptyState
import com.j41k.fridamanager.ui.components.SectionHeader
import com.j41k.fridamanager.ui.components.SkeletonBlock
import com.j41k.fridamanager.ui.components.panel
import com.j41k.fridamanager.ui.components.rememberSkeletonAlpha
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen)
            .padding(top = Spacing.sm, bottom = Spacing.xl)
    ) {
        SectionHeader(
            title = "Última versión oficial",
            subtitle = "github.com/frida/frida",
            actionIcon = Icons.Outlined.Refresh,
            actionDescription = "Buscar actualizaciones",
            isLoading = isChecking,
            onAction = { viewModel.checkRemoteRelease() }
        )
        Spacer(Modifier.height(Spacing.sm))

        when {
            release != null -> ReleaseCard(
                release = release,
                arch = arch,
                isInstalled = viewModel.availableBinaries.any { it.name.contains(release.version) },
                isDownloading = viewModel.isDownloading,
                onDownload = { viewModel.downloadLatestFrida() }
            )
            isChecking -> ReleaseSkeleton()
            else -> EmptyState(
                icon = Icons.Outlined.CloudOff,
                title = "No se pudo consultar GitHub",
                message = "Comprueba tu conexión a internet e inténtalo de nuevo.",
                actionLabel = "Reintentar",
                actionIcon = Icons.Outlined.Refresh,
                onAction = { viewModel.checkRemoteRelease() }
            )
        }
    }
}

@Composable
private fun ReleaseCard(
    release: FridaDownloader.FridaRelease,
    arch: String,
    isInstalled: Boolean,
    isDownloading: Boolean,
    onDownload: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel()
            .padding(Spacing.lg)
    ) {
        Text(
            text = "Frida ${release.version}",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary
        )
        if (isInstalled) {
            Spacer(Modifier.height(Spacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = StatusOnline,
                    modifier = Modifier.size(Sizes.iconSm)
                )
                Spacer(Modifier.width(Spacing.xs))
                Text("Ya instalada en el dispositivo", style = MaterialTheme.typography.bodySmall, color = StatusOnlineHi)
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        Row(Modifier.fillMaxWidth()) {
            MetaCell("Arquitectura", arch.uppercase(), mono = true, modifier = Modifier.weight(1f))
            MetaCell("Tamaño", formatBytes(release.sizeBytes), mono = true, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(Spacing.md))
        Row(Modifier.fillMaxWidth()) {
            MetaCell("Publicada", formatPublishedDate(release.publishedAt), mono = false, modifier = Modifier.weight(1f))
            MetaCell("Tag", release.version, mono = true, modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(Spacing.lg))
        ChangelogSection(changelog = release.changelog)
        Spacer(Modifier.height(Spacing.lg))

        val label = if (isInstalled) "Reinstalar" else "Descargar e instalar"
        if (isInstalled) {
            OutlinedButton(
                onClick = onDownload,
                enabled = !isDownloading,
                modifier = Modifier.fillMaxWidth().height(Sizes.buttonHeight),
                shape = Shapes.control
            ) { DownloadButtonContent(label) }
        } else {
            Button(
                onClick = onDownload,
                enabled = !isDownloading,
                modifier = Modifier.fillMaxWidth().height(Sizes.buttonHeight),
                shape = Shapes.control,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary, contentColor = TextOnAccent)
            ) { DownloadButtonContent(label) }
        }
    }
}

@Composable
private fun DownloadButtonContent(label: String) {
    Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(Sizes.iconMd))
    Spacer(Modifier.width(Spacing.sm))
    Text(label, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun ChangelogSection(changelog: String?) {
    if (changelog.isNullOrBlank()) return
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = SurfaceDivider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Sizes.touchTarget)
                .clickable(role = Role.Button) { expanded = !expanded }
                .semantics { stateDescription = if (expanded) "Expandido" else "Contraído" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notas de la versión",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = TextSecondary
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            SelectionContainer {
                Text(
                    text = changelog.take(2400),
                    style = MonoBodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = Spacing.sm)
                )
            }
        }
        HorizontalDivider(color = SurfaceDivider)
    }
}

@Composable
private fun MetaCell(label: String, value: String, mono: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(end = Spacing.sm)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextTertiary)
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            text = value,
            style = if (mono) MonoBodyMedium else MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Skeleton con la misma geometría que ReleaseCard: sin saltos de layout al cargar. */
@Composable
private fun ReleaseSkeleton() {
    val alpha = rememberSkeletonAlpha()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel()
            .padding(Spacing.lg)
    ) {
        SkeletonBlock(alpha, width = 160.dp, height = 26.dp)
        Spacer(Modifier.height(Spacing.lg))
        repeat(2) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    SkeletonBlock(alpha, width = 72.dp, height = 12.dp)
                    Spacer(Modifier.height(Spacing.xs))
                    SkeletonBlock(alpha, width = 96.dp, height = 18.dp)
                }
                Column(Modifier.weight(1f)) {
                    SkeletonBlock(alpha, width = 56.dp, height = 12.dp)
                    Spacer(Modifier.height(Spacing.xs))
                    SkeletonBlock(alpha, width = 80.dp, height = 18.dp)
                }
            }
            Spacer(Modifier.height(Spacing.md))
        }
        Spacer(Modifier.height(Spacing.lg))
        SkeletonBlock(alpha, height = Sizes.buttonHeight)
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
    return String.format(java.util.Locale.getDefault(), "%.1f %s", b, units[i])
}

private fun formatPublishedDate(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return try {
        // Formato GitHub: "2024-09-26T15:34:08Z"
        val parser = java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            java.util.Locale.US
        ).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        val formatter = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM)
        val date = parser.parse(iso) ?: return iso
        formatter.format(date)
    } catch (e: Exception) {
        iso
    }
}
