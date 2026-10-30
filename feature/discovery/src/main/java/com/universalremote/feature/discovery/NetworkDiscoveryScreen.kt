package com.universalremote.feature.discovery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.universalremote.core.network.DiscoveryError
import com.universalremote.core.network.DiscoveryState
import com.universalremote.core.network.EpsonWebControlLauncher
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun NetworkDiscoveryScreen(
    viewModel: NetworkDiscoveryViewModel,
    onBack: () -> Unit,
    onDeviceConnected: (String) -> Unit
) {
    val state by viewModel.discoveryState.collectAsStateWithLifecycle()
    val manualForm by viewModel.manualForm.collectAsStateWithLifecycle()
    var showManualDialog by remember { mutableStateOf(false) }
    var showConsent by remember { mutableStateOf(false) }
    val diagnostic by viewModel.diagnostics.collectAsStateWithLifecycle()
    val context = LocalContext.current
    DisposableEffect(Unit) { onDispose { viewModel.cancelDiscovery() } }

    Page("Buscar dispositivos Wi-Fi", onBack) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Red local Wi-Fi", style = MaterialTheme.typography.titleMedium)
                    Text("Anuncios mDNS / SSDP. Epson puede requerir búsqueda local.", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = { viewModel.startDiscovery() }) {
                    Text("Escanear")
                }
            }
        }

        OutlinedButton(onClick = { showConsent = true }, enabled = state !is DiscoveryState.Searching) {
            Text("Buscar Epson en la subred local")
        }
        TextButton(onClick = { viewModel.diagnose() }) { Text("Diagnosticar conexión / Ver guía de red") }
        if (diagnostic.isNotEmpty()) Text(diagnostic, style = MaterialTheme.typography.bodySmall)
        if (manualForm.isTesting) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (!showManualDialog && manualForm.validationError != null) {
            Text(manualForm.validationError!!, color = MaterialTheme.colorScheme.error)
            OutlinedTextField(manualForm.password, viewModel::updatePassword, label = { Text("Contraseña PJLink") },
                visualTransformation = PasswordVisualTransformation())
            TextButton(onClick = { viewModel.retryPending(onDeviceConnected) }, enabled = !manualForm.isTesting) { Text("Reintentar conexión") }
        }

        when (val current = state) {
            is DiscoveryState.Searching -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Buscando TVs y proyectores en la red…", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = viewModel::cancelDiscovery) { Text("Cancelar búsqueda") }
                }
            }
            is DiscoveryState.Results -> {
                if (current.devices.isEmpty()) {
                    InfoCard(
                        "No encontramos proyectores Epson ni otros equipos en esta búsqueda",
                        "Reintenta o conecta mediante IP. Comprueba Red → Info. red-LAN con cables en el proyector. También puedes volver y elegir infrarrojo con un perfil documentado."
                    )
                } else {
                    Text("Dispositivos detectados (${current.devices.size})", style = MaterialTheme.typography.titleMedium)
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 320.dp)
                    ) {
                        items(current.devices) { device ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(device.name, style = MaterialTheme.typography.titleMedium)
                                        Text("${device.brand.id} · ${device.ipAddress}:${device.port ?: 80}", style = MaterialTheme.typography.bodySmall)
                                        Text("${device.model} · ${device.protocolId} · Pendiente de validar", style = MaterialTheme.typography.bodySmall)
                                    }
                                     Button(onClick = { viewModel.saveDiscoveredDevice(device, onDeviceConnected) }, enabled = !manualForm.isTesting) {
                                        Text("Conectar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is DiscoveryState.Failed -> {
                val msg = when (current.error) {
                    DiscoveryError.NETWORK_UNAVAILABLE -> "Conéctate a una red Wi-Fi para buscar dispositivos"
                    DiscoveryError.PERMISSION_REQUIRED -> "Permiso de red local requerido"
                    else -> "Error en el escaneo de red"
                }
                InfoCard("Error de red", msg)
            }
            is DiscoveryState.Cancelled -> {
                InfoCard("Búsqueda cancelada", "Puedes reiniciar la búsqueda cuando lo desees.")
            }
            is DiscoveryState.Idle -> {
                InfoCard("Búsqueda lista", "Presiona Escanear para buscar equipos en tu red.")
            }
        }

        Button(
            onClick = { showManualDialog = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        ) {
            Text("Conectar mediante IP")
        }
    }

    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            title = { Text("Agregar por Dirección IP") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Tipo y modelo")
                    viewModel.epsonModels.forEach { model ->
                        FilterChip(selected = manualForm.model == model, onClick = { viewModel.selectEpsonModel(model) }, label = { Text("Epson $model") })
                    }
                    Text("Protocolo")
                    listOf(ProtocolType.UNKNOWN, ProtocolType.PJLINK, ProtocolType.LG_WEBOS, ProtocolType.SAMSUNG_WS, ProtocolType.ANDROID_TV).forEach { protocol ->
                        FilterChip(selected = manualForm.protocolType == protocol, onClick = { viewModel.updateManualProtocol(protocol) },
                            label = { Text(if (protocol == ProtocolType.UNKNOWN) "Automático (modelo documentado)" else protocol.name) })
                    }
                    OutlinedTextField(
                        value = manualForm.name,
                        onValueChange = viewModel::updateManualName,
                        label = { Text("Nombre del equipo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualForm.ipAddress,
                        onValueChange = viewModel::updateManualIp,
                        label = { Text("Dirección IP (ej. 192.168.1.50)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = manualForm.portText,
                        onValueChange = viewModel::updateManualPort,
                        label = { Text("Puerto (ej. 3000 LG, 4352 PJLink, 8001 Samsung)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (manualForm.validationError != null) {
                        Text(manualForm.validationError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(manualForm.password, viewModel::updatePassword, label = { Text("Contraseña PJLink (si se solicita)") },
                        visualTransformation = PasswordVisualTransformation(), singleLine = true)
                    if (manualForm.brand == DeviceBrand.EPSON) {
                        TextButton(onClick = {
                            if (!EpsonWebControlLauncher.open(context, manualForm.ipAddress.trim())) viewModel.diagnose()
                        }) { Text("Abrir Epson Web Control") }
                        Text("Abre la IP en el navegador; autentícate allí. Abrir el navegador no confirma conexión.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveManualDevice { deviceId ->
                        showManualDialog = false
                        onDeviceConnected(deviceId)
                    }
                }, enabled = !manualForm.isTesting) {
                    Text(if (manualForm.isTesting) "Validando…" else "Validar y conectar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
    if (showConsent) AlertDialog(
        onDismissRequest = { showConsent = false }, title = { Text("Autorizar búsqueda local") },
        text = { Text("Se comprobará únicamente PJLink (TCP 4352) en la subred Wi-Fi activa: máximo 1022 direcciones, 16 conexiones simultáneas y 90 segundos. Continúa si tienes autorización para esta red. En redes mayores usa la IP del proyector.") },
        confirmButton = { TextButton(onClick = { showConsent = false; viewModel.searchLocal() }) { Text("Autorizar y buscar") } },
        dismissButton = { TextButton(onClick = { showConsent = false }) { Text("Cancelar") } }
    )
}
