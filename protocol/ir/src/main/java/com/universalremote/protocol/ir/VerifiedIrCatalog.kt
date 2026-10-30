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
        profile("epson-emp822h-v1", DeviceBrand.EPSON, DeviceType.PROJECTOR, "EMP / EB Series (ESC/VP21)", "NECext", 0x83, 0x55,
            "Projectors/Epson/Epson_EMP822H.ir", mapOf(
                RemoteCommand.POWER to 0x90,
                RemoteCommand.POWER_ON to 0x8D,
                RemoteCommand.POWER_OFF to 0x8E,
                RemoteCommand.INPUT to 0x8C,
                RemoteCommand.HDMI to 0x8B,
                RemoteCommand.VGA to 0x87,
                RemoteCommand.LAN to 0x8A,
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
