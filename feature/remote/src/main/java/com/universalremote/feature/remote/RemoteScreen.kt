package com.universalremote.feature.remote

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universalremote.core.designsystem.*
import com.universalremote.core.designsystem.R
import com.universalremote.core.model.*

@Composable
fun RemoteScreen(viewModel: RemoteViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RemoteScreen(type = state.deviceType, viewModel = viewModel, onBack = onBack)
}

@Composable
fun RemoteScreen(type: DeviceType, viewModel: RemoteViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RemoteScreenContent(
        type = if (state.deviceId != null) state.deviceType else type,
        onBack = onBack,
        capabilities = state.capabilities,
        deviceName = state.deviceName,
        connectionType = state.connectionType,
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
) {
    fun supports(command: RemoteCommand) = capabilities == null || command in capabilities.supportedCommands
    val title = deviceName ?: stringResource(if (type == DeviceType.TV) R.string.tv_remote else R.string.projector_remote)
    val buttonEnabled = !isTransmitting && !isDemo && onCommand != null

    if (pendingConfirmationCommand != null) {
        ConfirmationDialog(
            title = stringResource(R.string.confirm_power_title),
            detail = stringResource(R.string.confirm_power_message),
            onConfirm = onConfirmPending,
            onDismiss = onDismissConfirmation,
            confirmLabel = stringResource(R.string.confirm_send)
        )
    }

    Page(title, onBack) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusIndicator(
                text = if (isDemo) stringResource(R.string.demo_short) else stringResource(R.string.status_confirmed),
                available = !isDemo,
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

        if (locationTag != null && locationTag.isNotBlank()) {
            Text("Ubicación: $locationTag", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

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

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            RemoteButton(
                icon = Icons.Default.PowerSettingsNew,
                label = stringResource(R.string.power),
                accent = true,
                supported = supports(RemoteCommand.POWER),
                enabled = buttonEnabled,
                onClick = onCommand?.let { { it(RemoteCommand.POWER) } }
            )
            BoxWithConstraints {
                if (maxWidth >= 600.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                        Rocker(
                            R.string.volume, Icons.AutoMirrored.Filled.VolumeUp, R.string.volume_up, Icons.AutoMirrored.Filled.VolumeDown, R.string.volume_down,
                            supports(RemoteCommand.VOLUME_UP), supports(RemoteCommand.VOLUME_DOWN),
                            enabled = buttonEnabled,
                            onUp = onCommand?.let { { it(RemoteCommand.VOLUME_UP) } },
                            onDown = onCommand?.let { { it(RemoteCommand.VOLUME_DOWN) } }
                        )
                        RemoteDPad(capabilities, onCommand = onCommand, enabled = buttonEnabled)
                        if (type == DeviceType.TV) {
                            Rocker(
                                R.string.channels, Icons.Default.Add, R.string.channel_up, Icons.Default.Remove, R.string.channel_down,
                                supports(RemoteCommand.CHANNEL_UP), supports(RemoteCommand.CHANNEL_DOWN),
                                enabled = buttonEnabled,
                                onUp = onCommand?.let { { it(RemoteCommand.CHANNEL_UP) } },
                                onDown = onCommand?.let { { it(RemoteCommand.CHANNEL_DOWN) } }
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        RemoteDPad(capabilities, onCommand = onCommand, enabled = buttonEnabled)
                        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                            Rocker(
                                R.string.volume, Icons.AutoMirrored.Filled.VolumeUp, R.string.volume_up, Icons.AutoMirrored.Filled.VolumeDown, R.string.volume_down,
                                supports(RemoteCommand.VOLUME_UP), supports(RemoteCommand.VOLUME_DOWN),
                                enabled = buttonEnabled,
                                onUp = onCommand?.let { { it(RemoteCommand.VOLUME_UP) } },
                                onDown = onCommand?.let { { it(RemoteCommand.VOLUME_DOWN) } }
                            )
                            if (type == DeviceType.TV) {
                                Rocker(
                                    R.string.channels, Icons.Default.Add, R.string.channel_up, Icons.Default.Remove, R.string.channel_down,
                                    supports(RemoteCommand.CHANNEL_UP), supports(RemoteCommand.CHANNEL_DOWN),
                                    enabled = buttonEnabled,
                                    onUp = onCommand?.let { { it(RemoteCommand.CHANNEL_UP) } },
                                    onDown = onCommand?.let { { it(RemoteCommand.CHANNEL_DOWN) } }
                                )
                            }
                        }
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                if (type == DeviceType.TV) {
                    RemoteButton(
                        Icons.Default.Home, stringResource(R.string.remote_home),
                        supported = supports(RemoteCommand.HOME),
                        enabled = buttonEnabled,
                        onClick = onCommand?.let { { it(RemoteCommand.HOME) } }
                    )
                    RemoteButton(
                        Icons.AutoMirrored.Filled.VolumeOff, stringResource(R.string.mute),
                        supported = supports(RemoteCommand.MUTE),
                        enabled = buttonEnabled,
                        onClick = onCommand?.let { { it(RemoteCommand.MUTE) } }
                    )
                }
                RemoteButton(
                    Icons.AutoMirrored.Filled.Input, stringResource(R.string.input),
                    supported = supports(RemoteCommand.INPUT),
                    enabled = buttonEnabled,
                    onClick = onCommand?.let { { it(RemoteCommand.INPUT) } }
                )
            }
        }
        if (type == DeviceType.PROJECTOR) {
            Text(stringResource(R.string.sources), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    RemoteCommand.HDMI to R.string.hdmi,
                    RemoteCommand.VGA to R.string.vga,
                    RemoteCommand.LAN to R.string.lan
                ).forEach { (command, label) ->
                    if (supports(command)) {
                        SourceButton(
                            label = label,
                            enabled = buttonEnabled,
                            onClick = onCommand?.let { { it(command) } }
                        )
                    }
                }
            }
            Text(stringResource(R.string.extra_controls), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (supports(RemoteCommand.FREEZE)) {
                    SourceButton(
                        label = R.string.freeze,
                        enabled = buttonEnabled,
                        onClick = onCommand?.let { { it(RemoteCommand.FREEZE) } }
                    )
                }
                if (supports(RemoteCommand.AV_MUTE)) {
                    SourceButton(
                        label = R.string.av_mute,
                        enabled = buttonEnabled,
                        onClick = onCommand?.let { { it(RemoteCommand.AV_MUTE) } }
                    )
                }
            }
        }
        Text(stringResource(R.string.capabilities_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Rocker(
    title: Int,
    upIcon: ImageVector,
    upLabel: Int,
    downIcon: ImageVector,
    downLabel: Int,
    upSupported: Boolean,
    downSupported: Boolean,
    enabled: Boolean,
    onUp: (() -> Unit)?,
    onDown: (() -> Unit)?,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.labelLarge)
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RemoteButton(upIcon, stringResource(upLabel), supported = upSupported, enabled = enabled, onClick = onUp)
                HorizontalDivider(Modifier.width(56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                RemoteButton(downIcon, stringResource(downLabel), supported = downSupported, enabled = enabled, onClick = onDown)
            }
        }
    }
}

@Composable
private fun SourceButton(
    label: Int,
    enabled: Boolean,
    onClick: (() -> Unit)? = null,
) {
    OutlinedButton(
        onClick = { onClick?.invoke() },
        enabled = onClick != null && enabled,
        modifier = Modifier.heightIn(min = 52.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Text(stringResource(label))
    }
}
