package com.universalremote.protocol.lgwebos

import com.universalremote.core.model.Device
import com.universalremote.core.model.DeviceBrand
import com.universalremote.core.model.DeviceType
import com.universalremote.core.model.ConnectionType
import com.universalremote.core.model.RemoteCommand
import com.universalremote.core.model.NetworkEndpoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class LgWebOsProtocolTest {

    @Test
    fun protocolIdAndTypeAreCorrect() {
        val protocol = LgWebOsProtocol()
        assertEquals("lg_webos_protocol", protocol.id)
        assertEquals(com.universalremote.core.model.ProtocolType.LG_WEBOS, protocol.protocolType)
    }

    @Test
    fun capabilitiesIncludeLgSupportedCommands() = runTest {
        val protocol = LgWebOsProtocol()
        val device = Device(
            id = "lg1", name = "LG TV", brand = DeviceBrand.LG, model = "webOS",
            deviceType = DeviceType.TV, connectionType = ConnectionType.WIFI
        )
        val caps = protocol.capabilities(device)
        assertTrue(caps.supportedCommands.contains(RemoteCommand.POWER))
        assertTrue(caps.supportedCommands.contains(RemoteCommand.VOLUME_UP))
        assertTrue(caps.supportedCommands.contains(RemoteCommand.OK))
        assertTrue(caps.supportedCommands.contains(RemoteCommand.HOME))
    }

    @Test
    fun probeEndpointHandlesUnreachableHostGracefully() = runTest {
        val protocol = LgWebOsProtocol()
        val result = protocol.probeEndpoint(NetworkEndpoint("127.0.0.1", 59999))
        assertFalse(result)
    }
}
