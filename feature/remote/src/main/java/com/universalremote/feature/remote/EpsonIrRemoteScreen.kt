package com.universalremote.feature.remote

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universalremote.core.model.RemoteCommand

/**
 * Full Epson PowerLite 118 remote. Compose only renders state and forwards
 * command ids; transmission happens in EpsonIrRemoteViewModel.
 */
@Composable
fun EpsonIrRemoteScreen(
    viewModel: EpsonIrRemoteViewModel,
    onBack: () -> Unit,
    onOpenWizard: () -> Unit,
    onOpenCustomize: () -> Unit = {},
    onOpenQuickActions: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var favorite by rememberSaveable { mutableStateOf(false) }

    if (state.pendingConfirmation != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissPendingCommand,
            icon = { Icon(Icons.Default.PowerSettingsNew, null, tint = EpsonRemoteColors.Power) },
            title = { Text("Enviar ${commandLabel(state.pendingConfirmation!!)}") },
            text = {
                Text(
                    "Este comando puede cambiar el estado del proyector. Se enviará un solo código IR por confirmación.",
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmPendingCommand,
                    colors = ButtonDefaults.buttonColors(containerColor = EpsonRemoteColors.Power),
                ) { Text("Enviar una vez") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissPendingCommand) { Text("Cancelar") }
            },
        )
    }

    CompositionLocalProvider(LocalEpsonSupportedCommands provides state.supportedCommands) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EpsonRemoteColors.Navy),
    ) {
        EpsonRemoteHeader(
            deviceName = state.deviceName,
            profileFamily = state.profileFamily,
            irAvailable = state.irAvailable,
            isFavorite = favorite,
            onToggleFavorite = { favorite = !favorite },
            onBack = onBack,
            onOpenWizard = onOpenWizard,
            onOpenCustomize = onOpenCustomize,
            onOpenQuickActions = onOpenQuickActions,
        )
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            val wide = maxWidth >= 760.dp
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 980.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 34.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    EpsonIrStatus(
                        state = state,
                        onConfirm = viewModel::confirmLastCommand,
                    )
                }
                item {
                    EpsonRemoteSection(title = "Encendido y entradas") {
                        if (wide) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                EpsonPowerButton(
                                    onPower = { viewModel.sendCommand(RemoteCommand.POWER) },
                                    enabled = state.canTransmit,
                                )
                                EpsonSourcePanel(
                                    enabled = state.canTransmit,
                                    onCommand = viewModel::sendCommand,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                EpsonPowerButton(
                                    onPower = { viewModel.sendCommand(RemoteCommand.POWER) },
                                    enabled = state.canTransmit,
                                )
                                EpsonSourcePanel(
                                    enabled = state.canTransmit,
                                    onCommand = viewModel::sendCommand,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        Text(
                            "Power usa alternancia (un solo código por pulsación). No se envía un segundo comando automático.",
                            color = EpsonRemoteColors.TextMuted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp,
                        )
                    }
                }
                item {
                    EpsonRemoteSection(
                        title = "Teclado numérico",
                        subtitle = "Num, ID, Auto y 0/All reproducen el comportamiento del control original.",
                    ) {
                        EpsonNumericKeypad(
                            enabled = state.canTransmit,
                            onCommand = viewModel::sendCommand,
                        )
                    }
                }
                item {
                    EpsonRemoteSection(title = "Navegación") {
                        if (wide) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(22.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                EpsonCircularDPad(
                                    enabled = state.canTransmit,
                                    onCommand = viewModel::sendCommand,
                                )
                                EpsonNavigationKeys(
                                    enabled = state.canTransmit,
                                    onCommand = viewModel::sendCommand,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                EpsonCircularDPad(
                                    enabled = state.canTransmit,
                                    onCommand = viewModel::sendCommand,
                                )
                                EpsonNavigationKeys(
                                    enabled = state.canTransmit,
                                    onCommand = viewModel::sendCommand,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        Text(
                            "El centro del D-Pad ejecuta Enter.",
                            color = EpsonRemoteColors.TextMuted,
                            fontSize = 10.sp,
                        )
                    }
                }
                item {
                    EpsonRemoteSection(title = "Control de imagen") {
                        EpsonImageControls(
                            enabled = state.canTransmit,
                            onCommand = viewModel::sendCommand,
                        )
                    }
                }
                item {
                    EpsonRemoteSection(title = "Audio") {
                        EpsonAudioControls(
                            enabled = state.canTransmit,
                            onCommand = viewModel::sendCommand,
                        )
                    }
                }
                item {
                    EpsonRemoteSection(title = "Funciones") {
                        EpsonFunctionPanel(
                            enabled = state.canTransmit,
                            onCommand = viewModel::sendCommand,
                        )
                    }
                }
                item {
                    EpsonIrTestWizard(
                        irAvailable = state.irAvailable,
                        profileLoaded = state.profileLoaded,
                        onOpenWizard = onOpenWizard,
                        deviceName = state.profileFamily ?: state.deviceName,
                        testCommandLabel = when (state.testCommand) {
                            RemoteCommand.MENU -> "Menu"
                            RemoteCommand.INPUT -> "Source Search"
                            null -> "Menu"
                            else -> state.testCommand!!.name
                        },
                        testNote = state.testNote,
                    )
                }
                item {
                    Text(
                        buildString {
                            append("Perfil ${state.profileId ?: "sin cargar"}")
                            state.profileFamily?.let { append(" · $it") }
                            state.profileProtocol?.let { append(" · $it") }
                            append(" · 38 kHz. Capturas comunitarias y funciones documentadas; la recepción física se confirma con una prueba real. ")
                            append("Los botones sin código verificado aparecen deshabilitados.")
                        },
                        color = EpsonRemoteColors.TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }
    }
}

private fun commandLabel(command: RemoteCommand): String = when (command) {
    RemoteCommand.POWER -> "Power"
    RemoteCommand.POWER_ON -> "Encendido"
    RemoteCommand.POWER_OFF -> "Standby"
    RemoteCommand.AV_MUTE -> "A/V Mute"
    else -> command.name
}
