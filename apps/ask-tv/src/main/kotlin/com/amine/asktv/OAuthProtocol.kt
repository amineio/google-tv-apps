package com.amine.asktv

import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

const val ISSUER = "https://auth.openai.com"
const val TOKEN_ENDPOINT = "$ISSUER/api/accounts/oauth/token"
const val RESOURCE = "https://api.openai.com/v1"
const val CALLBACK = "http://127.0.0.1:1455/auth/callback"
val REQUIRED_SCOPES = setOf("openid", "profile", "email", "offline_access", "resource.invoke", "chatgpt.tokens.use.direct")
open class AuthFailure(val safeMessage: String = "Sign-in could not be verified. Please try again.") : Exception(safeMessage)
class InvalidGrant : AuthFailure("Your session ended. Continue with ChatGPT to sign in again.")

fun form(values: Map<String, String>): String = values.entries.joinToString("&") {
    URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
}

data class AuthAttempt(val state: String, val nonce: String, val verifier: String, val started: Long,
    val returningClient: String?, var consumed: Boolean = false)
data class AuthGrant(val code: String, val clientId: String, val attempt: AuthAttempt)

/** Attempt values never leave Android except through the OAuth protocol. */
class OAuthProtocol(private val hostId: String, private val clientId: String? = null,
    private val requestConsent: Boolean = false, private val now: () -> Long = System::currentTimeMillis) {
    var pending: AuthAttempt? = null
        private set
    @Synchronized fun begin(): AuthAttempt = AuthAttempt(random(), random(), random(), now(), clientId).also { pending = it }
    @Synchronized fun authorizationUrl(): String {
        val a = pending ?: throw AuthFailure()
        checkActive(a)
        val params = linkedMapOf("client_id" to (a.returningClient ?: "dynamic_agent_client"),
            "ext_agent_host_id" to hostId, "response_type" to "code", "redirect_uri" to CALLBACK,
            "scope" to REQUIRED_SCOPES.joinToString(" "), "resource" to RESOURCE, "state" to a.state,
            "nonce" to a.nonce, "code_challenge_method" to "S256", "code_challenge" to challenge(a.verifier))
        if (a.returningClient == null) params["agent_name_hint"] = "AI Chat"
        if (requestConsent) params["prompt"] = "consent"
        return "$ISSUER/api/accounts/authorize?${form(params)}"
    }
    @Synchronized fun callback(params: Map<String, String>): AuthGrant {
        val a = pending ?: throw AuthFailure()
        checkActive(a)
        if (a.consumed || !MessageDigest.isEqual(a.state.toByteArray(), (params["state"] ?: "").toByteArray())) throw AuthFailure()
        a.consumed = true
        if (params.containsKey("error")) { pending = null; throw AuthFailure("Sign-in was declined. You can try again.") }
        val issued = params["client_id"] ?: a.returningClient ?: throw AuthFailure()
        if (issued.isBlank() || issued == "dynamic_agent_client" || (a.returningClient != null && issued != a.returningClient)) throw AuthFailure()
        val code = params["code"]?.takeIf { it.isNotBlank() } ?: throw AuthFailure()
        return AuthGrant(code, issued, a)
    }
    @Synchronized fun withActiveAttempt(attempt: AuthAttempt, action: () -> Unit) {
        checkActive(attempt)
        if (!attempt.consumed) throw AuthFailure()
        action()
    }
    @Synchronized fun commit(attempt: AuthAttempt, action: () -> Unit) {
        withActiveAttempt(attempt, action)
        pending = null
    }
    @Synchronized fun cancel() { pending = null }
    private fun checkActive(a: AuthAttempt) {
        if (pending !== a || now() - a.started !in 0..600_000) throw AuthFailure("Sign-in expired. Start a new attempt.")
    }
    companion object {
        fun challenge(verifier: String): String = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
        private fun random(): String = ByteArray(32).also { SecureRandom().nextBytes(it) }.let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
    }
}
