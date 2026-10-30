package com.amine.asktv.phoneauth

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/** Shared by both callback transports. Errors never include callback contents. */
object AuthCallbackParser {
    const val MAX_TARGET_BYTES = 7_168

    fun parseTarget(target: String): Map<String, String> = try {
        require(target.length <= MAX_TARGET_BYTES)
        require(target.toByteArray(Charsets.UTF_8).size <= MAX_TARGET_BYTES)
        require(target.startsWith("/auth/callback?"))
        require(target.none { it <= ' ' || it >= '\u007f' || it == '#' })
        strictForm(target.substringAfter('?'))
    } catch (_: Exception) {
        throw IllegalArgumentException("Invalid authorization callback.")
    }
}

/** Bounded callers supply ASCII wire text. Decode once and reject malformed UTF-8. */
internal fun strictForm(query: String): Map<String, String> {
    require(query.isNotEmpty())
    val result = linkedMapOf<String, String>()
    for (field in query.split('&')) {
        val separator = field.indexOf('=')
        require(separator > 0)
        val key = strictDecode(field.substring(0, separator))
        val value = strictDecode(field.substring(separator + 1))
        require(key.isNotEmpty() && !result.containsKey(key))
        result[key] = value
    }
    return result
}

private fun strictDecode(value: String): String {
    val bytes = ByteArrayOutputStream(value.length)
    var index = 0
    while (index < value.length) {
        val char = value[index++]
        when (char) {
            '%' -> {
                require(index + 1 < value.length)
                val high = value[index++].digitToIntOrNull(16) ?: throw IllegalArgumentException()
                val low = value[index++].digitToIntOrNull(16) ?: throw IllegalArgumentException()
                bytes.write(high * 16 + low)
            }
            '+' -> bytes.write(32)
            else -> { require(char in '!'..'~'); bytes.write(char.code) }
        }
    }
    val decoder = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
    return decoder.decode(ByteBuffer.wrap(bytes.toByteArray())).toString().also { decoded ->
        require(decoded.none { Character.isISOControl(it) })
    }
}
