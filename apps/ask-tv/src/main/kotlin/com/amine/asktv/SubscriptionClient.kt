package com.amine.asktv

import java.io.Closeable
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.serialization.json.*

class Cancellation {
    @Volatile var canceled = false
        private set
    private val actions = mutableListOf<() -> Unit>()
    @Synchronized fun onCancel(action: () -> Unit) { if(canceled) action() else actions += action }
    @Synchronized fun cancel() { if(canceled) return; canceled = true; actions.forEach { runCatching(it) }; actions.clear() }
    fun check() { if(canceled) throw AuthFailure("Stopped.") }
}
class ApiFailure(val status: Int) : Exception(when(status) {
    401 -> "Your session needs sign-in. Continue with ChatGPT."
    403 -> "ChatGPT subscription access is unavailable. Check your plan permissions."
    429 -> "Your ChatGPT plan usage limit was reached. Check ChatGPT Settings → Usage, then try later."
    else -> "The service could not complete this request. Please try again."
})
data class HttpResult(val status: Int, val body: InputStream, private val closeAction: () -> Unit = {}) : Closeable {
    override fun close() { runCatching { body.close() }; closeAction() }
    fun text(limit: Int = 1_048_576): String {
        val bytes = body.readNBytes(limit + 1)
        if(bytes.size > limit) throw AuthFailure("The service response was too large.")
        return String(bytes, Charsets.UTF_8)
    }
}
fun interface HttpTransport {
    fun execute(method: String, url: String, headers: Map<String, String>, body: String?, cancel: Cancellation): HttpResult
}
class DirectHttpTransport : HttpTransport {
    override fun execute(method: String, url: String, headers: Map<String, String>, body: String?, cancel: Cancellation): HttpResult {
        val parsed = URL(url)
        require(parsed.protocol == "https" && parsed.host in setOf("auth.openai.com", "api.openai.com") && parsed.userInfo == null && parsed.port in setOf(-1,443))
        cancel.check()
        val connection = parsed.openConnection() as HttpURLConnection
        cancel.onCancel { connection.disconnect() }
        connection.requestMethod = method
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 20000
        connection.readTimeout = 60000
        headers.forEach { (key, value) -> connection.setRequestProperty(key,value) }
        try {
            if(body != null) {
                connection.doOutput = true
                val bytes = body.toByteArray(Charsets.UTF_8)
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
            }
            cancel.check()
            val status = connection.responseCode
            val stream = if(status in 200..299) connection.inputStream else connection.errorStream ?: InputStream.nullInputStream()
            return HttpResult(status, stream) { connection.disconnect() }
        } catch(e: Exception) { connection.disconnect(); throw e }
    }
}
data class Model(val slug: String, val displayName: String)

class SubscriptionClient(private val transport: HttpTransport = DirectHttpTransport()) {
    fun models(access: String, cancel: Cancellation): List<Model> = transport.execute("GET", "$RESOURCE/models",
        mapOf("Authorization" to "Bearer $access"),null,cancel).use { response ->
        if(response.status !in 200..299) throw ApiFailure(response.status)
        parseModels(response.text())
    }
    fun answer(access: String, model: String, catalog: List<Model>, input: JsonArray, cancel: Cancellation,
        onText: (String) -> Unit): StreamResult {
        requireModel(model,catalog)
        return transport.execute("POST", "$RESOURCE/responses",mapOf("Authorization" to "Bearer $access",
            "Content-Type" to "application/json", "Accept" to "text/event-stream"),payload(model,input).toString(),cancel).use { response ->
            if(response.status !in 200..299) throw ApiFailure(response.status)
            ResponsesStream.consume(response.body.bufferedReader(Charsets.UTF_8),{cancel.canceled},onText)
        }
    }
    companion object {
        fun requireModel(slug: String, catalog: List<Model>) { require(catalog.any { it.slug == slug }) { "Select a currently available model." } }
        fun parseModels(raw: String): List<Model> = Json.parseToJsonElement(raw).jsonObject["models"]?.jsonArray?.mapNotNull {
            val m = it.jsonObject
            if(m["visibility"]?.jsonPrimitive?.content == "list") {
                val slug=m["slug"]?.jsonPrimitive?.contentOrNull
                val name=m["display_name"]?.jsonPrimitive?.contentOrNull
                if(slug.isNullOrBlank() || name.isNullOrBlank()) null else Model(slug,name)
            } else null
        }?.distinctBy { it.slug } ?: throw AuthFailure("The model list could not be read. Try signing in again.")
        fun payload(model: String,input: JsonArray): JsonObject = buildJsonObject {
            put("model",model); put("input",input); put("store",false); put("stream",true)
        }
    }
}
