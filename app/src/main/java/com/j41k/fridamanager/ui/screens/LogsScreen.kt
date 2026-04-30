package com.j41k.fridamanager.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.j41k.fridamanager.ui.theme.*
import com.j41k.fridamanager.viewmodel.FridaViewModel
import com.j41k.fridamanager.viewmodel.LogEntry
import com.j41k.fridamanager.viewmodel.LogLevel
import kotlinx.coroutines.launch

@Composable
fun LogsScreen(viewModel: FridaViewModel) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val filtered = viewModel.filteredLogs()
    val totalCount = viewModel.logEntries.size

    // Auto-scroll cuando hay nuevas entradas y el flag está activo
    LaunchedEffect(filtered.size, viewModel.logAutoScroll) {
        if (viewModel.logAutoScroll && filtered.isNotEmpty()) {
            listState.animateScrollToItem(filtered.size - 1)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
        onResult = { uri ->
            if (uri != null) {
                coroutineScope.launch {
                    val resolver = context.contentResolver
                    resolver.openOutputStream(uri)?.use { stream ->
                        val n = viewModel.exportLogsTo(stream)
                        viewModel.addRawLog("✓ Exportadas $n entradas a $uri")
                    } ?: viewModel.addRawLog("✗ No se pudo abrir el archivo de exportación.")
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp)
            .padding(bottom = 110.dp)
    ) {

        SectionHeader(
            title = "REGISTROS DEL SISTEMA",
            subtitle = "Captura logcat · frida-server",
            isLoading = false,
            actionIcon = null
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Toolbar: search + acciones
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search box
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceLow)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = viewModel.logSearchQuery,
                        onValueChange = { viewModel.logSearchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        cursorBrush = SolidColor(AccentPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (viewModel.logSearchQuery.isEmpty()) {
                        Text(
                            text = "Filtrar por contenido…",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
                if (viewModel.logSearchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.logSearchQuery = "" },
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Limpiar búsqueda",
                            tint = TextTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Auto-scroll toggle
            ToolbarToggle(
                checked = viewModel.logAutoScroll,
                onClick = { viewModel.logAutoScroll = !viewModel.logAutoScroll },
                label = "AUTO"
            )

            // Export
            ToolbarButton(
                icon = Icons.Outlined.FileDownload,
                tint = AccentCyan,
                onClick = {
                    val ts = java.text.SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date())
                    exportLauncher.launch("frida-manager-logs-$ts.txt")
                }
            )

            // Clear
            ToolbarButton(
                icon = Icons.Outlined.Delete,
                tint = StatusCritical,
                onClick = { viewModel.clearLogs() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Console
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceLow.copy(alpha = 0.4f))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Stripe superior estilo "console"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceLow)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Terminal,
                        contentDescription = null,
                        tint = AccentPrimaryHi,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "CONSOLE OUTPUT",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${filtered.size} / $totalCount",
                        style = MonoCaption,
                        color = TextTertiary,
                        fontSize = 10.sp
                    )
                }

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (totalCount == 0) "SIN ACTIVIDAD REGISTRADA"
                                   else "SIN COINCIDENCIAS PARA EL FILTRO",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextTertiary.copy(alpha = 0.55f),
                            letterSpacing = 1.5.sp,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        items(filtered) { entry ->
                            LogRow(entry = entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    val color = when (entry.level) {
        LogLevel.OK -> LogOk
        LogLevel.ERROR -> LogError
        LogLevel.WARN -> LogWarn
        LogLevel.INFO -> LogInfo
        LogLevel.TRACE -> LogTrace
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Indicador de nivel
        Text(
            text = entry.timestamp,
            style = MonoCaption,
            color = TextTertiary,
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = entry.message,
            style = MonoCaption,
            color = if (entry.level == LogLevel.TRACE) TextSecondary else color,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceLow)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun ToolbarToggle(
    checked: Boolean,
    onClick: () -> Unit,
    label: String
) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (checked) AccentPrimary.copy(alpha = 0.18f) else SurfaceLow)
            .border(
                1.dp,
                if (checked) AccentPrimary.copy(alpha = 0.45f) else SurfaceBorder,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (checked) AccentPrimaryHi else TextTertiary)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (checked) AccentPrimaryHi else TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }
    }
}

