package com.universalremote.domain

import com.universalremote.core.model.ConnectionType
import com.universalremote.core.model.Device
import com.universalremote.core.model.DeviceBrand
import com.universalremote.core.model.DeviceDisplayNameResolver
import com.universalremote.core.model.DeviceType
import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceDisplayNameResolverTest {

    @Test
    fun rawTechnicalMdnsNameIsSanitizedToFriendlyName() {
        val rawDevice = Device(
            id = "d1",
            name = "MiTV-AESP0-ff5b3a8f427892e7ece1a74e6d0f2627",
            brand = DeviceBrand.XIAOMI,
            model = "TV Stick 4K",
            deviceType = DeviceType.TV,
            connectionType = ConnectionType.WIFI
        )

        val cleanName = DeviceDisplayNameResolver.resolveDisplayName(rawDevice)
        assertEquals("Android TV (TV Stick Mi)", cleanName)
    }

    @Test
    fun cleanCustomNameIsPreserved() {
        val device = Device(
            id = "d2",
            name = "TV Sala de Estar",
            brand = DeviceBrand.LG,
            model = "OLED55",
            deviceType = DeviceType.TV,
            connectionType = ConnectionType.WIFI
        )

        val cleanName = DeviceDisplayNameResolver.resolveDisplayName(device)
        assertEquals("TV Sala de Estar", cleanName)
    }

    @Test
    fun resolveSubtitleGeneratesBrandAndModel() {
        val device = Device(
            id = "d3",
            name = "Proyector Aula 16",
            brand = DeviceBrand.EPSON,
            model = "EB-FH52",
            deviceType = DeviceType.PROJECTOR,
            connectionType = ConnectionType.WIFI
        )

        val subtitle = DeviceDisplayNameResolver.resolveSubtitle(device)
        assertEquals("EPSON · EB-FH52", subtitle)
    }
}
