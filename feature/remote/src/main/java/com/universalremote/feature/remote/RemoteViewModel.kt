package com.universalremote.feature.remote

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universalremote.core.model.*
import com.universalremote.domain.*
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
    private val availabilityProvider: IrAvailabilityProvider,
) : ViewModel() {

    private val mutableState = MutableStateFlow(
        RemoteUiState(
            deviceId = saved.get<String>("deviceId"),
            deviceType = DeviceType.entries.firstOrNull { it.name == saved.get<String>("type") } ?: DeviceType.TV,
        )
    )
    val state = mutableState.asStateFlow()

    init {
        loadDevice()
    }

    fun loadDevice(id: String? = mutableState.value.deviceId) {
        viewModelScope.launch {
            val availability = availabilityProvider.availability()
            val deviceId = id ?: saved.get<String>("deviceId")
            if (deviceId == null) {
                mutableState.update { it.copy(isDemo = true, irAvailability = availability) }
                return@launch
            }
            val device = deviceRepository.find(deviceId)
            if (device == null || device.connectionType != ConnectionType.INFRARED || device.irProfileId == null) {
                mutableState.update {
                    it.copy(
                        deviceId = deviceId,
                        deviceName = device?.name,
                        deviceType = device?.deviceType ?: it.deviceType,
                        isDemo = true,
                        irAvailability = availability
                    )
                }
                return@launch
            }
            val profile = infraredRepository.profiles.firstOrNull { it.id == device.irProfileId }
            val isHardwareReady = availability == IrAvailability.Available && profile != null
            mutableState.update {
                it.copy(
                    deviceId = device.id,
                    deviceName = device.name,
                    deviceType = device.deviceType,
                    isDemo = !isHardwareReady,
                    irAvailability = availability,
                    capabilities = profile?.capabilities
                )
            }
        }
    }

    fun sendCommand(command: RemoteCommand, confirmed: Boolean = false) {
        val current = mutableState.value
        // Only transmit when device is configured, IR is available, profile exists, not demo, and not busy
        if (current.isDemo || current.isTransmitting || current.deviceId == null) return
        val capabilities = current.capabilities ?: return
        if (command !in capabilities.supportedCommands) return

        // Safety check: Power or AV_MUTE on projectors, or commands requiring confirmation
        if (!confirmed && command.requiresConfirmation()) {
            mutableState.update { it.copy(pendingConfirmationCommand = command) }
            return
        }

        mutableState.update { it.copy(isTransmitting = true, isError = false, lastResultText = "Transmitiendo…") }

        viewModelScope.launch {
            val result = sendRemoteCommand(current.deviceId, command, confirmed = true)
            when (result) {
                is IrResult.Success -> {
                    mutableState.update {
                        it.copy(
                            lastResultText = "Señal enviada localmente",
                            isError = false
                        )
                    }
                }
                is IrResult.Failure -> {
                    val errorMsg = when (result.error) {
                        IrError.BUSY -> "Transmisor ocupado"
                        IrError.UNSUPPORTED_FREQUENCY -> "Frecuencia no compatible con el celular"
                        IrError.FREQUENCY_UNKNOWN -> "Frecuencia desconocida"
                        IrError.INVALID_PATTERN -> "Patrón IR inválido"
                        IrError.INVALID_FREQUENCY -> "Frecuencia fuera de rango"
                        IrError.PERMISSION_DENIED -> "Permiso de transmisión denegado"
                        IrError.PLATFORM_FAILURE -> "Error del emisor infrarrojo"
                        IrError.CONFIRMATION_REQUIRED -> "Confirmación requerida"
                        else -> "Error al transmitir comando"
                    }
                    mutableState.update {
                        it.copy(
                            lastResultText = errorMsg,
                            isError = true
                        )
                    }
                }
                is IrResult.Unavailable -> {
                    mutableState.update {
                        it.copy(
                            lastResultText = "Emisor IR no disponible en este celular",
                            isError = true
                        )
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
