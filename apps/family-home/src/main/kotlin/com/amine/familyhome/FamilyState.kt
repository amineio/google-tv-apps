package com.amine.familyhome

import kotlinx.serialization.Serializable

@Serializable
enum class Viewer(val label: String) {
    AMINE("Amine"), ELIAS("Elias"), SOFIA("Sofia");
    val isChild: Boolean get() = this != AMINE
}

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
    val schema: Int = 1,
    val selected: Viewer? = null,
    val sessions: Map<Viewer, ChildSession> = emptyMap(),
    val allowed: Map<Viewer, Set<String>> = DefaultApps.allowed(),
    val pinSalt: String = "",
    val pinHash: String = "",
    val revision: Long = 0,
    val recovered: Boolean = false,
) {
    val pinEnabled: Boolean get() = pinHash.isNotEmpty()

    fun validated(): FamilyState {
        require(schema == 1 && revision >= 0) { "Unsupported saved family state" }
        require(pinSalt.isEmpty() == pinHash.isEmpty()) { "Incomplete parent PIN" }
        require(sessions.size <= 2 && allowed.keys.all { it.isChild }) { "Invalid family profiles" }
        sessions.forEach { (viewer, session) ->
            require(viewer.isChild && session.totalMs in 300_000..3_600_000 && session.totalMs % 300_000 == 0L)
            require(session.remainingMs in 0..session.totalMs)
            require(session.deadlineElapsedMs == null || (selected == viewer && session.deadlineElapsedMs >= 0))
        }
        return this
    }
}
