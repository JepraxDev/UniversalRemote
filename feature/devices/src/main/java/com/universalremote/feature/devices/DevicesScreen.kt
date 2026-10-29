package com.universalremote.feature.devices

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DevicesUiState(val devices: List<Device> = emptyList(), val total: Int = 0,
                          val loading: Boolean = true, val error: Boolean = false, val actionError: Boolean = false,
                          val filter: DeviceFilter = DeviceFilter())
@HiltViewModel
class DevicesViewModel @Inject constructor(private val repository: DeviceRepository, private val saved: SavedStateHandle) : ViewModel() {
    private val filter = MutableStateFlow(DeviceFilter(
        query = saved["query"] ?: "",
        type = DeviceType.entries.firstOrNull { it.name == saved.get<String>("type") },
        connection = ConnectionType.entries.firstOrNull { it.name == saved.get<String>("connection") },
        sort = DeviceSort.entries.firstOrNull { it.name == saved.get<String>("sort") } ?: DeviceSort.FAVORITES,
    ))
    private val actionError = MutableStateFlow(false)
    private val retry = MutableStateFlow(0)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val source = retry.flatMapLatest {
        repository.observeDevices().map { DevicesUiState(devices = it, total = it.size, loading = false) }
            .catch { emit(DevicesUiState(loading = false, error = true)) }
    }
    val state = combine(source, filter, actionError) { base, filter, error ->
        base.copy(devices = filterDevices(base.devices, filter), filter = filter, actionError = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DevicesUiState())

    fun filter(value: DeviceFilter) {
        saved["query"] = value.query; saved["type"] = value.type?.name
        saved["connection"] = value.connection?.name; saved["sort"] = value.sort.name
        filter.value = value
    }
    fun retry() { retry.value++ }
    fun toggleFavorite(id: String) = mutate {
        repository.find(id)?.let { repository.update(it.copy(isFavorite = !it.isFavorite)) }
    }
    fun delete(id: String) = mutate { repository.delete(id) }
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
    val pendingDelete = state.devices.firstOrNull { it.id == deleteId }
    if (pendingDelete != null) ConfirmationDialog(stringResource(R.string.delete_device), stringResource(R.string.delete_detail, pendingDelete.name),
        onConfirm = { viewModel.delete(pendingDelete.id); deleteId = null }, onDismiss = { deleteId = null })
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { AppTopBar(stringResource(R.string.devices), onBack) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.testTag("devices_list").widthIn(max = 840.dp).fillMaxWidth(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    OutlinedTextField(value = state.filter.query, onValueChange = { viewModel.filter(state.filter.copy(query = it)) },
                        label = { Text(stringResource(R.string.search_devices)) }, leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                }
                item {
                    Column {
                        Text(stringResource(R.string.type_label), style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(state.filter.type == null, { viewModel.filter(state.filter.copy(type = null)) }, { Text(stringResource(R.string.all)) })
                            DeviceType.entries.forEach { type -> FilterChip(state.filter.type == type, { viewModel.filter(state.filter.copy(type = type)) }, { Text(deviceTypeText(type)) }) }
                        }
                        Text(stringResource(R.string.method_label), style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(state.filter.connection == null, { viewModel.filter(state.filter.copy(connection = null)) }, { Text(stringResource(R.string.all)) })
                            ConnectionType.entries.forEach { method -> FilterChip(state.filter.connection == method, { viewModel.filter(state.filter.copy(connection = method)) }, { Text(connectionText(method)) }) }
                        }
                        Text(stringResource(R.string.sort_label), style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DeviceSort.entries.forEach { sort -> FilterChip(state.filter.sort == sort, { viewModel.filter(state.filter.copy(sort = sort)) },
                                { Text(stringResource(if (sort == DeviceSort.NAME) R.string.sort_name else R.string.sort_favorites)) }) }
                        }
                    }
                }
                if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                if (state.error) item {
                    Text(stringResource(R.string.read_error))
                    TextButton(onClick = viewModel::retry) { Text(stringResource(R.string.retry)) }
                }
                if (state.actionError) item { Text(stringResource(R.string.action_error), color = MaterialTheme.colorScheme.error) }
                if (!state.loading && !state.error && state.devices.isEmpty()) item {
                    if (state.total == 0) EmptyState(onAction = onAdd)
                    else EmptyState(stringResource(R.string.no_results), stringResource(R.string.no_results_detail), stringResource(R.string.clear_filters)) { viewModel.filter(DeviceFilter()) }
                }
                items(state.devices, key = { it.id }) { device ->
                    DeviceCard(
                        device = device,
                        onPreview = { onOpenDevice?.invoke(device) ?: onPreview(device.deviceType) },
                        onFavorite = { viewModel.toggleFavorite(device.id) },
                        onDelete = { deleteId = device.id }
                    )
                }
                if (state.total > 0) item { Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.add_device)) } }
            }
        }
    }
}
