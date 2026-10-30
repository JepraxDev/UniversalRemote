package com.universalremote.feature.remote

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universalremote.core.designsystem.AppTopBar
import com.universalremote.core.model.*
import com.universalremote.domain.DeviceCapabilityResolver
import com.universalremote.domain.DeviceRepository
import com.universalremote.domain.RemoteLayoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomizationUiState(
    val deviceId: String? = null,
    val deviceName: String = "",
    val style: RemoteLayoutStyle = RemoteLayoutStyle.CLASSIC,
    val availableCommands: List<RemoteCommand> = emptyList(),
    val hiddenCommands: Set<RemoteCommand> = emptySet()
)

@HiltViewModel
class RemoteCustomizationViewModel @Inject constructor(
    private val saved: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    private val layoutRepository: RemoteLayoutRepository,
    private val capabilityResolver: DeviceCapabilityResolver
) : ViewModel() {

    private val mutableState = MutableStateFlow(CustomizationUiState())
    val state: StateFlow<CustomizationUiState> = mutableState.asStateFlow()

    init {
        val deviceId = saved.get<String>("deviceId")
        if (deviceId != null) {
            load(deviceId)
        }
    }

    fun load(deviceId: String) {
        viewModelScope.launch {
            val device = deviceRepository.find(deviceId) ?: return@launch
            val caps = capabilityResolver.resolve(device)
            val layout = layoutRepository.getLayout(deviceId) ?: RemoteLayoutPreference(deviceId)

            mutableState.update {
                it.copy(
                    deviceId = device.id,
                    deviceName = device.name,
                    style = layout.layoutStyle,
                    availableCommands = caps.supportedCommands.toList(),
                    hiddenCommands = layout.hiddenCommands
                )
            }
        }
    }

    fun setStyle(style: RemoteLayoutStyle) {
        mutableState.update { it.copy(style = style) }
    }

    fun toggleCommandVisibility(command: RemoteCommand) {
        mutableState.update { current ->
            val hidden = current.hiddenCommands.toMutableSet()
            if (hidden.contains(command)) hidden.remove(command) else hidden.add(command)
            current.copy(hiddenCommands = hidden)
        }
    }

    fun resetToDefault() {
        mutableState.update { it.copy(style = RemoteLayoutStyle.CLASSIC, hiddenCommands = emptySet()) }
    }

    fun save(onSaved: () -> Unit) {
        val current = state.value
        val deviceId = current.deviceId ?: return
        viewModelScope.launch {
            val pref = RemoteLayoutPreference(
                deviceId = deviceId,
                layoutStyle = current.style,
                hiddenCommands = current.hiddenCommands
            )
            layoutRepository.saveLayout(pref)
            onSaved()
        }
    }
}

@Composable
fun RemoteCustomizationScreen(
    viewModel: RemoteCustomizationViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { AppTopBar("Personalizar Control: ${state.deviceName}", onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Estilo de distribución", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RemoteLayoutStyle.entries.forEach { style ->
                    FilterChip(
                        selected = state.style == style,
                        onClick = { viewModel.setStyle(style) },
                        label = {
                            Text(when (style) {
                                RemoteLayoutStyle.COMPACT -> "Compacto"
                                RemoteLayoutStyle.CLASSIC -> "Clásico"
                                RemoteLayoutStyle.FULL -> "Completo"
                            })
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Divider()

            Text("Visibilidad de botones soportados", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(state.availableCommands) { cmd ->
                    val isVisible = !state.hiddenCommands.contains(cmd)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cmd.name, style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = isVisible,
                                onCheckedChange = { viewModel.toggleCommandVisibility(cmd) }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.resetToDefault() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Restablecer")
                }
                Button(
                    onClick = { viewModel.save(onBack) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Guardar")
                }
            }
        }
    }
}
