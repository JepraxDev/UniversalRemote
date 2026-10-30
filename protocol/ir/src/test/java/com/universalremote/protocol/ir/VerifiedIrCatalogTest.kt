package com.universalremote.protocol.ir

import com.universalremote.core.model.*
import com.universalremote.domain.IrSignalValidator
import org.junit.Assert.*
import org.junit.Test

class VerifiedIrCatalogTest {
    @Test fun epsonCaptureDoesNotMislabelAspectAsLanOrInventDiscretePower() {
        val profile = VerifiedIrCatalog().profiles.first { it.id == "epson-emp822h-v1" }
        assertFalse(profile.commands.containsKey(RemoteCommand.LAN))
        assertFalse(profile.commands.containsKey(RemoteCommand.POWER_ON))
        assertEquals(NecEncoder.encode(0x83, 0x55, 0x9A), profile.commands[RemoteCommand.MENU])
        assertEquals(NecEncoder.encode(0x83, 0x55, 0x8A), profile.commands[RemoteCommand.ASPECT_RATIO])
    }

    @Test fun powerLite118ProfileCoversOriginalRemoteWithTraceableNecCodes() {
        val profile = VerifiedIrCatalog().profiles.first { it.id == "EPSON_POWERLITE_118_ORIGINAL_IR" }
        assertEquals(DeviceBrand.EPSON, profile.brand)
        assertEquals(DeviceType.PROJECTOR, profile.deviceType)
        assertEquals(38_000, profile.carrierHz)
        assertEquals(RemoteCommand.MENU, profile.testCommand)
        assertTrue(profile.source.contains("131,85.csv"))
        assertEquals("NECext (NEC2, device 131.85)", profile.protocol)

        val expected = mapOf(
            RemoteCommand.POWER to 0x90,
            RemoteCommand.POWER_OFF to 0x91,
            RemoteCommand.INPUT to 0x8C,
            RemoteCommand.HDMI to 0x73,
            RemoteCommand.HDMI_2 to 0x77,
            RemoteCommand.COMPUTER to 0x94,
            RemoteCommand.USB to 0x76,
            RemoteCommand.LAN to 0x74,
            RemoteCommand.MENU to 0x9A,
            RemoteCommand.BACK to 0x84,
            RemoteCommand.OK to 0x85,
            RemoteCommand.UP to 0xB0,
            RemoteCommand.RIGHT to 0xB1,
            RemoteCommand.DOWN to 0xB2,
            RemoteCommand.LEFT to 0xB3,
            RemoteCommand.USER to 0x9F,
            RemoteCommand.AUTO_SETUP to 0x9E,
            RemoteCommand.ID to 0x31,
            RemoteCommand.DEFAULT to 0x39,
            RemoteCommand.ASPECT_RATIO to 0x8A,
            RemoteCommand.COLOR_MODE to 0x8F,
            RemoteCommand.E_ZOOM_IN to 0x88,
            RemoteCommand.E_ZOOM_OUT to 0x89,
            RemoteCommand.VOLUME_UP to 0x98,
            RemoteCommand.VOLUME_DOWN to 0x99,
            RemoteCommand.AV_MUTE to 0x93,
            RemoteCommand.FREEZE to 0x92,
            RemoteCommand.SPLIT to 0x82,
            RemoteCommand.HOME to 0x95,
            RemoteCommand.NUM_0 to 0xA0,
            RemoteCommand.NUM_9 to 0xA9,
        )
        expected.forEach { (command, code) ->
            assertEquals("$command", NecEncoder.encode(0x83, 0x55, code), profile.commands[command])
        }
        // Power alternates with one frame; POWER_ON is the same code, never a second automatic send.
        assertEquals(profile.commands[RemoteCommand.POWER], profile.commands[RemoteCommand.POWER_ON])
        assertNotEquals(profile.commands[RemoteCommand.POWER], profile.commands[RemoteCommand.POWER_OFF])
        // Num is a modifier and must not invent a standalone IR frame.
        assertFalse(profile.commands.containsKey(RemoteCommand.NUM_MODIFIER))
        // Every command carries provenance and never claims physical verification.
        assertEquals(profile.commands.keys, profile.commandInfo.keys)
        profile.commandInfo.values.forEach { info ->
            assertTrue(info.source.isNotBlank())
            assertNotEquals(IrCommandVerification.CONFIRMED_BY_USER, info.verification)
        }
    }
    @Test
    fun ebL260fUsesItsOwnRawProfileAndNeverReusesPowerLiteCodes() {
        val catalog = VerifiedIrCatalog()
        val eb = catalog.profiles.first { it.id == "EPSON_EB_L260F" }
        val powerLite = catalog.profiles.first { it.id == "EPSON_POWERLITE_118_ORIGINAL_IR" }

        assertEquals(DeviceBrand.EPSON, eb.brand)
        assertEquals(DeviceType.PROJECTOR, eb.deviceType)
        assertEquals(38_000, eb.carrierHz)
        assertTrue(eb.source.contains("Epson_EB-L260F.ir"))
        assertTrue(eb.protocol.contains("raw", ignoreCase = true))
        assertEquals(RemoteCommand.INPUT, eb.testCommand)
        assertNotNull(eb.testNote)

        // Captured buttons only.
        val expected = setOf(
            RemoteCommand.POWER, RemoteCommand.INPUT, RemoteCommand.FREEZE,
            RemoteCommand.E_ZOOM_IN, RemoteCommand.E_ZOOM_OUT, RemoteCommand.AV_MUTE,
            RemoteCommand.SPLIT, RemoteCommand.HOME, RemoteCommand.OK,
            RemoteCommand.VOLUME_UP, RemoteCommand.VOLUME_DOWN,
            RemoteCommand.LEFT, RemoteCommand.DOWN, RemoteCommand.RIGHT, RemoteCommand.UP,
        )
        assertEquals(expected, eb.commands.keys)
        // Menu/Esc/Aspect/Color Mode/Computer/USB/LAN/Info/Settings have no verified capture.
        assertFalse(eb.commands.containsKey(RemoteCommand.MENU))
        assertFalse(eb.commands.containsKey(RemoteCommand.BACK))
        assertFalse(eb.commands.containsKey(RemoteCommand.ASPECT_RATIO))
        assertFalse(eb.commands.containsKey(RemoteCommand.COLOR_MODE))
        assertFalse(eb.commands.containsKey(RemoteCommand.COMPUTER))
        assertFalse(eb.commands.containsKey(RemoteCommand.USB))
        assertFalse(eb.commands.containsKey(RemoteCommand.LAN))
        assertFalse(eb.commands.containsKey(RemoteCommand.INFO))
        assertFalse(eb.commands.containsKey(RemoteCommand.SETTINGS_KEY))

        // Every raw capture is valid and not interchangeable with the PowerLite profile.
        eb.commands.forEach { (command, signal) ->
            assertNull(IrSignalValidator.validate(signal.carrierHz, signal.patternMicros))
            assertNotEquals("$command must not reuse PowerLite", powerLite.commands[command], signal)
        }
        assertEquals(eb.commands.keys, eb.commandInfo.keys)
        eb.commandInfo.values.forEach {
            assertTrue(it.source.contains("Epson_EB-L260F.ir"))
            assertEquals(IrCommandVerification.COMMUNITY_CAPTURED, it.verification)
        }
    }

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
