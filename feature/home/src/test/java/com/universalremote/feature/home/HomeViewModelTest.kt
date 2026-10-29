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
        val vm = HomeViewModel(FakeRepository(), { IrAvailability.NoEmitter }, dispatcher)
        advanceUntilIdle()
        assertEquals(HomeUiState(loading = false, ir = IrAvailability.NoEmitter), vm.state.value)
    }
    @Test fun devicesAreObservedWithoutClaimingConnections() = runTest(dispatcher) {
        val device = Device("1", "TV", DeviceBrand.SONY, "", DeviceType.TV, ConnectionType.WIFI)
        val vm = HomeViewModel(FakeRepository(flowOf(listOf(device))), { IrAvailability.Available }, dispatcher)
        advanceUntilIdle()
        assertEquals(1, vm.state.value.deviceCount)
        assertFalse(vm.state.value.error)
    }
    @Test fun favoritesContainOnlySavedFavorites() = runTest(dispatcher) {
        val favorite = Device("1", "TV", DeviceBrand.SONY, "", DeviceType.TV, ConnectionType.WIFI, isFavorite = true)
        val ordinary = favorite.copy(id = "2", isFavorite = false)
        val vm = HomeViewModel(FakeRepository(flowOf(listOf(favorite, ordinary))), { IrAvailability.NoEmitter }, dispatcher)
        advanceUntilIdle()
        assertEquals(listOf(favorite), vm.state.value.favorites)
        assertEquals(2, vm.state.value.deviceCount)
    }
    @Test fun databaseFailureIsNotShownAsEmptySuccessAndCanRetry() = runTest(dispatcher) {
        val repository = FakeRepository(flow { throw IllegalStateException("database unavailable") })
        val vm = HomeViewModel(repository, { IrAvailability.ServiceUnavailable }, dispatcher)
        advanceUntilIdle()
        assertTrue(vm.state.value.error)
        assertFalse(vm.state.value.loading)
        repository.source = flowOf(emptyList())
        vm.refresh()
        advanceUntilIdle()
        assertFalse(vm.state.value.error)
        assertEquals(0, vm.state.value.deviceCount)
    }
    private class FakeRepository(var source: Flow<List<Device>> = flowOf(emptyList())) : DeviceRepository {
        override fun observeDevices() = source
        override suspend fun find(id: String): Device? = null
        override suspend fun insert(device: Device) = Unit
        override suspend fun update(device: Device) = Unit
        override suspend fun delete(id: String) = Unit
    }
}
