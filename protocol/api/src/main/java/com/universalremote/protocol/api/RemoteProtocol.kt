package com.universalremote.protocol.api

import com.universalremote.core.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface CommandResult {
    data object Sent : CommandResult
    data object Unsupported : CommandResult
    data object NotConfigured : CommandResult
    data class Failed(val reason: String) : CommandResult
}

sealed interface DevicePairingState {
    data object NotRequired : DevicePairingState
    data object Idle : DevicePairingState
    data object PromptingOnDevice : DevicePairingState
    data class PinRequired(val promptMessage: String = "Ingrese el PIN mostrado en la pantalla") : DevicePairingState
    data class PasswordRequired(val promptMessage: String = "Ingrese la contraseña de PJLink") : DevicePairingState
    data object Paired : DevicePairingState
    data class Failed(val reason: String) : DevicePairingState
}

interface DevicePairingManager {
    val pairingState: StateFlow<DevicePairingState>
    suspend fun pair(device: Device, secret: String? = null): DevicePairingState
    suspend fun cancel()
}

data class ProtocolMetadata(
    val adapterId: String,
    val protocolType: ProtocolType,
    val name: String,
    val supportedDeviceTypes: Set<DeviceType>,
    val modelFamilies: List<String>,
    val supportedConnectionTypes: Set<ConnectionType>,
    val availableCommands: Set<RemoteCommand>,
    val requiresAuthentication: Boolean,
    val compatibilityStatus: CompatibilityStatus
)

interface DeviceProtocolAdapter {
    val metadata: ProtocolMetadata
    val remoteProtocol: RemoteProtocol
}

interface RemoteProtocol {
    val id: String
    val protocolType: ProtocolType
    val status: StateFlow<ConnectionStatus>
    val pairingState: StateFlow<DevicePairingState> get() = MutableStateFlow(DevicePairingState.NotRequired)
    suspend fun capabilities(device: Device): DeviceCapabilities
    suspend fun send(device: Device, command: RemoteCommand): CommandResult
    suspend fun disconnect()
}

interface NetworkRemoteProtocol : RemoteProtocol {
    suspend fun connect(endpoint: NetworkEndpoint): ConnectionStatus
    suspend fun probeEndpoint(endpoint: NetworkEndpoint): Boolean
}

data class ProjectorInfo(
    val manufacturer: String, val model: String, val name: String, val pjClass: String,
    val power: String, val inputs: List<String>, val errors: String, val lamps: String,
    val inputNames: Map<String, String> = emptyMap()
)

interface ProjectorProtocol : RemoteProtocol {
    suspend fun information(device: Device): ProjectorInfo
    suspend fun selectInput(device: Device, input: String): CommandResult
}

interface ProtocolRegistry {
    fun getProtocol(protocolType: ProtocolType): RemoteProtocol?
    fun getProtocolForDevice(device: Device): RemoteProtocol?
    fun getAllAdapters(): List<DeviceProtocolAdapter> = emptyList()
}
