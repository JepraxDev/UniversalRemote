package com.universalremote.domain

import com.universalremote.core.model.*
import com.universalremote.protocol.api.ProtocolRegistry

class DeviceCapabilityResolver(
    private val irCatalog: IrProfileCatalog,
    private val protocolRegistry: ProtocolRegistry
) {

    fun resolve(device: Device): DeviceCapabilities {
        if (device.brand == DeviceBrand.EPSON && device.connectionType == ConnectionType.WIFI) return DeviceCapabilities()
        if (device.connectionType == ConnectionType.INFRARED) {
            val profileId = device.irProfileId
            if (profileId != null) {
                val profile = irCatalog.find(profileId)
                if (profile != null) {
                    return profile.capabilities
                }
            }
            return defaultIrCapabilities(device.deviceType)
        } else {
            val protocol = protocolRegistry.getProtocolForDevice(device)
            if (protocol != null) {
                return when (protocol.protocolType) {
                    ProtocolType.LG_WEBOS, ProtocolType.SAMSUNG_WS -> defaultSmartTvCapabilities()
                    ProtocolType.PJLINK -> defaultProjectorCapabilities()
                    ProtocolType.ANDROID_TV -> defaultAndroidTvCapabilities()
                    else -> defaultNetworkCapabilities(device.deviceType)
                }
            }
            return defaultNetworkCapabilities(device.deviceType)
        }
    }

    private fun defaultSmartTvCapabilities(): DeviceCapabilities = DeviceCapabilities(
        supportedCommands = setOf(
            RemoteCommand.POWER, RemoteCommand.POWER_OFF, RemoteCommand.POWER_ON,
            RemoteCommand.VOLUME_UP, RemoteCommand.VOLUME_DOWN, RemoteCommand.MUTE,
            RemoteCommand.CHANNEL_UP, RemoteCommand.CHANNEL_DOWN,
            RemoteCommand.UP, RemoteCommand.DOWN, RemoteCommand.LEFT, RemoteCommand.RIGHT, RemoteCommand.OK,
            RemoteCommand.BACK, RemoteCommand.HOME, RemoteCommand.INPUT, RemoteCommand.MENU,
            RemoteCommand.NUM_0, RemoteCommand.NUM_1, RemoteCommand.NUM_2, RemoteCommand.NUM_3, RemoteCommand.NUM_4,
            RemoteCommand.NUM_5, RemoteCommand.NUM_6, RemoteCommand.NUM_7, RemoteCommand.NUM_8, RemoteCommand.NUM_9
        )
    )

    private fun defaultAndroidTvCapabilities(): DeviceCapabilities = DeviceCapabilities(
        supportedCommands = setOf(
            RemoteCommand.POWER, RemoteCommand.POWER_OFF,
            RemoteCommand.VOLUME_UP, RemoteCommand.VOLUME_DOWN, RemoteCommand.MUTE,
            RemoteCommand.UP, RemoteCommand.DOWN, RemoteCommand.LEFT, RemoteCommand.RIGHT, RemoteCommand.OK,
            RemoteCommand.BACK, RemoteCommand.HOME
        )
    )

    private fun defaultProjectorCapabilities(): DeviceCapabilities = DeviceCapabilities(
        supportedCommands = setOf(
            RemoteCommand.POWER, RemoteCommand.POWER_ON, RemoteCommand.POWER_OFF,
            RemoteCommand.VOLUME_UP, RemoteCommand.VOLUME_DOWN, RemoteCommand.MUTE,
            RemoteCommand.INPUT, RemoteCommand.HDMI, RemoteCommand.HDMI_1, RemoteCommand.HDMI_2, RemoteCommand.VGA,
            RemoteCommand.FREEZE, RemoteCommand.AV_MUTE
        )
    )

    private fun defaultIrCapabilities(type: DeviceType): DeviceCapabilities = when (type) {
        DeviceType.TV -> DeviceCapabilities(
            supportedCommands = setOf(
                RemoteCommand.POWER, RemoteCommand.VOLUME_UP, RemoteCommand.VOLUME_DOWN, RemoteCommand.MUTE,
                RemoteCommand.CHANNEL_UP, RemoteCommand.CHANNEL_DOWN,
                RemoteCommand.UP, RemoteCommand.DOWN, RemoteCommand.LEFT, RemoteCommand.RIGHT, RemoteCommand.OK,
                RemoteCommand.BACK, RemoteCommand.HOME, RemoteCommand.INPUT, RemoteCommand.MENU
            )
        )
        DeviceType.PROJECTOR -> DeviceCapabilities(
            supportedCommands = setOf(
                RemoteCommand.POWER, RemoteCommand.VOLUME_UP, RemoteCommand.VOLUME_DOWN, RemoteCommand.MUTE,
                RemoteCommand.INPUT, RemoteCommand.HDMI, RemoteCommand.VGA, RemoteCommand.FREEZE, RemoteCommand.AV_MUTE
            )
        )
    }

    private fun defaultNetworkCapabilities(type: DeviceType): DeviceCapabilities =
        if (type == DeviceType.TV) defaultSmartTvCapabilities() else defaultProjectorCapabilities()
}
