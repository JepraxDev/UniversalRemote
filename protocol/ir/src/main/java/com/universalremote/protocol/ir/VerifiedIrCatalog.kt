package com.universalremote.protocol.ir

import com.universalremote.core.model.*
import com.universalremote.domain.IrProfileCatalog

/** One NEC/NECext frame, bytes in wire order, each byte LSB first. No repeats. */
object NecEncoder {
    fun encode(addressLow: Int, addressHigh: Int, command: Int, commandHigh: Int = command xor 0xFF): IrSignal {
        val bytes = listOf(addressLow, addressHigh, command, commandHigh)
        require(bytes.all { it in 0..255 })
        return IrSignal(38_000, buildList {
            add(9000); add(4500)
            bytes.forEach { byte -> repeat(8) { bit -> add(560); add(if (byte and (1 shl bit) != 0) 1690 else 560) } }
            add(560)
        })
    }
}

/** Sony SIRC 12-bit protocol. 40kHz carrier. */
object SonyEncoder {
    fun encode(device: Int, command: Int): IrSignal {
        require(device in 0..31 && command in 0..127)
        return IrSignal(40_000, buildList {
            add(2400); add(600) // Header
            repeat(7) { bit -> add(if (command and (1 shl bit) != 0) 1200 else 600); add(600) }
            repeat(5) { bit -> add(if (device and (1 shl bit) != 0) 1200 else 600); add(600) }
        })
    }
}

/** Codes for the Epson 2198635 / 219863500 remote used by the PowerLite 118. */
object EpsonPowerLite118 {
    const val SOURCE =
        "https://github.com/probonopd/irdb/blob/master/codes/Epson/Projector/131,85.csv"
    private const val IRDB = "irdb NEC2 Epson 131,85 (CSV)"
    private const val FLIPPER = "Flipper-IRDB Epson 83 55 NECext captures"
    private const val MANUAL = "Epson PowerLite 118 User's Guide (remote functions)"

    val commands: Map<RemoteCommand, Int> = mapOf(
        RemoteCommand.POWER to 0x90,
        RemoteCommand.POWER_ON to 0x90,
        RemoteCommand.POWER_OFF to 0x91,
        RemoteCommand.INPUT to 0x8C,        // Source Search
        RemoteCommand.HDMI to 0x73,
        RemoteCommand.HDMI_1 to 0x73,
        RemoteCommand.HDMI_2 to 0x77,
        RemoteCommand.COMPUTER to 0x94,
        RemoteCommand.USB to 0x76,
        RemoteCommand.LAN to 0x74,
        RemoteCommand.MENU to 0x9A,
        RemoteCommand.BACK to 0x84,        // Esc
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
        RemoteCommand.NUM_1 to 0xA1,
        RemoteCommand.NUM_2 to 0xA2,
        RemoteCommand.NUM_3 to 0xA3,
        RemoteCommand.NUM_4 to 0xA4,
        RemoteCommand.NUM_5 to 0xA5,
        RemoteCommand.NUM_6 to 0xA6,
        RemoteCommand.NUM_7 to 0xA7,
        RemoteCommand.NUM_8 to 0xA8,
        RemoteCommand.NUM_9 to 0xA9,
    )

    private val labels = mapOf(
        RemoteCommand.POWER to "Power (alternancia)",
        RemoteCommand.POWER_ON to "Encendido",
        RemoteCommand.POWER_OFF to "Standby discreto",
        RemoteCommand.INPUT to "Source Search",
        RemoteCommand.HDMI to "HDMI",
        RemoteCommand.HDMI_1 to "HDMI 1",
        RemoteCommand.HDMI_2 to "HDMI 2",
        RemoteCommand.COMPUTER to "Computer",
        RemoteCommand.USB to "USB",
        RemoteCommand.LAN to "LAN",
        RemoteCommand.MENU to "Menu",
        RemoteCommand.BACK to "Esc",
        RemoteCommand.OK to "Enter",
        RemoteCommand.UP to "Arriba",
        RemoteCommand.RIGHT to "Derecha",
        RemoteCommand.DOWN to "Abajo",
        RemoteCommand.LEFT to "Izquierda",
        RemoteCommand.USER to "User",
        RemoteCommand.AUTO_SETUP to "Auto",
        RemoteCommand.ID to "ID",
        RemoteCommand.DEFAULT to "Default",
        RemoteCommand.ASPECT_RATIO to "Aspect",
        RemoteCommand.COLOR_MODE to "Color Mode",
        RemoteCommand.E_ZOOM_IN to "E-Zoom +",
        RemoteCommand.E_ZOOM_OUT to "E-Zoom −",
        RemoteCommand.VOLUME_UP to "Volume +",
        RemoteCommand.VOLUME_DOWN to "Volume −",
        RemoteCommand.AV_MUTE to "A/V Mute",
        RemoteCommand.FREEZE to "Freeze",
        RemoteCommand.SPLIT to "Split",
        RemoteCommand.HOME to "Home",
        RemoteCommand.NUM_0 to "0 / All",
        RemoteCommand.NUM_1 to "1",
        RemoteCommand.NUM_2 to "2",
        RemoteCommand.NUM_3 to "3",
        RemoteCommand.NUM_4 to "4",
        RemoteCommand.NUM_5 to "5",
        RemoteCommand.NUM_6 to "6",
        RemoteCommand.NUM_7 to "7",
        RemoteCommand.NUM_8 to "8",
        RemoteCommand.NUM_9 to "9",
    )

