package com.amine.asktv

import java.io.BufferedReader
import kotlinx.serialization.json.*

data class StreamResult(val text: String, val complete: Boolean, val message: String = "")
object ResponsesStream {
    fun consume(reader: BufferedReader, canceled: () -> Boolean, onText: (String) -> Unit): StreamResult {
        val text = StringBuilder()
        val data = StringBuilder()
        fun event(): StreamResult? {
            if (data.isEmpty()) return null
            val raw = data.toString(); data.setLength(0)
            if (raw == "[DONE]") return StreamResult(text.toString(), false, "The connection ended before the answer completed.")
            val obj = try { Json.parseToJsonElement(raw).jsonObject } catch (_: Exception) { return StreamResult(text.toString(), false, "The answer stream could not be read.") }
            return when(obj["type"]?.jsonPrimitive?.content) {
                "response.output_text.delta" -> {
                    val delta = obj["delta"]?.jsonPrimitive?.contentOrNull ?: return StreamResult(text.toString(), false, "The answer stream could not be read.")
                    if (text.length + delta.length > 48000) return StreamResult(text.toString(), false, "The answer exceeded the local display limit.")
                    text.append(delta); onText(text.toString()); null
                }
                "response.completed" -> if (text.isNotBlank()) StreamResult(text.toString(), true) else StreamResult("",false,"No readable answer was returned.")
                "response.failed", "error" -> {
                    val error = obj["response"]?.jsonObject?.get("error")?.jsonObject ?: obj["error"] as? JsonObject ?: obj
                    val code = error["code"]?.jsonPrimitive?.contentOrNull
                    val message = when(code) {
                        "subscription_sharing_usage_limit_exceeded" -> "Your ChatGPT plan usage limit was reached. Check ChatGPT Settings → Usage, then try later."
                        "subscription_sharing_usage_unavailable" -> "ChatGPT subscription use is unavailable. Check plan access in ChatGPT Settings."
                        else -> "The answer failed. Please try again."
                    }
                    StreamResult(text.toString(), false, message)
                }
                "response.incomplete" -> StreamResult(text.toString(), false, "This answer is incomplete. Please try again.")
                else -> null
            }
        }
        return try {
            while (!canceled()) {
                val line = reader.readLine() ?: break
                if (line.length > 131072) return StreamResult(text.toString(),false,"The answer stream could not be read.")
                if (line.isEmpty()) {
                    event()?.let { return if(canceled()) StreamResult(text.toString(),false,"Stopped.") else it }
                } else if (line.startsWith("data:")) {
                    if (data.isNotEmpty()) data.append('\n')
                    data.append(line.substring(5).removePrefix(" "))
                    if(data.length > 131072) return StreamResult(text.toString(),false,"The answer stream could not be read.")
                }
            }
            if (!canceled() && data.isNotEmpty()) event()?.let { return it }
            StreamResult(text.toString(), false, if(canceled()) "Stopped." else "The connection ended before the answer completed.")
        } catch (_: Exception) { StreamResult(text.toString(),false,if(canceled()) "Stopped." else "The connection was interrupted. This answer is incomplete.") }
    }
}
