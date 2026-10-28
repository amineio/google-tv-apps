package com.amine.familyhome

import kotlinx.serialization.json.*

/** Durable wire format. Session recovery remains a separate controller operation. */
object FamilyStateCodec {
    private const val MAX_LENGTH = 65_536
    private val json = Json { encodeDefaults = true }

    fun decode(text: String): FamilyState {
        require(text.length <= MAX_LENGTH) { "The family state is too large" }
        val saved = json.parseToJsonElement(text).jsonObject
        val schema = saved["schema"]?.let {
            require(!it.jsonPrimitive.isString) { "Invalid family state schema" }
            requireNotNull(it.jsonPrimitive.intOrNull) { "Invalid family state schema" }
        } ?: 1
        val current = when (schema) {
            1 -> {
                require("profiles" !in saved) { "Invalid legacy family profiles" }
                JsonObject(saved + mapOf("schema" to JsonPrimitive(2),
                    "profiles" to json.encodeToJsonElement(defaultProfiles())))
            }
            2 -> {
                require("profiles" in saved) { "Missing family profiles" }
                saved
            }
            else -> throw IllegalArgumentException("Unsupported saved family state")
        }
        return json.decodeFromJsonElement<FamilyState>(current).validated()
    }

    fun encode(state: FamilyState): String {
        val text = json.encodeToString(state.validated())
        require(text.length <= MAX_LENGTH) { "The family state is too large" }
        return text
    }
}