    fun info(): Map<RemoteCommand, IrCommandInfo> = commands.keys.associateWith { command ->
        val communityOnly = command in setOf(
            RemoteCommand.ID, RemoteCommand.DEFAULT, RemoteCommand.SPLIT, RemoteCommand.HOME,
            RemoteCommand.POWER_OFF, RemoteCommand.HDMI_2,
        ) || command.name.startsWith("NUM_")
        val note = when (command) {
            RemoteCommand.POWER -> "Alterna encendido/espera. La app envía un solo comando por pulsación."
            RemoteCommand.POWER_OFF -> "Standby discreto; pendiente de confirmación física en el 118."
            RemoteCommand.DEFAULT -> "Función 57 documentada en foros; pendiente de confirmación física."
            RemoteCommand.HOME -> "Comparte la función Help en la familia Epson; pendiente de confirmación física."
            RemoteCommand.ID -> "Selecciona ID; mantener ID y pulsar un número asigna el ID. Por sí solo solo muestra el ID actual."
            RemoteCommand.SPLIT -> "Split solo está disponible si el modelo/configuración admite pantalla dividida."
            RemoteCommand.AV_MUTE -> "Oculta temporalmente imagen y sonido; no es un mute de audio independiente."
            RemoteCommand.E_ZOOM_IN, RemoteCommand.E_ZOOM_OUT -> "Zoom digital de imagen; no mueve el enfoque de la lente."
            RemoteCommand.NUM_0 -> "La tecla 0/All envía el dígito 0 y permite operar todos los proyectores."
            else -> if (command.name.startsWith("NUM_")) "Tecla numérica; pendiente de confirmación física." else null
        }
        IrCommandInfo(
            function = labels[command] ?: command.name,
            protocol = "NECext",
            source = if (communityOnly) FLIPPER else "$IRDB; $MANUAL",
            verification = IrCommandVerification.COMMUNITY_CAPTURED,
            note = note,
        )
    }
}

/**
 * Raw 38 kHz captures for the Epson EB-L260F published in Flipper-IRDB.
 *
 * These are the exact captured timings, including the repeat frames and inter-frame
 * gaps. They are NOT decoded into NEC/NECext and they are NOT the PowerLite 118
 * codes: the EB-L260F capture has 14-bit frames that do not match 83 55.
 */
object EpsonEbL260F {
    const val SOURCE =
        "https://github.com/Lucaslhm/Flipper-IRDB/blob/main/Projectors/Epson/Epson_EB-L260F.ir"
    private const val MANUAL = "Epson EB-L210W/EB-L260F/EB-L265F/EB-L270F User's Guide (function list only)"

