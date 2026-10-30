package com.universalremote.domain

import com.universalremote.core.model.Device
import com.universalremote.core.model.RemoteCommand
import com.universalremote.protocol.api.CommandResult
import com.universalremote.protocol.api.ProtocolRegistry

class SendNetworkCommand(
    private val protocolRegistry: ProtocolRegistry
) {
    suspend operator fun invoke(device: Device, command: RemoteCommand): CommandResult {
        val protocol = protocolRegistry.getProtocolForDevice(device)
            ?: return CommandResult.Failed("Protocolo de red no registrado para esta marca/dispositivo")
        return protocol.send(device, command)
    }
}
