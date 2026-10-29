package com.universalremote.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universalremote.core.designsystem.*
import com.universalremote.core.designsystem.R
import com.universalremote.core.model.IrAvailability
import com.universalremote.core.model.Device
import com.universalremote.core.model.DeviceType
import com.universalremote.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(val loading: Boolean = true, val deviceCount: Int = 0, val ir: IrAvailability? = null,
                       val error: Boolean = false, val favorites: List<Device> = emptyList())
@HiltViewModel
class HomeViewModel(
    private val devices: DeviceRepository,
    private val infrared: IrAvailabilityProvider,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    @Inject constructor(devices: DeviceRepository, infrared: IrAvailabilityProvider) :
        this(devices, infrared, Dispatchers.IO)
    private val mutableState = MutableStateFlow(HomeUiState())
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null
    init { refresh() }
    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = false) }
            val ir = withContext(ioDispatcher) { infrared.availability() }
            mutableState.update { it.copy(ir = ir) }
            devices.observeDevices().catch {
                mutableState.update { it.copy(loading = false, error = true) }
            }.collect { list ->
                mutableState.update { it.copy(loading = false, deviceCount = list.size, favorites = list.filter { device -> device.isFavorite }) }
            }
        }
    }
}
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onDevices: () -> Unit,
    onAdd: () -> Unit,
    onRemote: (DeviceType) -> Unit,
    onOpenDevice: ((Device) -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Page(stringResource(R.string.app_name)) {
        Text(stringResource(R.string.hello), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.welcome), style = MaterialTheme.typography.headlineLarge)
        StatusIndicator(irText(state.ir), state.ir == IrAvailability.Available, Icons.Default.SettingsRemote)
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.error) {
            Text(stringResource(R.string.read_error))
            TextButton(onClick = viewModel::refresh) { Text(stringResource(R.string.retry)) }
        }
        AnimatedVisibility(!state.loading && !state.error && state.deviceCount == 0) {
            EmptyState(onAction = onAdd)
        }
        if (!state.loading && !state.error && state.deviceCount > 0) {
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.add_device)) }
            Text(stringResource(R.string.favorites), style = MaterialTheme.typography.titleLarge)
            if (state.favorites.isEmpty()) Text(stringResource(R.string.no_favorites))
            state.favorites.take(4).forEach { device ->
                DeviceCard(device, onPreview = { onOpenDevice?.invoke(device) ?: onRemote(device.deviceType) })
            }
        }
        OutlinedButton(onClick = onDevices, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.devices)) }
        Text(stringResource(R.string.explore_controls), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.demo_short), color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { onRemote(DeviceType.TV) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.tv_remote)) }
        OutlinedButton(onClick = { onRemote(DeviceType.PROJECTOR) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.projector_remote)) }
    }
}
