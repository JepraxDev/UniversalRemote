package com.universalremote.protocol.pjlink

import com.universalremote.core.model.*
import com.universalremote.protocol.api.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.net.ServerSocket
import java.net.SocketException
import kotlin.concurrent.thread

class PjLinkSessionTest {
    @Test fun authenticatedDeviceLifecycleQueriesInputsAndReconnects() = runBlocking {
        ServerSocket(0).use { server ->
            val requests = java.util.Collections.synchronizedList(mutableListOf<String>())
            val worker = thread(isDaemon = true) {
                try {
                    while (!server.isClosed) server.accept().use { socket ->
                        socket.soTimeout = 2000
                        socket.getOutputStream().write("PJLINK 0\r".toByteArray())
                        val request = buildString {
                            while (true) {
                                val c = socket.getInputStream().read()
                                if (c == 13 || c < 0) break
                                append(c.toChar())
                            }
                        }
                        requests.add(request)
                        val command = request.take(6)
                        val value = when (request) {
                            "%1POWR ?" -> "1"
                            "%1INF1 ?" -> "EPSON"
                            "%1INF2 ?" -> "PowerLite 118"
                            "%1NAME ?" -> "Aula"
                            "%1CLSS ?" -> "1"
                            "%1INST ?" -> "11 31 32 51"
                            "%1ERST ?" -> "000000"
                            "%1LAMP ?" -> "100 1"
                            "%1INPT 32" -> "OK"
                            else -> "ERR1"
                        }
                        socket.getOutputStream().write("$command=$value\r".toByteArray())
                    }
                } catch (_: SocketException) { }
            }
            val device = Device("test", "Aula", DeviceBrand.EPSON, "PowerLite 118", DeviceType.PROJECTOR,
                ConnectionType.WIFI, "127.0.0.1", server.localPort, ProtocolType.PJLINK.id)
            val protocol = PjLinkProtocol()
            assertEquals(DevicePairingState.Paired, protocol.pair(device, null))
            assertEquals(listOf("11", "31", "32", "51"), protocol.information(device).inputs)
            assertEquals(CommandResult.Sent, protocol.selectInput(device, "32"))
            assertEquals(CommandResult.Unsupported, protocol.selectInput(device, "33"))
            protocol.disconnect()
            assertEquals(DevicePairingState.Paired, protocol.pair(device, null))
            assertFalse(requests.any { it.startsWith("%1POWR ") && !it.endsWith("?") })
            server.close()
            worker.join(3000)
            assertFalse(worker.isAlive)
        }
    }
}
