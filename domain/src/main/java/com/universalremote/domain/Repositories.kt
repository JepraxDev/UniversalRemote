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
    val hapticEnabled: Flow<Boolean>
    val reduceAnimations: Flow<Boolean>
    val remoteMode: Flow<RemoteMode>
    suspend fun setTheme(theme: ThemePreference)
    suspend fun setHapticEnabled(enabled: Boolean)
    suspend fun setReduceAnimations(reduced: Boolean)
    suspend fun setRemoteMode(mode: RemoteMode)
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
