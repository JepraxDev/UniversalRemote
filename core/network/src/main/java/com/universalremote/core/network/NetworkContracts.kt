package com.universalremote.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.nsd.NsdManager
import com.universalremote.core.model.Device
import kotlinx.coroutines.flow.StateFlow

/** Independent facts, never proof of a compatible device. null means not checked. */
data class NetworkEnvironment(
    val wifiConnected: Boolean? = null,
    val localNetworkAvailable: Boolean? = null,
    val internetValidated: Boolean? = null,
)
sealed interface DiscoveryState {
    data object Idle : DiscoveryState
    data object Searching : DiscoveryState
    data class Results(val devices: List<Device>) : DiscoveryState
    data class Failed(val error: DiscoveryError) : DiscoveryState
    data object Cancelled : DiscoveryState
}
enum class DiscoveryError { NETWORK_UNAVAILABLE, PERMISSION_REQUIRED, PLATFORM_FAILURE, NOT_IMPLEMENTED }
interface NetworkDeviceDiscovery {
    val state: StateFlow<DiscoveryState>
    suspend fun start()
    suspend fun cancel()
}
data class NetworkEndpoint(val host: String, val port: Int) {
    init { require(host.isNotBlank()); require(port in 1..65535) }
}
/** Future adapters obtain platform services here; no discovery or callbacks yet. */
class AndroidNetworkServices(private val context: Context) {
    val connectivityManager: ConnectivityManager?
        get() = context.getSystemService(ConnectivityManager::class.java)
    val nsdManager: NsdManager?
        get() = context.getSystemService(NsdManager::class.java)
}
