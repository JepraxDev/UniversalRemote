package com.universalremote.domain

import com.universalremote.core.model.ConnectionType
import com.universalremote.core.model.Device
import com.universalremote.core.model.DeviceBrand
import com.universalremote.core.model.DeviceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenRemoteUseCaseTest {

    private class FakeDeviceRepository : DeviceRepository {
        val devices = mutableMapOf<String, Device>()
        override fun observeDevices(): Flow<List<Device>> = flowOf(devices.values.toList())
        override suspend fun find(id: String): Device? = devices[id]
        override suspend fun insert(device: Device) { devices[device.id] = device }
        override suspend fun update(device: Device) { devices[device.id] = device }
        override suspend fun delete(id: String) { devices.remove(id) }
    }

    @Test
    fun openingRemoteUpdatesLastUsedTimestamp() = runTest {
        val repo = FakeDeviceRepository()
        val device = Device(
            id = "d1",
            name = "TV LG",
            brand = DeviceBrand.LG,
            model = "webOS",
            deviceType = DeviceType.TV,
            connectionType = ConnectionType.WIFI,
            lastUsedAt = null
        )
        repo.insert(device)

        val useCase = OpenRemoteUseCase(repo)
        val result = useCase("d1")

        assertNotNull(result)
        assertNotNull(result?.lastUsedAt)
        assertTrue(result!!.lastUsedAt!! > 0)
    }
}
