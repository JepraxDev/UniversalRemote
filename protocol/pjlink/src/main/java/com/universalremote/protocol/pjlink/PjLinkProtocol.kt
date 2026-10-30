package com.universalremote.protocol.pjlink

import com.universalremote.core.model.*
import com.universalremote.core.network.PjLinkClient
import com.universalremote.core.network.PjLinkException
import com.universalremote.protocol.api.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PjLinkProtocol(
    private val passwordStorage: (String) -> String? = { null },
    private val passwordSaver: (String, String) -> Unit = { _, _ -> },
    private val client: PjLinkClient = PjLinkClient()
) : NetworkRemoteProtocol, DevicePairingManager, ProjectorProtocol {
    override val id = "pjlink_protocol"
    override val protocolType = ProtocolType.PJLINK
    private val mutableStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.NotConfigured)
    override val status = mutableStatus.asStateFlow()
    private val pairing = MutableStateFlow<DevicePairingState>(DevicePairingState.NotRequired)
    override val pairingState = pairing.asStateFlow()
    private val gate = Mutex()
    private val inputs = java.util.concurrent.ConcurrentHashMap<String, Set<String>>()
    private val classes = java.util.concurrent.ConcurrentHashMap<String, Int>()
    private val freezeSupported = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    override suspend fun probeEndpoint(endpoint: NetworkEndpoint): Boolean = try {
        client.greeting(endpoint.host, endpoint.port)
    } catch (e: CancellationException) { throw e } catch (_: Exception) { false }

    override suspend fun connect(endpoint: NetworkEndpoint): ConnectionStatus {
        mutableStatus.value = ConnectionStatus.Connecting
        mutableStatus.value = try {
            val power = client.query(endpoint.host, endpoint.port, "POWR")
            check(power in setOf("0", "1", "2", "3")) { "Estado PJLink inválido" }
            ConnectionStatus.Connected
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            if (e is PjLinkException && e.code == "PASSWORD") pairing.value = DevicePairingState.PasswordRequired()
            ConnectionStatus.Failed(message(e))
        }
        return status.value
    }

    override suspend fun pair(device: Device, secret: String?): DevicePairingState = gate.withLock {
        mutableStatus.value = ConnectionStatus.Connecting
        try {
            val password = secret ?: passwordStorage(device.id)
            val host = device.ipAddress ?: error("Falta la dirección IP")
            val port = device.port ?: 4352
            val power = client.query(host, port, "POWR", password)
            check(power in setOf("0", "1", "2", "3")) { "Estado PJLink inválido" }
            val manufacturer = client.query(host, port, "INF1", password)
            check(device.brand != DeviceBrand.EPSON || manufacturer.equals("EPSON", true)) {
                "El servicio PJLink no identifica un fabricante Epson"
            }
            val pjClass = client.query(host, port, "CLSS", password)
            check(pjClass in setOf("1", "2")) { "Clase PJLink no compatible" }
            classes[key(device)] = pjClass.toInt()
            inputs[key(device)] = optional { client.query(host, port, "INST", password, pjClass.toInt()) }.split(' ')
                .filter { it.matches(Regex(if (pjClass == "2") "[1-6][1-9A-Z]" else "[1-5][1-9]")) }.toSet()
            freezeSupported[key(device)] = pjClass == "2" && optional { client.query(host, port, "FREZ", password, 2) } in setOf("0", "1")
            if (secret != null) passwordSaver(device.id, secret)
            pairing.value = DevicePairingState.Paired
            mutableStatus.value = ConnectionStatus.Connected
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            pairing.value = if (e is PjLinkException && e.code == "PASSWORD") DevicePairingState.PasswordRequired()
                else DevicePairingState.Failed(message(e))
            mutableStatus.value = ConnectionStatus.Failed(message(e))
        }
        pairing.value
    }

    override suspend fun capabilities(device: Device): DeviceCapabilities {
        return DeviceCapabilities(buildSet {
            addAll(setOf(RemoteCommand.POWER, RemoteCommand.POWER_ON, RemoteCommand.POWER_OFF,
                RemoteCommand.AV_MUTE, RemoteCommand.MUTE))
            if (freezeSupported[key(device)] == true) add(RemoteCommand.FREEZE)
        })
    }

    override suspend fun send(device: Device, command: RemoteCommand): CommandResult = gate.withLock {
        val host = device.ipAddress ?: return@withLock CommandResult.NotConfigured
        val port = device.port ?: 4352
        try {
            val password = passwordStorage(device.id)
            suspend fun query(name: String) = client.query(host, port, name, password)
            val pair = when (command) {
                RemoteCommand.POWER_ON -> "POWR" to "1"
                RemoteCommand.POWER_OFF -> "POWR" to "0"
                RemoteCommand.POWER -> "POWR" to when (query("POWR")) {
                    "0" -> "1"; "1" -> "0"; else -> return@withLock CommandResult.Failed("El proyector está calentando o enfriando")
                }
                RemoteCommand.AV_MUTE -> "AVMT" to when (query("AVMT")) {
                    "11", "21", "31" -> "30"; "10", "20", "30" -> "31"
                    else -> return@withLock CommandResult.Failed("Estado A/V Mute desconocido")
                }
                RemoteCommand.MUTE -> "AVMT" to when (query("AVMT")) {
                    "21", "31" -> "20"; "11", "30" -> "21"
                    else -> return@withLock CommandResult.Failed("Estado de silencio desconocido")
                }
                RemoteCommand.FREEZE -> {
                    if (freezeSupported[key(device)] != true) return@withLock CommandResult.Unsupported
                    "FREZ" to when (client.query(host, port, "FREZ", password, 2)) {
                        "0" -> "1"; "1" -> "0"; else -> return@withLock CommandResult.Failed("Estado Freeze desconocido")
                    }
                }
                else -> {
                    return@withLock CommandResult.Unsupported
                }
            }
            val response = client.request(host, port, pair.first, pair.second, password, if (pair.first == "FREZ") 2 else 1)
            if (response != "OK") return@withLock CommandResult.Failed("El proyector no confirmó el comando")
            mutableStatus.value = ConnectionStatus.Connected
            CommandResult.Sent
        } catch (e: CancellationException) { throw e } catch (e: Exception) {
            mutableStatus.value = ConnectionStatus.Failed(message(e))
            if (e is PjLinkException && e.code == "ERR1") CommandResult.Unsupported else CommandResult.Failed(message(e))
        }
    }

    override suspend fun disconnect() { mutableStatus.value = ConnectionStatus.Disconnected }
    override suspend fun information(device: Device): ProjectorInfo = gate.withLock {
        val host = device.ipAddress ?: error("Falta la IP")
        val password = passwordStorage(device.id)
        suspend fun query(command: String) = client.query(host, device.port ?: 4352, command, password)
        val pjClass = query("CLSS").toInt()
        check(pjClass in 1..2)
        val sourceList = optional { client.query(host, device.port ?: 4352, "INST", password, pjClass) }
            .split(' ').filter { it.matches(Regex(if (pjClass == 2) "[1-6][1-9A-Z]" else "[1-5][1-9]")) }
        val names = if (pjClass == 2) sourceList.associateWith {
            optional { client.request(host, device.port ?: 4352, "INNM", "?$it", password, 2) }
        }.filterValues { it.isNotBlank() } else emptyMap()
        ProjectorInfo(query("INF1"), query("INF2"), optional { query("NAME") }, pjClass.toString(), query("POWR"),
            sourceList, optional { query("ERST") }.ifEmpty { "No disponible" }, optional { query("LAMP") }.ifEmpty { "No disponible" }, names)
    }

    override suspend fun selectInput(device: Device, input: String): CommandResult = gate.withLock {
        val host = device.ipAddress ?: return@withLock CommandResult.NotConfigured
        try {
            val password = passwordStorage(device.id)
            val port = device.port ?: 4352
            val pjClass = client.query(host, port, "CLSS", password).toInt()
            if (input !in client.query(host, port, "INST", password, pjClass).split(' ')) return@withLock CommandResult.Unsupported
            if (client.request(host, port, "INPT", input, password, pjClass) == "OK") CommandResult.Sent
            else CommandResult.Failed("El proyector no confirmó la entrada")
        } catch (e: CancellationException) { throw e } catch (e: Exception) { CommandResult.Failed(message(e)) }
    }
    override suspend fun cancel() { disconnect(); pairing.value = DevicePairingState.Idle }
    private fun key(device: Device) = "${device.id}:${device.ipAddress}:${device.port}"
    private suspend fun optional(block: suspend () -> String): String = try { block() }
    catch (e: PjLinkException) { if (e.code in setOf("ERR1", "ERR2", "ERR3", "ERR4")) "" else throw e }
    private fun message(e: Exception) = when (e) {
        is java.net.SocketTimeoutException -> "El proyector no responde. Comprueba LAN, servicio de control y aislamiento/VLAN."
        is java.net.ConnectException -> "No se pudo abrir el puerto de control. Comprueba la IP y el servicio."
        else -> e.message ?: "No se pudo comunicar con el proyector"
    }
}
