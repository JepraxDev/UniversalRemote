package com.universalremote.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.universalremote.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.Inet4Address

object LocalSubnet {
    fun hosts(address: String, prefix: Int): List<String> {
        require(prefix in 22..30) { "Subred demasiado grande o sin hosts: utiliza Conectar mediante IP" }
        val octets = address.split('.').map { it.toLong() }
        require(octets.size == 4 && octets.all { it in 0..255 })
        val ip = octets.fold(0L) { result, octet -> (result shl 8) or octet }
        val mask = (0xffffffffL shl (32 - prefix)) and 0xffffffffL
        val network = ip and mask
        val broadcast = network or (mask xor 0xffffffffL)
        return ((network + 1) until broadcast).filter { it != ip }.map { value ->
            (3 downTo 0).joinToString(".") { ((value shr (8 * it)) and 255).toString() }
        }
    }
}

/** Only invoked after explicit permission in the UI. No internet-wide or routed scans. */
class EpsonDiscoveryService(context: Context) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)

    fun environment(): String {
        val network = cm.activeNetwork ?: return "Sin red activa. Conecta el celular al Wi-Fi de la institución."
        val wifi = cm.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val links = cm.getLinkProperties(network)
        return "Wi-Fi: ${if (wifi) "activo" else "no activo"}\nInterfaz: ${links?.interfaceName ?: "desconocida"}\nIP/subred: ${links?.linkAddresses?.joinToString() ?: "desconocida"}\nLa falta de descubrimiento no demuestra falta de conectividad. VLAN o aislamiento pueden separar Wi-Fi y RJ45."
    }

    suspend fun discover(): List<DiscoveredDevice> = coroutineScope {
        val network = cm.activeNetwork ?: error("No hay una red activa")
        check(cm.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) { "Conecta el celular al Wi-Fi" }
        val link = cm.getLinkProperties(network)?.linkAddresses?.firstOrNull { it.address is Inet4Address }
            ?: error("La red no tiene dirección IPv4; utiliza conexión manual")
        val hosts = LocalSubnet.hosts(link.address.hostAddress!!, link.prefixLength)
        val client = PjLinkClient({ network.socketFactory.createSocket() }, timeoutMs = 2500, connectTimeoutMs = 500)
        val gate = Semaphore(16)
        withTimeout(90_000) {
            hosts.map { host -> async {
                gate.withPermit {
                    ensureActive()
                    try {
                        if (!client.greeting(host, 4352)) return@withPermit null
                        var brand = DeviceBrand.OTHER
                        var model = "PJLink (identidad pendiente de autenticación)"
                        var name = model
                        try {
                            val manufacturer = client.query(host, 4352, "INF1")
                            if (!manufacturer.equals("EPSON", true)) return@withPermit null
                            brand = DeviceBrand.EPSON
                            model = client.query(host, 4352, "INF2")
                            name = client.query(host, 4352, "NAME")
                        } catch (e: PjLinkException) {
                            if (e.code != "PASSWORD") throw e
                        }
                        DiscoveredDevice("pjlink_$host", name, brand, model, DeviceType.PROJECTOR,
                            host, 4352, ProtocolType.PJLINK,
                            mapOf("identity" to if (brand == DeviceBrand.EPSON) "EPSON" else "authentication_required"))
                    } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
                }
            } }.awaitAll().filterNotNull()
        }
    }
}
