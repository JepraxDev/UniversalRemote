package com.universalremote.feature.remote

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universalremote.core.designsystem.*
import com.universalremote.core.designsystem.R
import com.universalremote.core.model.*
import com.universalremote.protocol.api.ProjectorInfo
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri

@Composable
fun RemoteScreen(
    viewModel: RemoteViewModel,
    onBack: () -> Unit,
    onOpenCustomize: (() -> Unit)? = null,
    onOpenQuickActions: (() -> Unit)? = null,
    onOpenLocations: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RemoteScreenContent(
        type = state.deviceType,
        onBack = onBack,
        capabilities = state.capabilities,
        deviceName = state.deviceName,
        connectionType = state.connectionType,
        remoteMode = state.remoteMode,
        locationTag = state.locationTag,
        isDemo = state.isDemo,
        isTransmitting = state.isTransmitting,
        lastResultText = state.lastResultText,
        isError = state.isError,
        pendingConfirmationCommand = state.pendingConfirmationCommand,
        onCommand = if (!state.isDemo) { cmd -> viewModel.sendCommand(cmd) } else null,
        onConfirmPending = viewModel::confirmPendingCommand,
        onDismissConfirmation = viewModel::dismissConfirmation,
        onToggleConnectionType = viewModel::toggleConnectionType,
        onSetRemoteMode = viewModel::setRemoteMode,
        onOpenCustomize = onOpenCustomize,
        onOpenQuickActions = onOpenQuickActions,
        onOpenLocations = onOpenLocations,
        connectionMessage = state.connectionMessage,
        networkConnected = state.networkConnected,
        webControlIp = state.webControlIp,
        projectorInfo = state.projectorInfo,
        onRefresh = viewModel::refreshProjectorInfo,
        onReconnect = { viewModel.loadDevice() },
        onInput = viewModel::selectProjectorInput
    )
}

