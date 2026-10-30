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

@Composable
fun NetworkDiscoveryScreen(
    viewModel: NetworkDiscoveryViewModel,
    onBack: () -> Unit,
    onDeviceConnected: (String) -> Unit
) {
    val state by viewModel.discoveryState.collectAsStateWithLifecycle()
    val manualForm by viewModel.manualForm.collectAsStateWithLifecycle()
    var showManualDialog by remember { mutableStateOf(false) }

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
                    Text("Búsqueda activa vía mDNS / SSDP", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = { viewModel.startDiscovery() }) {
                    Text("Escanear")
                }
            }
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
                }
            }
            is DiscoveryState.Results -> {
                if (current.devices.isEmpty()) {
                    InfoCard(
                        "Sin dispositivos automáticos",
                        "No se encontraron equipos respondiendo a mDNS o SSDP. Puedes agregar tu equipo manualmente ingresando su dirección IP."
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
                                    }
                                    Button(onClick = { viewModel.saveDiscoveredDevice(device, onDeviceConnected) }) {
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
            Text("Agregar manualmente por IP")
        }
    }

    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            title = { Text("Agregar por Dirección IP") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveManualDevice { deviceId ->
                        showManualDialog = false
                        onDeviceConnected(deviceId)
                    }
                }) {
                    Text("Guardar y Conectar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