    private val raw: Map<RemoteCommand, List<Int>> = mapOf(
        RemoteCommand.POWER to listOf(2383, 509, 648, 504, 1227, 504, 648, 528, 624, 1079, 677, 1054, 651, 529, 624, 529, 624, 529, 623, 529, 623, 530, 621, 532, 620, 534, 618, 536, 617, 77252, 2374, 536, 619, 535, 1196, 535, 617, 536, 617, 1114, 617, 1114, 617, 536, 617, 536, 617, 536, 617, 536, 617, 536, 617, 536, 617, 536, 617, 536, 617),
        RemoteCommand.INPUT to listOf(2372, 516, 643, 509, 1220, 511, 642, 534, 594, 1109, 621, 558, 1171, 1112, 1222, 1112, 1195, 1113, 1193, 539, 615, 538, 615, 538, 615, 538, 615, 538, 615, 73785, 2369, 541, 617, 537, 1192, 539, 614, 538, 615, 1116, 615, 538, 1192, 1117, 1192, 1117, 1192, 1117, 1192, 540, 615, 538, 615, 538, 615, 538, 615, 538, 615),
        RemoteCommand.FREEZE to listOf(2405, 487, 618, 532, 1199, 532, 620, 533, 620, 1109, 622, 1110, 1201, 557, 1200, 532, 1198, 533, 1196, 537, 616, 537, 616, 537, 616, 537, 616, 537, 616, 74942, 2374, 538, 617, 536, 1195, 537, 615, 537, 616, 1115, 616, 1117, 1194, 538, 1194, 538, 1194, 538, 1194, 539, 615, 538, 615, 538, 615, 538, 615, 539, 614),
        RemoteCommand.E_ZOOM_IN to listOf(2409, 482, 674, 479, 1198, 531, 621, 532, 621, 1108, 623, 556, 1174, 557, 596, 556, 596, 556, 622, 531, 1198, 534, 618, 535, 617, 536, 617, 536, 617, 76690, 2351, 537, 618, 535, 1195, 536, 616, 536, 617, 1114, 617, 536, 1195, 537, 617, 536, 617, 537, 616, 536, 1195, 538, 616, 537, 616, 537, 616, 536, 617, 76686, 2352, 537, 618, 535, 1195, 537, 615, 537, 616, 1115, 616, 537, 1194, 538, 615, 537, 616, 537, 616, 536, 1195, 537, 616, 536, 617, 536, 617, 536, 617),
        RemoteCommand.E_ZOOM_OUT to listOf(2355, 535, 620, 532, 1224, 529, 623, 530, 623, 1083, 647, 530, 621, 531, 1196, 536, 617, 536, 616, 536, 1195, 536, 617, 536, 617, 536, 617, 536, 617, 76664, 2375, 536, 618, 535, 1195, 536, 616, 536, 617, 1114, 617, 536, 616, 536, 1195, 537, 617, 536, 617, 536, 1195, 537, 617, 536, 617, 536, 617, 536, 617),
        RemoteCommand.AV_MUTE to listOf(2384, 505, 649, 525, 1204, 525, 626, 526, 626, 1082, 648, 1105, 625, 1108, 1199, 532, 621, 532, 1199, 533, 620, 533, 620, 533, 620, 533, 620, 533, 620, 75498, 2377, 533, 621, 532, 1199, 532, 620, 532, 620, 1111, 619, 1110, 620, 1112, 1198, 533, 620, 533, 1198, 533, 620, 533, 620, 533, 620, 533, 620, 533, 620, 75510, 2377, 533, 622, 532, 1199, 532, 620, 533, 620, 1111, 620, 1111, 619, 1112, 1198, 533, 620, 533, 1198, 533, 620, 533, 620, 533, 620, 533, 620, 533, 620),
        RemoteCommand.SPLIT to listOf(2382, 531, 625, 528, 1201, 528, 623, 530, 621, 1109, 620, 533, 619, 534, 1196, 536, 617, 535, 1196, 536, 617, 536, 617, 536, 617, 535, 618, 535, 618, 76651, 2375, 535, 619, 534, 1196, 534, 618, 535, 618, 1112, 618, 535, 618, 535, 1195, 535, 618, 535, 1196, 535, 618, 535, 618, 535, 618, 535, 618, 535, 618, 76644, 2375, 535, 619, 533, 1197, 534, 618, 535, 618, 1113, 617, 535, 618, 535, 1195, 536, 617, 535, 1196, 536, 617, 535, 618, 535, 618, 535, 617, 535, 617, 76675, 2351, 536, 618, 534, 1196, 534, 618, 535, 618, 1113, 618, 535, 618, 535, 1196, 536, 617, 535, 1196, 536, 617, 536, 617, 535, 618, 536, 617, 535, 617),
        RemoteCommand.HOME to listOf(2384, 503, 652, 498, 1231, 499, 652, 501, 676, 1051, 653, 525, 627, 1078, 652, 525, 626, 526, 625, 527, 624, 529, 622, 530, 621, 532, 620, 532, 620, 77219, 2376, 532, 622, 531, 1199, 532, 619, 533, 620, 1110, 620, 533, 620, 1110, 620, 533, 620, 533, 620, 533, 620, 533, 620, 533, 620, 533, 620, 533, 619, 77229, 2377, 533, 621, 531, 1198, 532, 619, 533, 619, 1110, 620, 533, 620, 1110, 620, 532, 620, 533, 619, 533, 620, 533, 619, 533, 619, 533, 619, 533, 619),
        RemoteCommand.OK to listOf(2354, 559, 623, 529, 1201, 529, 623, 529, 623, 529, 622, 1108, 621, 532, 619, 535, 617, 1114, 617, 535, 617, 535, 618, 535, 618, 535, 618, 535, 617, 77221, 2377, 535, 619, 534, 1196, 534, 618, 535, 618, 535, 617, 1113, 618, 535, 618, 535, 618, 1113, 618, 535, 617, 536, 616, 536, 617, 535, 617, 536, 617, 77226, 2375, 536, 618, 534, 1196, 535, 617, 536, 617, 535, 618, 1113, 617, 535, 617, 535, 618, 1113, 617, 535, 617, 535, 617, 535, 617, 535, 617, 536, 617, 77226, 2375, 536, 618, 534, 1196, 534, 618, 536, 617, 535, 618, 1113, 617, 536, 617, 536, 617, 1113, 617, 536, 616, 535, 618, 536, 617, 536, 617, 535, 618, 77226, 2375, 536, 618, 534, 1196, 535, 617, 535, 618, 536, 617, 1113, 617, 536, 617, 535, 618, 1113, 617, 535, 617, 536, 616, 536, 617, 536, 617, 536, 617),
        RemoteCommand.VOLUME_UP to listOf(2384, 530, 650, 501, 1204, 527, 625, 527, 625, 1106, 623, 529, 622, 531, 620, 533, 1197, 535, 618, 534, 1197, 535, 619, 535, 618, 534, 619, 535, 618, 76689, 2353, 535, 619, 534, 1197, 534, 618, 535, 618, 1113, 618, 535, 618, 535, 618, 535, 1196, 536, 618, 535, 1196, 536, 617, 535, 618, 535, 618, 535, 618, 76668, 2377, 535, 619, 534, 1196, 534, 618, 535, 618, 1113, 617, 535, 618, 535, 618, 534, 1197, 535, 618, 534, 1197, 535, 618, 535, 618, 535, 618, 535, 618, 76650, 2376, 535, 619, 533, 1197, 534, 618, 535, 618, 1112, 618, 534, 618, 535, 618, 535, 1195, 535, 618, 534, 1196, 535, 618, 535, 617, 535, 617, 535, 618),
        RemoteCommand.VOLUME_DOWN to listOf(2383, 508, 647, 526, 1203, 527, 624, 528, 624, 1083, 646, 1108, 1199, 532, 1198, 534, 1197, 536, 617, 535, 1196, 536, 617, 535, 618, 535, 618, 535, 618, 74923, 2377, 535, 620, 534, 1197, 535, 617, 535, 618, 1113, 618, 1114, 1196, 535, 1197, 536, 1196, 536, 618, 535, 1196, 536, 618, 536, 617, 535, 618, 536, 617, 74930, 2375, 536, 618, 534, 1196, 534, 618, 535, 618, 1113, 617, 1114, 1196, 535, 1196, 535, 1196, 536, 617, 535, 1196, 536, 617, 535, 618, 535, 618, 535, 617),
        RemoteCommand.LEFT to listOf(2410, 478, 651, 500, 1228, 500, 650, 501, 650, 525, 626, 526, 1202, 1081, 648, 527, 624, 528, 1200, 531, 621, 532, 620, 532, 620, 532, 620, 533, 619, 31833, 2351, 535, 620, 532, 1197, 532, 619, 533, 619, 533, 619, 533, 1197, 1111, 619, 533, 619, 533, 1197, 533, 619, 533, 619, 533, 619, 533, 619, 533, 619, 31834, 2351, 535, 620, 532, 1198, 532, 619, 533, 619, 533, 619, 533, 1197, 1111, 619, 533, 619, 533, 1197, 533, 620, 533, 619, 533, 619, 533, 619, 533, 619),
        RemoteCommand.DOWN to listOf(2386, 502, 653, 500, 1229, 525, 651, 500, 626, 526, 625, 526, 1204, 527, 625, 527, 624, 529, 622, 530, 621, 1109, 620, 532, 620, 533, 1197, 534, 619, 31839, 2352, 536, 619, 533, 1197, 532, 619, 533, 619, 533, 619, 533, 1197, 534, 619, 533, 619, 533, 619, 533, 619, 1111, 619, 533, 619, 533, 1197, 534, 619, 31845, 2352, 536, 619, 533, 1197, 532, 620, 533, 619, 533, 620, 533, 1197, 534, 619, 533, 619, 533, 619, 533, 619, 1111, 619, 533, 619, 533, 1197, 534, 619, 31843, 2352, 536, 619, 533, 1197, 533, 618, 533, 620, 533, 619, 533, 1197, 534, 619, 533, 619, 533, 619, 533, 620, 1111, 619, 534, 618, 533, 1197, 534, 619),
        RemoteCommand.RIGHT to listOf(2354, 531, 624, 527, 1202, 528, 624, 529, 651, 501, 651, 1077, 652, 1077, 652, 526, 625, 526, 625, 527, 624, 528, 623, 530, 621, 532, 620, 533, 619, 32416, 2351, 535, 620, 532, 1198, 532, 620, 533, 619, 533, 620, 1111, 619, 1111, 619, 533, 619, 533, 619, 533, 619, 533, 619, 533, 619, 533, 620, 533, 619, 32421, 2352, 535, 620, 532, 1198, 532, 620, 533, 619, 533, 620, 1111, 619, 1111, 619, 533, 619, 533, 619, 533, 620, 533, 619, 533, 619, 533, 620, 533, 619, 32427, 2352, 535, 620, 532, 1198, 532, 620, 533, 619, 533, 619, 1111, 620, 1111, 619, 533, 620, 533, 619, 533, 619, 533, 620, 533, 619, 533, 619, 533, 620),
        RemoteCommand.UP to listOf(2354, 532, 623, 529, 1201, 528, 623, 529, 652, 501, 651, 1076, 654, 525, 627, 526, 626, 526, 625, 527, 624, 1106, 622, 530, 621, 532, 620, 533, 619, 32421, 2351, 535, 620, 532, 1198, 532, 619, 533, 619, 533, 619, 1110, 620, 533, 619, 533, 620, 533, 619, 533, 619, 1111, 619, 533, 620, 533, 619, 533, 619, 32418, 2352, 535, 620, 532, 1198, 532, 620, 533, 620, 533, 620, 1111, 620, 533, 620, 533, 619, 533, 620, 533, 620, 1111, 620, 533, 620, 533, 619, 533, 620),
    )

