package com.universalremote.feature.devices

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universalremote.core.designsystem.*
import com.universalremote.core.designsystem.R
import com.universalremote.core.model.*
import com.universalremote.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DevicesUiState(
    val devices: List<Device> = emptyList(),
    val total: Int = 0,
    val loading: Boolean = true,
    val error: Boolean = false,
    val actionError: Boolean = false,
    val filter: DeviceFilter = DeviceFilter()
)

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val repository: DeviceRepository,
    private val openRemoteUseCase: OpenRemoteUseCase,
    private val saved: SavedStateHandle
) : ViewModel() {

    private val filter = MutableStateFlow(DeviceFilter(
        query = saved["query"] ?: "",
        type = DeviceType.entries.firstOrNull { it.name == saved.get<String>("type") },
        connection = ConnectionType.entries.firstOrNull { it.name == saved.get<String>("connection") },
        sort = DeviceSort.entries.firstOrNull { it.name == saved.get<String>("sort") } ?: DeviceSort.FAVORITES,
    ))
    private val actionError = MutableStateFlow(false)
    private val retry = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val source = retry.flatMapLatest {
        repository.observeDevices()
            .map { DevicesUiState(devices = it, total = it.size, loading = false) }
            .catch { emit(DevicesUiState(loading = false, error = true)) }
    }

    val state = combine(source, filter, actionError) { base, filter, error ->
        base.copy(devices = filterDevices(base.devices, filter), filter = filter, actionError = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DevicesUiState())

    fun filter(value: DeviceFilter) {
        saved["query"] = value.query
        saved["type"] = value.type?.name
        saved["connection"] = value.connection?.name
        saved["sort"] = value.sort.name
        filter.value = value
    }

    fun retry() { retry.value++ }

    fun toggleFavorite(id: String) = mutate {
        repository.find(id)?.let { repository.update(it.copy(isFavorite = !it.isFavorite)) }
    }

    fun renameDevice(id: String, newName: String) = mutate {
        repository.find(id)?.let { repository.update(it.copy(name = newName)) }
    }

    fun updateLocation(id: String, newLocation: String) = mutate {
        repository.find(id)?.let { repository.update(it.copy(locationTag = newLocation)) }
    }

    fun updateIpAddress(id: String, newIp: String) = mutate {
        repository.find(id)?.let { repository.update(it.copy(ipAddress = newIp)) }
    }

    fun delete(id: String) = mutate { repository.delete(id) }

    fun openDevice(id: String, onOpen: (Device) -> Unit) {
        viewModelScope.launch {
            val opened = openRemoteUseCase(id)
            if (opened != null) {
                onOpen(opened)
            }
        }
    }

    private fun mutate(action: suspend () -> Unit) {
        viewModelScope.launch {
            actionError.value = false
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { actionError.value = true }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DevicesScreen(
    viewModel: DevicesViewModel,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onPreview: (DeviceType) -> Unit,
    onOpenDevice: ((Device) -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameDeviceState by remember { mutableStateOf<Device?>(null) }
    var locationDeviceState by remember { mutableStateOf<Device?>(null) }
    var editConnDeviceState by remember { mutableStateOf<Device?>(null) }
    var detailsDeviceState by remember { mutableStateOf<Device?>(null) }

    val pendingDelete = state.devices.firstOrNull { it.id == deleteId }
    if (pendingDelete != null) {
        ConfirmationDialog(
            stringResource(R.string.delete_device),
            stringResource(R.string.delete_detail, pendingDelete.name),
            onConfirm = { viewModel.delete(pendingDelete.id); deleteId = null },
            onDismiss = { deleteId = null }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(stringResource(R.string.devices), onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Agregar dispositivo")
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier
                    .testTag("devices_list")
                    .widthIn(max = 840.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = state.filter.query,
                        onValueChange = { viewModel.filter(state.filter.copy(query = it)) },
                        placeholder = { Text(stringResource(R.string.search_devices)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                }

                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.filter.type == null,
                            onClick = { viewModel.filter(state.filter.copy(type = null)) },
                            label = { Text(stringResource(R.string.all)) }
                        )
                        DeviceType.entries.forEach { type ->
                            FilterChip(
                                selected = state.filter.type == type,
                                onClick = { viewModel.filter(state.filter.copy(type = type)) },
                                label = { Text(deviceTypeText(type)) }
                            )
                        }
                    }
                }

                if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }

                if (state.error) item {
                    Text(stringResource(R.string.read_error))
                    TextButton(onClick = viewModel::retry) { Text(stringResource(R.string.retry)) }
                }

                if (state.actionError) item {
                    Text(stringResource(R.string.action_error), color = MaterialTheme.colorScheme.error)
                }

                if (!state.loading && !state.error && state.devices.isEmpty()) item {
                    if (state.total == 0) EmptyState(onAction = onAdd)
                    else EmptyState(stringResource(R.string.no_results), stringResource(R.string.no_results_detail), stringResource(R.string.clear_filters)) { viewModel.filter(DeviceFilter()) }
                }

                items(state.devices, key = { it.id }) { device ->
                    DeviceCard(
                        device = device,
                        onOpenRemote = {
                            if (onOpenDevice != null) {
                                viewModel.openDevice(device.id, onOpenDevice)
                            } else {
                                onPreview(device.deviceType)
                            }
                        },
                        onFavorite = { viewModel.toggleFavorite(device.id) },
                        onRename = { renameDeviceState = device },
                        onEditConnection = { editConnDeviceState = device },
                        onChangeLocation = { locationDeviceState = device },
                        onShowDetails = { detailsDeviceState = device },
                        onDelete = { deleteId = device.id }
                    )
                }
            }
        }
    }

    // Rename Dialog
    renameDeviceState?.let { dev ->
        var newName by remember { mutableStateOf(dev.name) }
        AlertDialog(
            onDismissRequest = { renameDeviceState = null },
            title = { Text("Renombrar dispositivo") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nombre") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) viewModel.renameDevice(dev.id, newName)
                    renameDeviceState = null
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { renameDeviceState = null }) { Text("Cancelar") }
            }
        )
    }

    // Location Dialog
    locationDeviceState?.let { dev ->
        var newLoc by remember { mutableStateOf(dev.locationTag ?: dev.roomName ?: "") }
        AlertDialog(
            onDismissRequest = { locationDeviceState = null },
            title = { Text("Cambiar ubicación / aula") },
            text = {
                OutlinedTextField(
                    value = newLoc,
                    onValueChange = { newLoc = it },
                    label = { Text("Ubicación o Aula") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateLocation(dev.id, newLoc)
                    locationDeviceState = null
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { locationDeviceState = null }) { Text("Cancelar") }
            }
        )
    }

    // Edit Connection Dialog
    editConnDeviceState?.let { dev ->
        var newIp by remember { mutableStateOf(dev.ipAddress ?: "") }
        AlertDialog(
            onDismissRequest = { editConnDeviceState = null },
            title = { Text("Editar conexión de red") },
            text = {
                OutlinedTextField(
                    value = newIp,
                    onValueChange = { newIp = it },
                    label = { Text("Dirección IP") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newIp.isNotBlank()) viewModel.updateIpAddress(dev.id, newIp)
                    editConnDeviceState = null
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { editConnDeviceState = null }) { Text("Cancelar") }
            }
        )
    }

    // Details Dialog
    detailsDeviceState?.let { dev ->
        AlertDialog(
            onDismissRequest = { detailsDeviceState = null },
            title = { Text("Detalles técnicos") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nombre interno: ${dev.name}", style = MaterialTheme.typography.bodyMedium)
                    Text("Marca: ${dev.brand.id}", style = MaterialTheme.typography.bodyMedium)
                    Text("Modelo: ${dev.model}", style = MaterialTheme.typography.bodyMedium)
                    Text("Método: ${dev.connectionType.name}", style = MaterialTheme.typography.bodyMedium)
                    if (dev.ipAddress != null) Text("Dirección IP: ${dev.ipAddress}:${dev.port ?: 80}", style = MaterialTheme.typography.bodyMedium)
                    Text("Estado de compatibilidad: ${dev.compatibilityStatus.name}", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { detailsDeviceState = null }) { Text("Cerrar") }
            }
        )
    }
}
