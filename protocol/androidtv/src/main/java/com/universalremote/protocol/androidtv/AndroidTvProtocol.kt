package com.universalremote.protocol.androidtv

import com.universalremote.core.model.*
import com.universalremote.protocol.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class AndroidTvProtocol : NetworkRemoteProtocol, DevicePairingManager {

    override val id: String = "android_tv_protocol"
    override val protocolType: ProtocolType = ProtocolType.ANDROID_TV

    private val mutableStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.NotConfigured)
    override val status: StateFlow<ConnectionStatus> = mutableStatus.asStateFlow()

    private val mutablePairingState = MutableStateFlow<DevicePairingState>(DevicePairingState.NotRequired)
    override val pairingState: StateFlow<DevicePairingState> = mutablePairingState.asStateFlow()

    private val supportedCommandsSet = setOf(
        RemoteCommand.POWER, RemoteCommand.POWER_OFF,
        RemoteCommand.VOLUME_UP, RemoteCommand.VOLUME_DOWN, RemoteCommand.MUTE,
        RemoteCommand.UP, RemoteCommand.DOWN, RemoteCommand.LEFT, RemoteCommand.RIGHT, RemoteCommand.OK,
        RemoteCommand.BACK, RemoteCommand.HOME, RemoteCommand.INPUT, RemoteCommand.MENU
    )

    override suspend fun capabilities(device: Device): DeviceCapabilities =
        DeviceCapabilities(supportedCommands = supportedCommandsSet)

    override suspend fun probeEndpoint(endpoint: NetworkEndpoint): Boolean = withContext(Dispatchers.IO) {
        val portsToProbe = listOf(endpoint.port, 6466, 8008, 8009, 8080, 5555).distinct()
        for (p in portsToProbe) {
            try {
                Socket().use { probeSocket ->
                    probeSocket.connect(InetSocketAddress(endpoint.host, p), 1200)
                    return@withContext true
                }
            } catch (_: Exception) {}
        }
        false
    }

    override suspend fun connect(endpoint: NetworkEndpoint): ConnectionStatus = withContext(Dispatchers.IO) {
        val reachable = probeEndpoint(endpoint)
        if (reachable) {
            mutablePairingState.value = DevicePairingState.PinRequired("Android TV / Google TV requiere emparejamiento con PIN de pantalla")
            mutableStatus.value = ConnectionStatus.Connected
        } else {
            mutableStatus.value = ConnectionStatus.Failed("Android TV host inalcanzable en ${endpoint.host}:${endpoint.port}")
        }
        mutableStatus.value
    }

    override suspend fun pair(device: Device, secret: String?): DevicePairingState = withContext(Dispatchers.IO) {
        val host = device.ipAddress ?: return@withContext DevicePairingState.Failed("IP no configurada")
        val port = device.port ?: 6466
        connect(NetworkEndpoint(host, port))
        mutablePairingState.value
    }

    override suspend fun cancel() {
        disconnect()
        mutablePairingState.value = DevicePairingState.Idle
    }

    override suspend fun send(device: Device, command: RemoteCommand): CommandResult = withContext(Dispatchers.IO) {
        if (command !in supportedCommandsSet) return@withContext CommandResult.Unsupported
        val host = device.ipAddress ?: return@withContext CommandResult.NotConfigured
        val port = device.port ?: 6466

        val reachable = probeEndpoint(NetworkEndpoint(host, port))
        if (!reachable) {
            return@withContext CommandResult.Failed("Android TV ($host) no responde. Verifica la IP del equipo.")
        }

        // If host is reachable on network port:
        return@withContext CommandResult.Failed("Android TV ($host): Requiere vincular PIN TLS en pantalla")
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        mutableStatus.value = ConnectionStatus.Disconnected
    }
}
