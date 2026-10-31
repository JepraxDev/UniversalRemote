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
    val epsonModel: String = "PowerLite 118",
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
        val effectiveBrand = brand
        var profiles = infraredRepository.profiles.filter { it.brand == effectiveBrand && it.deviceType == type }
        val pending = infraredRepository.pendingBrands.firstOrNull { it.brand == effectiveBrand && it.deviceType == type }
        val selected = if (effectiveBrand == DeviceBrand.EPSON) {
            profiles.firstOrNull { it.id == epsonProfileId(mutableState.value.epsonModel) } ?: profiles.firstOrNull()
        } else {
            profiles.firstOrNull()
        }

        mutableState.update {
            it.copy(
                deviceType = type,
                brand = effectiveBrand,
                irAvailability = availability,
                availableProfiles = profiles,
                pendingBrandInfo = pending,
                selectedProfile = selected,
                customDeviceName = if (effectiveBrand == DeviceBrand.EPSON) {
                    "Epson ${it.epsonModel}"
                } else {
                    "${effectiveBrand.id} ${if (type == DeviceType.TV) "TV" else "Proyector"}"
                },
                testStep = IrTestStep.SELECT_PROFILE,
                lastTransmissionMessage = null,
                validationStatus = IrValidationStatus.UNVERIFIED,
                transmissionSuccess = false,
                isSaved = false,
            )
        }
    }

    fun selectProfile(profile: IrProfile) {
        if (mutableState.value.isTransmitting || profile !in mutableState.value.availableProfiles) return
        mutableState.update {
            val model = if (it.brand == DeviceBrand.EPSON) {
                EPSON_MODELS.firstOrNull { model -> epsonProfileId(model) == profile.id } ?: it.epsonModel
            } else it.epsonModel
            it.copy(
                selectedProfile = profile,
                epsonModel = model,
                customDeviceName = if (it.brand == DeviceBrand.EPSON && it.customDeviceName == "Epson ${it.epsonModel}") "Epson $model" else it.customDeviceName,
                testStep = IrTestStep.SELECT_PROFILE,
                lastTransmissionMessage = null,
                validationStatus = IrValidationStatus.UNVERIFIED,
                transmissionSuccess = false,
            )
        }
    }

    fun sendTestCommand() {
        val current = mutableState.value
        val profile = current.selectedProfile ?: return
        if (current.isTransmitting) return

        mutableState.update {
            it.copy(isTransmitting = true, transmissionSuccess = false, validationStatus = IrValidationStatus.UNVERIFIED, errorMessage = null, testStep = IrTestStep.TESTING_COMMAND)
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
                            lastTransmissionMessage = buildString {
                                append("Comando ${profile.testCommand.name} transmitido en $khz kHz. IR no confirma recepción: comprueba la respuesta del proyector.")
                                profile.testNote?.let { append(" ").append(it) }
                            },
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
        if (!mutableState.value.transmissionSuccess || mutableState.value.isTransmitting) return
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
                transmissionSuccess = false,
            )
        }
    }

    fun selectEpsonModel(model: String) {
        if (mutableState.value.isTransmitting || model !in EPSON_MODELS) return
        val profile = mutableState.value.availableProfiles.firstOrNull { it.id == epsonProfileId(model) }
        mutableState.update {
            it.copy(
                epsonModel = model,
                selectedProfile = profile ?: it.selectedProfile,
                customDeviceName = "Epson $model",
                transmissionSuccess = false,
                validationStatus = IrValidationStatus.UNVERIFIED,
                testStep = IrTestStep.SELECT_PROFILE,
                lastTransmissionMessage = null,
            )
        }
    }

    private fun epsonProfileId(model: String): String = when (model) {
        "PowerLite 118" -> "EPSON_POWERLITE_118_ORIGINAL_IR"
        "EB-L260F" -> "EPSON_EB_L260F"
        "EMP822H" -> "epson-emp822h-v1"
        else -> ""
    }

    fun setDeviceName(name: String) {
        mutableState.update { it.copy(customDeviceName = name) }
    }

    fun setRoomName(room: String) {
        mutableState.update { it.copy(customRoomName = room) }
    }

    fun saveDevice(onSaved: (String) -> Unit) {
        val current = mutableState.value
        if (current.validationStatus != IrValidationStatus.CONFIRMED_BY_USER || current.isSaved) return
        val profile = current.selectedProfile ?: return

        val newId = "dev-" + UUID.randomUUID().toString().take(8)
        val device = Device(
            id = newId,
            name = current.customDeviceName.ifBlank { "${current.brand.id} ${if (current.deviceType == DeviceType.TV) "TV" else "Proyector"}" },
            brand = current.brand,
            model = if (current.brand == DeviceBrand.EPSON) current.epsonModel else profile.family,
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

    companion object {
        val EPSON_MODELS = listOf("PowerLite 118", "EB-L260F", "EMP822H")
    }
}
