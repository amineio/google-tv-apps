package com.amine.familyhome

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object ParentPin {
    private const val ITERATIONS = 120_000
    private fun valid(pin: String) = pin.length == 4 && pin.all { it in '0'..'9' }

    fun enable(state: FamilyState, pin: String): FamilyState {
        require(valid(pin)) { "Enter four digits" }
        val salt = ByteArray(24).also { SecureRandom().nextBytes(it) }
        return state.copy(pinSalt = Base64.getEncoder().encodeToString(salt),
            pinHash = Base64.getEncoder().encodeToString(derive(pin, salt)))
    }

    fun accepts(state: FamilyState, pin: String): Boolean {
        if (!state.pinEnabled) return true
        if (!valid(pin)) return false
        return try {
            val salt = Base64.getDecoder().decode(state.pinSalt)
            val expected = Base64.getDecoder().decode(state.pinHash)
            salt.size == 24 && expected.size == 32 && MessageDigest.isEqual(expected, derive(pin, salt))
        } catch (_: IllegalArgumentException) { false }
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
}
