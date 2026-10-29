package com.amine.asktv

import org.junit.Assert.*
import org.junit.Test
import kotlinx.serialization.json.*
import java.net.URLDecoder
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.util.Base64

class TransportContractsTest {
    private fun response(body:String,status:Int=200)=HttpResult(status,body.byteInputStream())
    @Test fun inferenceUsesBearerOnlyAndExactSubscriptionEndpoint() {
        var sent=false
        val client=SubscriptionClient(HttpTransport { method,url,headers,body,_ ->
            assertEquals("POST",method); assertEquals("https://api.openai.com/v1/responses",url)
            assertEquals("Bearer synthetic",headers["Authorization"])
            assertEquals(setOf("Authorization","Content-Type","Accept"),headers.keys)
            val payload=Json.parseToJsonElement(body!!).jsonObject
            assertFalse(payload["store"]!!.jsonPrimitive.boolean); assertTrue(payload["stream"]!!.jsonPrimitive.boolean)
            sent=true
            response("data: {\"type\":\"response.output_text.delta\",\"delta\":\"OK\"}\n\ndata: {\"type\":\"response.completed\"}\n\n")
        })
        val result=client.answer("synthetic","listed",listOf(Model("listed","Listed")),ConversationSession().input("hello"),Cancellation()){}
        assertTrue(sent); assertTrue(result.complete)
    }
    @Test fun unlistedModelNeverSendsRequest() {
        var requests=0
        val client=SubscriptionClient(HttpTransport { _,_,_,_,_ -> requests++; response("") })
        assertThrows(IllegalArgumentException::class.java) { client.answer("synthetic","absent",emptyList(),ConversationSession().input("hello"),Cancellation()){} }
        assertEquals(0,requests)
    }
    @Test fun canceledTransportIsClosed() { val cancel=Cancellation(); var closed=false; cancel.onCancel{closed=true}; cancel.cancel(); assertTrue(closed); assertThrows(AuthFailure::class.java){cancel.check()} }
    private fun decode(body:String)=body.split('&').associate { val p=it.split('=',limit=2); URLDecoder.decode(p[0],"UTF-8") to URLDecoder.decode(p[1],"UTF-8") }
    @Test fun refreshUsesIssuedClientAndRotatesWithoutScopeRequest() {
        val client=OAuthClient(HttpTransport { method,url,headers,body,_ ->
            assertEquals("POST",method); assertEquals(TOKEN_ENDPOINT,url); assertEquals("application/x-www-form-urlencoded",headers["Content-Type"])
            val fields=decode(body!!)
            assertEquals("refresh_token",fields["grant_type"]); assertEquals("issued",fields["client_id"]); assertEquals("refresh-old",fields["refresh_token"])
            assertFalse(fields.containsKey("scope")); assertEquals(RESOURCE,fields["resource"])
            response("""{"access_token":"new","refresh_token":"rotated","token_type":"Bearer","expires_in":3600}""")
        },{1000})
        val refreshed=client.refresh(record(),Cancellation())
        assertEquals("rotated",refreshed.refreshToken); assertEquals(REQUIRED_SCOPES,refreshed.scopes); assertEquals(4600,refreshed.expiresAt)
    }
    @Test fun invalidGrantBecomesReauthentication() {
        val client=OAuthClient(HttpTransport{_,_,_,_,_->response("""{"error":"invalid_grant"}""",400)})
        assertThrows(InvalidGrant::class.java){client.refresh(record(),Cancellation())}
    }
    @Test fun revocationUsesDiscoveredEndpointAndIssuedClient() {
        val client=OAuthClient(HttpTransport{_,url,_,body,_->
            if(url.endsWith("openid-configuration")) response("""{"issuer":"https://auth.openai.com","id_token_signing_alg_values_supported":["RS256"],"revocation_endpoint":"https://auth.openai.com/revoke-fixture"}""")
            else {
                assertEquals("https://auth.openai.com/revoke-fixture",url)
                val fields=decode(body!!); assertEquals("refresh-old",fields["token"]); assertEquals("issued",fields["client_id"]); assertEquals("refresh_token",fields["token_type_hint"])
                response("")
            }
        })
        assertTrue(client.revoke(record(),Cancellation()))
    }
    @Test fun exchangeValidatesPublishedJwksAndAttemptNonce() {
        val pair=KeyPairGenerator.getInstance("RSA").apply{initialize(2048)}.generateKeyPair()
        val key=pair.public as RSAPublicKey
        fun enc(bytes:ByteArray)=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        fun unsigned(number:java.math.BigInteger)=number.toByteArray().let { if(it[0]==0.toByte()) it.copyOfRange(1,it.size) else it }
        val protocol=OAuthProtocol("urn:uuid:test",now={1000}); val attempt=protocol.begin()
        val grant=protocol.callback(mapOf("code" to "code","state" to attempt.state,"client_id" to "issued"))
        val signed=enc("""{"alg":"RS256","kid":"fixture"}""".toByteArray())+"."+enc("""{"iss":"https://auth.openai.com","sub":"generic-viewer","aud":"issued","exp":4000,"nonce":"${attempt.nonce}"}""".toByteArray())
        val jwt=signed+"."+enc(Signature.getInstance("SHA256withRSA").run{initSign(pair.private);update(signed.toByteArray());sign()})
        var calls=0
        val client=OAuthClient(HttpTransport{_,url,_,body,_->
            calls++
            when {
                url==TOKEN_ENDPOINT -> {
                    val fields=decode(body!!); assertEquals("issued",fields["client_id"]); assertEquals(CALLBACK,fields["redirect_uri"]); assertEquals(attempt.verifier,fields["code_verifier"])
                    response(buildJsonObject{put("access_token","synthetic");put("refresh_token","rotated");put("id_token",jwt);put("token_type","Bearer");put("expires_in",3600);put("scope",REQUIRED_SCOPES.joinToString(" "))}.toString())
                }
                url.endsWith("openid-configuration") -> response("""{"issuer":"https://auth.openai.com","id_token_signing_alg_values_supported":["RS256"],"jwks_uri":"https://auth.openai.com/jwks-fixture"}""")
                else -> response("""{"keys":[{"kty":"RSA","use":"sig","alg":"RS256","kid":"fixture","n":"${enc(unsigned(key.modulus))}","e":"${enc(unsigned(key.publicExponent))}"}]}""")
            }
        },{1000})
        val result=client.exchange(grant,CredentialRecord("urn:uuid:test"),Cancellation())
        assertEquals("generic-viewer",result.subject); assertTrue(result.canInfer); assertEquals(3,calls)
    }
    @Test fun cancellationBetweenCallbackAndCommitNeverInstalls() {
        val protocol=OAuthProtocol("urn:uuid:test",now={1000}); val attempt=protocol.begin()
        protocol.callback(mapOf("code" to "code","state" to attempt.state,"client_id" to "issued"))
        protocol.cancel(); var installed=false
        assertThrows(AuthFailure::class.java){protocol.commit(attempt){installed=true}}
        assertFalse(installed)
    }
    @Test fun replacedAttemptCannotCommitOldIdentity() {
        val protocol=OAuthProtocol("urn:uuid:test",now={1000}); val first=protocol.begin()
        protocol.callback(mapOf("code" to "code","state" to first.state,"client_id" to "issued")); protocol.begin()
        assertThrows(AuthFailure::class.java){protocol.commit(first){fail("Old identity installed")}}
    }
}
