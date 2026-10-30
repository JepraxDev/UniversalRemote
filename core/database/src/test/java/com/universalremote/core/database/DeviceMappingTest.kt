package com.universalremote.core.database

import com.universalremote.core.model.*
import org.junit.Assert.*
import org.junit.Test

class DeviceMappingTest {
    @Test fun fullDeviceRoundTripPreservesUnknownBrandAndOptionalFields() {
        val device = Device(
            id = "id-42", name = "Sala", brand = DeviceBrand("FUTURE_BRAND"), model = "X1", deviceType = DeviceType.PROJECTOR,
            connectionType = ConnectionType.WIFI, ipAddress = "192.168.1.5", port = 4352, protocolId = "PJLINK",
            irProfileId = "profile-1", roomName = "Sala principal", isFavorite = true
        )
        assertEquals(device, device.toEntity().toModel())
    }
    @Test fun infraredDeviceWithNullNetworkFieldsRoundTrips() {
        val device = Device("ir-1", "TV", DeviceBrand.LG, "", DeviceType.TV, ConnectionType.INFRARED)
        val result = device.toEntity().toModel()
        assertEquals(device, result)
        assertNull(result.ipAddress)
        assertNull(result.port)
    }
    @Test fun irAssessmentRoundTripPreservesStatusAndCommands() {
        val assessment = IrAssessment(
            id = "test-device-1",
            profileId = "lg-akb72915206-v1",
            status = IrValidationStatus.CONFIRMED_BY_USER,
            testedAt = 123456789L,
            testCommand = RemoteCommand.MENU,
            supportedCommands = setOf(RemoteCommand.POWER, RemoteCommand.MENU, RemoteCommand.VOLUME_UP)
        )
        val entity = assessment.toEntity()
        val mapped = entity.toModel()
        assertEquals(assessment, mapped)
        assertEquals(3, mapped.supportedCommands.size)
        assertTrue(mapped.supportedCommands.contains(RemoteCommand.POWER))
    }
}
