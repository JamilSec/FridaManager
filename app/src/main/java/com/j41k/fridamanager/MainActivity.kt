package com.j41k.fridamanager

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.j41k.fridamanager.ui.components.*
import com.j41k.fridamanager.ui.screens.LogsScreen
import com.j41k.fridamanager.ui.screens.RemoteScreen
import com.j41k.fridamanager.ui.screens.ServerScreen
import com.j41k.fridamanager.ui.screens.SettingsScreen
import com.j41k.fridamanager.ui.theme.*
import com.j41k.fridamanager.viewmodel.FridaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // App solo-oscura: iconos de barras claros aunque el sistema esté en modo claro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
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
    var showInfoSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            val result = snackbarHostState.showSnackbar(
                message = message.text,
                actionLabel = message.actionLabel,
                withDismissAction = message.actionLabel != null,
                duration = if (message.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) message.onAction?.invoke()
        }
    }

    // Atrás desde una pestaña secundaria vuelve a Servidor antes de salir (patrón Android).
    BackHandler(enabled = viewModel.currentTab != FridaTabs.SERVER) {
        viewModel.currentTab = FridaTabs.SERVER
    }

    Scaffold(
        containerColor = SurfaceBase,
        contentColor = TextPrimary,
        topBar = {
            ProTopBar(
                versionLabel = "v${BuildConfig.VERSION_NAME}",
                isLive = viewModel.isFridaRunning,
                onInfoClick = { showInfoSheet = true }
            )
        },
        bottomBar = { BottomBars(viewModel) },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = Shapes.control,
                    containerColor = SurfaceHigh,
                    contentColor = TextPrimary,
                    actionColor = AccentPrimaryHi,
                    dismissActionContentColor = TextSecondary
                )
            }
        }
    ) { innerPadding ->
        TabContent(
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Con el teclado abierto, el contenido se eleva sin sumar el alto de las barras inferiores.
                .consumeWindowInsets(innerPadding)
                .imePadding()
        )
    }

    if (viewModel.isDownloading) {
        DownloadDialog(status = viewModel.downloadStatus, progress = viewModel.downloadProgress)
    }

    if (viewModel.isHiding) {
        HideProgressDialog()
    }

    if (showInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            sheetState = sheetState,
            containerColor = SurfaceMid,
            contentColor = TextPrimary
        ) {
            CreditsSheetContent(
                onOpenGithub = {
                    try {
                        context.startActivity(viewModel.openGitHubRepo())
                    } catch (e: Exception) {
                        viewModel.addRawLog("⚠ No se encontró navegador instalado: ${e.message}")
                        viewModel.notify("No hay ningún navegador disponible")
                    }
                }
            )
        }
    }
}

/** Scope propio: los cambios de badge/acción no recomponen el resto de la pantalla. */
@Composable
private fun BottomBars(viewModel: FridaViewModel) {
    val currentTab = viewModel.currentTab
    val unread = if (currentTab == FridaTabs.LOGS) 0 else viewModel.unreadLogCount
    val tabs = listOf(
        FridaTabs.Server,
        FridaTabs.Remote,
        FridaTabs.Logs.copy(badge = unread.takeIf { it > 0 }),
        FridaTabs.Settings
    )
    Column {
        ServiceActionBar(viewModel)
        AppNavigationBar(
            tabs = tabs,
            selectedIndex = currentTab,
            onSelected = { viewModel.currentTab = it }
        )
    }
}

@Composable
private fun TabContent(viewModel: FridaViewModel, modifier: Modifier = Modifier) {
    // Conserva scroll y estado local de cada pestaña al cambiar entre ellas.
    val stateHolder = rememberSaveableStateHolder()
    AnimatedContent(
        targetState = viewModel.currentTab,
        // Fade-through: transición estándar de Material para navegación inferior.
        transitionSpec = { fadeIn(tween(180, delayMillis = 60)) togetherWith fadeOut(tween(90)) },
        label = "tab_content",
        modifier = modifier
    ) { tab ->
        stateHolder.SaveableStateProvider(tab) {
            when (tab) {
                FridaTabs.SERVER -> ServerScreen(viewModel)
                FridaTabs.REMOTE -> RemoteScreen(viewModel)
                FridaTabs.LOGS -> LogsScreen(viewModel)
                else -> SettingsScreen(viewModel)
            }
        }
    }
}
