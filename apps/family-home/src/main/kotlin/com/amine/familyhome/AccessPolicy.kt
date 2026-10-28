package com.amine.familyhome

enum class AccessDecision { ALLOW, CHOOSER, CHILD_HOME, TIME_UP }

object AccessPolicy {
    const val FAMILY_PACKAGE = "com.amine.familyhome"

    fun decide(state: FamilyState, pkg: String?, window: String?, now: Long): AccessDecision {
        if (pkg == null || pkg == FAMILY_PACKAGE || transientSystemWindow(pkg, window.orEmpty())) return AccessDecision.ALLOW
        val viewer = state.selected ?: return AccessDecision.CHOOSER
        if (!viewer.isChild) return AccessDecision.ALLOW
        if (SessionEngine.remaining(state, viewer, now) <= 0) {
            return if (state.sessions.containsKey(viewer)) AccessDecision.TIME_UP else AccessDecision.CHILD_HOME
        }
        return if (pkg in state.allowed[viewer].orEmpty()) AccessDecision.ALLOW else AccessDecision.CHILD_HOME
    }

    private fun transientSystemWindow(pkg: String, window: String): Boolean = when (pkg) {
        "com.android.systemui" -> window.contains("volume", ignoreCase = true) || window.contains("GlobalActions")
        "com.google.android.permissioncontroller", "com.android.permissioncontroller" -> true
        "android" -> window == "android.app.AlertDialog" || window == "com.android.internal.app.ResolverActivity"
        "com.google.android.gms" -> window.contains(".auth.") || window.contains("SignIn")
        else -> false
    }
}

object HomeRouting {
    const val STOCK_PACKAGE = "com.google.android.apps.tv.launcherx"
    fun isStockHome(pkg: String?, window: String?, homeWindows: Set<String>): Boolean =
        pkg == STOCK_PACKAGE && window in homeWindows
    fun isShortPress(down: Long, up: Long, timeout: Long): Boolean = up >= down && up - down < timeout
}
