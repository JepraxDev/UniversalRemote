package com.universalremote.core.model

enum class DeviceType { TV, PROJECTOR }
enum class ConnectionType { WIFI, INFRARED }
enum class ThemePreference { SYSTEM, LIGHT, DARK }

/** Stable, open identifier: unknown brands survive database round trips. */
@JvmInline
value class DeviceBrand(val id: String) {
    companion object {
        val LG = DeviceBrand("LG")
        val SAMSUNG = DeviceBrand("SAMSUNG")
        val SONY = DeviceBrand("SONY")
        val EPSON = DeviceBrand("EPSON")
        val BENQ = DeviceBrand("BENQ")
        val TCL = DeviceBrand("TCL")
        val HISENSE = DeviceBrand("HISENSE")
        val VIEWSONIC = DeviceBrand("VIEWSONIC")
        val OPTOMA = DeviceBrand("OPTOMA")
        val PANASONIC = DeviceBrand("PANASONIC")
        val PHILIPS = DeviceBrand("PHILIPS")
        val XIAOMI = DeviceBrand("XIAOMI")
        val NEC = DeviceBrand("NEC")
        val OTHER = DeviceBrand("OTHER")
        val catalog = listOf(LG, SAMSUNG, SONY, EPSON, BENQ, TCL, HISENSE, VIEWSONIC, OPTOMA, PANASONIC, PHILIPS, XIAOMI, NEC, OTHER)
    }
}

data class Device(
    val id: String,
    val name: String,
    val brand: DeviceBrand,
    val model: String,
    val deviceType: DeviceType,
    val connectionType: ConnectionType,
    val ipAddress: String? = null,
    val port: Int? = null,
    val irProfileId: String? = null,
    val roomName: String? = null,
    val isFavorite: Boolean = false,
)

enum class RemoteCommand {
    POWER, VOLUME_UP, VOLUME_DOWN, UP, DOWN, LEFT, RIGHT, OK, MENU, BACK, INPUT,
    CHANNEL_UP, CHANNEL_DOWN, MUTE, HOME, HDMI, VGA, LAN, FREEZE, AV_MUTE,
    POWER_ON, POWER_OFF, HDMI_1, HDMI_2,
}
data class DeviceCapabilities(val supportedCommands: Set<RemoteCommand> = emptySet())
sealed interface ConnectionStatus {
    data object NotConfigured : ConnectionStatus
    data object Disconnected : ConnectionStatus
    data object Connecting : ConnectionStatus
    data object Connected : ConnectionStatus
    data class Failed(val reason: String) : ConnectionStatus
}
sealed interface IrAvailability {
    data object Available : IrAvailability
    data object NoEmitter : IrAvailability
    data object ServiceUnavailable : IrAvailability
    data object PlatformError : IrAvailability
}
