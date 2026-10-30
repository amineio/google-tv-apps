package com.amine.asktv.phoneauth

import java.net.URI

data class PhonePairingLink(val host: String, val port: Int, val pin: String, val token: String) {
    fun encode(): String {
        validate(host, port, pin, token)
        return "aichat://pair?v=1&host=$host&port=$port&pin=$pin&token=$token"
    }

    override fun toString(): String = "PhonePairingLink(redacted)"

    companion object {
        fun parse(value: String): PhonePairingLink = try {
            require(value.length <= 2_048)
            val uri = URI(value)
            require(uri.scheme == "aichat" && uri.rawAuthority == "pair" &&
                uri.rawPath.orEmpty().isEmpty() && uri.rawFragment == null)
            val fields = linkedMapOf<String, String>()
            val parts = requireNotNull(uri.rawQuery).split('&')
            require(parts.size == 5)
            for (part in parts) {
                val pair = part.split('=')
                require(pair.size == 2 && !fields.containsKey(pair[0]))
                fields[pair[0]] = pair[1]
            }
            require(fields.keys == setOf("v", "host", "port", "pin", "token") && fields["v"] == "1")
            val port = fields.getValue("port")
            require(port.matches(Regex("[1-9][0-9]{0,4}")))
            PhonePairingLink(fields.getValue("host"), port.toInt(), fields.getValue("pin"), fields.getValue("token"))
                .also { validate(it.host, it.port, it.pin, it.token) }
        } catch (_: Exception) {
            throw IllegalArgumentException("Invalid pairing link.")
        }

        internal fun validLocalHost(host: String): Boolean {
            if (!host.matches(Regex("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}"))) return false
            val parts = host.split('.').map { it.toInt() }
            if (parts.any { it !in 0..255 } || parts.joinToString(".") != host) return false
            return parts[0] == 10 || parts[0] == 172 && parts[1] in 16..31 || parts[0] == 192 && parts[1] == 168
        }

        private fun validate(host: String, port: Int, pin: String, token: String) {
            require(validLocalHost(host) && port in 1..65535 && pin.matches(Regex("[0-9a-f]{64}")) &&
                token.matches(Regex("[A-Za-z0-9_-]{43}"))) { "Invalid pairing link." }
        }
    }
}
