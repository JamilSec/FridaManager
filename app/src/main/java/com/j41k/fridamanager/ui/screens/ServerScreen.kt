package com.j41k.fridamanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.j41k.fridamanager.ui.components.PrerequisitesCard
import com.j41k.fridamanager.ui.components.SectionHeader
import com.j41k.fridamanager.ui.components.StatusCard
import com.j41k.fridamanager.ui.components.binaryItems
import com.j41k.fridamanager.ui.components.FridaTabs
import com.j41k.fridamanager.ui.theme.Spacing
import com.j41k.fridamanager.viewmodel.FridaViewModel

/**
 * Pantalla principal: estado → requisitos → binarios, en una única superficie
 * de scroll para que el contenido nunca quede comprimido en pantallas pequeñas
 * o con tamaño de fuente grande.
 */
@Composable
fun ServerScreen(viewModel: FridaViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.screen,
            end = Spacing.screen,
            top = Spacing.lg,
            bottom = Spacing.xl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item(key = "status", contentType = "status") {
            StatusCard(
                isRooted = viewModel.isRooted,
                fridaStatus = viewModel.fridaStatus,
                uptime = { viewModel.getUptimeString() },
                architecture = viewModel.deviceArchitecture(),
                selectedBinaryName = viewModel.selectedBinary?.name
            )
        }
        item(key = "requirements", contentType = "requirements") {
            PrerequisitesCard(
                isRooted               = viewModel.isRooted,
                isDeveloperModeEnabled = viewModel.isDeveloperModeEnabled,
                isUsbDebuggingEnabled  = viewModel.isUsbDebuggingEnabled,
                isSelinuxPermissive    = viewModel.isSelinuxPermissive,
                hasBinarySelected      = viewModel.selectedBinary != null,
                isBinaryCompatible     = viewModel.isBinaryCompatibleWithDevice(),
                onNavigateToSettings   = { viewModel.currentTab = FridaTabs.SETTINGS }
            )
        }
        item(key = "binaries_header", contentType = "header") {
            SectionHeader(
                title = "Binarios instalados",
                subtitle = "/data/local/tmp",
                actionIcon = Icons.Outlined.Refresh,
                actionDescription = "Volver a escanear binarios",
                onAction = { viewModel.refreshBinaries(userInitiated = true) }
            )
        }
        binaryItems(
            binaries = viewModel.availableBinaries,
            selectedBinary = viewModel.selectedBinary,
            onSelect = { viewModel.selectedBinary = it },
            onDownload = { viewModel.downloadLatestFrida() }
        )
    }
}
