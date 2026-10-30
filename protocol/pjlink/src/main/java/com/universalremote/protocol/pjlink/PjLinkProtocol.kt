package com.universalremote.protocol.pjlink

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
import java.security.MessageDigest

class PjLinkProtocol(
    private val passwordStorage: (deviceId: String) -> String? = { null }
) : NetworkRemoteProtocol, DevicePairingManager {

    override val id: String = "pjlink_protocol"
    override val protocolType: ProtocolType = ProtocolType.PJLINK

    private val mutableStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.NotConfigured)
    override val status: StateFlow<ConnectionStatus> = mutableStatus.asStateFlow()

    private val mutablePairingState = MutableStateFlow<DevicePairingState>(DevicePairingState.NotRequired)
    override val pairingState: StateFlow<DevicePairingState> = mutablePairingState.asStateFlow()

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var randomDigest: String? = null
    private var requiresAuth: Boolean = false

    private val supportedCommandsSet = setOf(
        RemoteCommand.POWER, RemoteCommand.POWER_ON, RemoteCommand.POWER_OFF,
        RemoteCommand.INPUT, RemoteCommand.HDMI, RemoteCommand.HDMI_1, RemoteCommand.HDMI_2, RemoteCommand.VGA,
        RemoteCommand.AV_MUTE, RemoteCommand.MUTE
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
            val inStream = sock.getInputStream()
            val outStream = sock.getOutputStream()
            inputStream = inStream
            outputStream = outStream

            val greetingBuffer = ByteArray(128)
            val read = inStream.read(greetingBuffer)
            if (read <= 0) {
                mutableStatus.value = ConnectionStatus.Failed("No greeting received")
                return@withContext mutableStatus.value
            }

            val greeting = String(greetingBuffer, 0, read, Charsets.US_ASCII).trim()
            if (!greeting.startsWith("PJLINK")) {
                mutableStatus.value = ConnectionStatus.Failed("Invalid PJLink greeting: $greeting")
                return@withContext mutableStatus.value
            }

            val parts = greeting.split(" ")
            if (parts.size >= 3 && parts[1] == "1") {
                requiresAuth = true
                randomDigest = parts[2]
                mutablePairingState.value = DevicePairingState.PasswordRequired()
            } else {
                requiresAuth = false
                randomDigest = null
                mutablePairingState.value = DevicePairingState.NotRequired
            }

            mutableStatus.value = ConnectionStatus.Connected
            mutableStatus.value
        } catch (e: Exception) {
            mutableStatus.value = ConnectionStatus.Failed(e.message ?: "PJLink connection failed")
            mutableStatus.value
        }
    }

    override suspend fun pair(device: Device, secret: String?): DevicePairingState = withContext(Dispatchers.IO) {
        val host = device.ipAddress ?: return@withContext DevicePairingState.Failed("IP missing")
        val port = device.port ?: 4352

        val conn = connect(NetworkEndpoint(host, port))
        if (conn !is ConnectionStatus.Connected) {
            return@withContext DevicePairingState.Failed("Could not connect to projector")
        }

        if (!requiresAuth) {
            mutablePairingState.value = DevicePairingState.Paired
            return@withContext DevicePairingState.Paired
        }

        val password = secret ?: passwordStorage(device.id)
        if (password.isNullOrBlank()) {
            mutablePairingState.value = DevicePairingState.PasswordRequired()
            return@withContext mutablePairingState.value
        }

        val result = sendCommandRaw("POWR ?", password)
        if (result.startsWith("%1POWR=") || result.contains("OK")) {
            mutablePairingState.value = DevicePairingState.Paired
            DevicePairingState.Paired
        } else if (result.contains("ERRA")) {
            mutablePairingState.value = DevicePairingState.Failed("Incorrect PJLink password")
            DevicePairingState.Failed("Incorrect PJLink password")
        } else {
            mutablePairingState.value = DevicePairingState.Failed("Auth response: $result")
            DevicePairingState.Failed("Auth response: $result")
        }
    }

    override suspend fun cancel() {
        disconnect()
        mutablePairingState.value = DevicePairingState.Idle
    }

    override suspend fun send(device: Device, command: RemoteCommand): CommandResult = withContext(Dispatchers.IO) {
        if (command !in supportedCommandsSet) return@withContext CommandResult.Unsupported

        val host = device.ipAddress ?: return@withContext CommandResult.NotConfigured
        val port = device.port ?: 4352
        val password = passwordStorage(device.id)

        if (status.value !is ConnectionStatus.Connected) {
            val conn = connect(NetworkEndpoint(host, port))
            if (conn !is ConnectionStatus.Connected) {
                return@withContext CommandResult.Failed("Connection error")
            }
        }

        val pjCmd = when (command) {
            RemoteCommand.POWER, RemoteCommand.POWER_OFF -> "POWR 0"
            RemoteCommand.POWER_ON -> "POWR 1"
            RemoteCommand.INPUT, RemoteCommand.HDMI, RemoteCommand.HDMI_1 -> "INPT 31"
            RemoteCommand.HDMI_2 -> "INPT 32"
            RemoteCommand.VGA -> "INPT 11"
            RemoteCommand.AV_MUTE, RemoteCommand.MUTE -> "MUTE 31"
            else -> return@withContext CommandResult.Unsupported
        }

        val resp = sendCommandRaw(pjCmd, password)
        return@withContext when {
            resp.contains("OK") || resp.startsWith("%1") -> CommandResult.Sent
            resp.contains("ERR1") -> CommandResult.Unsupported
            resp.contains("ERR2") -> CommandResult.Failed("Parameter Error (ERR2)")
            resp.contains("ERR3") -> CommandResult.Failed("Projector Busy (ERR3)")
            resp.contains("ERR4") -> CommandResult.Failed("Projector Hardware Error (ERR4)")
            resp.contains("ERRA") -> CommandResult.Failed("Authentication Failed (ERRA)")
            else -> CommandResult.Failed("Response: $resp")
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

    private fun sendCommandRaw(command: String, password: String?): String {
        val out = outputStream ?: throw IllegalStateException("No output stream")
        val inStream = inputStream ?: throw IllegalStateException("No input stream")

        val prefix = if (requiresAuth && !randomDigest.isNullOrEmpty() && !password.isNullOrEmpty()) {
            calculateDigestHex(randomDigest!!, password)
        } else {
            ""
        }

        val fullMsg = "$prefix%1$command\r"
        out.write(fullMsg.toByteArray(Charsets.US_ASCII))
        out.flush()

        val respBuf = ByteArray(128)
        val read = inStream.read(respBuf)
        return if (read > 0) String(respBuf, 0, read, Charsets.US_ASCII).trim() else ""
    }

    private fun calculateDigestHex(digestHex: String, password: String): String {
        val md = MessageDigest.getInstance("MD5")
        val combined = "$digestHex$password"
        val hash = md.digest(combined.toByteArray(Charsets.US_ASCII))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
