package com.universalremote.protocol.ir

import com.universalremote.core.model.*
import org.junit.Assert.*
import org.junit.Test

class VerifiedIrCatalogTest {
    private val catalog = VerifiedIrCatalog()

    @Test
    fun necEncoderGeneratesCompliantSignal() {
        val signal = NecEncoder.encode(0x04, 0xFB, 0x08)
        assertEquals(38_000, signal.carrierHz)
        // Leader (2) + 32 bits * 2 (64) + stop (1) = 67 entries
        assertEquals(67, signal.patternMicros.size)
        assertEquals(9000, signal.patternMicros[0])
        assertEquals(4500, signal.patternMicros[1])
        assertEquals(560, signal.patternMicros.last())
        // All intervals must be positive
        assertTrue(signal.patternMicros.all { it > 0 })
    }

    @Test
    fun sonyEncoderGeneratesCompliantSignal() {
        val signal = SonyEncoder.encode(1, 0x15)
        assertEquals(40_000, signal.carrierHz)
        // Header (2) + 7 command bits * 2 (14) + 5 device bits * 2 (10) = 26 entries
        assertEquals(26, signal.patternMicros.size)
        assertEquals(2400, signal.patternMicros[0])
        assertEquals(600, signal.patternMicros[1])
        assertTrue(signal.patternMicros.all { it > 0 })
    }

    @Test
    fun catalogContainsPrioritizedBrandsForTvsAndProjectors() {
        // TV profiles
        val lg = catalog.profilesFor(DeviceBrand.LG, DeviceType.TV)
        assertTrue(lg.isNotEmpty())
        assertTrue(lg.first().commands.containsKey(RemoteCommand.POWER))
        assertTrue(lg.first().commands.containsKey(RemoteCommand.MENU))

        val samsung = catalog.profilesFor(DeviceBrand.SAMSUNG, DeviceType.TV)
        assertTrue(samsung.isNotEmpty())

        val sony = catalog.profilesFor(DeviceBrand.SONY, DeviceType.TV)
        assertTrue(sony.isNotEmpty())

        val tcl = catalog.profilesFor(DeviceBrand.TCL, DeviceType.TV)
        assertTrue(tcl.isNotEmpty())

        val hisense = catalog.profilesFor(DeviceBrand.HISENSE, DeviceType.TV)
        assertTrue(hisense.isNotEmpty())

        val xiaomi = catalog.profilesFor(DeviceBrand.XIAOMI, DeviceType.TV)
        assertTrue(xiaomi.isNotEmpty())

        // Projector profiles
        val epson = catalog.profilesFor(DeviceBrand.EPSON, DeviceType.PROJECTOR)
        assertTrue(epson.isNotEmpty())
        assertTrue(epson.first().commands.containsKey(RemoteCommand.POWER))
        assertTrue(epson.first().commands.containsKey(RemoteCommand.FREEZE))
        assertTrue(epson.first().commands.containsKey(RemoteCommand.AV_MUTE))

        val benq = catalog.profilesFor(DeviceBrand.BENQ, DeviceType.PROJECTOR)
        assertTrue(benq.isNotEmpty())

        val viewsonic = catalog.profilesFor(DeviceBrand.VIEWSONIC, DeviceType.PROJECTOR)
        assertTrue(viewsonic.isNotEmpty())

        val optoma = catalog.profilesFor(DeviceBrand.OPTOMA, DeviceType.PROJECTOR)
        assertTrue(optoma.isNotEmpty())

        val nec = catalog.profilesFor(DeviceBrand.NEC, DeviceType.PROJECTOR)
        assertTrue(nec.isNotEmpty())
    }

    @Test
    fun pendingBrandsListIdentifiesMissingProtocolsWithoutFakingSupport() {
        val philipsPending = catalog.pendingFor(DeviceBrand.PHILIPS, DeviceType.TV)
        assertNotNull(philipsPending)
        assertTrue(philipsPending!!.reason.contains("RC-5"))

        val panasonicTvPending = catalog.pendingFor(DeviceBrand.PANASONIC, DeviceType.TV)
        assertNotNull(panasonicTvPending)
        assertTrue(panasonicTvPending!!.reason.contains("Kaseikyo"))

        val panasonicProjPending = catalog.pendingFor(DeviceBrand.PANASONIC, DeviceType.PROJECTOR)
        assertNotNull(panasonicProjPending)

        val sonyProjPending = catalog.pendingFor(DeviceBrand.SONY, DeviceType.PROJECTOR)
        assertNotNull(sonyProjPending)
    }

    @Test
    fun profilesHaveDistinctIdsAndSources() {
        val ids = catalog.profiles.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        catalog.profiles.forEach { profile ->
            assertTrue(profile.source.isNotBlank())
            assertTrue(profile.family.isNotBlank())
            assertTrue(profile.commands.isNotEmpty())
        }
    }
}
