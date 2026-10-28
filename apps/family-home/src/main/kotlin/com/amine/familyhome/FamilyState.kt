package com.amine.familyhome

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class Viewer(val id: String) {
    companion object {
        val AMINE = Viewer("AMINE")
        val ELIAS = Viewer("ELIAS")
        val SOFIA = Viewer("SOFIA")
    }
    val isChild: Boolean get() = this != AMINE
    val label: String get() = when (this) {
        AMINE -> "Amine"
        ELIAS -> "Elias"
        SOFIA -> "Sofia"
        else -> "Child"
    }
    val defaultPortrait: Portrait get() = when (this) {
        AMINE -> Portrait.AMINE
        ELIAS -> Portrait.ELIAS
        SOFIA -> Portrait.SOFIA
        else -> Portrait.CHILD
    }
}

@Serializable
enum class Portrait { AMINE, ELIAS, SOFIA, CHILD }

@Serializable
data class FamilyProfile(val viewer: Viewer, val name: String, val portrait: Portrait = Portrait.CHILD)

internal fun defaultProfiles() = listOf(Viewer.AMINE, Viewer.ELIAS, Viewer.SOFIA).map {
    FamilyProfile(it, it.label, it.defaultPortrait)
}

internal fun validProfileName(name: String): Boolean =
    name == name.trim() && name.length in 1..24 && name.none { it.isISOControl() }

@Serializable
data class ChildSession(
    val totalMs: Long,
    val remainingMs: Long,
    val deadlineElapsedMs: Long? = null,
    val warnedFive: Boolean = false,
    val warnedOne: Boolean = false,
)

object DefaultApps {
    val labels = linkedMapOf(
        "com.amine.chessmath" to "Chess & Math",
        "com.yanka.mc.tv.prod.android" to "MasterClass",
        "be.vmma.vtm.zenderapp" to "VTM GO",
        "be.vrt.vrtnu" to "VRT MAX",
        "com.spotify.tv.android" to "Spotify",
        "com.amazon.amazonvideo.livingroom" to "Prime Video",
    )
    fun allowed() = mapOf(Viewer.ELIAS to labels.keys.toSet(), Viewer.SOFIA to labels.keys.toSet())
}

@Serializable
data class FamilyState(
    val schema: Int = 2,
    val selected: Viewer? = null,
    val sessions: Map<Viewer, ChildSession> = emptyMap(),
    val allowed: Map<Viewer, Set<String>> = DefaultApps.allowed(),
    val pinSalt: String = "",
    val pinHash: String = "",
    val revision: Long = 0,
    val recovered: Boolean = false,
    val profiles: List<FamilyProfile> = defaultProfiles(),
) {
    val pinEnabled: Boolean get() = pinHash.isNotEmpty()
    val children: List<FamilyProfile> get() = profiles.filter { it.viewer.isChild }
    fun profile(viewer: Viewer): FamilyProfile? = profiles.firstOrNull { it.viewer == viewer }
    fun label(viewer: Viewer): String = requireNotNull(profile(viewer)) { "Unknown family profile" }.name

    fun validated(): FamilyState {
        require(schema == 2 && revision >= 0) { "Unsupported saved family state" }
        require(pinSalt.isEmpty() == pinHash.isEmpty()) { "Incomplete parent PIN" }
        val ids = profiles.map { it.viewer }.toSet()
        require(ids.size == profiles.size && Viewer.AMINE in ids) { "Invalid family profiles" }
        require(profile(Viewer.AMINE) == defaultProfiles().first()) { "Invalid parent profile" }
        profiles.forEach { profile ->
            val viewer = profile.viewer
            require(viewer in listOf(Viewer.AMINE, Viewer.ELIAS, Viewer.SOFIA) ||
                viewer.id.matches(Regex("child_[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))) {
                "Invalid family profile ID"
            }
            require(validProfileName(profile.name)) { "Invalid family profile name" }
            require(profile.portrait == viewer.defaultPortrait) { "Invalid family profile portrait" }
        }
        require(profiles.indices.all { index -> profiles.take(index).none {
            it.name.equals(profiles[index].name, ignoreCase = true)
        } }) { "Duplicate family profile name" }
        require(selected == null || selected in ids) { "Unknown selected family profile" }
        require(allowed.keys.all { it.isChild && it in ids }) { "Invalid family app lists" }
        sessions.forEach { (viewer, session) ->
            require(viewer in ids && viewer.isChild && session.totalMs in 300_000..3_600_000 && session.totalMs % 300_000 == 0L)
            require(session.remainingMs in 0..session.totalMs)
            require(session.deadlineElapsedMs == null || (selected == viewer && session.deadlineElapsedMs >= 0))
        }
        return this
    }
}
