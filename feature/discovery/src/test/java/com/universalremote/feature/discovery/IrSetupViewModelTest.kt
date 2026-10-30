package com.universalremote.feature.discovery

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
class IrSetupViewModelTest {
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
                id = "lg-profile-1",
                brand = DeviceBrand.LG,
                deviceType = DeviceType.TV,
                family = "AKB",
                protocol = "NEC",
                carrierHz = 38000,
                commands = mapOf(
                    RemoteCommand.POWER to IrSignal(38000, listOf(9000, 4500, 560)),
                    RemoteCommand.MENU to IrSignal(38000, listOf(9000, 4500, 560)),
                ),
                source = "test",
                testCommand = RemoteCommand.MENU
            ),
            IrProfile(
                id = "EPSON_POWERLITE_118_ORIGINAL_IR",
                brand = DeviceBrand.EPSON,
                deviceType = DeviceType.PROJECTOR,
                family = "PowerLite 118",
                protocol = "NECext",
                carrierHz = 38000,
                commands = mapOf(
                    RemoteCommand.MENU to IrSignal(38000, listOf(9000, 4500, 560)),
                    RemoteCommand.POWER to IrSignal(38000, listOf(9000, 4500, 560)),
                ),
                source = "test",
                testCommand = RemoteCommand.MENU
            )
        )
        override val pendingBrands = listOf(
            PendingBrandInfo(DeviceBrand.PHILIPS, DeviceType.TV, "RC-5 unverified", "Missing codes")
        )

        var testCalls = mutableListOf<String>()
        var confirmedCalls = mutableListOf<Pair<String, Boolean>>()

        override suspend fun test(profileId: String): IrResult<IrAssessment> {
            testCalls.add(profileId)
            return IrResult.Success(
                IrAssessment(
                    id = profileId,
                    profileId = profileId,
                    status = IrValidationStatus.UNVERIFIED,
                    testedAt = 1000L,
                    testCommand = RemoteCommand.MENU,
                    supportedCommands = setOf(RemoteCommand.POWER, RemoteCommand.MENU)
                )
            )
        }

        override suspend fun assessment(id: String): IrAssessment? = null

        override suspend fun confirm(id: String, responded: Boolean): IrResult<IrAssessment> {
            confirmedCalls.add(id to responded)
            return IrResult.Success(
                IrAssessment(
                    id = id,
                    profileId = id,
                    status = if (responded) IrValidationStatus.CONFIRMED_BY_USER else IrValidationStatus.INCOMPATIBLE,
                    testedAt = 2000L,
                    testCommand = RemoteCommand.MENU,
                    supportedCommands = setOf(RemoteCommand.POWER, RemoteCommand.MENU)
                )
            )
        }

        override suspend fun save(id: String, name: String, room: String?) = IrResult.Failure(IrError.STORAGE_FAILURE)
        override suspend fun send(deviceId: String, command: RemoteCommand, confirmed: Boolean) = IrResult.Success(Unit)
    }

    private lateinit var deviceRepository: FakeDeviceRepository
    private lateinit var infraredRepository: FakeInfraredRepository

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        deviceRepository = FakeDeviceRepository()
        infraredRepository = FakeInfraredRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initializesWithAvailableProfilesForBrandAndType() = runTest(dispatcher) {
        val saved = SavedStateHandle(mapOf("type" to "TV", "brand" to "LG"))
        val vm = IrSetupViewModel(saved, deviceRepository, infraredRepository) { IrAvailability.Available }
        advanceUntilIdle()

        assertEquals(1, vm.state.value.availableProfiles.size)
        assertEquals("lg-profile-1", vm.state.value.selectedProfile?.id)
        assertNull(vm.state.value.pendingBrandInfo)
        assertEquals(IrValidationStatus.UNVERIFIED, vm.state.value.validationStatus)
    }

    @Test
    fun identifiesPendingBrandProtocols() = runTest(dispatcher) {
        val saved = SavedStateHandle(mapOf("type" to "TV", "brand" to "PHILIPS"))
        val vm = IrSetupViewModel(saved, deviceRepository, infraredRepository) { IrAvailability.Available }
        advanceUntilIdle()

        assertTrue(vm.state.value.availableProfiles.isEmpty())
        assertNotNull(vm.state.value.pendingBrandInfo)
        assertEquals(DeviceBrand.PHILIPS, vm.state.value.pendingBrandInfo!!.brand)
    }

    @Test
    fun sendTestCommandTransmitsAndSetsTestingStep() = runTest(dispatcher) {
        val saved = SavedStateHandle(mapOf("type" to "TV", "brand" to "LG"))
        val vm = IrSetupViewModel(saved, deviceRepository, infraredRepository) { IrAvailability.Available }
        advanceUntilIdle()

        vm.sendTestCommand()
        advanceUntilIdle()

        assertEquals(1, infraredRepository.testCalls.size)
        assertEquals("lg-profile-1", infraredRepository.testCalls.first())
        assertEquals(IrTestStep.TESTING_COMMAND, vm.state.value.testStep)
        assertTrue(vm.state.value.transmissionSuccess)
        assertNotNull(vm.state.value.lastTransmissionMessage)
    }

    @Test
    fun confirmResponseTrueMarksConfirmedByUser() = runTest(dispatcher) {
        val saved = SavedStateHandle(mapOf("type" to "TV", "brand" to "LG"))
        val vm = IrSetupViewModel(saved, deviceRepository, infraredRepository) { IrAvailability.Available }
        advanceUntilIdle()

        vm.sendTestCommand()
        advanceUntilIdle()

        vm.confirmResponse(true)
        advanceUntilIdle()

        assertEquals(IrValidationStatus.CONFIRMED_BY_USER, vm.state.value.validationStatus)
        assertEquals(IrTestStep.CONFIRMED_SUCCESS, vm.state.value.testStep)
        assertEquals(1, infraredRepository.confirmedCalls.size)
        assertEquals("lg-profile-1" to true, infraredRepository.confirmedCalls.first())
    }

    @Test
    fun confirmResponseFalseMarksIncompatible() = runTest(dispatcher) {
        val saved = SavedStateHandle(mapOf("type" to "TV", "brand" to "LG"))
        val vm = IrSetupViewModel(saved, deviceRepository, infraredRepository) { IrAvailability.Available }
        advanceUntilIdle()

        vm.sendTestCommand()
        advanceUntilIdle()

        vm.confirmResponse(false)
        advanceUntilIdle()

        assertEquals(IrValidationStatus.INCOMPATIBLE, vm.state.value.validationStatus)
        assertEquals(IrTestStep.INCOMPATIBLE_RESULT, vm.state.value.testStep)
        assertEquals(1, infraredRepository.confirmedCalls.size)
        assertEquals("lg-profile-1" to false, infraredRepository.confirmedCalls.first())
    }

    @Test
    fun saveDeviceInsertsDeviceAndConfirmsInRepository() = runTest(dispatcher) {
        val saved = SavedStateHandle(mapOf("type" to "TV", "brand" to "LG"))
        val vm = IrSetupViewModel(saved, deviceRepository, infraredRepository) { IrAvailability.Available }
        advanceUntilIdle()

        vm.setDeviceName("Mi Televisor LG")
        vm.setRoomName("Sala")
        vm.sendTestCommand()
        advanceUntilIdle()
        vm.confirmResponse(true)
        advanceUntilIdle()

        var savedId: String? = null
        vm.saveDevice { id -> savedId = id }
        advanceUntilIdle()

        assertNotNull(savedId)
        val savedDevice = deviceRepository.find(savedId!!)
        assertNotNull(savedDevice)
        assertEquals("Mi Televisor LG", savedDevice!!.name)
        assertEquals("Sala", savedDevice.roomName)
        assertEquals(ConnectionType.INFRARED, savedDevice.connectionType)
        assertEquals("lg-profile-1", savedDevice.irProfileId)
    }

    @Test
    fun cannotSaveOrConfirmWithoutPhysicalTest() = runTest(dispatcher) {
        val vm = IrSetupViewModel(SavedStateHandle(), deviceRepository, infraredRepository) { IrAvailability.Available }
        vm.confirmResponse(true)
        vm.saveDevice { fail("Unconfirmed profile saved") }
        advanceUntilIdle()
        assertTrue(deviceRepository.devices.isEmpty())
        assertTrue(infraredRepository.confirmedCalls.isEmpty())
    }

    @Test
    fun epsonWizardSelectsPowerLite118AndTestsMenuOnly() = runTest(dispatcher) {
        val saved = SavedStateHandle(mapOf("type" to "PROJECTOR", "brand" to "EPSON"))
        val vm = IrSetupViewModel(saved, deviceRepository, infraredRepository) { IrAvailability.Available }
        advanceUntilIdle()

        assertEquals("EPSON_POWERLITE_118_ORIGINAL_IR", vm.state.value.selectedProfile?.id)
        assertEquals(RemoteCommand.MENU, vm.state.value.selectedProfile?.testCommand)

        vm.sendTestCommand()
        advanceUntilIdle()

        assertEquals(listOf("EPSON_POWERLITE_118_ORIGINAL_IR"), infraredRepository.testCalls)
        assertEquals(IrTestStep.TESTING_COMMAND, vm.state.value.testStep)
        assertFalse(vm.state.value.lastTransmissionMessage!!.contains("correctamente", ignoreCase = true))
    }
}
