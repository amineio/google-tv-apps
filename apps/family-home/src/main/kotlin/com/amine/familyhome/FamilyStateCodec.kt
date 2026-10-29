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
        if (schema < 4) require("appOrder" !in saved) { "Invalid legacy family app order" }
        val current = when (schema) {
            1 -> {
                require("profiles" !in saved) { "Invalid legacy family profiles" }
                require("usage" !in saved) { "Invalid legacy family usage" }
                JsonObject(saved + mapOf("schema" to JsonPrimitive(5),
                    "profiles" to json.encodeToJsonElement(defaultProfiles().filter { it.viewer != Viewer.HASNAA })))
            }
            2 -> {
                require("profiles" in saved) { "Missing family profiles" }
                require("usage" !in saved) { "Invalid legacy family usage" }
                JsonObject(saved + ("schema" to JsonPrimitive(5)))
            }
            3, 4, 5 -> {
                require("profiles" in saved) { "Missing family profiles" }
                JsonObject(saved + ("schema" to JsonPrimitive(5)))
            }
            else -> throw IllegalArgumentException("Unsupported saved family state")
        }
        val state = json.decodeFromJsonElement<FamilyState>(current)
        val withAdult = if (state.profile(Viewer.HASNAA) == null) state.copy(profiles = state.profiles +
            FamilyProfile(Viewer.HASNAA, Viewer.HASNAA.label, Portrait.HASNAA)) else state
        return withAdult.validated()
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
        // A trailing fixed adult is implicit. Legacy cores at the size limit keep their wire size.
        // Keep an explicit record elsewhere so decoding cannot change the profile order.
        val compact = if (state.profiles.lastOrNull()?.viewer == Viewer.HASNAA)
            state.copy(profiles = state.profiles.dropLast(1)) else state
        val encoded = json.encodeToJsonElement(compact).jsonObject
        val saved = if (state.voiceAnnouncementsEnabled) encoded - "voiceAnnouncementsEnabled" else encoded
        // An empty optional field must not increase the size of an existing near-limit core state.
        val withoutUsage = if (state.usage.isEmpty()) saved - "usage" else saved
        return JsonObject(if (state.appOrder.isEmpty()) withoutUsage - "appOrder" else withoutUsage).toString()
    }
}