/** Backward-compatible overload for design preview or standalone usage. */
@Composable
fun RemoteScreen(type: DeviceType, onBack: () -> Unit, capabilities: DeviceCapabilities? = null) {
    RemoteScreenContent(
        type = type,
        onBack = onBack,
        capabilities = capabilities,
        deviceName = null,
        isDemo = true,
        isTransmitting = false,
        lastResultText = null,
        isError = false,
        pendingConfirmationCommand = null,
        onCommand = null,
        onConfirmPending = {},
        onDismissConfirmation = {},
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RemoteScreenContent(
    type: DeviceType,
    onBack: () -> Unit,
    capabilities: DeviceCapabilities?,
    deviceName: String?,
    connectionType: ConnectionType = ConnectionType.WIFI,
    remoteMode: RemoteMode = RemoteMode.BASIC,
    locationTag: String? = null,
    isDemo: Boolean,
    isTransmitting: Boolean,
    lastResultText: String?,
    isError: Boolean,
    pendingConfirmationCommand: RemoteCommand?,
    onCommand: ((RemoteCommand) -> Unit)?,
    onConfirmPending: () -> Unit,
    onDismissConfirmation: () -> Unit,
    onToggleConnectionType: (() -> Unit)? = null,
    onSetRemoteMode: ((RemoteMode) -> Unit)? = null,
    onOpenCustomize: (() -> Unit)? = null,
    onOpenQuickActions: (() -> Unit)? = null,
    onOpenLocations: (() -> Unit)? = null,
    connectionMessage: String = "Sin comprobar",
    networkConnected: Boolean = false,
    webControlIp: String? = null,
    projectorInfo: ProjectorInfo? = null,
    onRefresh: (() -> Unit)? = null,
    onReconnect: (() -> Unit)? = null,
    onInput: ((String) -> Unit)? = null
) {
    fun supports(command: RemoteCommand) = capabilities == null || command in capabilities.supportedCommands
    val title = deviceName ?: stringResource(if (type == DeviceType.TV) R.string.tv_remote else R.string.projector_remote)
    val buttonEnabled = !isTransmitting && !isDemo && onCommand != null
    val context = LocalContext.current

    if (pendingConfirmationCommand != null) {
        ConfirmationDialog(
            title = stringResource(R.string.confirm_power_title),
            detail = stringResource(R.string.confirm_power_message),
            onConfirm = onConfirmPending,
            onDismiss = onDismissConfirmation,
            confirmLabel = stringResource(R.string.confirm_send)
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            RemoteHeader(
                title = title,
                subtitle = if (locationTag.isNullOrBlank()) {
                    if (connectionType == ConnectionType.WIFI) "Control por Wi-Fi" else "Control por Infrarrojo"
                } else {
                    "$locationTag · ${if (connectionType == ConnectionType.WIFI) "Wi-Fi" else "IR"}"
                },
                isFavorite = false,
                onBack = onBack,
                onOpenCustomize = onOpenCustomize,
                onOpenQuickActions = onOpenQuickActions,
                onOpenLocations = onOpenLocations
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Mode Selector Bar (Modo Básico vs Modo Completo)
                if (onSetRemoteMode != null) {
                    RemoteModeSelector(
                        selectedMode = remoteMode,
                        onSelectMode = onSetRemoteMode
                    )
                }

                // Connection Status & Transport Selector Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusIndicator(
                        text = if (type == DeviceType.PROJECTOR && connectionType == ConnectionType.WIFI) connectionMessage else if (isDemo) stringResource(R.string.demo_short) else "Configurado",
                        available = if (type == DeviceType.PROJECTOR && connectionType == ConnectionType.WIFI) networkConnected else !isDemo,
                        icon = if (connectionType == ConnectionType.WIFI) Icons.Default.Wifi else Icons.Default.SettingsRemote
                    )
                    if (onToggleConnectionType != null) {
                        AssistChip(
                            onClick = onToggleConnectionType,
                            label = {
                                Text("Método: ${if (connectionType == ConnectionType.WIFI) "Wi-Fi" else "IR"}")
                            },
                            leadingIcon = {
                                Icon(
                                    if (connectionType == ConnectionType.WIFI) Icons.Default.Wifi else Icons.Default.SettingsRemote,
                                    null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }

                if (type == DeviceType.PROJECTOR && connectionType == ConnectionType.WIFI) {
                    onReconnect?.let { TextButton(onClick = it) { Text("Reconectar / Diagnosticar conexión") } }
                    webControlIp?.let { ip ->
                        Text("IP: $ip")
                        Button(onClick = {
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://$ip/"))) }
                            catch (_: android.content.ActivityNotFoundException) { }
                        }) { Text("Abrir Epson Web Control") }
                        Text("Menú, navegación, fuentes y funciones adicionales disponibles en la interfaz oficial del proyector.", style = MaterialTheme.typography.bodySmall)
                    }
                    onRefresh?.let { TextButton(onClick = it) { Text("Consultar información, lámpara y errores") } }
                    projectorInfo?.let { info ->
                        Text("${info.manufacturer} ${info.model} · ${info.name}\nPJLink clase ${info.pjClass}\nEstado de alimentación: ${info.power} (0 espera, 1 encendido, 2 enfriando, 3 calentando)\nErrores: ${info.errors} (ventilador/lámpara/temperatura/tapa/filtro/otros; 0 normal, 1 aviso, 2 error)\nLámparas [horas, encendida]: ${info.lamps}", style = MaterialTheme.typography.bodySmall)
                        Text("Entradas anunciadas por el proyector")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            info.inputs.forEach { input ->
                                val label = when (input.firstOrNull()) { '1' -> "RGB"; '2' -> "Video"; '3' -> "Digital"; '4' -> "Almacenamiento"; '5' -> "Red"; else -> "Entrada" }
                                OutlinedButton(onClick = { onInput?.invoke(input) }, enabled = !isTransmitting && networkConnected) { Text("${info.inputNames[input] ?: label} ($input)") }
                            }
                        }
                    }
                }

                // Feedback / Error Banner
                AnimatedVisibility(visible = lastResultText != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = lastResultText ?: "",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                if (type == DeviceType.TV) {
                    // ==========================================
                    // SMART TV & ANDROID TV REMOTE INTERFACE
                    // ==========================================

                    // SECCIÓN B — CONTROLES PRINCIPALES
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RemoteButton(
                            Icons.Default.Home, stringResource(R.string.remote_home),
                            supported = supports(RemoteCommand.HOME),
                            enabled = buttonEnabled,
                            onClick = onCommand?.let { { it(RemoteCommand.HOME) } }
                        )

                        PowerButton(
                            enabled = buttonEnabled,
                            supported = supports(RemoteCommand.POWER),
                            onClick = onCommand?.let { { it(RemoteCommand.POWER) } }
                        )

                        RemoteButton(
                            Icons.AutoMirrored.Filled.Input, stringResource(R.string.input),
                            supported = supports(RemoteCommand.INPUT),
                            enabled = buttonEnabled,
                            onClick = onCommand?.let { { it(RemoteCommand.INPUT) } }
                        )
                    }

                    // SECCIÓN C — PANEL DIRECCIONAL & SECCIÓN D — VOLUMEN Y CANALES
                    BoxWithConstraints {
                        if (maxWidth >= 600.dp) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(36.dp)
                            ) {
                                VerticalRocker(
                                    title = stringResource(R.string.volume),
                                    upIcon = Icons.AutoMirrored.Filled.VolumeUp,
                                    upLabel = stringResource(R.string.volume_up),
                                    downIcon = Icons.AutoMirrored.Filled.VolumeDown,
                                    downLabel = stringResource(R.string.volume_down),
                                    upSupported = supports(RemoteCommand.VOLUME_UP),
                                    downSupported = supports(RemoteCommand.VOLUME_DOWN),
                                    enabled = buttonEnabled,
                                    onUp = onCommand?.let { { it(RemoteCommand.VOLUME_UP) } },
                                    onDown = onCommand?.let { { it(RemoteCommand.VOLUME_DOWN) } },
                                    centerIcon = Icons.AutoMirrored.Filled.VolumeOff,
                                    centerLabel = stringResource(R.string.mute),
                                    centerSupported = supports(RemoteCommand.MUTE),
                                    onCenter = onCommand?.let { { it(RemoteCommand.MUTE) } }
                                )

                                CircularDPad(capabilities, enabled = buttonEnabled, onCommand = onCommand)

                                VerticalRocker(
                                    title = stringResource(R.string.channels),
                                    upIcon = Icons.Default.Add,
                                    upLabel = stringResource(R.string.channel_up),
                                    downIcon = Icons.Default.Remove,
                                    downLabel = stringResource(R.string.channel_down),
                                    upSupported = supports(RemoteCommand.CHANNEL_UP),
                                    downSupported = supports(RemoteCommand.CHANNEL_DOWN),
                                    enabled = buttonEnabled,
                                    onUp = onCommand?.let { { it(RemoteCommand.CHANNEL_UP) } },
                                    onDown = onCommand?.let { { it(RemoteCommand.CHANNEL_DOWN) } }
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                CircularDPad(capabilities, enabled = buttonEnabled, onCommand = onCommand)

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    VerticalRocker(
                                        title = stringResource(R.string.volume),
                                        upIcon = Icons.AutoMirrored.Filled.VolumeUp,
                                        upLabel = stringResource(R.string.volume_up),
                                        downIcon = Icons.AutoMirrored.Filled.VolumeDown,
                                        downLabel = stringResource(R.string.volume_down),
                                        upSupported = supports(RemoteCommand.VOLUME_UP),
                                        downSupported = supports(RemoteCommand.VOLUME_DOWN),
                                        enabled = buttonEnabled,
                                        onUp = onCommand?.let { { it(RemoteCommand.VOLUME_UP) } },
                                        onDown = onCommand?.let { { it(RemoteCommand.VOLUME_DOWN) } },
                                        centerIcon = Icons.AutoMirrored.Filled.VolumeOff,
                                        centerLabel = stringResource(R.string.mute),
                                        centerSupported = supports(RemoteCommand.MUTE),
                                        onCenter = onCommand?.let { { it(RemoteCommand.MUTE) } }
                                    )

                                    VerticalRocker(
                                        title = stringResource(R.string.channels),
                                        upIcon = Icons.Default.Add,
                                        upLabel = stringResource(R.string.channel_up),
                                        downIcon = Icons.Default.Remove,
                                        downLabel = stringResource(R.string.channel_down),
                                        upSupported = supports(RemoteCommand.CHANNEL_UP),
                                        downSupported = supports(RemoteCommand.CHANNEL_DOWN),
                                        enabled = buttonEnabled,
                                        onUp = onCommand?.let { { it(RemoteCommand.CHANNEL_UP) } },
                                        onDown = onCommand?.let { { it(RemoteCommand.CHANNEL_DOWN) } }
                                    )
                                }
                            }
                        }
                    }

                    // Navegación secundaria
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RemoteButton(
                            Icons.Default.Menu, stringResource(R.string.menu),
                            supported = supports(RemoteCommand.MENU),
                            enabled = buttonEnabled,
                            onClick = onCommand?.let { { it(RemoteCommand.MENU) } }
                        )
                        RemoteButton(
                            Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back),
                            supported = supports(RemoteCommand.BACK),
                            enabled = buttonEnabled,
                            onClick = onCommand?.let { { it(RemoteCommand.BACK) } }
                        )
                        RemoteButton(
                            Icons.Default.Home, stringResource(R.string.remote_home),
                            supported = supports(RemoteCommand.HOME),
                            enabled = buttonEnabled,
                            onClick = onCommand?.let { { it(RemoteCommand.HOME) } }
                        )
                        RemoteButton(
                            Icons.AutoMirrored.Filled.Input, stringResource(R.string.input),
                            supported = supports(RemoteCommand.INPUT),
                            enabled = buttonEnabled,
                            onClick = onCommand?.let { { it(RemoteCommand.INPUT) } }
                        )
                    }

                    // SECCIÓN E — TECLADO NUMÉRICO (Visible en MODO COMPLETO y BÁSICO)
                    RemoteSection(title = "Teclado numérico") {
                        NumericKeypad(
                            capabilities = capabilities,
                            enabled = buttonEnabled,
                            onCommand = onCommand
                        )
                    }

                    // SECCIÓN F — CONTROLES MULTIMEDIA (Visible en MODO COMPLETO)
                    if (remoteMode == RemoteMode.FULL) {
                        RemoteSection(title = "Controles multimedia") {
                            MediaControlPanel(
                                capabilities = capabilities,
                                enabled = buttonEnabled,
                                onCommand = onCommand
                            )
                        }

                        // SECCIÓN G — FUNCIONES ADICIONALES
                        RemoteSection(title = "Funciones adicionales") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    RemoteCommand.INFO to "Información",
                                    RemoteCommand.GUIDE to "Guía TV",
                                    RemoteCommand.SUBTITLE to "Subtítulos",
                                    RemoteCommand.SETTINGS_KEY to "Ajustes",
                                    RemoteCommand.KEYBOARD to "Teclado Texto"
                                ).forEach { (cmd, label) ->
                                    val isSup = supports(cmd)
                                    OutlinedButton(
                                        onClick = { onCommand?.invoke(cmd) },
                                        enabled = buttonEnabled && isSup,
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Text(if (isSup) label else "$label (N/A)")
                                    }
                                }
                            }
                        }

                        // SECCIÓN H — APPS Y ACCESOS
                        RemoteSection(title = "Aplicaciones y accesos rápidos") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("YouTube", "Netflix", "Prime Video", "Disney+").forEach { appName ->
                                    OutlinedButton(
                                        onClick = { onCommand?.invoke(RemoteCommand.APPS) },
                                        enabled = buttonEnabled && supports(RemoteCommand.APPS),
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Text(if (supports(RemoteCommand.APPS)) appName else "$appName (N/A)")
                                    }
                                }
                            }
                        }
                    }

                } else {
                    // ==========================================
                    // PROJECTOR REMOTE INTERFACE
                    // ==========================================

                    // SECCIÓN B — CONTROLES PRINCIPALES DE PROYECTOR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PowerButton(
                            enabled = buttonEnabled,
                            supported = supports(RemoteCommand.POWER),
                            onClick = onCommand?.let { { it(RemoteCommand.POWER) } }
                        )

                        RemoteButton(
                            Icons.AutoMirrored.Filled.Input, stringResource(R.string.input),
                            supported = supports(RemoteCommand.INPUT),
                            enabled = buttonEnabled,
                            onClick = onCommand?.let { { it(RemoteCommand.INPUT) } }
                        )

                        if (supports(RemoteCommand.AV_MUTE)) {
                            OutlinedButton(
                                onClick = { onCommand?.invoke(RemoteCommand.AV_MUTE) },
                                enabled = buttonEnabled,
                                modifier = Modifier.heightIn(min = 52.dp)
                            ) {
                                Text("A/V Mute")
                            }
                        }

                        if (supports(RemoteCommand.FREEZE)) {
                            OutlinedButton(
                                onClick = { onCommand?.invoke(RemoteCommand.FREEZE) },
                                enabled = buttonEnabled,
                                modifier = Modifier.heightIn(min = 52.dp)
                            ) {
                                Text("Freeze")
                            }
                        }
                    }

                    // SECCIÓN C — D-PAD CIRCULAR
                    CircularDPad(capabilities, enabled = buttonEnabled, onCommand = onCommand)

                    // SECCIÓN D — VOLUMEN DE PROYECTOR
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VerticalRocker(
                            title = stringResource(R.string.volume),
                            upIcon = Icons.AutoMirrored.Filled.VolumeUp,
                            upLabel = stringResource(R.string.volume_up),
                            downIcon = Icons.AutoMirrored.Filled.VolumeDown,
                            downLabel = stringResource(R.string.volume_down),
                            upSupported = supports(RemoteCommand.VOLUME_UP),
                            downSupported = supports(RemoteCommand.VOLUME_DOWN),
                            enabled = buttonEnabled,
                            onUp = onCommand?.let { { it(RemoteCommand.VOLUME_UP) } },
                            onDown = onCommand?.let { { it(RemoteCommand.VOLUME_DOWN) } },
                            centerIcon = Icons.AutoMirrored.Filled.VolumeOff,
                            centerLabel = stringResource(R.string.mute),
                            centerSupported = supports(RemoteCommand.MUTE),
                            onCenter = onCommand?.let { { it(RemoteCommand.MUTE) } }
                        )
                    }

                    // SECCIÓN E — FUENTES DE ENTRADA
                    RemoteSection(title = "Fuentes de entrada") {
                        ProjectorInputGrid(
                            capabilities = capabilities,
                            enabled = buttonEnabled,
                            onCommand = onCommand
                        )
                    }

                    // SECCIÓN F — IMAGEN Y PROYECCIÓN (Visible en MODO COMPLETO)
                    if (remoteMode == RemoteMode.FULL) {
                        RemoteSection(title = "Imagen y proyección") {
                            ProjectorImageControls(
                                capabilities = capabilities,
                                enabled = buttonEnabled,
                                onCommand = onCommand
                            )
                        }

                        // SECCIÓN G — TECLADO NUMÉRICO PROYECTOR
                        RemoteSection(title = "Teclado numérico") {
                            NumericKeypad(
                                capabilities = capabilities,
                                enabled = buttonEnabled,
                                onCommand = onCommand
                            )
                        }

                        // SECCIÓN H — INFORMACIÓN DEL EQUIPO
                        RemoteSection(title = "Información del equipo") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Protocolo: ${if (connectionType == ConnectionType.WIFI) "PJLink TCP (:4352)" else "Infrarrojo ESC/VP21"}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Estado de Lámpara: Normal / OK", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Text(
                    stringResource(R.string.capabilities_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
