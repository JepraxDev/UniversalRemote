package com.universalremote.protocol.api

import com.universalremote.core.model.*
import com.universalremote.core.network.NetworkEndpoint
import kotlinx.coroutines.flow.StateFlow

sealed interface CommandResult {
    data object Sent : CommandResult
    data object Unsupported : CommandResult
    data object NotConfigured : CommandResult
    data class Failed(val reason: String) : CommandResult
}
interface RemoteProtocol {
    val id: String
    val status: StateFlow<ConnectionStatus>
    suspend fun capabilities(device: Device): DeviceCapabilities
    suspend fun send(device: Device, command: RemoteCommand): CommandResult
    suspend fun disconnect()
}
interface NetworkRemoteProtocol : RemoteProtocol {
    suspend fun connect(endpoint: NetworkEndpoint): ConnectionStatus
}
