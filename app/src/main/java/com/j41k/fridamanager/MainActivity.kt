package com.j41k.fridamanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.j41k.fridamanager.ui.components.*
import com.j41k.fridamanager.ui.screens.LogsScreen
import com.j41k.fridamanager.ui.screens.RemoteScreen
import com.j41k.fridamanager.ui.screens.SectionHeader
import com.j41k.fridamanager.ui.theme.*
import com.j41k.fridamanager.viewmodel.FridaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FridaManagerTheme {
                val viewModel: FridaViewModel = viewModel()
                FridaManagerApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FridaManagerApp(viewModel: FridaViewModel) {
    var showSplash by remember { mutableStateOf(true) }
    var showInfoSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    // Simulación de carga inicial con el logo
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1200)
        showSplash = false
    }

    FridaManagerTheme {
        if (showSplash) {
            SplashScreen()
        } else {
            MainContent(viewModel, onInfoClick = { showInfoSheet = true })

            if (showInfoSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showInfoSheet = false },
                    sheetState = sheetState,
                    containerColor = SurfaceLow,
                    scrimColor = Color.Black.copy(alpha = 0.7f)
                ) {
                    CreditsSheetContent(
                        onOpenGithub = {
                            try {
                                context.startActivity(viewModel.openGitHubRepo())
                            } catch (e: Exception) {
                                viewModel.addRawLog("⚠ No se encontró navegador instalado: ${e.message}")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBase),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { this.alpha = alpha }
        ) {
            AppLogo(
                size = 52.dp,
                activeAccent = true,
                animate = true
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "FRIDA MANAGER",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp
            )
            Text(
                text = "PRO TOOLKIT",
                style = MonoCaption,
                color = AccentPrimary,
                letterSpacing = 3.sp,
                fontSize = 11.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(viewModel: FridaViewModel, onInfoClick: () -> Unit) {
    val tabs = remember(viewModel.availableBinaries.size, viewModel.logEntries.size) {
        listOf(
            FridaTabs.Local.copy(badge = viewModel.availableBinaries.size.takeIf { it > 0 }),
            FridaTabs.Remote,
            FridaTabs.Logs.copy(badge = viewModel.logEntries.size.takeIf { it > 0 }),
            FridaTabs.Settings
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SurfaceBase, Color(0xFF06080D))
                )
            )
    ) {
        // Halo radial sutil
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AccentPrimary.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        radius = 1200f
                    )
                )
        )
        // Grid blueprint global
        BlueprintGrid(
            modifier = Modifier.fillMaxSize(),
            spacing = 36.dp,
            dotColor = Color.White.copy(alpha = 0.025f)
        )

        Scaffold(
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                ProTopBar(
                    versionLabel = "v${BuildConfig.VERSION_NAME}",
                    isLive = viewModel.isFridaRunning,
                    onInfoClick = onInfoClick,
                    onSettingsClick = { viewModel.currentTab = 3 }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(14.dp))

                    StatusCard(
                        isRooted = viewModel.isRooted,
                        fridaStatus = viewModel.fridaStatus,
                        uptime = viewModel.getUptimeString(),
                        heartbeat = viewModel.heartbeat,
                        architecture = viewModel.deviceArchitecture(),
                        selectedBinaryName = viewModel.selectedBinary?.name
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    PrerequisitesCard(
                        isRooted              = viewModel.isRooted,
                        isDeveloperModeEnabled = viewModel.isDeveloperModeEnabled,
                        isUsbDebuggingEnabled  = viewModel.isUsbDebuggingEnabled,
                        isSelinuxPermissive    = viewModel.isSelinuxPermissive,
                        hasBinarySelected      = viewModel.selectedBinary != null,
                        isBinaryCompatible     = viewModel.isBinaryCompatibleWithDevice(),
                        onNavigateToSettings   = { viewModel.currentTab = 3 }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ProTabs(
                        tabs = tabs,
                        selectedIndex = viewModel.currentTab,
                        onSelected = { viewModel.currentTab = it }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    AnimatedContent(
                        targetState = viewModel.currentTab,
                        transitionSpec = {
                            val direction = if (targetState > initialState) 1 else -1
                            (slideInHorizontally { w -> direction * w / 6 } + fadeIn()) togetherWith
                            (slideOutHorizontally { w -> -direction * w / 6 } + fadeOut())
                        },
                        label = "tab_content",
                        modifier = Modifier.weight(1f)
                    ) { tab ->
                        when (tab) {
                            0 -> Column(modifier = Modifier.fillMaxSize()) {
                                SectionHeader(
                                    title = "BINARIOS LOCALES",
                                    subtitle = "/data/local/tmp",
                                    actionIcon = Icons.Outlined.Refresh,
                                    actionTint = AccentPrimary,
//                                    onAction = { viewModel.refreshBinaries() }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                BinaryList(
                                    binaries = viewModel.availableBinaries,
                                    selectedBinary = viewModel.selectedBinary,
                                    onSelect = { viewModel.selectedBinary = it }
                                )
                            }
                            1 -> RemoteScreen(viewModel)
                            2 -> LogsScreen(viewModel)
                            3 -> SettingsScreen(viewModel)
                        }
                    }
                }
            }

            FloatingActionPanel(
                viewModel = viewModel,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )

            if (viewModel.isDownloading) {
                DownloadOverlay(viewModel)
            }
        }
    }
}

@Composable
fun CreditsSheetContent(onOpenGithub: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppLogo(
            size = 44.dp,
            activeAccent = false,
            animate = true
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "FRIDA MANAGER",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            letterSpacing = 1.5.sp
        )
        Text(
            text = "v${BuildConfig.VERSION_NAME} · Pro Toolkit",
            style = MonoCaption,
            color = TextTertiary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "por J41K",
            style = MaterialTheme.typography.bodySmall,
            color = AccentCyan
        )

        Spacer(modifier = Modifier.height(28.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            ManualStep(
                number = "01",
                title = "PREPARACIÓN",
                description = "Asegúrate de tener acceso root concedido para gestionar archivos en /data/local/tmp."
            )
            ManualStep(
                number = "02",
                title = "ADQUISICIÓN",
                description = "Descarga la última versión desde la pestaña REMOTOS o instala manualmente con ADB push."
            )
            ManualStep(
                number = "03",
                title = "CONFIGURACIÓN",
                description = "En AJUSTES puedes personalizar el puerto (27042 por defecto) y la dirección de escucha."
            )
            ManualStep(
                number = "04",
                title = "EJECUCIÓN",
                description = "Selecciona el binario y pulsa INICIAR SERVICIO. La app aplica chmod 755 automáticamente."
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AccentCyan.copy(alpha = 0.06f))
                .border(1.dp, AccentCyan.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "El servicio se ejecuta como foreground service para evitar que Android lo cierre cuando la app pase a segundo plano.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onOpenGithub,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SurfaceMid,
                contentColor = TextPrimary
            ),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true)
        ) {
            Icon(
                Icons.Outlined.Code,
                contentDescription = null,
                tint = AccentPrimaryHi,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "REPOSITORIO EN GITHUB",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun ManualStep(number: String, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AccentPrimary.copy(alpha = 0.12f))
                .border(1.dp, AccentPrimary.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = AccentPrimaryHi,
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = 1.6.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
fun SettingsScreen(viewModel: FridaViewModel) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isRunning = viewModel.isFridaRunning

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 110.dp)
    ) {
        SectionHeader(
            title = "CONFIGURACIÓN DE RED",
            subtitle = "Endpoint del frida-server",
            actionTint = AccentPrimary
        )
        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = viewModel.fridaPort,
            onValueChange = { viewModel.fridaPort = it },
            enabled = !isRunning,
            label = { Text("Puerto del servidor", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentPrimary,
                unfocusedBorderColor = SurfaceBorder,
                disabledBorderColor = SurfaceBorder.copy(alpha = 0.5f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                disabledTextColor = TextTertiary,
                cursorColor = AccentPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = viewModel.fridaAddress,
            onValueChange = { viewModel.fridaAddress = it },
            enabled = !isRunning,
            label = { Text("Dirección de escucha", color = TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentPrimary,
                unfocusedBorderColor = SurfaceBorder,
                disabledBorderColor = SurfaceBorder.copy(alpha = 0.5f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                disabledTextColor = TextTertiary,
                cursorColor = AccentPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        if (isRunning) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Detén el servicio para modificar la configuración.",
                style = MaterialTheme.typography.labelSmall,
                color = StatusWarning
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        SectionHeader(
            title = "AJUSTES DE SEGURIDAD (ROOT)",
            subtitle = "Fix para Samsung y dispositivos modernos",
            actionTint = AccentCyan
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Toggle SELinux Automático
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceLow)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "SELinux Permisivo Automático",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Text(
                    "Pone SELinux en '0' al iniciar el servicio (Recomendado).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }
            Switch(
                checked = viewModel.autoPermissive,
                onCheckedChange = { viewModel.autoPermissive = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AccentCyan,
                    checkedTrackColor = AccentCyan.copy(alpha = 0.5f)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Botón Manual SELinux
        Button(
            onClick = { viewModel.toggleSelinux() },
            enabled = viewModel.isRooted,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (viewModel.isSelinuxPermissive) StatusWarning.copy(alpha = 0.1f) else SurfaceLow,
                contentColor = if (viewModel.isSelinuxPermissive) StatusWarning else TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true)
        ) {
            Icon(
                if (viewModel.isSelinuxPermissive) Icons.Outlined.SecurityUpdateWarning else Icons.Outlined.Security,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                if (viewModel.isSelinuxPermissive) "SELinux: PERMISIVO" else "SELinux: ENFORCING",
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontSize = 12.sp
            )
        }

        if (viewModel.isSelinuxPermissive) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "⚠ Algunos Samsung revierten este cambio automáticamente. Si el crash persiste, pulsa el botón de arriba nuevamente antes de lanzar Frida.",
                style = MaterialTheme.typography.labelSmall,
                color = StatusWarning,
                lineHeight = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón Turbo Fix (Basado en la sugerencia del usuario)
        Button(
            onClick = { viewModel.applyTurboFix() },
            enabled = viewModel.isRooted,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentPrimary.copy(alpha = 0.15f),
                contentColor = AccentPrimaryHi
            ),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true)
        ) {
            Icon(Icons.Outlined.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "APLICAR SAMSUNG TURBO-FIX",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                Text(
                    "Desactiva USAP + SELinux Permisivo",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = AccentPrimaryHi.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        SectionHeader(
            title = "MANTENIMIENTO",
            subtitle = "Operaciones destructivas",
            actionTint = StatusCritical
        )
        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = { showDeleteConfirm = true },
            enabled = viewModel.selectedBinary != null && !isRunning && !viewModel.isDeleting,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = StatusCritical.copy(alpha = 0.10f),
                contentColor = StatusCritical,
                disabledContainerColor = SurfaceLow,
                disabledContentColor = TextTertiary
            ),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true)
        ) {
            if (viewModel.isDeleting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = StatusCritical
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "ELIMINANDO…",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    fontSize = 12.sp
                )
            } else {
                Icon(
                    Icons.Outlined.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "ELIMINAR BINARIO SELECCIONADO",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    fontSize = 12.sp
                )
            }
        }

        if (viewModel.selectedBinary == null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Selecciona un binario en la pestaña LOCAL para habilitar esta opción.",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar Binario?", color = TextPrimary) },
            text = {
                Text(
                    "Se eliminará '${viewModel.selectedBinary?.name}' de /data/local/tmp.\nEsta acción no se puede deshacer.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSelectedBinary()
                    showDeleteConfirm = false
                }) {
                    Text(
                        "ELIMINAR",
                        color = StatusCritical,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("CANCELAR", color = TextSecondary, letterSpacing = 1.2.sp)
                }
            },
            containerColor = SurfaceLow,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun FloatingActionPanel(viewModel: FridaViewModel, modifier: Modifier = Modifier) {
    val isRunning = viewModel.isFridaRunning
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceMid.copy(alpha = 0.95f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(SurfaceBorder, Color.Transparent)
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FabIconButton(
                icon = Icons.Outlined.Refresh,
                tint = TextSecondary,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.refreshBinaries()
                }
            )

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleFrida()
                },
                enabled = viewModel.isRooted && viewModel.selectedBinary != null,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) StatusCritical else AccentPrimary,
                    contentColor = TextOnAccent,
                    disabledContainerColor = SurfaceHigh.copy(alpha = 0.5f),
                    disabledContentColor = TextTertiary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isRunning) Icons.Outlined.StopCircle
                                       else Icons.Outlined.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isRunning) "DETENER SERVICIO" else "INICIAR SERVICIO",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        fontSize = 12.sp
                    )
                }
            }

            FabIconButton(
                icon = Icons.Outlined.CloudDownload,
                tint = AccentCyan,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.downloadLatestFrida()
                },
                enabled = !viewModel.isDownloading
            )
        }
    }
}

@Composable
private fun FabIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceLow)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) tint else TextTertiary.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun DownloadOverlay(viewModel: FridaViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBase.copy(alpha = 0.85f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceLow)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                .padding(28.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = AccentPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = viewModel.downloadStatus.ifEmpty { "Procesando…" },
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { viewModel.downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = AccentCyan,
                    trackColor = SurfaceHigh
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${(viewModel.downloadProgress * 100).toInt()}%",
                    style = MonoCaption,
                    color = TextTertiary
                )
            }
        }
    }
}
