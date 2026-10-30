package com.universalremote.protocol.samsung

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

class SamsungWsProtocol(
    private val tokenStorage: (deviceId: String) -> String? = { null },
    private val tokenSaver: (deviceId: String, token: String) -> Unit = { _, _ -> }
) : NetworkRemoteProtocol, DevicePairingManager {

    override val id: String = "samsung_ws_protocol"
    override val protocolType: ProtocolType = ProtocolType.SAMSUNG_WS

    private val mutableStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.NotConfigured)
    override val status: StateFlow<ConnectionStatus> = mutableStatus.asStateFlow()

    private val mutablePairingState = MutableStateFlow<DevicePairingState>(DevicePairingState.NotRequired)
    override val pairingState: StateFlow<DevicePairingState> = mutablePairingState.asStateFlow()

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

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

            val deviceId = "samsung_${endpoint.host.replace('.', '_')}"
            val existingToken = tokenStorage(deviceId)

            val success = performHandshake(endpoint.host, endpoint.port, existingToken)
            if (!success) {
                mutableStatus.value = ConnectionStatus.Failed("Samsung WS Handshake Failed")
                return@withContext mutableStatus.value
            }

            mutableStatus.value = ConnectionStatus.Connected
            mutableStatus.value
        } catch (e: Exception) {
            mutableStatus.value = ConnectionStatus.Failed(e.message ?: "Connection Error")
            mutableStatus.value
        }
    }

    override suspend fun pair(device: Device, secret: String?): DevicePairingState = withContext(Dispatchers.IO) {
        val host = device.ipAddress ?: return@withContext DevicePairingState.Failed("IP missing")
        val port = device.port ?: 8001

        mutablePairingState.value = DevicePairingState.PromptingOnDevice
        val conn = connect(NetworkEndpoint(host, port))

        if (conn is ConnectionStatus.Connected) {
            mutablePairingState.value = DevicePairingState.Paired
            DevicePairingState.Paired
        } else {
            mutablePairingState.value = DevicePairingState.Failed("Pairing rejected on TV")
            DevicePairingState.Failed("Pairing rejected on TV")
        }
    }

    override suspend fun cancel() {
        disconnect()
        mutablePairingState.value = DevicePairingState.Idle
    }

    override suspend fun send(device: Device, command: RemoteCommand): CommandResult = withContext(Dispatchers.IO) {
        if (command !in supportedCommandsSet) return@withContext CommandResult.Unsupported

        val host = device.ipAddress ?: return@withContext CommandResult.NotConfigured
        val port = device.port ?: 8001

        if (status.value !is ConnectionStatus.Connected) {
            val conn = connect(NetworkEndpoint(host, port))
            if (conn !is ConnectionStatus.Connected) {
                return@withContext CommandResult.Failed("Connection failed")
            }
        }

        val samsungKey = when (command) {
            RemoteCommand.POWER, RemoteCommand.POWER_OFF -> "KEY_POWER"
            RemoteCommand.VOLUME_UP -> "KEY_VOLUP"
            RemoteCommand.VOLUME_DOWN -> "KEY_VOLDOWN"
            RemoteCommand.MUTE -> "KEY_MUTE"
            RemoteCommand.CHANNEL_UP -> "KEY_CHUP"
            RemoteCommand.CHANNEL_DOWN -> "KEY_CHDOWN"
            RemoteCommand.UP -> "KEY_UP"
            RemoteCommand.DOWN -> "KEY_DOWN"
            RemoteCommand.LEFT -> "KEY_LEFT"
            RemoteCommand.RIGHT -> "KEY_RIGHT"
            RemoteCommand.OK -> "KEY_ENTER"
            RemoteCommand.BACK -> "KEY_RETURN"
            RemoteCommand.HOME -> "KEY_HOME"
            RemoteCommand.INPUT -> "KEY_SOURCE"
            RemoteCommand.MENU -> "KEY_MENU"
            else -> return@withContext CommandResult.Unsupported
        }

        val payload = """
            {
                "method": "ms.remote.control",
                "params": {
                    "Cmd": "Click",
                    "DataOfCmd": "$samsungKey",
                    "TypeOfRemote": "SendRemoteKey",
                    "TypeOfCmd": "RemoteKey"
                }
            }
        """.trimIndent()

        return@withContext try {
            sendWsFrame(payload)
            CommandResult.Sent
        } catch (e: Exception) {
            CommandResult.Failed(e.message ?: "Failed to transmit key")
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

    private fun performHandshake(host: String, port: Int, token: String?): Boolean {
        val out = outputStream ?: return false
        val inStream = inputStream ?: return false

        val appNameBase64 = Base64.getEncoder().encodeToString("Universal Remote".toByteArray(Charsets.UTF_8))
        val tokenParam = if (!token.isNullOrBlank()) "&token=$token" else ""
        val path = "/api/v2/channels/com.samsung.tv.remote?name=$appNameBase64$tokenParam"

        val nonce = ByteArray(16)
        SecureRandom().nextBytes(nonce)
        val secKey = Base64.getEncoder().encodeToString(nonce)

        val request = "GET $path HTTP/1.1\r\n" +
                "Host: $host:$port\r\n" +
                "Upgrade: websocket\r\n" +
                "Connection: Upgrade\r\n" +
                "Sec-WebSocket-Key: $secKey\r\n" +
                "Sec-WebSocket-Version: 13\r\n\r\n"

        out.write(request.toByteArray(Charsets.UTF_8))
        out.flush()

        val buffer = ByteArray(2048)
        val read = inStream.read(buffer)
        if (read <= 0) return false

        val response = String(buffer, 0, read, Charsets.UTF_8)
        return response.contains("101")
    }

    private fun sendWsFrame(text: String) {
        val out = outputStream ?: throw IllegalStateException("Socket stream unavailable")
        val bytes = text.toByteArray(Charsets.UTF_8)

        val maskKey = ByteArray(4)
        SecureRandom().nextBytes(maskKey)

        val frame = mutableListOf<Byte>()
        frame.add(0x81.toByte())

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
}
