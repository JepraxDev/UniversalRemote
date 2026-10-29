package com.universalremote.feature.discovery

import androidx.lifecycle.SavedStateHandle
import com.universalremote.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class AddDeviceViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { Dispatchers.resetMain() }

    @Test fun selectedTypeBrandAndStepSurviveNewViewModel() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        val original = AddDeviceViewModel(saved, { IrAvailability.NoEmitter }, dispatcher)
        advanceUntilIdle()
        original.selectType(DeviceType.PROJECTOR)
        original.selectBrand(DeviceBrand.OTHER)
        original.next()
        val restored = AddDeviceViewModel(saved, { IrAvailability.NoEmitter }, dispatcher)
        advanceUntilIdle()
        assertEquals(DeviceType.PROJECTOR, restored.state.value.type)
        assertEquals(DeviceBrand.OTHER, restored.state.value.brand)
        assertEquals(1, restored.state.value.step)
    }
    @Test fun irCanOnlyBeSelectedWithAnEmitter() = runTest(dispatcher) {
        listOf(IrAvailability.NoEmitter, IrAvailability.ServiceUnavailable, IrAvailability.PlatformError).forEach { ir ->
            val vm = AddDeviceViewModel(SavedStateHandle(), { ir }, dispatcher)
            advanceUntilIdle()
            vm.selectConnection(ConnectionType.INFRARED)
            assertEquals(ConnectionType.WIFI, vm.state.value.connection)
        }
    }
    @Test fun availableIrSelectionSurvivesRestoration() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        val vm = AddDeviceViewModel(saved, { IrAvailability.Available }, dispatcher)
        advanceUntilIdle()
        vm.selectConnection(ConnectionType.INFRARED)
        val restored = AddDeviceViewModel(saved, { IrAvailability.Available }, dispatcher)
        advanceUntilIdle()
        assertEquals(ConnectionType.INFRARED, restored.state.value.connection)
    }
    @Test fun restoredIrSelectionFallsBackToWifiWhenHardwareDisappears() = runTest(dispatcher) {
        val vm = AddDeviceViewModel(SavedStateHandle(mapOf("connection" to "INFRARED")), { IrAvailability.NoEmitter }, dispatcher)
        advanceUntilIdle()
        assertEquals(ConnectionType.WIFI, vm.state.value.connection)
    }
    @Test fun wizardCannotMoveOutsideSelectionSteps() = runTest(dispatcher) {
        val vm = AddDeviceViewModel(SavedStateHandle(), { IrAvailability.NoEmitter }, dispatcher)
        advanceUntilIdle()
        repeat(5) { vm.next() }
        assertEquals(2, vm.state.value.step)
        repeat(5) { vm.previous() }
        assertEquals(0, vm.state.value.step)
    }
}
