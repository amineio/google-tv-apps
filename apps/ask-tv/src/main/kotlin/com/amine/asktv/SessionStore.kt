package com.amine.asktv

import kotlinx.serialization.Serializable

@Serializable
data class CredentialRecord(val hostId: String, val clientId: String? = null, val subject: String? = null,
    val issuer: String? = null, val email: String? = null, val accessToken: String? = null,
    val refreshToken: String? = null, val idToken: String? = null, val expiresAt: Long = 0,
    val scopes: Set<String> = emptySet(), val pendingClientId: String? = null) {
    val canInfer: Boolean get() = accessToken != null && REQUIRED_SCOPES.all { it in scopes }
    fun cleared() = copy(email = null, accessToken = null, refreshToken = null, idToken = null, expiresAt = 0, scopes = emptySet())
}
interface RecordStorage { fun read(): CredentialRecord; fun write(record: CredentialRecord) }

/** One lock covers refresh, validation, durable replacement, and local sign-out. */
class SessionStore(private val storage: RecordStorage) {
    @Volatile private var record = storage.read()
    fun snapshot(): CredentialRecord = record
    /** An issued client is durable before exchange, but is not yet a verified account. */
    @Synchronized fun rememberIssuedClient(clientId: String) {
        if (clientId.isBlank() || clientId == "dynamic_agent_client" ||
            (record.clientId != null && record.clientId != clientId) ||
            (record.pendingClientId != null && record.pendingClientId != clientId)) throw AuthFailure()
        if (record.clientId == clientId || record.pendingClientId == clientId) return
        val next = record.copy(pendingClientId = clientId)
        storage.write(next)
        record = next
    }
    @Synchronized fun install(next: CredentialRecord) {
        if (next.hostId != record.hostId || next.clientId.isNullOrBlank() || next.clientId == "dynamic_agent_client" ||
            next.subject.isNullOrBlank() || next.issuer != ISSUER ||
            (record.pendingClientId != null && next.clientId != record.pendingClientId) ||
            (record.clientId != null && (next.clientId != record.clientId || next.subject != record.subject || next.issuer != record.issuer))) throw AuthFailure()
        val verified = next.copy(pendingClientId = null)
        storage.write(verified)
        record = verified
    }
    @Synchronized fun access(nowSeconds: Long, refresh: (CredentialRecord) -> CredentialRecord): String {
        if (!record.canInfer) throw AuthFailure("ChatGPT subscription access is unavailable. Continue with ChatGPT and allow plan use.")
        if (record.expiresAt <= nowSeconds + 60) {
            if (record.refreshToken == null) throw InvalidGrant()
            try { install(refresh(record)) } catch (e: InvalidGrant) { clear(); throw e }
        }
        if (!record.canInfer) throw AuthFailure("ChatGPT plan use is not enabled. Sign in again and allow subscription access.")
        return record.accessToken ?: throw InvalidGrant()
    }
    @Synchronized fun signOut(revoke: (CredentialRecord) -> Boolean): Boolean {
        val confirmed = try { record.refreshToken == null || revoke(record) } catch (_: Exception) { false }
        clear()
        return confirmed
    }
    private fun clear() { val next = record.cleared(); storage.write(next); record = next }
}
