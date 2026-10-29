package com.amine.asktv

import java.security.PublicKey
import java.security.Signature
import java.security.KeyFactory
import java.security.spec.RSAPublicKeySpec
import java.math.BigInteger
import java.util.Base64
import kotlinx.serialization.json.*

data class VerifiedIdentity(val subject: String, val issuer: String, val email: String?)

class IdTokenValidator {
    fun verify(token: String, keys: Map<String, PublicKey>, issuer: String, clientId: String,
        nonce: String?, nowSeconds: Long): VerifiedIdentity = try {
        val pieces = token.split('.')
        if (pieces.size != 3 || token.length > 65_536) throw AuthFailure()
        val decoder = Base64.getUrlDecoder()
        val header = Json.parseToJsonElement(String(decoder.decode(pieces[0]), Charsets.UTF_8)).jsonObject
        if (header["alg"]?.jsonPrimitive?.content != "RS256") throw AuthFailure()
        val key = keys[header["kid"]?.jsonPrimitive?.content] ?: throw AuthFailure()
        val valid = Signature.getInstance("SHA256withRSA").run {
            initVerify(key); update("${pieces[0]}.${pieces[1]}".toByteArray(Charsets.US_ASCII)); verify(decoder.decode(pieces[2]))
        }
        if (!valid) throw AuthFailure()
        val c = Json.parseToJsonElement(String(decoder.decode(pieces[1]), Charsets.UTF_8)).jsonObject
        val aud = when(val a = c["aud"]) { is JsonArray -> a.map { it.jsonPrimitive.content }; is JsonPrimitive -> listOf(a.content); else -> emptyList() }
        if (c["iss"]?.jsonPrimitive?.content != issuer || issuer != ISSUER || clientId !in aud ||
            (c["exp"]?.jsonPrimitive?.longOrNull ?: 0) <= nowSeconds ||
            (c["nbf"]?.jsonPrimitive?.longOrNull ?: 0) > nowSeconds + 60 ||
            (nonce != null && c["nonce"]?.jsonPrimitive?.content != nonce)) throw AuthFailure()
        if (aud.size > 1 && c["azp"] != null && c["azp"]?.jsonPrimitive?.content != clientId) throw AuthFailure()
        VerifiedIdentity(c["sub"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: throw AuthFailure(),
            issuer, c["email"]?.jsonPrimitive?.contentOrNull)
    } catch (e: AuthFailure) { throw e } catch (_: Exception) { throw AuthFailure() }

    fun keys(document: String): Map<String, PublicKey> = try {
        Json.parseToJsonElement(document).jsonObject["keys"]!!.jsonArray.mapNotNull { element ->
            val k = element.jsonObject
            if (k["kty"]?.jsonPrimitive?.content != "RSA" || k["use"]?.jsonPrimitive?.content?.let { it != "sig" } == true ||
                k["alg"]?.jsonPrimitive?.content?.let { it != "RS256" } == true) null else {
                val decoder = Base64.getUrlDecoder()
                val spec = RSAPublicKeySpec(BigInteger(1, decoder.decode(k["n"]!!.jsonPrimitive.content)), BigInteger(1, decoder.decode(k["e"]!!.jsonPrimitive.content)))
                k["kid"]!!.jsonPrimitive.content to KeyFactory.getInstance("RSA").generatePublic(spec)
            }
        }.toMap()
    } catch (_: Exception) { throw AuthFailure() }
}
