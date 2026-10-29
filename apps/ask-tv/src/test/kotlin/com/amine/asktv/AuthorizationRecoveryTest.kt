package com.amine.asktv

import org.junit.Assert.*
import org.junit.Test
import java.net.URI
import java.net.URLDecoder

class AuthorizationRecoveryTest {
    private fun query(url: String): Map<String, String> = URI(url).rawQuery.split('&').associate {
        val pieces = it.split('=', limit=2)
        URLDecoder.decode(pieces[0],"UTF-8") to URLDecoder.decode(pieces[1],"UTF-8")
    }

    @Test fun enablePlanExplicitlyRequestsConsentWithIssuedRegistrationAndFullScopes() {
        val protocol = OAuthProtocol("urn:uuid:test", "issued", requestConsent=true)
        protocol.begin()
        val parameters = query(protocol.authorizationUrl())
        assertEquals("consent", parameters["prompt"])
        assertEquals("issued", parameters["client_id"])
        assertEquals(REQUIRED_SCOPES, parameters["scope"]!!.split(' ').toSet())
        assertFalse(parameters.containsKey("agent_name_hint"))
    }

    @Test fun ordinarySignInDoesNotForceConsent() {
        val protocol = OAuthProtocol("urn:uuid:test", "issued")
        protocol.begin()
        assertFalse(query(protocol.authorizationUrl()).containsKey("prompt"))
    }

    @Test fun pendingRegistrationSurvivesCodeFailureAndRestartWithoutActivatingIdentity() {
        val records = MemoryRecords(CredentialRecord("urn:uuid:test"))
        val store = SessionStore(records)
        val client = OAuthClient(HttpTransport { _,_,_,_,_ ->
            HttpResult(400,"""{"error":"invalid_grant"}""".byteInputStream())
        })
        val protocol = OAuthProtocol("urn:uuid:test")
        val attempt = protocol.begin()
        val grant = protocol.callback(mapOf("state" to attempt.state,"code" to "synthetic-code","client_id" to "issued-pending"))
        protocol.withActiveAttempt(grant.attempt) { store.rememberIssuedClient(grant.clientId) }
        assertThrows(InvalidGrant::class.java) { client.exchange(grant,store.snapshot(),Cancellation()) }

        val restored = SessionStore(records).snapshot()
        assertEquals("issued-pending",restored.pendingClientId)
        assertNull(restored.clientId)
        assertNull(restored.subject)
        assertNull(restored.accessToken)
        assertFalse(restored.canInfer)
        val retry = OAuthProtocol(restored.hostId,restored.pendingClientId)
        retry.begin()
        assertEquals("issued-pending",query(retry.authorizationUrl())["client_id"])
    }

    @Test fun pendingRegistrationCannotChangeAndVerifiedIdentityMustCompleteItsBinding() {
        val store = SessionStore(MemoryRecords(CredentialRecord("urn:uuid:test")))
        store.rememberIssuedClient("issued")
        assertThrows(AuthFailure::class.java) { store.rememberIssuedClient("other") }
        assertNull(store.snapshot().subject)
        store.install(record())
        assertEquals("issued",store.snapshot().clientId)
        assertEquals("subject",store.snapshot().subject)
        assertNull(store.snapshot().pendingClientId)
    }

    @Test fun canceledCallbackCannotPersistPendingRegistration() {
        val store = SessionStore(MemoryRecords(CredentialRecord("urn:uuid:test")))
        val protocol = OAuthProtocol("urn:uuid:test")
        val attempt = protocol.begin()
        val grant = protocol.callback(mapOf("state" to attempt.state,"code" to "synthetic","client_id" to "issued"))
        protocol.cancel()
        assertThrows(AuthFailure::class.java) {
            protocol.withActiveAttempt(grant.attempt) { store.rememberIssuedClient(grant.clientId) }
        }
        assertNull(store.snapshot().pendingClientId)
    }

    @Test fun everyTerminalRefreshCodeClearsTokensAndRetainsVerifiedBinding() {
        val codes = listOf("invalid_grant","invalid_refresh_token","token_expired","refresh_token_expired",
            "refresh_token_invalidated","refresh_token_reused")
        codes.forEach { code ->
            val store = SessionStore(MemoryRecords())
            val client = OAuthClient(HttpTransport { _,_,_,_,_ ->
                HttpResult(400,"""{"error":"$code"}""".byteInputStream())
            })
            assertThrows("refresh code: $code",AuthFailure::class.java) {
                store.access(1000) { client.refresh(it,Cancellation()) }
            }
            val cleared = store.snapshot()
            assertNull("refresh code: $code",cleared.accessToken)
            assertNull(cleared.refreshToken)
            assertNull(cleared.idToken)
            assertEquals("issued",cleared.clientId)
            assertEquals("subject",cleared.subject)
        }
    }

    @Test fun temporaryRefreshFailurePreservesTheTokenSet() {
        val store = SessionStore(MemoryRecords())
        val original = store.snapshot()
        val client = OAuthClient(HttpTransport { _,_,_,_,_ ->
            HttpResult(503,"""{"error":"temporarily_unavailable"}""".byteInputStream())
        })
        assertThrows(AuthFailure::class.java) { store.access(1000) { client.refresh(it,Cancellation()) } }
        assertEquals(original,store.snapshot())
    }
}
