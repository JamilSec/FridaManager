package com.j41k.fridamanager.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.j41k.fridamanager.ui.theme.*
import com.j41k.fridamanager.viewmodel.FridaViewModel
import com.j41k.fridamanager.viewmodel.LogEntry
import com.j41k.fridamanager.viewmodel.LogLevel
import kotlinx.coroutines.launch

private val levelFilters = listOf(
    null to "Todo",
    LogLevel.INFO to "Info",
    LogLevel.OK to "OK",
    LogLevel.WARN to "Avisos",
    LogLevel.ERROR to "Errores"
)

@Composable
fun LogsScreen(viewModel: FridaViewModel) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Solo se recalcula cuando cambian los logs, la búsqueda o el filtro.
    val filtered by remember { derivedStateOf { viewModel.filteredLogs() } }
    val totalCount = viewModel.logEntries.size

    // Marca como leídos mientras esta pestaña está visible.
    LaunchedEffect(totalCount) { viewModel.markLogsSeen() }

    // Auto-scroll: salto directo si estamos lejos, animado si la entrada es contigua.
    LaunchedEffect(filtered.size, viewModel.logAutoScroll) {
        if (viewModel.logAutoScroll && filtered.isNotEmpty()) {
            val last = filtered.lastIndex
            val visibleLast = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (last - visibleLast > 5) listState.scrollToItem(last) else listState.animateScrollToItem(last)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
        onResult = { uri ->
            if (uri != null) {
                coroutineScope.launch {
                    val n = context.contentResolver.openOutputStream(uri)?.use { viewModel.exportLogsTo(it) }
                    if (n != null) {
                        viewModel.addRawLog("✓ Exportadas $n entradas a $uri")
                        viewModel.notify("$n entradas exportadas")
                    } else {
                        viewModel.addRawLog("✗ No se pudo abrir el archivo de exportación.")
                        viewModel.notify("No se pudo exportar el archivo")
                    }
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.screen)
            .padding(top = Spacing.md, bottom = Spacing.md)
    ) {
        // Toolbar: búsqueda + acciones (todas de 48dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchField(
                value = viewModel.logSearchQuery,
                onValueChange = { viewModel.logSearchQuery = it },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(Spacing.xs))
            IconToggleButton(
                checked = viewModel.logAutoScroll,
                onCheckedChange = { viewModel.logAutoScroll = it },
                colors = IconButtonDefaults.iconToggleButtonColors(
                    contentColor = TextSecondary,
                    checkedContentColor = AccentPrimaryHi
                )
            ) {
                Icon(Icons.Outlined.VerticalAlignBottom, contentDescription = "Seguir nuevas entradas")
            }
            IconButton(
                onClick = {
                    val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                    exportLauncher.launch("frida-manager-logs-$ts.txt")
                },
                enabled = totalCount > 0
            ) {
                Icon(Icons.Outlined.FileDownload, contentDescription = "Exportar registros", tint = if (totalCount > 0) TextSecondary else TextDisabled)
            }
            IconButton(onClick = { viewModel.clearLogs() }, enabled = totalCount > 0) {
                Icon(Icons.Outlined.DeleteSweep, contentDescription = "Borrar registros", tint = if (totalCount > 0) TextSecondary else TextDisabled)
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        // Filtros por nivel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            levelFilters.forEach { (level, label) ->
                val selected = viewModel.logLevelFilter == level
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.logLevelFilter = level },
                    label = { Text(label) },
                    shape = Shapes.control,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SurfaceLow,
                        labelColor = TextSecondary,
                        selectedContainerColor = AccentPrimary.copy(alpha = 0.18f),
                        selectedLabelColor = AccentPrimaryHi
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = SurfaceBorder,
                        selectedBorderColor = AccentPrimary.copy(alpha = 0.45f)
                    )
                )
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        // Consola
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(Shapes.container)
                .background(SurfaceLow)
                .border(1.dp, SurfaceBorder, Shapes.container)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "logcat · frida-server",
                    style = MonoCaption,
                    color = TextTertiary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (filtered.size == totalCount) "$totalCount" else "${filtered.size} de $totalCount",
                    style = MonoCaption,
                    color = TextTertiary
                )
            }
            HorizontalDivider(color = SurfaceDivider)

            if (filtered.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.Terminal, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = if (totalCount == 0) "Sin actividad registrada" else "Ninguna entrada coincide con el filtro",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm)
                    ) {
                        items(filtered, key = { it.id }, contentType = { "log" }) { entry ->
                            LogRow(entry = entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
        cursorBrush = SolidColor(AccentPrimaryHi),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .height(Sizes.touchTarget)
                    .clip(Shapes.control)
                    .background(SurfaceLow)
                    .border(1.dp, SurfaceBorder, Shapes.control)
                    .padding(start = Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(Sizes.iconMd))
                Spacer(Modifier.width(Spacing.sm))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text("Buscar en registros", style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
                    }
                    inner()
                }
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Limpiar búsqueda", tint = TextSecondary, modifier = Modifier.size(Sizes.iconMd))
                    }
                } else {
                    Spacer(Modifier.width(Spacing.md))
                }
            }
        }
    )
}

@Composable
private fun LogRow(entry: LogEntry) {
    val color = when (entry.level) {
        LogLevel.OK -> LogOk
        LogLevel.ERROR -> LogError
        LogLevel.WARN -> LogWarn
        LogLevel.INFO -> LogInfo
        LogLevel.TRACE -> TextSecondary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xxs),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = entry.timestamp, style = MonoCaption, color = TextTertiary)
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = entry.message,
            style = MonoBodySmall,
            color = color,
            modifier = Modifier.weight(1f)
        )
    }
}
