package com.universalremote.core.model

enum class DeviceType { TV, PROJECTOR }
enum class ConnectionType { WIFI, INFRARED }
enum class ThemePreference { SYSTEM, LIGHT, DARK }

enum class RemoteMode { BASIC, FULL }

enum class CompatibilityStatus {
    VERIFIED_PHYSICAL,
    VERIFIED_PROTOCOL,
    EXPERIMENTAL,
    NOT_IMPLEMENTED,
    UNSUPPORTED
}

data class Location(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

enum class RemoteLayoutStyle { COMPACT, CLASSIC, FULL }

data class RemoteLayoutPreference(
    val deviceId: String,
    val layoutStyle: RemoteLayoutStyle = RemoteLayoutStyle.CLASSIC,
    val hiddenCommands: Set<RemoteCommand> = emptySet(),
    val customOrder: List<RemoteCommand> = emptyList()
)

data class QuickActionStep(
    val id: String,
    val actionId: String,
    val stepOrder: Int,
    val command: RemoteCommand,
    val delayMs: Long = 500L
)

data class QuickAction(
    val id: String,
    val name: String,
    val deviceId: String,
    val steps: List<QuickActionStep> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class NetworkEndpoint(val host: String, val port: Int) {
    init {
        require(host.isNotBlank()) { "Host address must not be blank" }
        require(port in 1..65535) { "Port must be in range 1..65535" }
    }
}

enum class ProtocolType(val id: String, val defaultPort: Int) {
    LG_WEBOS("LG_WEBOS", 3000),
    PJLINK("PJLINK", 4352),
    SAMSUNG_WS("SAMSUNG_WS", 8001),
    ANDROID_TV("ANDROID_TV", 6466),
    INFRARED("INFRARED", 0),
    UNKNOWN("UNKNOWN", 80);

    companion object {
        fun fromId(id: String?): ProtocolType =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: UNKNOWN
    }
}

data class DiscoveredDevice(
    val id: String,
    val name: String,
    val brand: DeviceBrand,
    val model: String = "Network Device",
    val deviceType: DeviceType,
    val ipAddress: String,
    val port: Int,
    val protocolType: ProtocolType,
    val rawServiceInfo: Map<String, String> = emptyMap()
) {
    fun toDevice(): Device = Device(
        id = id,
        name = name,
        brand = brand,
        model = model,
        deviceType = deviceType,
        connectionType = ConnectionType.WIFI,
        ipAddress = ipAddress,
        port = port,
        protocolId = protocolType.name,
        irProfileId = null,
        roomName = null,
        isFavorite = false
    )
}

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
    val protocolId: String? = null,
    val irProfileId: String? = null,
    val roomName: String? = null,
    val locationTag: String? = null,
    val locationId: String? = null,
    val compatibilityStatus: CompatibilityStatus = CompatibilityStatus.VERIFIED_PROTOCOL,
    val isFavorite: Boolean = false,
    val lastConnectedAt: Long? = null,
    val lastUsedAt: Long? = null,
)

enum class RemoteCommand {
    POWER, VOLUME_UP, VOLUME_DOWN, UP, DOWN, LEFT, RIGHT, OK, MENU, BACK, INPUT,
    CHANNEL_UP, CHANNEL_DOWN, MUTE, HOME, HDMI, VGA, LAN, FREEZE, AV_MUTE,
    POWER_ON, POWER_OFF, HDMI_1, HDMI_2,
    NUM_0, NUM_1, NUM_2, NUM_3, NUM_4, NUM_5, NUM_6, NUM_7, NUM_8, NUM_9,
    STAR, HASH,
    PLAY, PAUSE, STOP, PREVIOUS, NEXT, REWIND, FAST_FORWARD,
    GUIDE, INFO, SUBTITLE, SETTINGS_KEY, KEYBOARD, APPS,
    ASPECT_RATIO, COLOR_MODE, BRIGHTNESS, CONTRAST, ECO_MODE, AUTO_SETUP, KEYSTONE,
    ZOOM_IN, ZOOM_OUT, FOCUS_NEAR, FOCUS_FAR,
    LAMP_STATUS, LAMP_HOURS,
    // Epson projector remote (2198635 / 219863500). NUM_MODIFIER is a
    // remote-control modifier only: it has no standalone IR frame.
    SOURCE_SEARCH, COMPUTER, USB, ID, USER, DEFAULT, SPLIT, E_ZOOM_IN, E_ZOOM_OUT,
    NUM_MODIFIER,
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