    val commands: Map<RemoteCommand, IrSignal> =
        raw.mapValues { (_, pattern) -> IrSignal(38_000, pattern) }

    private val labels = mapOf(
        RemoteCommand.POWER to "Power (alternancia)",
        RemoteCommand.INPUT to "Source Search",
        RemoteCommand.FREEZE to "Freeze",
        RemoteCommand.E_ZOOM_IN to "E-Zoom +",
        RemoteCommand.E_ZOOM_OUT to "E-Zoom −",
        RemoteCommand.AV_MUTE to "A/V Mute",
        RemoteCommand.SPLIT to "Split",
        RemoteCommand.HOME to "Home",
        RemoteCommand.OK to "Enter",
        RemoteCommand.VOLUME_UP to "Volume +",
        RemoteCommand.VOLUME_DOWN to "Volume −",
        RemoteCommand.LEFT to "Izquierda",
        RemoteCommand.DOWN to "Abajo",
        RemoteCommand.RIGHT to "Derecha",
        RemoteCommand.UP to "Arriba",
    )

    fun info(): Map<RemoteCommand, IrCommandInfo> = commands.keys.associateWith { command ->
        IrCommandInfo(
            function = labels[command] ?: command.name,
            protocol = "Raw capturado a 38 kHz (14 bits por trama)",
            source = SOURCE,
            verification = IrCommandVerification.COMMUNITY_CAPTURED,
            note = when (command) {
                RemoteCommand.POWER ->
                    "Captura de alternancia. Sin POWER_ON/POWER_OFF discretos verificados para este modelo."
                RemoteCommand.INPUT -> "Botón Source Search del control original; también se usa como prueba disponible porque Menu no tiene captura."
                RemoteCommand.AV_MUTE -> "La captura Flipper se llama Black_screen: oculta la imagen, no hay mute de audio independiente verificado."
                RemoteCommand.E_ZOOM_IN, RemoteCommand.E_ZOOM_OUT ->
                    "Zoom digital de imagen; no mueve el enfoque ni el zoom manual de la lente."
                RemoteCommand.SPLIT -> "Solo actúa si el proyector admite pantalla dividida."
                else -> "Patrón raw comunitario; incluye las repeticiones capturadas. Recepción pendiente de prueba física."
            },
        )
    }
}

