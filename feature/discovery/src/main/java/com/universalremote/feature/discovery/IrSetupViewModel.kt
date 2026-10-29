package com.universalremote.feature.discovery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universalremote.core.model.*
import com.universalremote.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class IrTestStep {
    SELECT_PROFILE,
    TESTING_COMMAND,
    CONFIRMED_SUCCESS,
    INCOMPATIBLE_RESULT
}

data class IrSetupUiState(
    val deviceType: DeviceType = DeviceType.TV,
    val brand: DeviceBrand = DeviceBrand.LG,
    val irAvailability: IrAvailability = IrAvailability.ServiceUnavailable,
    val availableProfiles: List<IrProfile> = emptyList(),
    val pendingBrandInfo: PendingBrandInfo? = null,
    val selectedProfile: IrProfile? = null,
    val testStep: IrTestStep = IrTestStep.SELECT_PROFILE,
    val isTransmitting: Boolean = false,
    val lastTransmissionMessage: String? = null,
    val transmissionSuccess: Boolean = false,
    val validationStatus: IrValidationStatus = IrValidationStatus.UNVERIFIED,
    val customDeviceName: String = "",
    val customRoomName: String = "",
    val isSaved: Boolean = false,
    val savedDeviceId: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class IrSetupViewModel @Inject constructor(
    private val saved: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    private val infraredRepository: InfraredRepository,
    private val availabilityProvider: IrAvailabilityProvider,
) : ViewModel() {

    private val mutableState = MutableStateFlow(
        IrSetupUiState(
            deviceType = DeviceType.entries.firstOrNull { it.name == saved.get<String>("type") } ?: DeviceType.TV,
            brand = DeviceBrand(saved.get<String>("brand") ?: DeviceBrand.LG.id),
        )
    )
    val state = mutableState.asStateFlow()

    init {
        initProfiles(mutableState.value.deviceType, mutableState.value.brand)
    }

    fun initProfiles(type: DeviceType, brand: DeviceBrand) {
        val availability = availabilityProvider.availability()
        val profiles = infraredRepository.profiles.filter { it.brand == brand && it.deviceType == type }
        val pending = infraredRepository.pendingBrands.firstOrNull { it.brand == brand && it.deviceType == type }
        val selected = profiles.firstOrNull()

        mutableState.update {
            it.copy(
                deviceType = type,
                brand = brand,
                irAvailability = availability,
                availableProfiles = profiles,
                pendingBrandInfo = pending,
                selectedProfile = selected,
                customDeviceName = "${brand.id} ${if (type == DeviceType.TV) "TV" else "Proyector"}",
                testStep = IrTestStep.SELECT_PROFILE,
                lastTransmissionMessage = null,
                validationStatus = IrValidationStatus.UNVERIFIED,
            )
        }
    }

    fun selectProfile(profile: IrProfile) {
        mutableState.update {
            it.copy(
                selectedProfile = profile,
                testStep = IrTestStep.SELECT_PROFILE,
                lastTransmissionMessage = null,
                validationStatus = IrValidationStatus.UNVERIFIED,
            )
        }
    }

    fun sendTestCommand() {
        val current = mutableState.value
        val profile = current.selectedProfile ?: return
        if (current.isTransmitting) return

        mutableState.update {
            it.copy(isTransmitting = true, errorMessage = null, testStep = IrTestStep.TESTING_COMMAND)
        }

        viewModelScope.launch {
            val result = infraredRepository.test(profile.id)
            when (result) {
                is IrResult.Success -> {
                    val khz = profile.carrierHz / 1000
                    mutableState.update {
                        it.copy(
                            isTransmitting = false,
                            transmissionSuccess = true,
                            lastTransmissionMessage = "Señal enviada localmente ($khz kHz). Nota: ConsumerIrManager no proporciona confirmación de recepción del equipo.",
                            validationStatus = IrValidationStatus.UNVERIFIED,
                        )
                    }
                }
                is IrResult.Failure -> {
                    val errorMsg = when (result.error) {
                        IrError.BUSY -> "Transmisor ocupado. Intenta de nuevo en unos segundos."
                        IrError.UNSUPPORTED_FREQUENCY -> "Frecuencia de ${profile.carrierHz} Hz no soportada por el emisor de este celular."
                        IrError.FREQUENCY_UNKNOWN -> "Frecuencia no confirmada por Android."
                        IrError.PERMISSION_DENIED -> "Permiso de transmisión IR denegado."
                        else -> "Error de plataforma al transmitir comando IR."
                    }
                    mutableState.update {
                        it.copy(
                            isTransmitting = false,
                            transmissionSuccess = false,
                            lastTransmissionMessage = errorMsg,
                        )
                    }
                }
                is IrResult.Unavailable -> {
                    mutableState.update {
                        it.copy(
                            isTransmitting = false,
                            transmissionSuccess = false,
                            lastTransmissionMessage = "El celular no dispone de emisor IR integrado.",
                        )
                    }
                }
            }
        }
    }

    fun confirmResponse(responded: Boolean) {
        val profile = mutableState.value.selectedProfile ?: return
        viewModelScope.launch {
            infraredRepository.confirm(profile.id, responded)
            if (responded) {
                mutableState.update {
                    it.copy(
                        validationStatus = IrValidationStatus.CONFIRMED_BY_USER,
                        testStep = IrTestStep.CONFIRMED_SUCCESS,
                    )
                }
            } else {
                mutableState.update {
                    it.copy(
                        validationStatus = IrValidationStatus.INCOMPATIBLE,
                        testStep = IrTestStep.INCOMPATIBLE_RESULT,
                    )
                }
            }
        }
    }

    fun retryAnotherProfile() {
        mutableState.update {
            it.copy(
                testStep = IrTestStep.SELECT_PROFILE,
                lastTransmissionMessage = null,
                validationStatus = IrValidationStatus.UNVERIFIED,
            )
        }
    }

    fun setDeviceName(name: String) {
        mutableState.update { it.copy(customDeviceName = name) }
    }

    fun setRoomName(room: String) {
        mutableState.update { it.copy(customRoomName = room) }
    }

    fun saveDevice(onSaved: (String) -> Unit) {
        val current = mutableState.value
        val profile = current.selectedProfile ?: return

        val newId = "dev-" + UUID.randomUUID().toString().take(8)
        val device = Device(
            id = newId,
            name = current.customDeviceName.ifBlank { "${current.brand.id} ${if (current.deviceType == DeviceType.TV) "TV" else "Proyector"}" },
            brand = current.brand,
            model = profile.family,
            deviceType = current.deviceType,
            connectionType = ConnectionType.INFRARED,
            irProfileId = profile.id,
            roomName = current.customRoomName.takeIf { it.isNotBlank() },
            isFavorite = false,
        )

        viewModelScope.launch {
            try {
                deviceRepository.insert(device)
                infraredRepository.confirm(newId, responded = true)
                mutableState.update { it.copy(isSaved = true, savedDeviceId = newId) }
                onSaved(newId)
            } catch (_: Exception) {
                mutableState.update { it.copy(errorMessage = "No se pudo guardar el dispositivo.") }
            }
        }
    }
}
