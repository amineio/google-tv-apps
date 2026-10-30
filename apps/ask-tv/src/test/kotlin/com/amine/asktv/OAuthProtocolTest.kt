package com.amine.asktv
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import kotlinx.serialization.json.*

class OAuthProtocolTest {
    private fun attempt(client: String? = null) = OAuthProtocol("urn:uuid:test", client, now = { 1000L }).also { it.begin() }
    @Test fun pkceUsesS256() { assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", OAuthProtocol.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")) }
    @Test fun authorizationUsesFreshStateNonceAndRegistrationOnlyHint() {
        val a=attempt(); val first=a.pending!!; a.begin(); assertNotEquals(first.state,a.pending!!.state); assertNotEquals(first.nonce,a.pending!!.nonce)
        assertTrue(a.authorizationUrl().contains("agent_name_hint=AI+Chat")); assertFalse(attempt("issued").authorizationUrl().contains("agent_name_hint"))
    }
    @Test fun wrongStateRejected() { val a=attempt(); assertThrows(AuthFailure::class.java) { a.callback(mapOf("state" to "wrong", "code" to "code", "client_id" to "issued")) } }
    @Test fun initialClientRequired() { val a=attempt(); assertThrows(AuthFailure::class.java) { a.callback(mapOf("state" to a.pending!!.state,"code" to "code")) } }
    @Test fun returningClientCannotChange() { val a=attempt("issued"); assertThrows(AuthFailure::class.java) { a.callback(mapOf("state" to a.pending!!.state,"code" to "code","client_id" to "other")) } }
    @Test fun denialConsumesAttempt() { val a=attempt(); assertThrows(AuthFailure::class.java) { a.callback(mapOf("state" to a.pending!!.state,"error" to "access_denied")) }; assertNull(a.pending) }
    @Test fun canceledAttemptRejectsLateCallback() { val a=attempt(); val s=a.pending!!.state; a.cancel(); assertThrows(AuthFailure::class.java) { a.callback(mapOf("state" to s,"code" to "late","client_id" to "issued")) } }
    @Test fun timeoutRejectsCallback() { var time=0L; val a=OAuthProtocol("urn:uuid:test",now={time}); a.begin(); val s=a.pending!!.state; time=600001; assertThrows(AuthFailure::class.java) { a.callback(mapOf("state" to s,"code" to "late","client_id" to "issued")) } }
    @Test fun returningIdentityMismatchPreservesCredentials() { val m=MemoryRecords(record()); val s=SessionStore(m); assertThrows(AuthFailure::class.java) { s.install(record(subject="other")) }; assertEquals("subject",s.snapshot().subject) }
    private val pair=KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private fun jwt(claims: String, badSignature: Boolean=false): String {
        fun enc(b:ByteArray)=Base64.getUrlEncoder().withoutPadding().encodeToString(b)
        val body=enc("{\"alg\":\"RS256\",\"kid\":\"test\"}".toByteArray())+"."+enc(claims.toByteArray())
        val signature=Signature.getInstance("SHA256withRSA").run { initSign(pair.private); update(body.toByteArray()); sign() }; if(badSignature) signature[0]=(signature[0].toInt() xor 1).toByte()
        return body+"."+enc(signature)
    }
    private fun claims(aud:String="[\"issued\",\"other\"]",issuer:String="https://auth.openai.com",expiry:Long=2000,nonce:String="nonce")="""{"iss":"$issuer","sub":"subject","aud":$aud,"exp":$expiry,"nonce":"$nonce","email":"viewer@example.test"}"""
    private fun verify(token:String)=IdTokenValidator().verify(token,mapOf("test" to pair.public),"https://auth.openai.com","issued","nonce",1000)
    @Test fun signedAudienceArrayAccepted() { assertEquals("subject",verify(jwt(claims())).subject) }
    @Test fun invalidSignatureRejected() { assertThrows(AuthFailure::class.java) { verify(jwt(claims(),true)) } }
    @Test fun wrongIssuerRejected() { assertThrows(AuthFailure::class.java) { verify(jwt(claims(issuer="https://example.test"))) } }
    @Test fun wrongAudienceRejected() { assertThrows(AuthFailure::class.java) { verify(jwt(claims(aud="\"wrong\""))) } }
    @Test fun expiredTokenRejected() { assertThrows(AuthFailure::class.java) { verify(jwt(claims(expiry=999))) } }
    @Test fun wrongNonceRejected() { assertThrows(AuthFailure::class.java) { verify(jwt(claims(nonce="other"))) } }
}
