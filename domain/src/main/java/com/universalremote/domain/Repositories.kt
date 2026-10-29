package com.universalremote.domain

import com.universalremote.core.model.*
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun observeDevices(): Flow<List<Device>>
    suspend fun find(id: String): Device?
    suspend fun insert(device: Device)
    suspend fun update(device: Device)
    suspend fun delete(id: String)
}
interface PreferencesRepository {
    val theme: Flow<ThemePreference>
    suspend fun setTheme(theme: ThemePreference)
}
fun interface IrAvailabilityProvider {
    fun availability(): IrAvailability
}

/** Wi-Fi selection does not assert connectivity or protocol compatibility. */
class GetConnectionMethods {
    operator fun invoke(ir: IrAvailability): Set<ConnectionType> = buildSet {
        add(ConnectionType.WIFI)
        if (ir == IrAvailability.Available) add(ConnectionType.INFRARED)
    }
}
