package com.universalremote.feature.remote

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universalremote.core.model.*
import com.universalremote.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State for the Epson projector screen (PowerLite 118 and EB-L260F). IR is
 * one-way: "transmitted" only means ConsumerIrManager accepted the frame,
 * never that the projector reacted.
 */
data class EpsonIrRemoteUiState(
    val deviceId: String? = null,
    val deviceName: String = "Epson",
    val profileId: String? = null,
    val profileFamily: String? = null,
    val profileProtocol: String? = null,
    val profileSource: String? = null,
    val testCommand: RemoteCommand? = null,
    val testNote: String? = null,
    val supportedCommands: Set<RemoteCommand> = emptySet(),
    val profileLoaded: Boolean = false,
    val isDemo: Boolean = true,
    val irAvailability: IrAvailability = IrAvailability.ServiceUnavailable,
    val capabilities: DeviceCapabilities? = null,
    val validationStatus: IrValidationStatus = IrValidationStatus.UNVERIFIED,
    val isTransmitting: Boolean = false,
    val lastTransmitted: RemoteCommand? = null,
    val lastResultText: String? = null,
    val isError: Boolean = false,
    val pendingConfirmation: RemoteCommand? = null,
    val canSaveConfirmation: Boolean = false,
) {
    val irAvailable: Boolean get() = irAvailability == IrAvailability.Available
    val canTransmit: Boolean
        get() = !isDemo && !isTransmitting && irAvailable && profileLoaded && deviceId != null
}

