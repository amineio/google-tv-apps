package com.amine.postbox.core

import java.net.URI

data class PairingLink(val host: String, val port: Int, val pin: String, val token: String) {
    fun encode(): String {
        validate(host, port, pin, token)
        return "postbox://pair?host=$host&port=$port&pin=$pin&token=$token"
    }

    companion object {
        fun parse(value: String): PairingLink =
            try {
                require(value.length <= 2048)
                val uri = URI(value)
                require(
                    uri.scheme == "postbox" &&
                        uri.rawAuthority == "pair" &&
                        uri.rawPath.orEmpty().isEmpty() &&
                        uri.rawFragment == null
                )
                val parts = uri.rawQuery!!.split('&')
                require(parts.size == 4)
                val pairs = parts.map {
                    val p = it.split('=')
                    require(p.size == 2)
                    p[0] to p[1]
                }
                require(pairs.map { it.first }.toSet() == setOf("host", "port", "pin", "token"))
                val fields = pairs.toMap()
                require(fields.getValue("port").matches(Regex("[1-9][0-9]{0,4}")))
                PairingLink(
                        fields.getValue("host"),
                        fields.getValue("port").toInt(),
                        fields.getValue("pin"),
                        fields.getValue("token"),
                    )
                    .also { validate(it.host, it.port, it.pin, it.token) }
            } catch (_: Exception) {
                throw IllegalArgumentException("Invalid pairing link.")
            }

        internal fun validLocalHost(host: String): Boolean {
            if (!host.matches(Regex("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}"))) return false
            val parts = host.split('.').map { it.toInt() }
            if (
                parts.any { it !in 0..255 } || parts.map { it.toString() }.joinToString(".") != host
            )
                return false
            return parts[0] == 10 ||
                parts[0] == 172 && parts[1] in 16..31 ||
                parts[0] == 192 && parts[1] == 168
        }

        private fun validate(host: String, port: Int, pin: String, token: String) {
            require(
                validLocalHost(host) &&
                    port in 1..65535 &&
                    pin.matches(Regex("[0-9a-f]{64}")) &&
                    token.matches(Regex("[A-Za-z0-9_-]{43}"))
            ) {
                "Invalid pairing link."
            }
        }
    }
}
