package com.universalremote.domain

import com.universalremote.core.model.*
import com.universalremote.protocol.api.*

class EpsonProtocolResolver(private val registry: ProtocolRegistry) {
    suspend fun resolve(device: Device, documented: Set<ProtocolType>, password: String?): Pair<Device, DevicePairingState> {
        val saved = ProtocolType.fromId(device.protocolId)
        val candidates = if (saved != ProtocolType.UNKNOWN) listOf(saved) else documented.toList()
        for (type in candidates) {
            if (type !in documented) continue
            val protocol = registry.getProtocol(type) as? NetworkRemoteProtocol ?: continue
            val endpoint = NetworkEndpoint(device.ipAddress ?: return device to DevicePairingState.Failed("Falta la IP"), device.port ?: type.defaultPort)
            if (!protocol.probeEndpoint(endpoint)) continue
            val selected = device.copy(protocolId = type.id, port = endpoint.port)
            val paired = (protocol as? DevicePairingManager)?.pair(selected, password)
                ?: DevicePairingState.Failed("El adaptador no permite validar el equipo")
            return selected to paired
        }
        return device to DevicePairingState.Failed("El proyector no responde con un protocolo compatible. Revisa la IP, el servicio de control y la separación entre Wi-Fi y LAN/VLAN.")
    }
}