/** Source-verified community captures; physical compatibility is confirmed per device by users. */
class VerifiedIrCatalog : IrProfileCatalog {
    override val profiles = listOf(
        // === TELEVISORES ===
        profile("lg-akb72915206-v1", DeviceBrand.LG, DeviceType.TV, "AKB72915206 (Smart TV)", "NEC", 0x04, 0xFB,
            "TVs/LG/LG_AKB72915206.ir", mapOf(
                RemoteCommand.POWER to 0x08,
                RemoteCommand.POWER_ON to 0xC4,
                RemoteCommand.POWER_OFF to 0xC5,
                RemoteCommand.INPUT to 0x0B,
                RemoteCommand.VOLUME_UP to 0x02,
                RemoteCommand.VOLUME_DOWN to 0x03,
                RemoteCommand.CHANNEL_UP to 0x00,
                RemoteCommand.CHANNEL_DOWN to 0x01,
                RemoteCommand.MUTE to 0x09,
                RemoteCommand.MENU to 0x43,
                RemoteCommand.UP to 0x40,
                RemoteCommand.DOWN to 0x41,
                RemoteCommand.LEFT to 0x07,
                RemoteCommand.RIGHT to 0x06,
                RemoteCommand.OK to 0x44,
                RemoteCommand.BACK to 0x28,
                RemoteCommand.HOME to 0x7C,
                RemoteCommand.HDMI_1 to 0xCE,
                RemoteCommand.HDMI_2 to 0xCC,
            )),
        profile("samsung-aa59-v1", DeviceBrand.SAMSUNG, DeviceType.TV, "AA59 Series (Smart TV)", "NEC", 0x07, 0x07,
            "TVs/Samsung/Samsung_AA59-00600A.ir", mapOf(
                RemoteCommand.POWER to 0x02,
                RemoteCommand.POWER_ON to 0x99,
                RemoteCommand.POWER_OFF to 0x98,
                RemoteCommand.VOLUME_UP to 0x07,
                RemoteCommand.VOLUME_DOWN to 0x0B,
                RemoteCommand.MUTE to 0x0F,
                RemoteCommand.CHANNEL_UP to 0x12,
                RemoteCommand.CHANNEL_DOWN to 0x10,
                RemoteCommand.UP to 0x60,
                RemoteCommand.DOWN to 0x61,
                RemoteCommand.LEFT to 0x65,
                RemoteCommand.RIGHT to 0x62,
                RemoteCommand.OK to 0x68,
                RemoteCommand.MENU to 0x1A,
                RemoteCommand.BACK to 0x58,
                RemoteCommand.HOME to 0x79,
                RemoteCommand.INPUT to 0x01,
                RemoteCommand.HDMI_1 to 0xEC,
                RemoteCommand.HDMI_2 to 0xED,
            )),
        IrProfile("sony-tv-sirc12", DeviceBrand.SONY, DeviceType.TV, "Bravia (SIRC 12-bit)", "SIRC12", 40_000,
            mapOf(
                RemoteCommand.POWER to SonyEncoder.encode(1, 0x15),
                RemoteCommand.POWER_ON to SonyEncoder.encode(1, 0x2E),
                RemoteCommand.POWER_OFF to SonyEncoder.encode(1, 0x2F),
                RemoteCommand.VOLUME_UP to SonyEncoder.encode(1, 0x12),
                RemoteCommand.VOLUME_DOWN to SonyEncoder.encode(1, 0x13),
                RemoteCommand.MUTE to SonyEncoder.encode(1, 0x14),
                RemoteCommand.CHANNEL_UP to SonyEncoder.encode(1, 0x10),
                RemoteCommand.CHANNEL_DOWN to SonyEncoder.encode(1, 0x11),
                RemoteCommand.UP to SonyEncoder.encode(1, 0x74),
                RemoteCommand.DOWN to SonyEncoder.encode(1, 0x75),
                RemoteCommand.LEFT to SonyEncoder.encode(1, 0x34),
                RemoteCommand.RIGHT to SonyEncoder.encode(1, 0x33),
                RemoteCommand.OK to SonyEncoder.encode(1, 0x65),
                RemoteCommand.MENU to SonyEncoder.encode(1, 0x60),
                RemoteCommand.BACK to SonyEncoder.encode(1, 0x25),
                RemoteCommand.HOME to SonyEncoder.encode(1, 0x60),
                RemoteCommand.INPUT to SonyEncoder.encode(1, 0x25),
            ),
            "https://github.com/Lucaslhm/Flipper-IRDB/blob/main/TVs/Sony/Sony_TV.ir",
            RemoteCommand.MENU
        ),
        profile("tcl-rc802n-v1", DeviceBrand.TCL, DeviceType.TV, "RC802N Series", "NEC", 0x20, 0xDF,
            "TVs/TCL/TCL_RC802N.ir", mapOf(
                RemoteCommand.POWER to 0x12,
                RemoteCommand.VOLUME_UP to 0x1A,
                RemoteCommand.VOLUME_DOWN to 0x1E,
                RemoteCommand.MUTE to 0x10,
                RemoteCommand.UP to 0x00,
                RemoteCommand.DOWN to 0x01,
                RemoteCommand.LEFT to 0x02,
                RemoteCommand.RIGHT to 0x03,
                RemoteCommand.OK to 0x07,
                RemoteCommand.MENU to 0x04,
                RemoteCommand.BACK to 0x05,
                RemoteCommand.HOME to 0x0B,
                RemoteCommand.INPUT to 0x0F,
            )),
        profile("hisense-en2b27-v1", DeviceBrand.HISENSE, DeviceType.TV, "EN2B27 Series", "NEC", 0xFD, 0x02,
            "TVs/Hisense/Hisense_EN2B27.ir", mapOf(
                RemoteCommand.POWER to 0x05,
                RemoteCommand.VOLUME_UP to 0x0A,
                RemoteCommand.VOLUME_DOWN to 0x0B,
                RemoteCommand.MUTE to 0x0D,
                RemoteCommand.CHANNEL_UP to 0x0E,
                RemoteCommand.CHANNEL_DOWN to 0x0F,
                RemoteCommand.UP to 0x16,
                RemoteCommand.DOWN to 0x17,
                RemoteCommand.LEFT to 0x18,
                RemoteCommand.RIGHT to 0x19,
                RemoteCommand.OK to 0x1A,
                RemoteCommand.MENU to 0x13,
                RemoteCommand.BACK to 0x14,
                RemoteCommand.HOME to 0x32,
                RemoteCommand.INPUT to 0x01,
            )),
        profile("xiaomi-mitv-v1", DeviceBrand.XIAOMI, DeviceType.TV, "Mi TV IR Series", "NEC", 0x10, 0xEF,
            "TVs/Xiaomi/Xiaomi_Mi_TV.ir", mapOf(
                RemoteCommand.POWER to 0x1A,
                RemoteCommand.VOLUME_UP to 0x07,
                RemoteCommand.VOLUME_DOWN to 0x0B,
                RemoteCommand.UP to 0x02,
                RemoteCommand.DOWN to 0x03,
                RemoteCommand.LEFT to 0x04,
                RemoteCommand.RIGHT to 0x05,
                RemoteCommand.OK to 0x06,
                RemoteCommand.BACK to 0x09,
                RemoteCommand.HOME to 0x08,
                RemoteCommand.MENU to 0x01,
            )),

        // === PROYECTORES ===
        profile("lg-projector-v1", DeviceBrand.LG, DeviceType.PROJECTOR, "CineBeam / LG Projector Series", "NEC", 0x04, 0xFB,
            "Projectors/LG/LG_Projector.ir", mapOf(
                RemoteCommand.POWER to 0x08,
                RemoteCommand.POWER_ON to 0xC4,
                RemoteCommand.POWER_OFF to 0xC5,
                RemoteCommand.INPUT to 0x0B,
                RemoteCommand.VOLUME_UP to 0x02,
                RemoteCommand.VOLUME_DOWN to 0x03,
                RemoteCommand.MUTE to 0x09,
                RemoteCommand.MENU to 0x43,
                RemoteCommand.UP to 0x40,
                RemoteCommand.DOWN to 0x41,
                RemoteCommand.LEFT to 0x07,
                RemoteCommand.RIGHT to 0x06,
                RemoteCommand.OK to 0x44,
                RemoteCommand.BACK to 0x28,
                RemoteCommand.HOME to 0x7C,
                RemoteCommand.HDMI to 0xCE,
                RemoteCommand.FREEZE to 0x12,
                RemoteCommand.AV_MUTE to 0x09,
            )),
        profile("samsung-projector-v1", DeviceBrand.SAMSUNG, DeviceType.PROJECTOR, "The Freestyle / Samsung Projector", "NEC", 0x07, 0x07,
            "Projectors/Samsung/Samsung_Projector.ir", mapOf(
                RemoteCommand.POWER to 0x02,
                RemoteCommand.POWER_ON to 0x99,
                RemoteCommand.POWER_OFF to 0x98,
                RemoteCommand.VOLUME_UP to 0x07,
                RemoteCommand.VOLUME_DOWN to 0x0B,
                RemoteCommand.MUTE to 0x0F,
                RemoteCommand.UP to 0x60,
                RemoteCommand.DOWN to 0x61,
                RemoteCommand.LEFT to 0x65,
                RemoteCommand.RIGHT to 0x62,
                RemoteCommand.OK to 0x68,
                RemoteCommand.MENU to 0x1A,
                RemoteCommand.BACK to 0x58,
                RemoteCommand.HOME to 0x79,
                RemoteCommand.INPUT to 0x01,
            )),
        IrProfile("sony-proj-sirc15", DeviceBrand.SONY, DeviceType.PROJECTOR, "VPL Series (SIRC 15-bit)", "SIRC15", 40_000,
            mapOf(
                RemoteCommand.POWER to SonyEncoder.encode(2, 0x15),
                RemoteCommand.POWER_ON to SonyEncoder.encode(2, 0x2E),
                RemoteCommand.POWER_OFF to SonyEncoder.encode(2, 0x2F),
                RemoteCommand.VOLUME_UP to SonyEncoder.encode(2, 0x12),
                RemoteCommand.VOLUME_DOWN to SonyEncoder.encode(2, 0x13),
                RemoteCommand.MUTE to SonyEncoder.encode(2, 0x14),
                RemoteCommand.UP to SonyEncoder.encode(2, 0x74),
                RemoteCommand.DOWN to SonyEncoder.encode(2, 0x75),
                RemoteCommand.LEFT to SonyEncoder.encode(2, 0x34),
                RemoteCommand.RIGHT to SonyEncoder.encode(2, 0x33),
                RemoteCommand.OK to SonyEncoder.encode(2, 0x65),
                RemoteCommand.MENU to SonyEncoder.encode(2, 0x60),
                RemoteCommand.BACK to SonyEncoder.encode(2, 0x25),
                RemoteCommand.INPUT to SonyEncoder.encode(2, 0x25),
            ),
            "https://github.com/Lucaslhm/Flipper-IRDB/blob/main/Projectors/Sony/Sony_Projector.ir",
            RemoteCommand.MENU
        ),
        IrProfile(
            id = "EPSON_POWERLITE_118_ORIGINAL_IR",
            brand = DeviceBrand.EPSON,
            deviceType = DeviceType.PROJECTOR,
            family = "PowerLite 118 · control 219863500",
            protocol = "NECext (NEC2, device 131.85)",
            carrierHz = 38_000,
            commands = EpsonPowerLite118.commands.mapValues { (_, code) ->
                NecEncoder.encode(0x83, 0x55, code)
            },
            source = EpsonPowerLite118.SOURCE,
            testCommand = RemoteCommand.MENU,
            commandInfo = EpsonPowerLite118.info(),
        ),
        IrProfile(
            id = "EPSON_EB_L260F",
            brand = DeviceBrand.EPSON,
            deviceType = DeviceType.PROJECTOR,
            family = "EB-L260F · captura IR comunitaria",
            protocol = "Epson raw 38 kHz (14 bits, sin decodificar)",
            carrierHz = 38_000,
            commands = EpsonEbL260F.commands,
            source = EpsonEbL260F.SOURCE,
            testCommand = RemoteCommand.INPUT,
            commandInfo = EpsonEbL260F.info(),
            testNote = "Menu todavía no tiene captura IR verificada para el EB-L260F. " +
                "El asistente prueba Source Search, que sí tiene captura, y nunca usa Power como prueba inicial. " +
                "Los botones Menu, Esc, Aspect, Color Mode, Computer, USB, LAN, Información y Ajustes quedan pendientes de captura.",
        ),
        profile("epson-emp822h-v1", DeviceBrand.EPSON, DeviceType.PROJECTOR, "EMP822H (captura documentada; otros modelos sin verificar)", "NECext", 0x83, 0x55,
            "Projectors/Epson/Epson_EMP822H.ir", mapOf(
                RemoteCommand.POWER to 0x90,
                RemoteCommand.INPUT to 0x8C,
                RemoteCommand.ASPECT_RATIO to 0x8A,
                RemoteCommand.COLOR_MODE to 0x8F,
                RemoteCommand.MENU to 0x9A,
                RemoteCommand.OK to 0x85,
                RemoteCommand.UP to 0xB0,
                RemoteCommand.DOWN to 0xB2,
                RemoteCommand.LEFT to 0xB3,
                RemoteCommand.RIGHT to 0xB1,
                RemoteCommand.BACK to 0x84,
                RemoteCommand.VOLUME_UP to 0xB1,
                RemoteCommand.VOLUME_DOWN to 0xB3,
                RemoteCommand.FREEZE to 0x92,
                RemoteCommand.AV_MUTE to 0x93,
            )),
        profile("benq-proj-v1", DeviceBrand.BENQ, DeviceType.PROJECTOR, "BenQ Series", "NEC", 0x0C, 0xF3,
            "Projectors/Benq/Benq_Projector.ir", mapOf(
                RemoteCommand.POWER to 0x08,
                RemoteCommand.POWER_ON to 0x05,
                RemoteCommand.POWER_OFF to 0x06,
                RemoteCommand.MENU to 0x1A,
                RemoteCommand.BACK to 0x1B,
                RemoteCommand.OK to 0x15,
                RemoteCommand.UP to 0x11,
                RemoteCommand.DOWN to 0x12,
                RemoteCommand.LEFT to 0x13,
                RemoteCommand.RIGHT to 0x14,
                RemoteCommand.INPUT to 0x0A,
                RemoteCommand.HDMI to 0x51,
                RemoteCommand.VGA to 0x52,
                RemoteCommand.FREEZE to 0x0E,
                RemoteCommand.AV_MUTE to 0x0F,
                RemoteCommand.VOLUME_UP to 0x17,
                RemoteCommand.VOLUME_DOWN to 0x18,
            )),
        profile("viewsonic-pjd-v1", DeviceBrand.VIEWSONIC, DeviceType.PROJECTOR, "PJD Series", "NEC", 0x83, 0x7C,
            "Projectors/ViewSonic/ViewSonic_PJD5134.ir", mapOf(
                RemoteCommand.POWER to 0x05,
                RemoteCommand.MENU to 0x15,
                RemoteCommand.BACK to 0x17,
                RemoteCommand.OK to 0x16,
                RemoteCommand.UP to 0x11,
                RemoteCommand.DOWN to 0x12,
                RemoteCommand.LEFT to 0x13,
                RemoteCommand.RIGHT to 0x14,
                RemoteCommand.INPUT to 0x07,
                RemoteCommand.FREEZE to 0x0C,
                RemoteCommand.AV_MUTE to 0x0D,
                RemoteCommand.VOLUME_UP to 0x18,
                RemoteCommand.VOLUME_DOWN to 0x19,
            )),
        profile("optoma-proj-v1", DeviceBrand.OPTOMA, DeviceType.PROJECTOR, "Optoma Universal", "NEC", 0x32, 0xCD,
            "Projectors/Optoma/Optoma_HD141X.ir", mapOf(
                RemoteCommand.POWER to 0x02,
                RemoteCommand.POWER_OFF to 0x2E,
                RemoteCommand.MENU to 0x11,
                RemoteCommand.OK to 0x10,
                RemoteCommand.UP to 0x14,
                RemoteCommand.DOWN to 0x15,
                RemoteCommand.LEFT to 0x16,
                RemoteCommand.RIGHT to 0x17,
                RemoteCommand.INPUT to 0x05,
                RemoteCommand.FREEZE to 0x0A,
                RemoteCommand.AV_MUTE to 0x03,
                RemoteCommand.VOLUME_UP to 0x0B,
                RemoteCommand.VOLUME_DOWN to 0x0C,
            )),
        profile("nec-proj-v1", DeviceBrand.NEC, DeviceType.PROJECTOR, "NP / VT Series", "NEC", 0x18, 0xE7,
            "Projectors/NEC/NEC_NP_Series.ir", mapOf(
                RemoteCommand.POWER to 0x08,
                RemoteCommand.POWER_ON to 0x12,
                RemoteCommand.POWER_OFF to 0x13,
                RemoteCommand.MENU to 0x0A,
                RemoteCommand.OK to 0x0B,
                RemoteCommand.BACK to 0x0C,
                RemoteCommand.UP to 0x0E,
                RemoteCommand.DOWN to 0x0F,
                RemoteCommand.LEFT to 0x10,
                RemoteCommand.RIGHT to 0x11,
                RemoteCommand.INPUT to 0x01,
                RemoteCommand.FREEZE to 0x1B,
                RemoteCommand.AV_MUTE to 0x1C,
                RemoteCommand.VOLUME_UP to 0x1E,
                RemoteCommand.VOLUME_DOWN to 0x1F,
            )),
    )