@HiltViewModel
class EpsonIrRemoteViewModel @Inject constructor(
    private val saved: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    private val infraredRepository: InfraredRepository,
    private val sendRemoteCommand: SendRemoteCommand,
    private val availabilityProvider: IrAvailabilityProvider,
) : ViewModel() {

    private val mutableState = MutableStateFlow(
        EpsonIrRemoteUiState(deviceId = saved.get<String>("deviceId"))
    )
    val state = mutableState.asStateFlow()

    init {
        loadDevice(mutableState.value.deviceId)
    }

    fun loadDevice(id: String? = mutableState.value.deviceId) {
        viewModelScope.launch {
            val availability = availabilityProvider.availability()
            val deviceId = id ?: saved.get<String>("deviceId")
            if (deviceId == null) {
                mutableState.update {
                    it.copy(
                        isDemo = true,
                        irAvailability = availability,
                        profileLoaded = false,
                        lastResultText = null,
                    )
                }
                return@launch
            }

            val device = deviceRepository.find(deviceId)
            if (device == null) {
                mutableState.update {
                    it.copy(deviceId = deviceId, isDemo = true, irAvailability = availability)
                }
                return@launch
            }

            val profile = device.irProfileId?.let { id -> infraredRepository.profiles.firstOrNull { p -> p.id == id } }
                ?: infraredRepository.profiles.firstOrNull { p ->
                    p.brand == DeviceBrand.EPSON && p.deviceType == DeviceType.PROJECTOR
                }
            val assessment = infraredRepository.assessment(device.id)
                ?: device.irProfileId?.let { infraredRepository.assessment(it) }
            val profileLoaded = profile != null
            val ready = availability == IrAvailability.Available && profileLoaded

            mutableState.update {
                it.copy(
                    deviceId = device.id,
                    deviceName = device.name.ifBlank { "Epson ${device.model}" },
                    profileId = profile?.id,
                    profileFamily = profile?.family,
                    profileProtocol = profile?.protocol,
                    profileSource = profile?.source,
                    testCommand = profile?.testCommand,
                    testNote = profile?.testNote,
                    supportedCommands = profile?.commands?.keys.orEmpty(),
                    profileLoaded = profileLoaded,
                    isDemo = !ready,
                    irAvailability = availability,
                    capabilities = profile?.capabilities,
                    validationStatus = assessment?.status ?: IrValidationStatus.UNVERIFIED,
                    lastResultText = when {
                        availability != IrAvailability.Available ->
                            "El emisor IR no está disponible en este dispositivo."
                        !profileLoaded -> "No hay un perfil IR cargado para este proyector."
                        else -> null
                    },
                    isError = availability != IrAvailability.Available || !profileLoaded,
                )
            }
        }
    }

    /** Power/A/V Mute ask first; everything else sends one frame immediately. */
    fun sendCommand(command: RemoteCommand, confirmed: Boolean = false) {
        val current = mutableState.value
        if (command == RemoteCommand.NUM_MODIFIER) {
            mutableState.update {
                it.copy(
                    lastResultText = "Num es un modificador del control: no envía una señal IR propia. Mantén Num y pulsa un número para asignar el ID.",
                    isError = false,
                )
            }
            return
        }
        if (current.isTransmitting) return
        if (!current.canTransmit) {
            mutableState.update {
                it.copy(
                    lastResultText = when {
                        !it.irAvailable -> "IR no disponible: no se transmitió ningún comando."
                        !it.profileLoaded -> "Perfil IR no cargado: no se transmitió ningún comando."
                        else -> "Guarda y abre el proyector para enviar comandos reales."
                    },
                    isError = true,
                )
            }
            return
        }
        val supported = current.capabilities?.supportedCommands
        if (supported != null && command !in supported) {
            mutableState.update {
                it.copy(
                    lastResultText = "${commandName(command)} no tiene un código confirmado en este perfil.",
                    isError = true,
                )
            }
            return
        }
        if (!confirmed && command.requiresConfirmation()) {
            mutableState.update { it.copy(pendingConfirmation = command) }
            return
        }
        transmit(command, confirmed)
    }

    fun confirmPendingCommand() {
        val command = mutableState.value.pendingConfirmation ?: return
        mutableState.update { it.copy(pendingConfirmation = null) }
        transmit(command, confirmed = true)
    }

    fun dismissPendingCommand() {
        mutableState.update { it.copy(pendingConfirmation = null) }
    }

    private fun transmit(command: RemoteCommand, confirmed: Boolean) {
        val deviceId = mutableState.value.deviceId ?: return
        mutableState.update {
            it.copy(
                isTransmitting = true,
                lastResultText = null,
                isError = false,
                lastTransmitted = null,
                canSaveConfirmation = false,
            )
        }
        viewModelScope.launch {
            try {
                when (val result = sendRemoteCommand(deviceId, command, confirmed || command.requiresConfirmation())) {
                    is IrResult.Success -> mutableState.update {
                        it.copy(
                            isTransmitting = false,
                            lastTransmitted = command,
                            lastResultText = "${commandName(command)} transmitido por IR. El proyector aún no ha confirmado la recepción.",
                            isError = false,
                            canSaveConfirmation = true,
                        )
                    }
                    is IrResult.Failure -> mutableState.update {
                        it.copy(
                            isTransmitting = false,
                            lastResultText = failureText(result.error),
                            isError = true,
                            canSaveConfirmation = false,
                        )
                    }
                    is IrResult.Unavailable -> mutableState.update {
                        it.copy(
                            isTransmitting = false,
                            irAvailability = result.availability,
                            isDemo = true,
                            lastResultText = "IR no disponible: no se transmitió ${commandName(command)}.",
                            isError = true,
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(
                        isTransmitting = false,
                        lastResultText = "Error al transmitir ${commandName(command)}.",
                        isError = true,
                    )
                }
            }
        }
    }

    /** User assertion after looking at the projector, never inferred from transmit(). */
    fun confirmLastCommand(responded: Boolean) {
        val deviceId = mutableState.value.deviceId ?: return
        if (mutableState.value.lastTransmitted == null) return
        viewModelScope.launch {
            val result = infraredRepository.confirm(deviceId, responded)
            if (result is IrResult.Success) {
                mutableState.update {
                    it.copy(
                        validationStatus = if (responded) IrValidationStatus.CONFIRMED_BY_USER
                        else IrValidationStatus.INCOMPATIBLE,
                        canSaveConfirmation = false,
                        lastResultText = if (responded) {
                            "Perfil confirmado por ti para este proyector."
                        } else {
                            "El proyector no respondió. Revisa el perfil o la frecuencia antes de continuar."
                        },
                        isError = !responded,
                    )
                }
            }
        }
    }

    private fun commandName(command: RemoteCommand): String = when (command) {
        RemoteCommand.POWER -> "Power"
        RemoteCommand.POWER_ON -> "Encendido"
        RemoteCommand.POWER_OFF -> "Standby"
        RemoteCommand.INPUT -> "Source Search"
        RemoteCommand.HDMI -> "HDMI"
        RemoteCommand.HDMI_1 -> "HDMI 1"
        RemoteCommand.HDMI_2 -> "HDMI 2"
        RemoteCommand.COMPUTER -> "Computer"
        RemoteCommand.USB -> "USB"
        RemoteCommand.LAN -> "LAN"
        RemoteCommand.MENU -> "Menu"
        RemoteCommand.BACK -> "Esc"
        RemoteCommand.OK -> "Enter"
        RemoteCommand.UP -> "Arriba"
        RemoteCommand.DOWN -> "Abajo"
        RemoteCommand.LEFT -> "Izquierda"
        RemoteCommand.RIGHT -> "Derecha"
        RemoteCommand.USER -> "User"
        RemoteCommand.AUTO_SETUP -> "Auto"
        RemoteCommand.ID -> "ID"
        RemoteCommand.DEFAULT -> "Default"
        RemoteCommand.ASPECT_RATIO -> "Aspect"
        RemoteCommand.COLOR_MODE -> "Color Mode"
        RemoteCommand.E_ZOOM_IN -> "E-Zoom +"
        RemoteCommand.E_ZOOM_OUT -> "E-Zoom −"
        RemoteCommand.VOLUME_UP -> "Volume +"
        RemoteCommand.VOLUME_DOWN -> "Volume −"
        RemoteCommand.AV_MUTE -> "A/V Mute"
        RemoteCommand.FREEZE -> "Freeze"
        RemoteCommand.SPLIT -> "Split"
        RemoteCommand.HOME -> "Home"
        RemoteCommand.INFO -> "Información"
        RemoteCommand.SETTINGS_KEY -> "Ajustes"
        else -> command.name
    }

    private fun failureText(error: IrError): String = when (error) {
        IrError.BUSY -> "El emisor está ocupado; espera un instante."
        IrError.UNSUPPORTED_FREQUENCY -> "El emisor no admite 38 kHz."
        IrError.FREQUENCY_UNKNOWN -> "Android no confirma la frecuencia portadora."
        IrError.PERMISSION_DENIED -> "Permiso IR denegado."
        IrError.COMMAND_UNSUPPORTED -> "Ese comando no existe en el perfil."
        IrError.PROFILE_MISSING -> "Perfil IR no encontrado."
        IrError.CONFIRMATION_REQUIRED -> "Confirma el comando antes de enviarlo."
        else -> "Error de plataforma al transmitir IR."
    }

    companion object {
        const val EPSON_PROFILE_ID = "EPSON_POWERLITE_118_ORIGINAL_IR"
        const val EB_L260F_PROFILE_ID = "EPSON_EB_L260F"
    }
}
