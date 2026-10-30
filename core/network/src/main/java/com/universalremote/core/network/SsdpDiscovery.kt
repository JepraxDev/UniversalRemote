package com.universalremote.core.network

import android.content.Context
import android.net.wifi.WifiManager
import com.universalremote.core.model.DeviceBrand
import com.universalremote.core.model.DeviceType
import com.universalremote.core.model.DiscoveredDevice
import com.universalremote.core.model.ProtocolType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.SocketTimeoutException
import java.util.Locale

class SsdpDiscovery(private val context: Context) {

    private val multicastAddress = "239.255.255.250"
    private val ssdpPort = 1900

    suspend fun discover(timeoutMs: Long = 5000): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val multicastLock = wifiManager?.createMulticastLock("UniversalRemoteSSDP")?.apply {
            setReferenceCounted(true)
            acquire()
        }

        val discovered = mutableMapOf<String, DiscoveredDevice>()

        var socket: MulticastSocket? = null
        try {
            socket = MulticastSocket(null).apply {
                reuseAddress = true
                soTimeout = 1000
            }

            val group = InetAddress.getByName(multicastAddress)
            val searchMessage = """
                M-SEARCH * HTTP/1.1
                HOST: 239.255.255.250:1900
                MAN: "ssdp:discover"
                MX: 2
                ST: ssdp:all

            """.trimIndent().replace("\n", "\r\n")

            val searchBytes = searchMessage.toByteArray(Charsets.UTF_8)
            val packet = DatagramPacket(searchBytes, searchBytes.size, group, ssdpPort)

            socket.send(packet)

            val startTime = System.currentTimeMillis()
            val buffer = ByteArray(2048)

            while (isActive && (System.currentTimeMillis() - startTime) < timeoutMs) {
                val recvPacket = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(recvPacket)
                    val responseText = String(recvPacket.data, 0, recvPacket.length, Charsets.UTF_8)
                    val hostAddress = recvPacket.address.hostAddress ?: continue

                    val device = parseSsdpResponse(hostAddress, responseText)
                    if (device != null) {
                        discovered[device.id] = device
                    }
                } catch (_: SocketTimeoutException) {
                    // Timeout per packet read loop, continue until total timeout
                } catch (_: Exception) {
                    break
                }
            }
        } catch (_: Exception) {
            // Platform / network socket error
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
            try {
                if (multicastLock?.isHeld == true) {
                    multicastLock.release()
                }
            } catch (_: Exception) {}
        }

        discovered.values.toList()
    }

    private fun parseSsdpResponse(ipAddress: String, response: String): DiscoveredDevice? {
        val lines = response.lines()
        val headers = mutableMapOf<String, String>()

        for (line in lines) {
            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
                val key = line.substring(0, colonIndex).trim().uppercase(Locale.ROOT)
                val value = line.substring(colonIndex + 1).trim()
                headers[key] = value
            }
        }

        val st = headers["ST"] ?: headers["NT"] ?: ""
        val usn = headers["USN"] ?: ""
        val server = headers["SERVER"] ?: ""
        val location = headers["LOCATION"] ?: ""

        val lowerContent = "$st $usn $server $location".lowercase(Locale.ROOT)

        val protocolType: ProtocolType
        val brand: DeviceBrand
        val deviceType: DeviceType
        val port: Int
        val name: String

        when {
            lowerContent.contains("webos") || lowerContent.contains("lge-com") || lowerContent.contains("lg") -> {
                protocolType = ProtocolType.LG_WEBOS
                brand = DeviceBrand.LG
                deviceType = DeviceType.TV
                port = 3000
                name = "LG webOS TV ($ipAddress)"
            }
            lowerContent.contains("samsung") -> {
                protocolType = ProtocolType.SAMSUNG_WS
                brand = DeviceBrand.SAMSUNG
                deviceType = DeviceType.TV
                port = 8001
                name = "Samsung Smart TV ($ipAddress)"
            }
            lowerContent.contains("pjlink") -> {
                protocolType = ProtocolType.PJLINK
                brand = DeviceBrand.OTHER
                deviceType = DeviceType.PROJECTOR
                port = 4352
                name = "PJLink Projector ($ipAddress)"
            }
            lowerContent.contains("dial") || lowerContent.contains("googlecast") || lowerContent.contains("androidtv") -> {
                protocolType = ProtocolType.ANDROID_TV
                brand = DeviceBrand.OTHER
                deviceType = DeviceType.TV
                port = 6466
                name = "Android TV ($ipAddress)"
            }
            lowerContent.contains("mediarenderer") || lowerContent.contains("upnp") -> {
                protocolType = ProtocolType.UNKNOWN
                brand = DeviceBrand.OTHER
                deviceType = DeviceType.TV
                port = 8080
                name = "UPnP Media Device ($ipAddress)"
            }
            else -> return null
        }

        val id = "ssdp_${ipAddress.replace('.', '_')}_${protocolType.name}"

        return DiscoveredDevice(
            id = id,
            name = name,
            brand = brand,
            model = server.takeIf { it.isNotBlank() } ?: "SSDP Device",
            deviceType = deviceType,
            ipAddress = ipAddress,
            port = port,
            protocolType = protocolType,
            rawServiceInfo = headers
        )
    }
}