    override val pendingBrands = listOf(
        PendingBrandInfo(
            DeviceBrand.PHILIPS,
            DeviceType.TV,
            "Protocolo RC-5 / RC-6 Manchester biphase (36 kHz).",
            "Requiere codificador biphase y captura de tiempos específicos por modelo."
        ),
        PendingBrandInfo(
            DeviceBrand.PANASONIC,
            DeviceType.TV,
            "Protocolo Kaseikyo / Panasonic 48-bit (36.7 kHz).",
            "Requiere verificación del cálculo de paridad/checksum OEM y validación física."
        ),
        PendingBrandInfo(
            DeviceBrand.PANASONIC,
            DeviceType.PROJECTOR,
            "Protocolo Panasonic Projector Kaseikyo.",
            "Requiere confirmación de tramas de modulación con equipo físico."
        ),
        PendingBrandInfo(
            DeviceBrand.SONY,
            DeviceType.PROJECTOR,
            "Protocolo Sony SIRC 15-bit / 20-bit (device code 2).",
            "Requiere conjunto de códigos verificado con proyector físico."
        ),
    )

    private fun profile(id: String, brand: DeviceBrand, type: DeviceType, family: String, protocol: String,
                        low: Int, high: Int, path: String, codes: Map<RemoteCommand, Int>) = IrProfile(
        id, brand, type, family, protocol, 38_000,
        codes.mapValues { (_, code) -> NecEncoder.encode(low, high, code) },
        "https://github.com/Lucaslhm/Flipper-IRDB/blob/main/$path",
        RemoteCommand.MENU,
    )
}
