package com.universalremote.data

import com.universalremote.core.database.*
import com.universalremote.core.model.*
import com.universalremote.domain.InfraredTransmitter
import com.universalremote.domain.IrProfileCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DefaultInfraredRepositoryTest {

    private class FakeDeviceDao : DeviceDao {
        val list = mutableMapOf<String, DeviceEntity>()
        override fun observeAll(): Flow<List<DeviceEntity>> = flowOf(list.values.toList())
        override suspend fun find(id: String) = list[id]
        override suspend fun insert(device: DeviceEntity) { list[device.id] = device }
        override suspend fun update(device: DeviceEntity) { list[device.id] = device }
        override suspend fun delete(id: String) { list.remove(id) }
    }

    private class FakeAssessmentDao : IrAssessmentDao {
        val list = mutableMapOf<String, IrAssessmentEntity>()
        override suspend fun find(id: String) = list[id]
        override suspend fun insert(assessment: IrAssessmentEntity) { list[assessment.id] = assessment }
        override suspend fun delete(id: String) { list.remove(id) }
    }

    private class FakeController : InfraredTransmitter {
        var availabilityResult: IrAvailability = IrAvailability.Available
        var transmitCalls = mutableListOf<Pair<Int, List<Int>>>()
        var nextTransmitResult: IrResult<Unit> = IrResult.Success(Unit)

        override fun availability() = availabilityResult
        override suspend fun supportedFrequencies() = IrResult.Success(listOf(CarrierFrequencyRange(30000, 60000)))
        override suspend fun transmit(carrierHz: Int, patternMicros: List<Int>): IrResult<Unit> {
            transmitCalls.add(carrierHz to patternMicros)
            return nextTransmitResult
        }
    }

    private val testProfile = IrProfile(
        id = "test-lg-v1",
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

    private val fakeCatalog = object : IrProfileCatalog {
        override val profiles = listOf(testProfile)
    }

    private lateinit var deviceDao: FakeDeviceDao
    private lateinit var assessmentDao: FakeAssessmentDao
    private lateinit var controller: FakeController
    private lateinit var repository: DefaultInfraredRepository

    @Before
    fun setup() {
        deviceDao = FakeDeviceDao()
        assessmentDao = FakeAssessmentDao()
        controller = FakeController()
        repository = DefaultInfraredRepository(controller, fakeCatalog, deviceDao, assessmentDao, clock = { 1000L })
    }

    @Test
    fun testProfileTransmitsAndSavesUnverifiedAssessment() = runTest {
        val result = repository.test("test-lg-v1")
        assertTrue(result is IrResult.Success)
        val assessment = (result as IrResult.Success).value
        assertEquals("test-lg-v1", assessment.id)
        assertEquals(IrValidationStatus.UNVERIFIED, assessment.status)
        assertEquals(RemoteCommand.MENU, assessment.testCommand)

        // Verify it was stored in DAO
        val saved = assessmentDao.find("test-lg-v1")
        assertNotNull(saved)
        assertEquals(IrValidationStatus.UNVERIFIED.name, saved!!.status)
        assertEquals(1, controller.transmitCalls.size)
    }

    @Test
    fun confirmRespondedUpdatesStatusToConfirmedByUser() = runTest {
        repository.test("test-lg-v1")
        val confirmResult = repository.confirm("test-lg-v1", responded = true)
        assertTrue(confirmResult is IrResult.Success)
        val updated = (confirmResult as IrResult.Success).value
        assertEquals(IrValidationStatus.CONFIRMED_BY_USER, updated.status)
        assertEquals(IrValidationStatus.CONFIRMED_BY_USER.name, assessmentDao.find("test-lg-v1")!!.status)
    }

    @Test
    fun confirmDidNotRespondUpdatesStatusToIncompatible() = runTest {
        repository.test("test-lg-v1")
        val confirmResult = repository.confirm("test-lg-v1", responded = false)
        assertTrue(confirmResult is IrResult.Success)
        val updated = (confirmResult as IrResult.Success).value
        assertEquals(IrValidationStatus.INCOMPATIBLE, updated.status)
        assertEquals(IrValidationStatus.INCOMPATIBLE.name, assessmentDao.find("test-lg-v1")!!.status)
    }

    @Test
    fun sendFailsIfDeviceNotConfigured() = runTest {
        val result = repository.send("missing-device", RemoteCommand.VOLUME_UP)
        assertEquals(IrResult.Failure(IrError.NOT_CONFIGURED), result)
    }

    @Test
    fun sendFailsIfDeviceIsNotInfrared() = runTest {
        val wifiDevice = Device("d-wifi", "TV Sala", DeviceBrand.LG, "Model", DeviceType.TV, ConnectionType.WIFI)
        deviceDao.insert(wifiDevice.toEntity())
        val result = repository.send("d-wifi", RemoteCommand.VOLUME_UP)
        assertEquals(IrResult.Failure(IrError.PROFILE_INCOMPATIBLE), result)
    }

    @Test
    fun sendPowerRequiresConfirmationIfUnconfirmed() = runTest {
        val irDevice = Device("d-ir", "TV Sala", DeviceBrand.LG, "Model", DeviceType.TV, ConnectionType.INFRARED, irProfileId = "test-lg-v1")
        deviceDao.insert(irDevice.toEntity())
        // Power requires confirmation when status is not CONFIRMED_BY_USER
        val result = repository.send("d-ir", RemoteCommand.POWER, confirmed = false)
        assertEquals(IrResult.Failure(IrError.CONFIRMATION_REQUIRED), result)
        assertEquals(0, controller.transmitCalls.size)

        // Passing confirmed = true transmits
        val confirmedResult = repository.send("d-ir", RemoteCommand.POWER, confirmed = true)
        assertEquals(IrResult.Success(Unit), confirmedResult)
        assertEquals(1, controller.transmitCalls.size)
    }

    @Test
    fun sendTransmitsNormalCommandWithoutRequiringConfirmation() = runTest {
        val irDevice = Device("d-ir", "TV Sala", DeviceBrand.LG, "Model", DeviceType.TV, ConnectionType.INFRARED, irProfileId = "test-lg-v1")
        deviceDao.insert(irDevice.toEntity())
        val result = repository.send("d-ir", RemoteCommand.VOLUME_UP)
        assertEquals(IrResult.Success(Unit), result)
        assertEquals(1, controller.transmitCalls.size)
    }

    @Test
    fun sendFailsForUnsupportedCommandInProfile() = runTest {
        val irDevice = Device("d-ir", "TV Sala", DeviceBrand.LG, "Model", DeviceType.TV, ConnectionType.INFRARED, irProfileId = "test-lg-v1")
        deviceDao.insert(irDevice.toEntity())
        val result = repository.send("d-ir", RemoteCommand.HDMI)
        assertEquals(IrResult.Failure(IrError.COMMAND_UNSUPPORTED), result)
    }
}
