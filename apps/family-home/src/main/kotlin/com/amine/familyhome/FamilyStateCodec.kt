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
                require("usage" !in saved) { "Invalid legacy family usage" }
                JsonObject(saved + mapOf("schema" to JsonPrimitive(3),
                    "profiles" to json.encodeToJsonElement(defaultProfiles())))
            }
            2 -> {
                require("profiles" in saved) { "Missing family profiles" }
                require("usage" !in saved) { "Invalid legacy family usage" }
                JsonObject(saved + ("schema" to JsonPrimitive(3)))
            }
            3 -> {
                require("profiles" in saved) { "Missing family profiles" }
                saved
            }
            else -> throw IllegalArgumentException("Unsupported saved family state")
        }
        return json.decodeFromJsonElement<FamilyState>(current).validated()
    }

    /** History must not prevent a valid profile, timer, app-list, or PIN change from being saved. */
    fun prepareForSave(state: FamilyState): FamilyState {
        var prepared = state.copy(usage = state.usage.toSortedMap().entries.toList()
            .takeLast(UsageEngine.HISTORY_DATES).associate { it.toPair() }).validated()
        while (wire(prepared).length > MAX_LENGTH) {
            require(prepared.usage.isNotEmpty()) { "The family state is too large" }
            prepared = prepared.copy(usage = prepared.usage - prepared.usage.keys.min())
        }
        return prepared
    }

    fun encode(state: FamilyState): String = wire(prepareForSave(state))

    private fun wire(state: FamilyState): String {
        val saved = json.encodeToJsonElement(state).jsonObject
        // An empty optional field must not increase the size of an existing near-limit core state.
        return (if (state.usage.isEmpty()) JsonObject(saved - "usage") else saved).toString()
    }
}
