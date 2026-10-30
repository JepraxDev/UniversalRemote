package com.universalremote.core.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.net.ServerSocket
import java.net.SocketTimeoutException
import kotlin.concurrent.thread

class PjLinkClientTest {
    private fun server(greeting: String, response: String, checkRequest: (String) -> Unit = {}, test: (Int) -> Unit) {
        ServerSocket(0).use { listener ->
            listener.soTimeout = 3000
            var failure: Throwable? = null
            val worker = thread(isDaemon = true) {
                try {
                    listener.accept().use { socket ->
                        socket.soTimeout = 3000
                        val out = socket.getOutputStream()
                        // TCP is a stream; deliberately fragment every byte.
                        greeting.forEach { out.write(it.code); out.flush() }
                        checkRequest(PjLinkClient.readFrame(socket.getInputStream()))
                        response.forEach { out.write(it.code); out.flush() }
                    }
                } catch (e: Throwable) { failure = e }
            }
            test(listener.localPort)
            worker.join(4000)
            failure?.let { throw AssertionError("Test server failed", it) }
            assertFalse(worker.isAlive)
        }
    }

    @Test fun fragmentedGreetingAndResponse() = server("PJLINK 0\r", "%1POWR=1\r", { assertEquals("%1POWR ?", it) }) { port ->
        runBlocking { assertEquals("1", PjLinkClient().query("127.0.0.1", port, "POWR")) }
    }

    @Test fun authenticationUsesChallengeAndPassword() = server("PJLINK 1 12345678\r", "%1POWR=0\r", {
        val expected = java.security.MessageDigest.getInstance("MD5").digest("12345678secret".toByteArray()).joinToString("") { b -> "%02x".format(b) }
        assertEquals("$expected%1POWR ?", it)
    }) { port -> runBlocking { assertEquals("0", PjLinkClient().query("127.0.0.1", port, "POWR", "secret")) } }

    @Test fun errorResponsesAreNotSuccess() {
        for (code in listOf("ERR1", "ERR2", "ERR3", "ERR4")) {
            server("PJLINK 0\r", "%1POWR=$code\r") { port ->
                runBlocking {
                    try { PjLinkClient().request("127.0.0.1", port, "POWR", "1"); fail("Accepted $code") }
                    catch (e: PjLinkException) { assertEquals(code, e.code) }
                }
            }
        }
    }

    @Test fun incorrectPasswordIsRejected() = server("PJLINK 1 12345678\r", "PJLINK ERRA\r") { port ->
        runBlocking {
            try { PjLinkClient().query("127.0.0.1", port, "POWR", "wrong"); fail() }
            catch (e: PjLinkException) { assertEquals("ERRA", e.code) }
        }
    }

    @Test fun mismatchedResponseIsRejected() = server("PJLINK 0\r", "%1INPT=OK\r") { port ->
        runBlocking {
            try { PjLinkClient().request("127.0.0.1", port, "POWR", "1"); fail() }
            catch (e: PjLinkException) { assertEquals("PROTOCOL", e.code) }
        }
    }

    @Test fun openPortIsNotPjLink() = runBlocking {
        ServerSocket(0).use { listener ->
            val worker = thread { listener.accept().use { it.getOutputStream().write("HTTP/1.1 200 OK\r".toByteArray()) } }
            assertFalse(PjLinkClient().greeting("127.0.0.1", listener.localPort))
            worker.join()
        }
    }

    @Test fun missingPasswordDoesNotSendCommand() = runBlocking {
        ServerSocket(0).use { listener ->
            var received = 0
            val worker = thread {
                listener.accept().use {
                    it.soTimeout = 2000
                    it.getOutputStream().write("PJLINK 1 12345678\r".toByteArray())
                    received = it.getInputStream().read()
                }
            }
            try { PjLinkClient().query("127.0.0.1", listener.localPort, "POWR"); fail() }
            catch (e: PjLinkException) { assertEquals("PASSWORD", e.code) }
            worker.join(3000)
            assertEquals(-1, received)
        }
    }

    @Test fun greetingTimeoutIsBounded() = runBlocking {
        ServerSocket(0).use { listener ->
            val worker = thread { listener.accept().use { it.soTimeout = 2000; it.getInputStream().read() } }
            try { PjLinkClient(timeoutMs = 100).greeting("127.0.0.1", listener.localPort); fail() }
            catch (_: SocketTimeoutException) { }
            worker.join(3000)
            assertFalse(worker.isAlive)
        }
    }

    @Test fun framingRejectsOversizeAndTruncation() {
        try { PjLinkClient.readFrame("x".repeat(514).byteInputStream()); fail() }
        catch (_: PjLinkException) { }
        try { PjLinkClient.readFrame("%1POWR=1".byteInputStream()); fail() }
        catch (_: java.io.EOFException) { }
    }
}
