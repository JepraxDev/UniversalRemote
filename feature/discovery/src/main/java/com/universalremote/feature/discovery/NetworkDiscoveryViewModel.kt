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

data class ManualIpFormState(
    val name: String = "Televisor Wi-Fi",
    val deviceType: DeviceType = DeviceType.TV,
    val brand: DeviceBrand = DeviceBrand.LG,
    val ipAddress: String = "",
    val portText: String = "3000",
    val protocolType: ProtocolType = ProtocolType.LG_WEBOS,
    val validationError: String? = null,
    val isTesting: Boolean = false
)

@HiltViewModel
class NetworkDiscoveryViewModel @Inject constructor(
    private val discovery: NetworkDeviceDiscovery,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    val discoveryState: StateFlow<DiscoveryState> = discovery.state

    private val mutableManualForm = MutableStateFlow(ManualIpFormState())
    val manualForm: StateFlow<ManualIpFormState> = mutableManualForm.asStateFlow()

    init {
        startDiscovery()
    }

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
            DeviceBrand.EPSON, DeviceBrand.BENQ, DeviceBrand.VIEWSONIC, DeviceBrand.OPTOMA -> ProtocolType.PJLINK
            else -> ProtocolType.LG_WEBOS
        }
        mutableManualForm.update {
            it.copy(
                brand = brand,
                protocolType = defaultProtocol,
                portText = defaultProtocol.defaultPort.toString()
            )
        }
    }
    fun updateManualType(type: DeviceType) { mutableManualForm.update { it.copy(deviceType = type) } }
    fun updateManualProtocol(protocol: ProtocolType) {
        mutableManualForm.update {
            it.copy(
                protocolType = protocol,
                portText = protocol.defaultPort.toString()
            )
        }
    }

    fun saveDiscoveredDevice(device: Device, onSaved: (String) -> Unit) {
        viewModelScope.launch {
            deviceRepository.insert(device)
            onSaved(device.id)
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
            model = "Manual IP (${form.protocolType.name})",
            deviceType = form.deviceType,
            connectionType = ConnectionType.WIFI,
            ipAddress = ip,
            port = port,
            protocolId = form.protocolType.name
        )

        viewModelScope.launch {
            deviceRepository.insert(device)
            onSaved(device.id)
        }
    }
}
