package com.universalremote.protocol.samsung

import com.universalremote.core.model.Device
import com.universalremote.core.model.DeviceBrand
import com.universalremote.core.model.DeviceType
import com.universalremote.core.model.ConnectionType
import com.universalremote.core.model.RemoteCommand
import com.universalremote.core.model.NetworkEndpoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SamsungWsProtocolTest {

    @Test
    fun protocolIdAndTypeAreCorrect() {
        val protocol = SamsungWsProtocol()
        assertEquals("samsung_ws_protocol", protocol.id)
        assertEquals(com.universalremote.core.model.ProtocolType.SAMSUNG_WS, protocol.protocolType)
    }

    @Test
    fun capabilitiesIncludeSamsungCommands() = runTest {
        val protocol = SamsungWsProtocol()
        val device = Device(
            id = "sam1", name = "Samsung TV", brand = DeviceBrand.SAMSUNG, model = "Smart TV",
            deviceType = DeviceType.TV, connectionType = ConnectionType.WIFI
        )
        val caps = protocol.capabilities(device)
        assertTrue(caps.supportedCommands.contains(RemoteCommand.POWER))
        assertTrue(caps.supportedCommands.contains(RemoteCommand.VOLUME_UP))
        assertTrue(caps.supportedCommands.contains(RemoteCommand.HOME))
    }

    @Test
    fun probeEndpointHandlesUnreachableHost() = runTest {
        val protocol = SamsungWsProtocol()
        val result = protocol.probeEndpoint(NetworkEndpoint("127.0.0.1", 59997))
        assertFalse(result)
    }
}
