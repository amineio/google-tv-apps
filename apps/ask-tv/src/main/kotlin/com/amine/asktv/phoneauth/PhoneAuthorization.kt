package com.amine.asktv.phoneauth

import com.amine.asktv.CALLBACK
import com.amine.asktv.REQUIRED_SCOPES
import com.amine.asktv.RESOURCE
import java.net.URI
import java.util.UUID

data class PhoneAuthorization(val url: String, val state: String) {
    override fun toString(): String = "PhoneAuthorization(redacted)"
}

object PhoneAuthorizationValidator {
    fun validate(value: String): PhoneAuthorization = try {
        require(value.length <= 16_384 && value.all { it in '!'..'~' })
        val uri = URI(value)
        require(uri.scheme == "https" && uri.rawAuthority == "auth.openai.com" &&
            uri.rawPath == "/api/accounts/authorize" && uri.rawFragment == null)
        val fields = strictForm(requireNotNull(uri.rawQuery))
        val required = setOf("client_id", "ext_agent_host_id", "response_type", "redirect_uri", "scope", "resource",
            "state", "nonce", "code_challenge_method", "code_challenge")
        require(fields.keys.containsAll(required) && (fields.keys - required).all { it in setOf("agent_name_hint", "prompt") })
        val client = fields.getValue("client_id")
        require(client.isNotBlank() && client.length <= 256 && client.none { Character.isISOControl(it) })
        val host = fields.getValue("ext_agent_host_id")
        require(host.startsWith("urn:uuid:") && UUID.fromString(host.removePrefix("urn:uuid:")).toString() == host.removePrefix("urn:uuid:"))
        require(fields["response_type"] == "code" && fields["redirect_uri"] == CALLBACK && fields["resource"] == RESOURCE)
        val scopes = fields.getValue("scope").split(' ')
        require(scopes.size == REQUIRED_SCOPES.size && scopes.toSet() == REQUIRED_SCOPES)
        require(fields["code_challenge_method"] == "S256")
        for (key in listOf("state", "nonce", "code_challenge")) require(fields.getValue(key).matches(Regex("[A-Za-z0-9_-]{43}")))
        if (client == "dynamic_agent_client") require(fields["agent_name_hint"] == "AI Chat")
        else require(!fields.containsKey("agent_name_hint"))
        require(!fields.containsKey("prompt") || fields["prompt"] == "consent")
        PhoneAuthorization(value, fields.getValue("state"))
    } catch (_: Exception) {
        throw IllegalArgumentException("Invalid authorization URL.")
    }
}
