package com.universalremote.feature.discovery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universalremote.core.designsystem.*
import com.universalremote.core.designsystem.R
import com.universalremote.core.model.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IrSetupScreen(
    viewModel: IrSetupViewModel,
    onBack: () -> Unit,
    onDeviceSaved: (String, DeviceType) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Page(
        if (state.brand == DeviceBrand.EPSON) "Configurar control Epson" else stringResource(R.string.test_screen_title),
        onBack
    ) {
        // Emitter status
        val irActive = state.irAvailability == IrAvailability.Available
        StatusIndicator(
            text = stringResource(if (irActive) R.string.emitter_detected else R.string.no_emitter_detected),
            available = irActive,
            icon = if (irActive) Icons.Default.SettingsRemote else Icons.Default.Warning
        )

        // Selected equipment info
        InfoCard(
            title = "${brandText(state.brand)} · ${deviceTypeText(state.deviceType)}",
            detail = stringResource(R.string.profile_label, state.selectedProfile?.family ?: stringResource(R.string.no_verified_codes))
        )

        // Pending brand info (unverified protocols)
        if (state.pendingBrandInfo != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.no_verified_codes), style = MaterialTheme.typography.titleMedium)
                    }
                    Text(state.pendingBrandInfo!!.reason, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        state.pendingBrandInfo!!.missingDetails,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.back))
            }
            return@Page
        }

        // Available profiles
        if (state.brand == DeviceBrand.EPSON) {
            Text("Selecciona el modelo Epson", style = MaterialTheme.typography.titleMedium)
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IrSetupViewModel.EPSON_MODELS.forEach { model ->
                    FilterChip(selected = state.epsonModel == model, onClick = { viewModel.selectEpsonModel(model) }, label = { Text(model) })
                }
            }
            if (state.epsonModel == "PowerLite 118") {
                InfoCard(
                    "Control original 219863500 · PowerLite 118",
                    "Perfil NECext 83 55 con los botones del control. Los códigos proceden de la base irdb y capturas comunitarias Flipper; el manual acredita la función de Menu, Esc, D-Pad, Enter, fuentes, volumen, Freeze, A/V Mute, Aspect y Color Mode, no sus tramas. Power, ID, Default, Split, Home y numéricos requieren confirmación física. El asistente prueba Menu, nunca Power."
                )
            }
            if (state.epsonModel == "EB-L260F") {
                InfoCard(
                    "EB-L260F · perfil IR propio",
                    "Perfil EPSON_EB_L260F a 38 kHz con 15 capturas propias. Las funciones adicionales usan capturas raw de referencia Epson 4650/G5650W y requieren prueba en tu proyector. HDMI y Auto son señales distintas. USB/LAN usa una tecla combinada, no entradas independientes confirmadas. 0/All, ID e Información siguen sin código verificado. Prueba Menu antes de guardar."
                )
            }
        }
        if (state.availableProfiles.isNotEmpty()) {
            Text("Selecciona el perfil IR", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.availableProfiles.forEach { profile ->
                    val isSelected = state.selectedProfile?.id == profile.id
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.selectProfile(profile) }
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(profile.family, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Protocolo: ${profile.protocol} · Frecuencia: ${profile.carrierHz} Hz",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val statusText = when (state.validationStatus) {
                                IrValidationStatus.CONFIRMED_BY_USER -> stringResource(R.string.status_confirmed)
                                IrValidationStatus.INCOMPATIBLE -> stringResource(R.string.status_incompatible)
                                IrValidationStatus.UNVERIFIED -> stringResource(R.string.status_unverified)
                            }
                            Text(
                                "Estado: $statusText",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.validationStatus == IrValidationStatus.CONFIRMED_BY_USER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            val profile = state.selectedProfile
            if (profile != null) {
                // Testing section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.test_screen_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Comando de prueba: ${profile.testCommand.name} · ${profile.carrierHz} Hz",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (state.brand == DeviceBrand.EPSON) {
                            Text(
                                if (profile.testNote == null) {
                                    "Apunta tu celular al receptor IR del proyector, a menos de 5 metros y sin obstáculos. Pulsa el botón y comprueba si aparece el menú en pantalla. No se probará Power."
                                } else {
                                    "Apunta tu celular al receptor IR del proyector, a menos de 5 metros y sin obstáculos. No se probará Power."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (profile.testNote != null) {
                            Text(
                                profile.testNote!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Button(
                            onClick = viewModel::sendTestCommand,
                            enabled = !state.isTransmitting && irActive,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        ) {
                            if (state.isTransmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(stringResource(R.string.transmitting))
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.send_test_command, profile.testCommand.name))
                            }
                        }
                    }
                }

                // Transmission local result message
                AnimatedVisibility(visible = state.lastTransmissionMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.transmissionSuccess) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.lastTransmissionMessage ?: "",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.transmissionSuccess) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // Incompatible result flow
                AnimatedVisibility(visible = state.testStep == IrTestStep.INCOMPATIBLE_RESULT) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "El equipo no respondió a este perfil.",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(
                                onClick = viewModel::retryAnotherProfile,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.try_another_profile))
                            }
                        }
                    }
                }
            }
        }

        // Modal: did the projector respond?
        if (state.testStep == IrTestStep.TESTING_COMMAND && state.transmissionSuccess) {
            AlertDialog(
                onDismissRequest = { viewModel.retryAnotherProfile() },
                icon = { Icon(Icons.Default.HelpOutline, null) },
                title = { Text(stringResource(R.string.did_device_respond)) },
                text = {
                    Text(
                        "La app solo confirma que el emisor aceptó la trama. " +
                            "Mira el proyector: si apareció el menú u otro cambio en pantalla, responde Sí."
                    )
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { viewModel.confirmResponse(true) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(R.string.yes_it_worked))
                        }
                        OutlinedButton(
                            onClick = { viewModel.confirmResponse(false) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(R.string.no_it_failed))
                        }
                        TextButton(
                            onClick = viewModel::retryAnotherProfile,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(R.string.try_another_profile))
                        }
                    }
                },
            )
        }

        // Modal: name and save the working control.
        if (state.testStep == IrTestStep.CONFIRMED_SUCCESS) {
            AlertDialog(
                onDismissRequest = { viewModel.retryAnotherProfile() },
                icon = { Icon(Icons.Default.Save, null) },
                title = { Text(stringResource(R.string.save_device_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = state.customDeviceName,
                            onValueChange = viewModel::setDeviceName,
                            label = { Text(stringResource(R.string.device_name_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = state.customRoomName,
                            onValueChange = viewModel::setRoomName,
                            label = { Text(stringResource(R.string.room_name_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.saveDevice { id -> onDeviceSaved(id, state.deviceType) } }) {
                        Text(stringResource(R.string.save_and_open))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.retryAnotherProfile() }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
    }
}
