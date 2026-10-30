package com.universalremote.feature.remote

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universalremote.core.model.*
import com.universalremote.domain.*
import com.universalremote.protocol.api.CommandResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RemoteUiState(
    val deviceId: String? = null,
    val deviceName: String? = null,
    val deviceType: DeviceType = DeviceType.TV,
    val connectionType: ConnectionType = ConnectionType.WIFI,
    val locationTag: String? = null,
    val compatibilityStatus: CompatibilityStatus = CompatibilityStatus.VERIFIED_PROTOCOL,
    val isDemo: Boolean = true,
    val irAvailability: IrAvailability = IrAvailability.ServiceUnavailable,
    val capabilities: DeviceCapabilities? = null,
    val isTransmitting: Boolean = false,
    val lastResultText: String? = null,
    val isError: Boolean = false,
    val pendingConfirmationCommand: RemoteCommand? = null,
)

@HiltViewModel
class RemoteViewModel @Inject constructor(
    private val saved: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    private val infraredRepository: InfraredRepository,
    private val sendRemoteCommand: SendRemoteCommand,
    private val sendNetworkCommand: SendNetworkCommand,
    private val availabilityProvider: IrAvailabilityProvider,
    private val capabilityResolver: DeviceCapabilityResolver
) : ViewModel() {

    private val mutableState = MutableStateFlow(
        RemoteUiState(
            deviceId = saved.get<String>("deviceId"),
            deviceType = DeviceType.entries.firstOrNull { it.name == saved.get<String>("type") } ?: DeviceType.TV,
        )
    )
    val state = mutableState.asStateFlow()

    private var loadedDevice: Device? = null

    init {
        loadDevice()
    }

    fun loadDevice(id: String? = mutableState.value.deviceId) {
        viewModelScope.launch {
            val availability = availabilityProvider.availability()
            val deviceId = id ?: saved.get<String>("deviceId")
            if (deviceId == null) {
                loadedDevice = null
                mutableState.update { it.copy(isDemo = true, irAvailability = availability) }
                return@launch
            }
            val device = deviceRepository.find(deviceId)
            loadedDevice = device
            if (device == null) {
                mutableState.update {
                    it.copy(
                        deviceId = deviceId,
                        isDemo = true,
                        irAvailability = availability
                    )
                }
                return@launch
            }

            val caps = capabilityResolver.resolve(device)

            if (device.connectionType == ConnectionType.WIFI) {
                mutableState.update {
                    it.copy(
                        deviceId = device.id,
                        deviceName = device.name,
                        deviceType = device.deviceType,
                        connectionType = ConnectionType.WIFI,
                        locationTag = device.locationTag,
                        compatibilityStatus = device.compatibilityStatus,
                        isDemo = false,
                        irAvailability = availability,
                        capabilities = caps
                    )
                }
            } else {
                val profile = infraredRepository.profiles.firstOrNull { it.id == device.irProfileId }
                val isHardwareReady = availability == IrAvailability.Available && profile != null
                mutableState.update {
                    it.copy(
                        deviceId = device.id,
                        deviceName = device.name,
                        deviceType = device.deviceType,
                        connectionType = ConnectionType.INFRARED,
                        locationTag = device.locationTag,
                        compatibilityStatus = device.compatibilityStatus,
                        isDemo = !isHardwareReady,
                        irAvailability = availability,
                        capabilities = caps
                    )
                }
            }
        }
    }

    fun toggleConnectionType() {
        val current = loadedDevice ?: return
        val newType = if (current.connectionType == ConnectionType.WIFI) ConnectionType.INFRARED else ConnectionType.WIFI
        val updatedDevice = current.copy(connectionType = newType)
        viewModelScope.launch {
            deviceRepository.update(updatedDevice)
            loadDevice(updatedDevice.id)
        }
    }

    fun sendCommand(command: RemoteCommand, confirmed: Boolean = false) {
        val current = mutableState.value
        val device = loadedDevice

        if (current.isDemo && current.connectionType == ConnectionType.INFRARED) return
        if (current.isTransmitting || device == null) return

        val capabilities = current.capabilities ?: return
        if (command !in capabilities.supportedCommands) return

        if (!confirmed && command.requiresConfirmation()) {
            mutableState.update { it.copy(pendingConfirmationCommand = command) }
            return
        }

        mutableState.update { it.copy(isTransmitting = true, isError = false, lastResultText = "Transmitiendo…") }

        viewModelScope.launch {
            if (device.connectionType == ConnectionType.WIFI) {
                when (val result = sendNetworkCommand(device, command)) {
                    is CommandResult.Sent -> {
                        mutableState.update {
                            it.copy(lastResultText = "Comando enviado por Wi-Fi", isError = false)
                        }
                    }
                    is CommandResult.Failed -> {
                        mutableState.update {
                            it.copy(lastResultText = "Error Wi-Fi: ${result.reason}", isError = true)
                        }
                    }
                    is CommandResult.Unsupported -> {
                        mutableState.update {
                            it.copy(lastResultText = "Comando no soportado por el dispositivo", isError = true)
                        }
                    }
                    is CommandResult.NotConfigured -> {
                        mutableState.update {
                            it.copy(lastResultText = "Dispositivo de red no configurado", isError = true)
                        }
                    }
                }
            } else {
                val result = sendRemoteCommand(device.id, command, confirmed = true)
                when (result) {
                    is IrResult.Success -> {
                        mutableState.update {
                            it.copy(lastResultText = "Señal enviada localmente (IR)", isError = false)
                        }
                    }
                    is IrResult.Failure -> {
                        mutableState.update {
                            it.copy(lastResultText = "Error al transmitir IR", isError = true)
                        }
                    }
                    is IrResult.Unavailable -> {
                        mutableState.update {
                            it.copy(lastResultText = "Emisor IR no disponible en este celular", isError = true)
                        }
                    }
                }
            }
            delay(1200)
            mutableState.update { it.copy(isTransmitting = false, lastResultText = null) }
        }
    }

    fun confirmPendingCommand() {
        val command = mutableState.value.pendingConfirmationCommand ?: return
        mutableState.update { it.copy(pendingConfirmationCommand = null) }
        sendCommand(command, confirmed = true)
    }

    fun dismissConfirmation() {
        mutableState.update { it.copy(pendingConfirmationCommand = null) }
    }
}
