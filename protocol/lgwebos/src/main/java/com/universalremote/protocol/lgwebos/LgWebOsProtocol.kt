package com.universalremote.protocol.lgwebos

import com.universalremote.core.model.*
import com.universalremote.protocol.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger

class LgWebOsProtocol(
    private val tokenStorage: (deviceId: String) -> String? = { null },
    private val tokenSaver: (deviceId: String, token: String) -> Unit = { _, _ -> }
) : NetworkRemoteProtocol, DevicePairingManager {

    override val id: String = "lg_webos_protocol"
    override val protocolType: ProtocolType = ProtocolType.LG_WEBOS

    private val mutableStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.NotConfigured)
    override val status: StateFlow<ConnectionStatus> = mutableStatus.asStateFlow()

    private val mutablePairingState = MutableStateFlow<DevicePairingState>(DevicePairingState.NotRequired)
    override val pairingState: StateFlow<DevicePairingState> = mutablePairingState.asStateFlow()

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private val requestId = AtomicInteger(1)
    private var currentDeviceId: String? = null
    private var clientKey: String? = null

    private val supportedCommandsSet = setOf(
        RemoteCommand.POWER, RemoteCommand.POWER_OFF,
        RemoteCommand.VOLUME_UP, RemoteCommand.VOLUME_DOWN, RemoteCommand.MUTE,
        RemoteCommand.CHANNEL_UP, RemoteCommand.CHANNEL_DOWN,
        RemoteCommand.UP, RemoteCommand.DOWN, RemoteCommand.LEFT, RemoteCommand.RIGHT, RemoteCommand.OK,
        RemoteCommand.BACK, RemoteCommand.HOME, RemoteCommand.INPUT, RemoteCommand.MENU
    )

    override suspend fun capabilities(device: Device): DeviceCapabilities =
        DeviceCapabilities(supportedCommands = supportedCommandsSet)

    override suspend fun probeEndpoint(endpoint: NetworkEndpoint): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket().use { probeSocket ->
                probeSocket.connect(InetSocketAddress(endpoint.host, endpoint.port), 2000)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun connect(endpoint: NetworkEndpoint): ConnectionStatus = withContext(Dispatchers.IO) {
        disconnect()
        mutableStatus.value = ConnectionStatus.Connecting

        try {
            val sock = Socket()
            sock.connect(InetSocketAddress(endpoint.host, endpoint.port), 3000)
            sock.soTimeout = 4000

            socket = sock
            inputStream = sock.getInputStream()
            outputStream = sock.getOutputStream()

            val success = performWebSocketHandshake(endpoint.host, endpoint.port)
            if (!success) {
                mutableStatus.value = ConnectionStatus.Failed("WebSocket Handshake Failed")
                return@withContext mutableStatus.value
            }

            val deviceId = currentDeviceId ?: "lg_${endpoint.host.replace('.', '_')}"
            val existingToken = tokenStorage(deviceId)

            sendRegistrationRequest(existingToken)
            mutableStatus.value = ConnectionStatus.Connected
            mutableStatus.value
        } catch (e: Exception) {
            mutableStatus.value = ConnectionStatus.Failed(e.message ?: "Connection error")
            mutableStatus.value
        }
    }

    override suspend fun pair(device: Device, secret: String?): DevicePairingState = withContext(Dispatchers.IO) {
        currentDeviceId = device.id
        clientKey = secret ?: tokenStorage(device.id)

        val host = device.ipAddress
        val port = device.port ?: 3000

        if (host.isNullOrBlank()) {
            mutablePairingState.value = DevicePairingState.Failed("IP address missing")
            return@withContext mutablePairingState.value
        }

        mutablePairingState.value = DevicePairingState.PromptingOnDevice
        val connectStatus = connect(NetworkEndpoint(host, port))

        if (connectStatus is ConnectionStatus.Connected) {
            mutablePairingState.value = DevicePairingState.Paired
        } else if (connectStatus is ConnectionStatus.Failed) {
            mutablePairingState.value = DevicePairingState.Failed(connectStatus.reason)
        }
        mutablePairingState.value
    }

    override suspend fun cancel() {
        disconnect()
        mutablePairingState.value = DevicePairingState.Idle
    }

    override suspend fun send(device: Device, command: RemoteCommand): CommandResult = withContext(Dispatchers.IO) {
        if (command !in supportedCommandsSet) return@withContext CommandResult.Unsupported

        if (status.value !is ConnectionStatus.Connected) {
            val host = device.ipAddress ?: return@withContext CommandResult.NotConfigured
            val port = device.port ?: 3000
            currentDeviceId = device.id
            val conn = connect(NetworkEndpoint(host, port))
            if (conn !is ConnectionStatus.Connected) {
                return@withContext CommandResult.Failed("Disconnected")
            }
        }

        val ssapUri = when (command) {
            RemoteCommand.POWER, RemoteCommand.POWER_OFF -> "ssap://system/turnOff"
            RemoteCommand.VOLUME_UP -> "ssap://audio/volumeUp"
            RemoteCommand.VOLUME_DOWN -> "ssap://audio/volumeDown"
            RemoteCommand.MUTE -> "ssap://audio/setMuted"
            RemoteCommand.CHANNEL_UP -> "ssap://tv/channelUp"
            RemoteCommand.CHANNEL_DOWN -> "ssap://tv/channelDown"
            RemoteCommand.UP -> "ssap://navigation/up"
            RemoteCommand.DOWN -> "ssap://navigation/down"
            RemoteCommand.LEFT -> "ssap://navigation/left"
            RemoteCommand.RIGHT -> "ssap://navigation/right"
            RemoteCommand.OK -> "ssap://navigation/ok"
            RemoteCommand.BACK -> "ssap://navigation/back"
            RemoteCommand.HOME -> "ssap://system.launcher/open"
            RemoteCommand.INPUT -> "ssap://tv/switchInput"
            RemoteCommand.MENU -> "ssap://system.launcher/open"
            else -> return@withContext CommandResult.Unsupported
        }

        val reqId = "req_${requestId.getAndIncrement()}"
        val message = """{"type":"request","id":"$reqId","uri":"$ssapUri"}"""

        return@withContext try {
            sendWsFrame(message)
            CommandResult.Sent
        } catch (e: Exception) {
            mutableStatus.value = ConnectionStatus.Failed(e.message ?: "Transmit error")
            CommandResult.Failed(e.message ?: "Failed to transmit")
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try { inputStream?.close() } catch (_: Exception) {}
        try { outputStream?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        inputStream = null
        outputStream = null
        mutableStatus.value = ConnectionStatus.Disconnected
    }

    private fun performWebSocketHandshake(host: String, port: Int): Boolean {
        val out = outputStream ?: return false
        val inStream = inputStream ?: return false

        val nonce = ByteArray(16)
        SecureRandom().nextBytes(nonce)
        val secKey = Base64.getEncoder().encodeToString(nonce)

        val handshake = "GET / HTTP/1.1\r\n" +
                "Host: $host:$port\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Key: $secKey\r\n" +
                "Sec-WebSocket-Version: 13\r\n\r\n"

        out.write(handshake.toByteArray(Charsets.UTF_8))
        out.flush()

        val buffer = ByteArray(1024)
        val read = inStream.read(buffer)
        if (read <= 0) return false

        val response = String(buffer, 0, read, Charsets.UTF_8)
        return response.contains("101") && response.lowercase().contains("upgrade")
    }

    private fun sendRegistrationRequest(token: String?) {
        val keyJson = if (token.isNullOrBlank()) "" else """"client-key":"$token","""
        val payload = """
            {
                "type": "register",
                "id": "register_0",
                "payload": {
                    $keyJson
                    "forcePairing": false,
                    "pairingType": "PROMPT",
                    "manifest": {
                        "manifestVersion": 1,
                        "appVersion": "0.4.0",
                        "signed": {
                            "appId": "com.universalremote.app",
                            "vendorId": "com.universalremote",
                            "localizedAppNames": {"": "Universal Remote"},
                            "permissions": ["LAUNCH", "CONTROL_AUDIO", "CONTROL_POWER"]
                        }
                    }
                }
            }
        """.trimIndent()

        sendWsFrame(payload)
    }

    private fun sendWsFrame(text: String) {
        val out = outputStream ?: throw IllegalStateException("Socket stream not available")
        val bytes = text.toByteArray(Charsets.UTF_8)

        val maskKey = ByteArray(4)
        SecureRandom().nextBytes(maskKey)

        val frame = mutableListOf<Byte>()
        frame.add(0x81.toByte()) // FIN bit + Text opcode

        val len = bytes.size
        when {
            len <= 125 -> frame.add((0x80 or len).toByte())
            len <= 65535 -> {
                frame.add((0x80 or 126).toByte())
                frame.add(((len shr 8) and 0xFF).toByte())
                frame.add((len and 0xFF).toByte())
            }
            else -> {
                frame.add((0x80 or 127).toByte())
                for (i in 7 downTo 0) {
                    frame.add(((len.toLong() shr (i * 8)) and 0xFF).toByte())
                }
            }
        }

        maskKey.forEach { frame.add(it) }

        for (i in bytes.indices) {
            frame.add((bytes[i].toInt() xor maskKey[i % 4].toInt()).toByte())
        }

        out.write(frame.toByteArray())
        out.flush()
    }

    private fun String?.isNullByBlank(): Boolean = this.isNullOrBlank()
}
