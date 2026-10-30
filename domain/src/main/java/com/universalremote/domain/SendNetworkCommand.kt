package com.universalremote.domain

import com.universalremote.core.model.Device
import com.universalremote.core.model.RemoteCommand
import com.universalremote.protocol.api.CommandResult
import com.universalremote.protocol.api.ProtocolRegistry
import com.universalremote.protocol.api.*
import com.universalremote.core.model.DeviceCapabilities

class SendNetworkCommand(
    private val protocolRegistry: ProtocolRegistry
) {
    suspend fun connectProjector(device: Device): DevicePairingState =
        (protocolRegistry.getProtocolForDevice(device) as? DevicePairingManager)?.pair(device)
            ?: DevicePairingState.Failed("Protocolo no configurado")
    suspend fun projectorCapabilities(device: Device): DeviceCapabilities =
        protocolRegistry.getProtocolForDevice(device)?.capabilities(device) ?: DeviceCapabilities()
    suspend fun information(device: Device): ProjectorInfo? =
        (protocolRegistry.getProtocolForDevice(device) as? ProjectorProtocol)?.information(device)
    suspend fun selectInput(device: Device, input: String): CommandResult =
        (protocolRegistry.getProtocolForDevice(device) as? ProjectorProtocol)?.selectInput(device, input) ?: CommandResult.Unsupported
    suspend operator fun invoke(device: Device, command: RemoteCommand): CommandResult {
        val protocol = protocolRegistry.getProtocolForDevice(device)
            ?: return CommandResult.Failed("Protocolo de red no registrado para esta marca/dispositivo")
        return protocol.send(device, command)
    }
}
