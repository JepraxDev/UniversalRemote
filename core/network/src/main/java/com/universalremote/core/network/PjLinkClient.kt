package com.universalremote.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.EOFException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest

class PjLinkException(val code: String, message: String) : java.io.IOException(message)

/** JBMIA PJLink: ASCII, CR framed. A fresh session per request avoids idle-disconnect
 * races and prevents sending a command to the previously selected projector. */
class PjLinkClient(
    private val socketFactory: () -> Socket = { Socket() },
    private val timeoutMs: Int = 2500,
    private val connectTimeoutMs: Int = timeoutMs
) {
    suspend fun greeting(host: String, port: Int): Boolean = runInterruptible(Dispatchers.IO) {
        open(host, port).use { socket -> parseGreeting(readFrame(socket.getInputStream())) != null }
    }

    suspend fun query(host: String, port: Int, command: String, password: String? = null, pjClass: Int = 1): String =
        request(host, port, command, "?", password, pjClass)

    suspend fun request(host: String, port: Int, command: String, parameter: String, password: String? = null, pjClass: Int = 1): String =
        runInterruptible(Dispatchers.IO) {
            require(command.matches(Regex("[A-Z0-9]{4}")))
            require(pjClass in 1..2)
            require(parameter.matches(Regex("[A-Za-z0-9? ]{1,128}")))
            open(host, port).use { socket ->
                val input = socket.getInputStream()
                val challenge = parseGreeting(readFrame(input))
                    ?: throw PjLinkException("PROTOCOL", "El protocolo no es compatible: saludo PJLink inválido")
                val prefix = if (challenge.isNotEmpty()) {
                    if (password == null) throw PjLinkException("PASSWORD", "Se necesita una contraseña PJLink")
                    require(password.length <= 32 && password.all { it.code in 32..126 }) { "Contraseña PJLink inválida" }
                    digest(challenge, password)
                } else ""
                socket.getOutputStream().apply {
                    write("$prefix%$pjClass$command $parameter\r".toByteArray(Charsets.US_ASCII))
                    flush()
                }
                val response = readFrame(input)
                if (response == "PJLINK ERRA") throw PjLinkException("ERRA", "Contraseña PJLink incorrecta")
                val expected = "%$pjClass$command="
                if (!response.startsWith(expected, ignoreCase = true)) throw PjLinkException("PROTOCOL", "Respuesta PJLink inesperada")
                val value = response.substring(expected.length)
                val error = when (value) {
                    "ERR1" -> "El comando no es compatible"
                    "ERR2" -> "Parámetro o entrada no compatible"
                    "ERR3" -> "El proyector está ocupado; espera y vuelve a intentar"
                    "ERR4" -> "El proyector informa un fallo de hardware"
                    "ERRA" -> "Contraseña PJLink incorrecta"
                    else -> null
                }
                if (error != null) throw PjLinkException(value, error)
                if (value.isEmpty() && command !in setOf("NAME", "INF1", "INF2", "INFO", "SNUM", "SVER", "INNM")) throw PjLinkException("PROTOCOL", "Respuesta PJLink vacía")
                value
            }
        }

    private fun open(host: String, port: Int): Socket {
        val socket = socketFactory()
        try {
            socket.soTimeout = timeoutMs
            socket.connect(InetSocketAddress(host, port), connectTimeoutMs)
            return socket
        } catch (e: Exception) { socket.close(); throw e }
    }

    companion object {
        fun parseGreeting(frame: String): String? = when {
            frame == "PJLINK 0" -> ""
            frame.matches(Regex("PJLINK 1 [0-9a-fA-F]{8}")) -> frame.substring(9)
            else -> null
        }

        fun digest(challenge: String, password: String): String = MessageDigest.getInstance("MD5")
            .digest((challenge + password).toByteArray(Charsets.US_ASCII))
            .joinToString("") { "%02x".format(it.toInt() and 255) }

        fun readFrame(input: InputStream): String {
            val result = java.io.ByteArrayOutputStream()
            val started = System.nanoTime()
            while (result.size() <= 512) {
                if (Thread.currentThread().isInterrupted) throw java.io.InterruptedIOException("Operación cancelada")
                if (System.nanoTime() - started > 5_000_000_000L) throw java.net.SocketTimeoutException("Tiempo de respuesta agotado")
                val byte = input.read()
                if (byte == -1) throw EOFException("El proyector cerró la conexión")
                if (byte == 13) return result.toString(Charsets.UTF_8.name())
                if (byte < 32 || byte == 127) throw PjLinkException("PROTOCOL", "Respuesta PJLink inválida")
                result.write(byte)
            }
            throw PjLinkException("PROTOCOL", "Respuesta PJLink demasiado larga")
        }
    }
}
