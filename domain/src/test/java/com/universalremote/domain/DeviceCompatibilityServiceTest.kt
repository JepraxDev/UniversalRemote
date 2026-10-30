package com.universalremote.domain

import com.universalremote.core.model.*
import com.universalremote.protocol.api.ProtocolRegistry
import com.universalremote.protocol.api.RemoteProtocol
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DeviceCompatibilityServiceTest {

    private class FakeIrCatalog : IrProfileCatalog {
        override val profiles: List<IrProfile> = listOf(
            IrProfile(
                id = "lg-ir-1",
                brand = DeviceBrand.LG,
                deviceType = DeviceType.TV,
                family = "AKB",
                protocol = "NEC",
                carrierHz = 38000,
                commands = mapOf(
                    RemoteCommand.POWER to IrSignal(38000, listOf(9000, 4500)),
                    RemoteCommand.VOLUME_UP to IrSignal(38000, listOf(9000, 4500))
                ),
                source = "test",
                testCommand = RemoteCommand.POWER
            )
        )
    }

    private class FakeProtocolRegistry : ProtocolRegistry {
        override fun getProtocol(protocolType: ProtocolType): RemoteProtocol? = null
        override fun getProtocolForDevice(device: Device): RemoteProtocol? = null
    }

    private lateinit var resolver: DeviceCapabilityResolver
    private lateinit var service: DeviceCompatibilityService

    @Before
    fun setup() {
        resolver = DeviceCapabilityResolver(FakeIrCatalog(), FakeProtocolRegistry())
        service = DeviceCompatibilityService(resolver)
    }

    @Test
    fun getSupportedBrandsReturnsCatalogBrandsForTv() {
        val tvBrands = service.getSupportedBrands(DeviceType.TV)
        assertTrue(tvBrands.contains(DeviceBrand.LG))
        assertTrue(tvBrands.contains(DeviceBrand.SAMSUNG))
        assertTrue(tvBrands.contains(DeviceBrand.SONY))
    }

    @Test
    fun getSupportedBrandsReturnsCatalogBrandsForProjector() {
        val projectorBrands = service.getSupportedBrands(DeviceType.PROJECTOR)
        assertTrue(projectorBrands.contains(DeviceBrand.EPSON))
        assertTrue(projectorBrands.contains(DeviceBrand.BENQ))
        assertTrue(projectorBrands.contains(DeviceBrand.VIEWSONIC))
    }

    @Test
    fun resolveCapabilitiesForIrDeviceReturnsProfileCommands() {
        val device = Device(
            id = "d1", name = "LG TV IR", brand = DeviceBrand.LG, model = "AKB",
            deviceType = DeviceType.TV, connectionType = ConnectionType.INFRARED,
            irProfileId = "lg-ir-1"
        )
        val caps = service.resolveCapabilities(device)
        assertTrue(caps.supportedCommands.contains(RemoteCommand.POWER))
        assertTrue(caps.supportedCommands.contains(RemoteCommand.VOLUME_UP))
    }

    @Test
    fun resolveCapabilitiesForWifiSmartTvReturnsNumericAndMediaKeys() {
        val device = Device(
            id = "d2", name = "LG Smart TV Wi-Fi", brand = DeviceBrand.LG, model = "webOS",
            deviceType = DeviceType.TV, connectionType = ConnectionType.WIFI,
            protocolId = "LG_WEBOS"
        )
        val caps = service.resolveCapabilities(device)
        assertTrue(caps.supportedCommands.contains(RemoteCommand.POWER))
        assertTrue(caps.supportedCommands.contains(RemoteCommand.HOME))
        assertTrue(caps.supportedCommands.contains(RemoteCommand.NUM_1))
    }

    @Test
    fun epsonModelsAreListedIndependentlyAndPointToTheirOwnIrProfiles() {
        val powerLite = DeviceCatalog.findModel(DeviceBrand.EPSON, DeviceType.PROJECTOR, "PowerLite 118")
        val ebL260F = DeviceCatalog.findModel(DeviceBrand.EPSON, DeviceType.PROJECTOR, "EB-L260F")
        assertNotNull(powerLite)
        assertNotNull(ebL260F)
        assertEquals("EPSON_POWERLITE_118_ORIGINAL_IR", powerLite!!.defaultIrProfileId)
        assertEquals("EPSON_EB_L260F", ebL260F!!.defaultIrProfileId)
        assertNotEquals(powerLite.defaultIrProfileId, ebL260F.defaultIrProfileId)
    }

    @Test
    fun getCompatibilityStatusReturnsVerifiedForLg() {
        val status = service.getCompatibilityStatus(DeviceBrand.LG, DeviceType.TV)
        assertEquals(CompatibilityStatus.VERIFIED_PHYSICAL, status)
    }
}
