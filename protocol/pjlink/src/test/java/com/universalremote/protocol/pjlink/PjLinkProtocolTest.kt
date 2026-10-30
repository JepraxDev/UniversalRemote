package com.universalremote.protocol.pjlink

import com.universalremote.core.model.Device
import com.universalremote.core.model.DeviceBrand
import com.universalremote.core.model.DeviceType
import com.universalremote.core.model.ConnectionType
import com.universalremote.core.model.RemoteCommand
import com.universalremote.core.model.NetworkEndpoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class PjLinkProtocolTest {

    @Test
    fun protocolIdAndTypeAreCorrect() {
        val protocol = PjLinkProtocol()
        assertEquals("pjlink_protocol", protocol.id)
        assertEquals(com.universalremote.core.model.ProtocolType.PJLINK, protocol.protocolType)
    }

    @Test
    fun capabilitiesIncludeProjectorCommands() = runTest {
        val protocol = PjLinkProtocol()
        val device = Device(
            id = "pj1", name = "Epson Projector", brand = DeviceBrand.EPSON, model = "EB-Serie",
            deviceType = DeviceType.PROJECTOR, connectionType = ConnectionType.WIFI
        )
        val caps = protocol.capabilities(device)
        assertTrue(caps.supportedCommands.contains(RemoteCommand.POWER))
        assertTrue(caps.supportedCommands.contains(RemoteCommand.POWER_ON))
        assertTrue(caps.supportedCommands.contains(RemoteCommand.HDMI))
        assertTrue(caps.supportedCommands.contains(RemoteCommand.AV_MUTE))
    }

    @Test
    fun probeEndpointHandlesUnreachableHost() = runTest {
        val protocol = PjLinkProtocol()
        val result = protocol.probeEndpoint(NetworkEndpoint("127.0.0.1", 59998))
        assertFalse(result)
    }
}
