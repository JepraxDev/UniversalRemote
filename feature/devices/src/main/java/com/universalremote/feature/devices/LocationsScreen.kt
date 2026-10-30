package com.universalremote.feature.devices

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universalremote.core.designsystem.AppTopBar
import com.universalremote.core.model.Location
import com.universalremote.domain.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class LocationsViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    val locations: StateFlow<List<Location>> = locationRepository.observeLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addLocation(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = "loc_${UUID.randomUUID()}"
            locationRepository.insert(Location(id, name.trim()))
        }
    }

    fun updateLocation(id: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val existing = locationRepository.find(id) ?: return@launch
            locationRepository.insert(existing.copy(name = name.trim()))
        }
    }

    fun deleteLocation(id: String) {
        viewModelScope.launch {
            locationRepository.delete(id)
        }
    }
}

@Composable
fun LocationsScreen(
    viewModel: LocationsViewModel,
    onBack: () -> Unit
) {
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingLocation by remember { mutableStateOf<Location?>(null) }
    var deletingLocation by remember { mutableStateOf<Location?>(null) }
    var inputName by remember { mutableStateOf("") }

    Scaffold(
        topBar = { AppTopBar("Gestionar Ubicaciones", onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                inputName = ""
                showAddDialog = true
            }) {
                Icon(Icons.Default.Add, "Añadir ubicación")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Ubicaciones y Aulas (${locations.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (locations.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No hay ubicaciones creadas", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Puedes organizar tus equipos creando ubicaciones como 'Aula 16', 'Sala de Reuniones' o 'Laboratorio'.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(locations, key = { it.id }) { loc ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.LocationOn, null)
                                    Text(loc.name, style = MaterialTheme.typography.titleMedium)
                                }
                                Row {
                                    IconButton(onClick = {
                                        editingLocation = loc
                                        inputName = loc.name
                                    }) {
                                        Icon(Icons.Default.Edit, "Editar")
                                    }
                                    IconButton(onClick = { deletingLocation = loc }) {
                                        Icon(Icons.Default.Delete, "Eliminar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nueva Ubicación") },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("Nombre (ej. Aula 16, Sala A)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addLocation(inputName)
                    showAddDialog = false
                }) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (editingLocation != null) {
        AlertDialog(
            onDismissRequest = { editingLocation = null },
            title = { Text("Renombrar Ubicación") },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("Nuevo nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    editingLocation?.let { viewModel.updateLocation(it.id, inputName) }
                    editingLocation = null
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingLocation = null }) { Text("Cancelar") }
            }
        )
    }

    if (deletingLocation != null) {
        AlertDialog(
            onDismissRequest = { deletingLocation = null },
            title = { Text("Eliminar Ubicación") },
            text = {
                Text("¿Eliminar '${deletingLocation?.name}'? Los dispositivos asignados a esta ubicación permanecerán guardados sin ubicación asignada.")
            },
            confirmButton = {
                Button(onClick = {
                    deletingLocation?.let { viewModel.deleteLocation(it.id) }
                    deletingLocation = null
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingLocation = null }) { Text("Cancelar") }
            }
        )
    }
}
