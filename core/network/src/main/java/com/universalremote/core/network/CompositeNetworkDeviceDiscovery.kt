package com.universalremote.core.network

import android.content.Context
import com.universalremote.core.model.DiscoveredDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CompositeNetworkDeviceDiscovery(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : NetworkDeviceDiscovery {

    private val mutableState = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)
    override val state: StateFlow<DiscoveryState> = mutableState.asStateFlow()

    private val services = AndroidNetworkServices(context)
    private val ssdp = SsdpDiscovery(context)
    private val mdns = MdnsDiscovery(context)

    private var activeJob: Job? = null

    override suspend fun start() {
        cancel()

        if (!services.isWifiConnected()) {
            mutableState.value = DiscoveryState.Failed(DiscoveryError.NETWORK_UNAVAILABLE)
            return
        }

        mutableState.value = DiscoveryState.Searching

        activeJob = scope.launch {
            try {
                val ssdpDeferred = async { ssdp.discover(timeoutMs = 6000) }
                val mdnsDeferred = async { mdns.discover(timeoutMs = 6000) }

                val ssdpResults = ssdpDeferred.await()
                val mdnsResults = mdnsDeferred.await()

                val mergedMap = mutableMapOf<String, DiscoveredDevice>()

                for (device in ssdpResults + mdnsResults) {
                    val key = "${device.ipAddress}_${device.protocolType.name}"
                    if (!mergedMap.containsKey(key)) {
                        mergedMap[key] = device
                    }
                }

                val detailsList = mergedMap.values.toList()
                val devicesList = detailsList.map { it.toDevice() }

                mutableState.value = DiscoveryState.Results(
                    devices = devicesList,
                    details = detailsList
                )
            } catch (_: Exception) {
                mutableState.value = DiscoveryState.Failed(DiscoveryError.PLATFORM_FAILURE)
            }
        }
    }

    override suspend fun cancel() {
        activeJob?.cancel()
        activeJob = null
        if (mutableState.value == DiscoveryState.Searching) {
            mutableState.value = DiscoveryState.Cancelled
        }
    }
}
