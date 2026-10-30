package com.universalremote.core.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.universalremote.core.model.DeviceBrand
import com.universalremote.core.model.DeviceType
import com.universalremote.core.model.DiscoveredDevice
import com.universalremote.core.model.ProtocolType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MdnsDiscovery(private val context: Context) {

    private val serviceTypes = listOf(
        "_lgsmarttv._tcp.",
        "_viziocast._tcp.",
        "_pjlink._tcp.",
        "_androidtvremote2._tcp.",
        "_googlecast._tcp.",
        "_http._tcp."
    )

    suspend fun discover(timeoutMs: Long = 5000): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
            ?: return@withContext emptyList()

        val discoveredMap = ConcurrentHashMap<String, DiscoveredDevice>()
        val listeners = mutableListOf<Pair<String, NsdManager.DiscoveryListener>>()

        for (serviceType in serviceTypes) {
            val listener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                    try { nsdManager.stopServiceDiscovery(this) } catch (_: Exception) {}
                }

                override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                    try { nsdManager.stopServiceDiscovery(this) } catch (_: Exception) {}
                }

                override fun onDiscoveryStarted(serviceType: String?) {}

                override fun onDiscoveryStopped(serviceType: String?) {}

                override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                    val service = serviceInfo ?: return
                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}

                        override fun onServiceResolved(resolvedService: NsdServiceInfo?) {
                            val info = resolvedService ?: return
                            val host = info.host?.hostAddress ?: return
                            val port = info.port
                            val serviceName = info.serviceName ?: "Unknown Device"

                            val device = parseMdnsService(serviceName, info.serviceType ?: "", host, port, info.attributes)
                            if (device != null) {
                                discoveredMap[device.id] = device
                            }
                        }
                    })
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo?) {}
            }

            try {
                nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
                listeners.add(serviceType to listener)
            } catch (_: Exception) {
                // Ignore platform exceptions during discovery start
            }
        }

        try { delay(timeoutMs) } finally {
            for ((_, listener) in listeners) {
                try { nsdManager.stopServiceDiscovery(listener) } catch (_: Exception) {}
            }
        }

        discoveredMap.values.toList()
    }

    private fun parseMdnsService(
        serviceName: String,
        serviceType: String,
        host: String,
        port: Int,
        attributes: Map<String, ByteArray>
    ): DiscoveredDevice? {
        val lowerName = serviceName.lowercase(Locale.ROOT)
        val lowerType = serviceType.lowercase(Locale.ROOT)

        val protocolType: ProtocolType
        val brand: DeviceBrand
        val deviceType: DeviceType
        val resolvedPort: Int
        val displayName: String

        when {
            lowerType.contains("lgsmarttv") || lowerName.contains("webos") || lowerName.contains("lg") -> {
                protocolType = ProtocolType.LG_WEBOS
                brand = DeviceBrand.LG
                deviceType = DeviceType.TV
                resolvedPort = if (port > 0) port else 3000
                displayName = "LG webOS TV ($serviceName)"
            }
            lowerType.contains("pjlink") || lowerName.contains("pjlink") -> {
                protocolType = ProtocolType.PJLINK
                brand = if (lowerName.contains("epson")) DeviceBrand.EPSON else DeviceBrand.OTHER
                deviceType = DeviceType.PROJECTOR
                resolvedPort = if (port > 0) port else 4352
                displayName = "$serviceName ($host)"
            }
            lowerName.contains("samsung") -> {
                protocolType = ProtocolType.SAMSUNG_WS
                brand = DeviceBrand.SAMSUNG
                deviceType = DeviceType.TV
                resolvedPort = if (port > 0) port else 8001
                displayName = "Samsung Smart TV ($serviceName)"
            }
            lowerType.contains("androidtvremote") || lowerType.contains("googlecast") -> {
                protocolType = ProtocolType.ANDROID_TV
                brand = DeviceBrand.OTHER
                deviceType = DeviceType.TV
                resolvedPort = if (port > 0) port else 6466
                displayName = "Android TV ($serviceName)"
            }
            else -> return null
        }

        val id = "mdns_${host.replace('.', '_')}_${protocolType.name}"

        val rawInfo = attributes.mapValues { String(it.value, Charsets.UTF_8) }

        return DiscoveredDevice(
            id = id,
            name = displayName,
            brand = brand,
            model = serviceName,
            deviceType = deviceType,
            ipAddress = host,
            port = resolvedPort,
            protocolType = protocolType,
            rawServiceInfo = rawInfo
        )
    }
}
