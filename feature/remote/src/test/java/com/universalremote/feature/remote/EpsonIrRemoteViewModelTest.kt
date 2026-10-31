package com.universalremote.feature.remote

import androidx.lifecycle.SavedStateHandle
import com.universalremote.core.model.*
import com.universalremote.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EpsonIrRemoteViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private class FakeDeviceRepository : DeviceRepository {
        val devices = mutableMapOf<String, Device>()
        override fun observeDevices(): Flow<List<Device>> = flowOf(devices.values.toList())
        override suspend fun find(id: String) = devices[id]
        override suspend fun insert(device: Device) { devices[device.id] = device }
        override suspend fun update(device: Device) { devices[device.id] = device }
        override suspend fun delete(id: String) { devices.remove(id) }
    }

    private class FakeInfraredRepository : InfraredRepository {
        override val profiles = listOf(
            IrProfile(
                id = EpsonIrRemoteViewModel.EPSON_PROFILE_ID,
                brand = DeviceBrand.EPSON,
                deviceType = DeviceType.PROJECTOR,
                family = "PowerLite 118",
                protocol = "NECext",
                carrierHz = 38_000,
                commands = mapOf(
                    RemoteCommand.POWER to IrSignal(38_000, listOf(9000, 4500, 560)),
                    RemoteCommand.MENU to IrSignal(38_000, listOf(9000, 4500, 560)),
                    RemoteCommand.OK to IrSignal(38_000, listOf(9000, 4500, 560)),
                ),
                source = "test",
                testCommand = RemoteCommand.MENU,
            ),
            IrProfile(
                id = EpsonIrRemoteViewModel.EB_L260F_PROFILE_ID,
                brand = DeviceBrand.EPSON,
                deviceType = DeviceType.PROJECTOR,
                family = "EB-L260F",
                protocol = "Epson raw 38 kHz",
                carrierHz = 38_000,
                commands = mapOf(
                    RemoteCommand.POWER to IrSignal(38_000, listOf(2383, 509, 648)),
                    RemoteCommand.INPUT to IrSignal(38_000, listOf(2372, 516, 643)),
                    RemoteCommand.OK to IrSignal(38_000, listOf(2354, 559, 623)),
                    RemoteCommand.MENU to IrSignal(38_000, listOf(2288, 611, 571)),
                ),
                source = "test",
                testCommand = RemoteCommand.MENU,
                testNote = "Menu verificado por coincidencia con capturas de 13 bits",
            )
        )
        val sent = mutableListOf<Triple<String, RemoteCommand, Boolean>>()
        var sendResult: IrResult<Unit> = IrResult.Success(Unit)
        var assessment: IrAssessment? = null

        override suspend fun test(profileId: String) = IrResult.Failure(IrError.PLATFORM_FAILURE)
        override suspend fun assessment(id: String): IrAssessment? = assessment
        override suspend fun confirm(id: String, responded: Boolean): IrResult<IrAssessment> {
            val updated = IrAssessment(
                id = id,
                profileId = EpsonIrRemoteViewModel.EPSON_PROFILE_ID,
                status = if (responded) IrValidationStatus.CONFIRMED_BY_USER else IrValidationStatus.INCOMPATIBLE,
                testedAt = 1L,
                testCommand = RemoteCommand.MENU,
                supportedCommands = setOf(RemoteCommand.POWER, RemoteCommand.MENU),
            )
            assessment = updated
            return IrResult.Success(updated)
        }
        override suspend fun save(id: String, name: String, room: String?) = IrResult.Failure(IrError.STORAGE_FAILURE)
        override suspend fun send(deviceId: String, command: RemoteCommand, confirmed: Boolean): IrResult<Unit> {
            sent.add(Triple(deviceId, command, confirmed))
            return sendResult
        }
    }

    private lateinit var devices: FakeDeviceRepository
    private lateinit var infrared: FakeInfraredRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        devices = FakeDeviceRepository()
        infrared = FakeInfraredRepository()
        devices.devices["epson-1"] = Device(
                id = "epson-1",
                name = "Epson PowerLite 118",
                brand = DeviceBrand.EPSON,
                model = "PowerLite 118",
                deviceType = DeviceType.PROJECTOR,
                connectionType = ConnectionType.INFRARED,
                irProfileId = EpsonIrRemoteViewModel.EPSON_PROFILE_ID,
            )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(availability: IrAvailability = IrAvailability.Available) =
        EpsonIrRemoteViewModel(
            saved = SavedStateHandle(mapOf("deviceId" to "epson-1")),
            deviceRepository = devices,
            infraredRepository = infrared,
            sendRemoteCommand = SendRemoteCommand(infrared),
            availabilityProvider = { availability },
        )

    @Test
    fun loadsSavedEpsonIrDeviceAndProfile() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        assertFalse(vm.state.value.isDemo)
        assertTrue(vm.state.value.profileLoaded)
        assertEquals("epson-1", vm.state.value.deviceId)
        assertEquals(EpsonIrRemoteViewModel.EPSON_PROFILE_ID, vm.state.value.profileId)
        assertTrue(vm.state.value.canTransmit)
        assertEquals(IrValidationStatus.UNVERIFIED, vm.state.value.validationStatus)
    }

    @Test
    fun menuSendsOneFrameWithoutClaimingProjectorConfirmation() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.sendCommand(RemoteCommand.MENU)
        advanceUntilIdle()

        assertEquals(1, infrared.sent.size)
        assertEquals(RemoteCommand.MENU, infrared.sent.first().second)
        assertTrue(vm.state.value.lastResultText!!.contains("aún no ha confirmado"))
        assertFalse(vm.state.value.isError)
        assertEquals(RemoteCommand.MENU, vm.state.value.lastTransmitted)
    }

    @Test
    fun powerWaitsForConfirmationAndSendsOnlyOneCommand() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.sendCommand(RemoteCommand.POWER)
        advanceUntilIdle()
        assertTrue(infrared.sent.isEmpty())
        assertEquals(RemoteCommand.POWER, vm.state.value.pendingConfirmation)

        vm.confirmPendingCommand()
        advanceUntilIdle()
        assertEquals(1, infrared.sent.size)
        assertEquals(RemoteCommand.POWER, infrared.sent.first().second)
        assertTrue(infrared.sent.first().third)
        assertNull(vm.state.value.pendingConfirmation)
    }

    @Test
    fun numModifierDoesNotInventAnIrFrame() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.sendCommand(RemoteCommand.NUM_MODIFIER)
        advanceUntilIdle()

        assertTrue(infrared.sent.isEmpty())
        assertTrue(vm.state.value.lastResultText!!.contains("modificador"))
    }

    @Test
    fun unavailableEmitterNeverTransmits() = runTest(dispatcher) {
        val vm = viewModel(IrAvailability.NoEmitter)
        advanceUntilIdle()

        vm.sendCommand(RemoteCommand.MENU)
        advanceUntilIdle()

        assertTrue(infrared.sent.isEmpty())
        assertTrue(vm.state.value.isDemo)
        assertTrue(vm.state.value.isError)
    }

    @Test
    fun ebL260fLoadsItsOwnProfileAndRejectsCommandsWithoutCapture() = runTest(dispatcher) {
        devices.devices["epson-l260f"] = Device(
            id = "epson-l260f",
            name = "Epson EB-L260F",
            brand = DeviceBrand.EPSON,
            model = "EB-L260F",
            deviceType = DeviceType.PROJECTOR,
            connectionType = ConnectionType.INFRARED,
            irProfileId = EpsonIrRemoteViewModel.EB_L260F_PROFILE_ID,
        )
        val vm = EpsonIrRemoteViewModel(
            saved = SavedStateHandle(mapOf("deviceId" to "epson-l260f")),
            deviceRepository = devices,
            infraredRepository = infrared,
            sendRemoteCommand = SendRemoteCommand(infrared),
            availabilityProvider = { IrAvailability.Available },
        )
        advanceUntilIdle()

        assertEquals("Epson EB-L260F", vm.state.value.deviceName)
        assertEquals(EpsonIrRemoteViewModel.EB_L260F_PROFILE_ID, vm.state.value.profileId)
        assertTrue(vm.state.value.supportedCommands.contains(RemoteCommand.MENU))
        assertTrue(vm.state.value.supportedCommands.contains(RemoteCommand.INPUT))
        assertFalse(vm.state.value.supportedCommands.contains(RemoteCommand.INFO))

        vm.sendCommand(RemoteCommand.INFO)
        advanceUntilIdle()
        assertTrue(infrared.sent.isEmpty())
        assertTrue(vm.state.value.lastResultText!!.contains("no tiene un código confirmado"))

        vm.sendCommand(RemoteCommand.MENU)
        advanceUntilIdle()
        assertEquals(1, infrared.sent.size)
        assertEquals(RemoteCommand.MENU, infrared.sent.first().second)
    }

    @Test
    fun userCanConfirmTheLastCommandExplicitly() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.sendCommand(RemoteCommand.MENU)
        advanceUntilIdle()

        vm.confirmLastCommand(true)
        advanceUntilIdle()

        assertEquals(IrValidationStatus.CONFIRMED_BY_USER, vm.state.value.validationStatus)
        assertFalse(vm.state.value.canSaveConfirmation)
    }
}
