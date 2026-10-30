package com.universalremote.feature.home

import com.universalremote.core.model.*
import com.universalremote.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun noIrLoadsRealEmptyState() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = HomeViewModel(repo, { IrAvailability.NoEmitter }, OpenRemoteUseCase(repo), dispatcher)
        advanceUntilIdle()
        assertEquals(false, vm.state.value.loading)
        assertEquals(IrAvailability.NoEmitter, vm.state.value.irAvailability)
    }

    @Test fun devicesAreObservedWithoutClaimingConnections() = runTest(dispatcher) {
        val device = Device("1", "TV", DeviceBrand.SONY, "", DeviceType.TV, ConnectionType.WIFI)
        val repo = FakeRepository(flowOf(listOf(device)))
        val vm = HomeViewModel(repo, { IrAvailability.Available }, OpenRemoteUseCase(repo), dispatcher)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.allDevices.size)
        assertFalse(vm.state.value.error)
    }

    @Test fun filterByTypeSelectsOnlyTvsOrProjectors() = runTest(dispatcher) {
        val tv = Device("1", "TV Living", DeviceBrand.SONY, "", DeviceType.TV, ConnectionType.WIFI)
        val projector = Device("2", "Proyector Aula", DeviceBrand.EPSON, "", DeviceType.PROJECTOR, ConnectionType.WIFI)
        val repo = FakeRepository(flowOf(listOf(tv, projector)))
        val vm = HomeViewModel(repo, { IrAvailability.Available }, OpenRemoteUseCase(repo), dispatcher)
        advanceUntilIdle()

        assertEquals(2, vm.state.value.filteredDevices.size)
        vm.selectTypeFilter(DeviceType.TV)
        assertEquals(1, vm.state.value.filteredDevices.size)
        assertEquals("1", vm.state.value.filteredDevices.first().id)
    }

    @Test fun databaseFailureIsNotShownAsEmptySuccessAndCanRetry() = runTest(dispatcher) {
        val repository = FakeRepository(flow { throw IllegalStateException("database unavailable") })
        val vm = HomeViewModel(repository, { IrAvailability.ServiceUnavailable }, OpenRemoteUseCase(repository), dispatcher)
        advanceUntilIdle()
        assertTrue(vm.state.value.error)
        assertFalse(vm.state.value.loading)
        repository.source = flowOf(emptyList())
        vm.refresh()
        advanceUntilIdle()
        assertFalse(vm.state.value.error)
        assertEquals(0, vm.state.value.allDevices.size)
    }

    private class FakeRepository(var source: Flow<List<Device>> = flowOf(emptyList())) : DeviceRepository {
        val map = mutableMapOf<String, Device>()
        override fun observeDevices() = source
        override suspend fun find(id: String): Device? = map[id]
        override suspend fun insert(device: Device) { map[device.id] = device }
        override suspend fun update(device: Device) { map[device.id] = device }
        override suspend fun delete(id: String) { map.remove(id) }
    }
}
