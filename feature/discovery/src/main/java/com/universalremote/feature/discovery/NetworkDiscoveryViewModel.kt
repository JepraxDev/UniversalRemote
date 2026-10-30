package com.universalremote.feature.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universalremote.core.model.*
import com.universalremote.core.network.DiscoveryError
import com.universalremote.core.network.DiscoveryState
import com.universalremote.core.network.NetworkDeviceDiscovery
import com.universalremote.domain.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.universalremote.protocol.api.*
import com.universalremote.core.network.EpsonDeviceRegistry
import com.universalremote.domain.EpsonProtocolResolver
import kotlinx.coroutines.CancellationException

data class ManualIpFormState(
    val name: String = "Televisor Wi-Fi",
    val deviceType: DeviceType = DeviceType.TV,
    val brand: DeviceBrand = DeviceBrand.LG,
    val ipAddress: String = "",
    val portText: String = "3000",
    val protocolType: ProtocolType = ProtocolType.LG_WEBOS,
    val validationError: String? = null,
    val isTesting: Boolean = false,
    val model: String = "",
    val password: String = ""
)

@HiltViewModel
class NetworkDiscoveryViewModel @Inject constructor(
    private val discovery: NetworkDeviceDiscovery,
    private val deviceRepository: DeviceRepository,
    private val protocols: ProtocolRegistry,
    private val epsonRegistry: EpsonDeviceRegistry
) : ViewModel() {

    val discoveryState: StateFlow<DiscoveryState> = discovery.state

    private val mutableManualForm = MutableStateFlow(ManualIpFormState())
    val manualForm: StateFlow<ManualIpFormState> = mutableManualForm.asStateFlow()

    val epsonModels get() = epsonRegistry.profiles.map { it.model }
    val diagnostics = MutableStateFlow("")
    private var pendingDevice: Device? = null

    fun configure(type: DeviceType, brand: DeviceBrand) {
        mutableManualForm.value = ManualIpFormState()
        updateManualType(type)
        updateManualBrand(brand)
        if (brand == DeviceBrand.EPSON) selectEpsonModel("PowerLite 118")
        startDiscovery()
    }

    fun selectEpsonModel(model: String) {
        if (epsonRegistry.find(model) == null) return
        mutableManualForm.update { it.copy(name = "Epson $model", model = model, brand = DeviceBrand.EPSON,
            deviceType = DeviceType.PROJECTOR, protocolType = ProtocolType.UNKNOWN, portText = "4352") }
    }

    fun updatePassword(value: String) { mutableManualForm.update { it.copy(password = value) } }
    fun diagnose() { diagnostics.value = discovery.diagnostics() }
    fun searchLocal() { viewModelScope.launch { discovery.startLocalSearch() } }

    fun startDiscovery() {
        viewModelScope.launch {
            discovery.start()
        }
    }

    fun cancelDiscovery() {
        viewModelScope.launch {
            discovery.cancel()
        }
    }

    fun updateManualName(name: String) { mutableManualForm.update { it.copy(name = name) } }
    fun updateManualIp(ip: String) { mutableManualForm.update { it.copy(ipAddress = ip) } }
    fun updateManualPort(port: String) { mutableManualForm.update { it.copy(portText = port) } }
    fun updateManualBrand(brand: DeviceBrand) {
        val defaultProtocol = when (brand) {
            DeviceBrand.LG -> ProtocolType.LG_WEBOS
            DeviceBrand.SAMSUNG -> ProtocolType.SAMSUNG_WS
            DeviceBrand.EPSON -> ProtocolType.UNKNOWN
            DeviceBrand.BENQ, DeviceBrand.VIEWSONIC, DeviceBrand.OPTOMA -> ProtocolType.PJLINK
            else -> ProtocolType.LG_WEBOS
        }
        mutableManualForm.update {
            it.copy(
                brand = brand,
                protocolType = defaultProtocol,
                portText = if (brand == DeviceBrand.EPSON) "4352" else defaultProtocol.defaultPort.toString()
            )
        }
    }
    fun updateManualType(type: DeviceType) { mutableManualForm.update { it.copy(deviceType = type) } }
    fun updateManualProtocol(protocol: ProtocolType) {
        mutableManualForm.update {
            val tvBrand = when (protocol) { ProtocolType.LG_WEBOS -> DeviceBrand.LG; ProtocolType.SAMSUNG_WS -> DeviceBrand.SAMSUNG; ProtocolType.ANDROID_TV -> DeviceBrand.OTHER; else -> null }
            it.copy(
                protocolType = protocol,
                brand = tvBrand ?: it.brand,
                deviceType = if (tvBrand != null) DeviceType.TV else DeviceType.PROJECTOR,
                model = if (tvBrand != null) "" else it.model,
                portText = if (protocol == ProtocolType.UNKNOWN) "4352" else protocol.defaultPort.toString()
            )
        }
    }

    fun saveDiscoveredDevice(device: Device, onSaved: (String) -> Unit) {
        pendingDevice = device
        validateAndSave(device, onSaved)
    }

    fun retryPending(onSaved: (String) -> Unit) { pendingDevice?.let { validateAndSave(it, onSaved) } }

    private fun validateAndSave(device: Device, onSaved: (String) -> Unit) {
        if (manualForm.value.isTesting) return
        mutableManualForm.update { it.copy(isTesting = true, validationError = null) }
        viewModelScope.launch {
            try {
                val secret = manualForm.value.password.takeIf { it.isNotEmpty() }
                val (selected, result) = if (device.brand == DeviceBrand.EPSON) {
                    val documented = epsonRegistry.find(device.model)?.protocols.orEmpty()
                        .map(ProtocolType::fromId).filter { it != ProtocolType.UNKNOWN }.toSet()
                    // Discovery explicitly validated a PJLink greeting; still require manufacturer/authentication.
                    val supported = if (device.protocolId == ProtocolType.PJLINK.id) setOf(ProtocolType.PJLINK) else documented
                    EpsonProtocolResolver(protocols).resolve(device, supported, secret)
                } else {
                    val protocol = protocols.getProtocolForDevice(device)
                    device to ((protocol as? DevicePairingManager)?.pair(device, secret)
                        ?: DevicePairingState.Failed("El protocolo no permite validar la conexión"))
                }
                when (result) {
                    DevicePairingState.Paired, DevicePairingState.NotRequired -> {
                        val info = (protocols.getProtocolForDevice(selected) as? ProjectorProtocol)?.information(selected)
                        val confirmed = selected.copy(model = info?.model ?: selected.model,
                            brand = if (info?.manufacturer.equals("EPSON", true)) DeviceBrand.EPSON else selected.brand,
                            compatibilityStatus = CompatibilityStatus.VERIFIED_PROTOCOL, lastConnectedAt = System.currentTimeMillis())
                        deviceRepository.insert(confirmed)
                        mutableManualForm.update { it.copy(password = "") }
                        pendingDevice = null
                        onSaved(confirmed.id)
                    }
                    is DevicePairingState.PasswordRequired -> mutableManualForm.update { it.copy(validationError = "Se necesita una contraseña PJLink") }
                    is DevicePairingState.Failed -> mutableManualForm.update { it.copy(validationError = result.reason) }
                    else -> mutableManualForm.update { it.copy(validationError = "Completa la autenticación en el equipo") }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutableManualForm.update { it.copy(validationError = e.message ?: "No se pudo conectar") } }
            finally { mutableManualForm.update { it.copy(isTesting = false) } }
        }
    }

    fun saveManualDevice(onSaved: (String) -> Unit) {
        val form = manualForm.value
        val ip = form.ipAddress.trim()
        val port = form.portText.toIntOrNull()

        if (form.name.isBlank()) {
            mutableManualForm.update { it.copy(validationError = "El nombre no puede estar vacío") }
            return
        }

        val ipPattern = Regex("""^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$""")
        if (!ipPattern.matches(ip)) {
            mutableManualForm.update { it.copy(validationError = "Dirección IP inválida (ej. 192.168.1.50)") }
            return
        }

        if (port == null || port !in 1..65535) {
            mutableManualForm.update { it.copy(validationError = "Puerto inválido (1-65535)") }
            return
        }

        val id = "manual_${ip.replace('.', '_')}_${form.protocolType.name}"
        val device = Device(
            id = id,
            name = form.name.trim(),
            brand = form.brand,
            model = form.model.ifBlank { "Manual IP (${form.protocolType.name})" },
            deviceType = form.deviceType,
            connectionType = ConnectionType.WIFI,
            ipAddress = ip,
            port = port,
            protocolId = form.protocolType.name
        )

        pendingDevice = device
        validateAndSave(device, onSaved)
    }
}
