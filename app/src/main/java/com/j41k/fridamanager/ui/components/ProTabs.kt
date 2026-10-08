package com.j41k.fridamanager.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.j41k.fridamanager.ui.theme.*

data class ProTabItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val badge: Int? = null
)

/** Conjunto de destinos canónicos de la app. El orden define el índice de pestaña. */
object FridaTabs {
    val Server   = ProTabItem("Servidor",  Icons.Outlined.Dns,           Icons.Filled.Dns)
    val Remote   = ProTabItem("Descargas", Icons.Outlined.CloudDownload, Icons.Filled.CloudDownload)
    val Logs     = ProTabItem("Registros", Icons.Outlined.Terminal,      Icons.Filled.Terminal)
    val Settings = ProTabItem("Ajustes",   Icons.Outlined.Tune,          Icons.Filled.Tune)

    const val SERVER = 0
    const val REMOTE = 1
    const val LOGS = 2
    const val SETTINGS = 3
}

/**
 * Navegación inferior nativa (Material 3 NavigationBar): zona del pulgar,
 * semántica de pestañas, indicador y gestión de insets de la barra del sistema.
 */
@Composable
fun AppNavigationBar(
    tabs: List<ProTabItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = SurfaceLow,
        tonalElevation = NavigationBarDefaults.Elevation
    ) {
        tabs.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            NavigationBarItem(
                selected = selected,
                onClick = { onSelected(index) },
                icon = {
                    BadgedBox(badge = {
                        if (item.badge != null && item.badge > 0) {
                            Badge(containerColor = AccentPrimary, contentColor = TextOnAccent) {
                                Text(if (item.badge > 99) "99+" else item.badge.toString())
                            }
                        }
                    }) {
                        Icon(
                            if (selected) item.selectedIcon else item.icon,
                            contentDescription = null
                        )
                    }
                },
                label = { Text(item.label, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentPrimaryHi,
                    selectedTextColor = TextPrimary,
                    indicatorColor = AccentPrimary.copy(alpha = 0.18f),
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}
