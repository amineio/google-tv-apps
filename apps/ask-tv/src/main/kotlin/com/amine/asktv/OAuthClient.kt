package com.amine.asktv

import kotlinx.serialization.json.*

class OAuthClient(private val transport: HttpTransport = DirectHttpTransport(),
    private val seconds: () -> Long = { System.currentTimeMillis()/1000 }) {
    private val validator = IdTokenValidator()
    private fun discovery(cancel: Cancellation): JsonObject = transport.execute("GET", "$ISSUER/.well-known/openid-configuration",emptyMap(),null,cancel).use {
        if(it.status != 200) throw AuthFailure()
        Json.parseToJsonElement(it.text()).jsonObject.also { d ->
            if(d["issuer"]?.jsonPrimitive?.content != ISSUER ||
                d["id_token_signing_alg_values_supported"]?.jsonArray?.any { a -> a.jsonPrimitive.content == "RS256" } != true) throw AuthFailure()
        }
    }
    private fun endpoint(discovery: JsonObject, name: String): String {
        val value = discovery[name]?.jsonPrimitive?.content ?: throw AuthFailure()
        val uri = java.net.URI(value)
        if(uri.scheme != "https" || uri.host != "auth.openai.com" || uri.userInfo != null || uri.port !in setOf(-1,443) || uri.fragment != null) throw AuthFailure()
        return value
    }
    private fun identity(idToken: String, clientId: String, nonce: String?, cancel: Cancellation): VerifiedIdentity {
        val d=discovery(cancel)
        val keys=transport.execute("GET",endpoint(d,"jwks_uri"),emptyMap(),null,cancel).use {
            if(it.status != 200) throw AuthFailure()
            validator.keys(it.text())
        }
        return validator.verify(idToken,keys,ISSUER,clientId,nonce,seconds())
    }
    private fun token(fields: Map<String,String>,cancel: Cancellation,isRefresh: Boolean = false): JsonObject = transport.execute("POST",TOKEN_ENDPOINT,
        mapOf("Content-Type" to "application/x-www-form-urlencoded"),form(fields),cancel).use {
        if(it.status !in 200..299) {
            val code=runCatching {
                when(val error=Json.parseToJsonElement(it.text(65536)).jsonObject["error"]) {
                    is JsonPrimitive -> error.contentOrNull
                    is JsonObject -> error["code"]?.jsonPrimitive?.contentOrNull
                    else -> null
                }
            }.getOrNull()
            val unusable=setOf("invalid_grant","invalid_refresh_token","token_expired","refresh_token_expired",
                "refresh_token_invalidated","refresh_token_reused")
            if(code=="invalid_grant" || (isRefresh && code in unusable)) throw InvalidGrant()
            if(code=="invalid_client") throw AuthFailure("This ChatGPT registration needs its client configuration checked.")
            throw AuthFailure("ChatGPT could not authorize this session. Try signing in again.")
        }
        Json.parseToJsonElement(it.text(65536)).jsonObject
    }
    fun exchange(grant: AuthGrant, current: CredentialRecord, cancel: Cancellation): CredentialRecord {
        val t=token(mapOf("grant_type" to "authorization_code","client_id" to grant.clientId,"code" to grant.code,
            "code_verifier" to grant.attempt.verifier,"redirect_uri" to CALLBACK,"resource" to RESOURCE),cancel)
        val id=t["id_token"]?.jsonPrimitive?.content ?: throw AuthFailure()
        val identity=identity(id,grant.clientId,grant.attempt.nonce,cancel)
        if(current.clientId != null && (current.clientId != grant.clientId || current.subject != identity.subject || current.issuer != identity.issuer)) throw AuthFailure("This registration belongs to another account. Use the original ChatGPT account.")
        return credentials(t,current.copy(clientId=grant.clientId,subject=identity.subject,issuer=identity.issuer,email=identity.email),id,initial=true)
    }
    fun refresh(current: CredentialRecord,cancel: Cancellation): CredentialRecord {
        val t=token(mapOf("grant_type" to "refresh_token","client_id" to (current.clientId ?: throw InvalidGrant()),
            "refresh_token" to (current.refreshToken ?: throw InvalidGrant()),"resource" to RESOURCE),cancel,isRefresh=true)
        val id=t["id_token"]?.jsonPrimitive?.contentOrNull
        var email=current.email
        if(id != null) {
            val identity=identity(id,current.clientId!!,null,cancel)
            if(identity.subject != current.subject || identity.issuer != current.issuer) throw AuthFailure()
            email=identity.email
        }
        return credentials(t,current.copy(email=email),id ?: current.idToken,initial=false)
    }
    private fun credentials(t: JsonObject,base: CredentialRecord,id: String?,initial: Boolean): CredentialRecord {
        if(!t["token_type"]?.jsonPrimitive?.content.orEmpty().equals("Bearer",true)) throw AuthFailure()
        val expiry=t["expires_in"]?.jsonPrimitive?.longOrNull?.takeIf { it in 1..31_536_000 } ?: throw AuthFailure()
        val access=t["access_token"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: throw AuthFailure()
        val refresh=t["refresh_token"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: if(initial) throw AuthFailure() else base.refreshToken
        val scopes=t["scope"]?.jsonPrimitive?.contentOrNull?.split(Regex("\\s+"))?.filter { it.isNotBlank() }?.toSet()
            ?: if(initial) emptySet() else base.scopes
        return base.copy(accessToken=access,refreshToken=refresh,idToken=id,expiresAt=seconds()+expiry,scopes=scopes)
    }
    fun revoke(record: CredentialRecord,cancel: Cancellation): Boolean {
        val refresh=record.refreshToken ?: return true
        val url=endpoint(discovery(cancel),"revocation_endpoint")
        repeat(3) { attempt ->
            try {
                transport.execute("POST",url,mapOf("Content-Type" to "application/x-www-form-urlencoded"),
                    form(mapOf("token" to refresh,"token_type_hint" to "refresh_token","client_id" to (record.clientId ?: return false))),cancel).use {
                    if(it.status==200) return true
                    if(it.status < 500) return false
                }
            } catch (_: Exception) { if(cancel.canceled) return false }
            if(attempt < 2) Thread.sleep(250L shl attempt)
        }
        return false
    }
}
