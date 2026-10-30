package com.amine.asktv.phoneauth

import com.amine.asktv.*
import org.junit.Assert.*
import org.junit.Test

class PhoneAuthorizationTest {
    private fun url(client: String? = null, consent: Boolean = false): String =
        OAuthProtocol("urn:uuid:00000000-0000-4000-8000-000000000001", client, consent).run { begin(); authorizationUrl() }

    @Test fun acceptsCurrentRegistrationAndConsentUrls() {
        for (value in listOf(url(), url("issued-client"), url("issued-client", true))) {
            val checked = PhoneAuthorizationValidator.validate(value)
            assertEquals(value, checked.url)
            assertTrue(checked.state.matches(Regex("[A-Za-z0-9_-]{43}")))
            assertFalse(checked.toString().contains(value))
            assertFalse(checked.toString().contains(checked.state))
        }
    }

    @Test fun rejectsExternalOriginsRedirectsAndCredentialHints() {
        val valid = url()
        val bad = listOf(valid.replace("https://auth.openai.com", "http://auth.openai.com"),
            valid.replace("auth.openai.com", "auth.openai.com.example.test"),
            valid.replace("auth.openai.com", "user@auth.openai.com"),
            valid.replace("auth.openai.com", "auth.openai.com:443"),
            valid.replace("/api/accounts/authorize?", "/other?"), valid + "#fragment",
            valid.replace("redirect_uri=http%3A%2F%2F127.0.0.1%3A1455%2Fauth%2Fcallback", "redirect_uri=https%3A%2F%2Fexample.test"),
            valid.replace("127.0.0.1%3A1455", "localhost%3A1455"), valid + "&id_token_hint=synthetic",
            valid + "&redirect_uri=http%3A%2F%2F127.0.0.1%3A1455%2Fauth%2Fcallback", valid + "&%73tate=other",
            valid + "&unknown=x", valid + "x".repeat(16384))
        for (value in bad) assertThrows(IllegalArgumentException::class.java) { PhoneAuthorizationValidator.validate(value) }
    }

    @Test fun rejectsMissingOrChangedOAuthRequirements() {
        val valid = url()
        val fields = listOf("client_id", "ext_agent_host_id", "response_type", "redirect_uri", "scope", "resource", "state", "nonce", "code_challenge_method", "code_challenge")
        for (field in fields) {
            val missing = valid.replace(Regex("([?&])$field=[^&]*&?")) { if (it.value.startsWith("?")) "?" else "" }.trimEnd('&')
            assertThrows(IllegalArgumentException::class.java) { PhoneAuthorizationValidator.validate(missing) }
        }
        for ((field, value) in listOf("client_id" to "", "client_id" to "%20", "client_id" to "%00issued",
            "response_type" to "token", "resource" to "https%3A%2F%2Fexample.test", "scope" to "openid",
            "scope" to "openid+profile+email+offline_access+resource.invoke+chatgpt.tokens.use.direct+extra",
            "code_challenge_method" to "plain", "code_challenge" to "short", "state" to "short", "nonce" to "short", "prompt" to "login")) {
            val changed = if (field == "prompt") "$valid&prompt=$value" else valid.replace(Regex("(?<=[?&])$field=[^&]*"), "$field=$value")
            assertThrows(IllegalArgumentException::class.java) { PhoneAuthorizationValidator.validate(changed) }
        }
    }
}
