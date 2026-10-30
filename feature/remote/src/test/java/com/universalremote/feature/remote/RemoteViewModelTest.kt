package com.universalremote.feature.remote

import androidx.lifecycle.SavedStateHandle
import com.universalremote.core.model.*
import com.universalremote.domain.*
import com.universalremote.protocol.api.ProtocolRegistry
import com.universalremote.protocol.api.RemoteProtocol
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
class RemoteViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private class FakeDeviceRepository : DeviceRepository {
        val devices = mutableMapOf<String, Device>()
        override fun observeDevices(): Flow<List<Device>> = flowOf(devices.values.toList())
        override suspend fun find(id: String) = devices[id]
        override suspend fun insert(device: Device) { devices[device.id] = device }
        override suspend fun update(device: Device) { devices[device.id] = device }
        override suspend fun delete(id: String) { devices.remove(id) }
    }

    private class FakeInfraredRepository : InfraredRepository, IrProfileCatalog {
        override val profiles = listOf(
            IrProfile(
                id = "lg-profile-1",
                brand = DeviceBrand.LG,
                deviceType = DeviceType.TV,
                family = "AKB",
                protocol = "NEC",
                carrierHz = 38000,
                commands = mapOf(
                    RemoteCommand.POWER to IrSignal(38000, listOf(9000, 4500, 560)),
                    RemoteCommand.MENU to IrSignal(38000, listOf(9000, 4500, 560)),
                    RemoteCommand.VOLUME_UP to IrSignal(38000, listOf(9000, 4500, 560)),
                ),
                source = "test",
                testCommand = RemoteCommand.MENU
            )
        )
        override val pendingBrands: List<PendingBrandInfo> get() = emptyList()
        val sentCommands = mutableListOf<Pair<String, RemoteCommand>>()
        var sendResult: IrResult<Unit> = IrResult.Success(Unit)

        override suspend fun test(profileId: String) = IrResult.Failure(IrError.PLATFORM_FAILURE)
        override suspend fun assessment(id: String): IrAssessment? = null
        override suspend fun confirm(id: String, responded: Boolean) = IrResult.Failure(IrError.PLATFORM_FAILURE)
        override suspend fun save(id: String, name: String, room: String?) = IrResult.Failure(IrError.STORAGE_FAILURE)
        override suspend fun send(deviceId: String, command: RemoteCommand, confirmed: Boolean): IrResult<Unit> {
            sentCommands.add(deviceId to command)
            return sendResult
        }
    }

    private class FakeProtocolRegistry : ProtocolRegistry {
        override fun getProtocol(protocolType: ProtocolType): RemoteProtocol? = null
        override fun getProtocolForDevice(device: Device): RemoteProtocol? = null
    }

    private lateinit var deviceRepository: FakeDeviceRepository
    private lateinit var infraredRepository: FakeInfraredRepository
    private lateinit var sendNetworkCommand: SendNetworkCommand
    private lateinit var capabilityResolver: DeviceCapabilityResolver

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        deviceRepository = FakeDeviceRepository()
        infraredRepository = FakeInfraredRepository()
        val registry = FakeProtocolRegistry()
        sendNetworkCommand = SendNetworkCommand(registry)
        capabilityResolver = DeviceCapabilityResolver(infraredRepository, registry)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initializesInDemoModeWhenNoDeviceId() = runTest(testDispatcher) {
        val saved = SavedStateHandle()
        val vm = RemoteViewModel(
            saved = saved,
            deviceRepository = deviceRepository,
            infraredRepository = infraredRepository,
            sendRemoteCommand = SendRemoteCommand(infraredRepository),
            sendNetworkCommand = sendNetworkCommand,
            availabilityProvider = { IrAvailability.Available },
            capabilityResolver = capabilityResolver
        )
        testScheduler.advanceUntilIdle()
        assertTrue(vm.state.value.isDemo)
        assertNull(vm.state.value.deviceId)
    }

    @Test
    fun loadsConfiguredIrDeviceSuccessfully() = runTest(testDispatcher) {
        val device = Device("d-1", "TV Sala", DeviceBrand.LG, "Model", DeviceType.TV, ConnectionType.INFRARED, irProfileId = "lg-profile-1")
        deviceRepository.insert(device)

        val saved = SavedStateHandle(mapOf("deviceId" to "d-1", "type" to "TV"))
        val vm = RemoteViewModel(
            saved = saved,
            deviceRepository = deviceRepository,
            infraredRepository = infraredRepository,
            sendRemoteCommand = SendRemoteCommand(infraredRepository),
            sendNetworkCommand = sendNetworkCommand,
            availabilityProvider = { IrAvailability.Available },
            capabilityResolver = capabilityResolver
        )
        testScheduler.advanceUntilIdle()

        assertFalse(vm.state.value.isDemo)
        assertEquals("d-1", vm.state.value.deviceId)
        assertEquals("TV Sala", vm.state.value.deviceName)
        assertNotNull(vm.state.value.capabilities)
        assertTrue(vm.state.value.capabilities!!.supportedCommands.contains(RemoteCommand.VOLUME_UP))
    }

    @Test
    fun sendCommandTransmitsWhenConfigured() = runTest(testDispatcher) {
        val device = Device("d-1", "TV Sala", DeviceBrand.LG, "Model", DeviceType.TV, ConnectionType.INFRARED, irProfileId = "lg-profile-1")
        deviceRepository.insert(device)

        val saved = SavedStateHandle(mapOf("deviceId" to "d-1"))
        val vm = RemoteViewModel(
            saved = saved,
            deviceRepository = deviceRepository,
            infraredRepository = infraredRepository,
            sendRemoteCommand = SendRemoteCommand(infraredRepository),
            sendNetworkCommand = sendNetworkCommand,
            availabilityProvider = { IrAvailability.Available },
            capabilityResolver = capabilityResolver
        )
        testScheduler.advanceUntilIdle()

        vm.sendCommand(RemoteCommand.VOLUME_UP)
        testScheduler.advanceUntilIdle()

        assertEquals(1, infraredRepository.sentCommands.size)
        assertEquals("d-1" to RemoteCommand.VOLUME_UP, infraredRepository.sentCommands.first())
    }

    @Test
    fun sendCommandInDemoModeDoesNotTransmit() = runTest(testDispatcher) {
        val saved = SavedStateHandle()
        val vm = RemoteViewModel(
            saved = saved,
            deviceRepository = deviceRepository,
            infraredRepository = infraredRepository,
            sendRemoteCommand = SendRemoteCommand(infraredRepository),
            sendNetworkCommand = sendNetworkCommand,
            availabilityProvider = { IrAvailability.Available },
            capabilityResolver = capabilityResolver
        )
        testScheduler.advanceUntilIdle()

        vm.sendCommand(RemoteCommand.VOLUME_UP)
        testScheduler.advanceUntilIdle()

        assertTrue(infraredRepository.sentCommands.isEmpty())
    }

    @Test
    fun sendCommandRequiringConfirmationSetsPendingConfirmation() = runTest(testDispatcher) {
        val device = Device("d-1", "TV Sala", DeviceBrand.LG, "Model", DeviceType.TV, ConnectionType.INFRARED, irProfileId = "lg-profile-1")
        deviceRepository.insert(device)

        val saved = SavedStateHandle(mapOf("deviceId" to "d-1"))
        val vm = RemoteViewModel(
            saved = saved,
            deviceRepository = deviceRepository,
            infraredRepository = infraredRepository,
            sendRemoteCommand = SendRemoteCommand(infraredRepository),
            sendNetworkCommand = sendNetworkCommand,
            availabilityProvider = { IrAvailability.Available },
            capabilityResolver = capabilityResolver
        )
        testScheduler.advanceUntilIdle()

        // Power requires confirmation
        vm.sendCommand(RemoteCommand.POWER)
        assertEquals(RemoteCommand.POWER, vm.state.value.pendingConfirmationCommand)
        assertEquals(0, infraredRepository.sentCommands.size)

        // Confirm
        vm.confirmPendingCommand()
        testScheduler.advanceUntilIdle()

        assertEquals(1, infraredRepository.sentCommands.size)
        assertEquals(RemoteCommand.POWER, infraredRepository.sentCommands.first().second)
        assertNull(vm.state.value.pendingConfirmationCommand)
    }

    @Test
    fun unsupportedCommandDoesNotTransmit() = runTest(testDispatcher) {
        val device = Device("d-1", "TV Sala", DeviceBrand.LG, "Model", DeviceType.TV, ConnectionType.INFRARED, irProfileId = "lg-profile-1")
        deviceRepository.insert(device)

        val saved = SavedStateHandle(mapOf("deviceId" to "d-1"))
        val vm = RemoteViewModel(
            saved = saved,
            deviceRepository = deviceRepository,
            infraredRepository = infraredRepository,
            sendRemoteCommand = SendRemoteCommand(infraredRepository),
            sendNetworkCommand = sendNetworkCommand,
            availabilityProvider = { IrAvailability.Available },
            capabilityResolver = capabilityResolver
        )
        testScheduler.advanceUntilIdle()

        // HDMI is not in test profile
        vm.sendCommand(RemoteCommand.HDMI)
        testScheduler.advanceUntilIdle()

        assertTrue(infraredRepository.sentCommands.isEmpty())
    }
}
