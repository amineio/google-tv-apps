package com.amine.familyhome

import org.junit.Assert.*
import org.junit.Test

class AccessPolicyTest {
    private val active = SessionEngine.start(FamilyState(), Viewer.ELIAS, 20, 0)

    @Test fun remoteShortcutToYoutubeReturnsToChildHome() {
        assertEquals(AccessDecision.CHILD_HOME,
            AccessPolicy.decide(active, "com.google.android.youtube.tv", "WatchActivity", 100))
        assertEquals(AccessDecision.ALLOW,
            AccessPolicy.decide(active, "be.vrt.vrtnu", "PlayerActivity", 100))
    }

    @Test fun noProfileAndExpiredBalanceNeverPermitAllowedApps() {
        assertEquals(AccessDecision.CHOOSER,
            AccessPolicy.decide(FamilyState(), "be.vrt.vrtnu", "PlayerActivity", 0))
        assertEquals(AccessDecision.TIME_UP,
            AccessPolicy.decide(active, "be.vrt.vrtnu", "PlayerActivity", 1_200_000))
    }

    @Test fun newAppsCastAndSettingsStayExcluded() {
        for (pkg in listOf("new.installed.app", "be.rtbf.auvio", "com.google.android.apps.mediashell", "com.android.tv.settings")) {
            assertEquals(AccessDecision.CHILD_HOME, AccessPolicy.decide(active, pkg, "Activity", 100))
        }
    }

    @Test fun systemVolumeAndPermissionDialogsDoNotLoop() {
        assertEquals(AccessDecision.ALLOW,
            AccessPolicy.decide(active, "com.android.systemui", "com.android.systemui.volume.VolumeDialogImpl", 100))
        assertEquals(AccessDecision.ALLOW,
            AccessPolicy.decide(active, "com.google.android.permissioncontroller", "GrantPermissionsActivity", 100))
        assertEquals(AccessDecision.CHILD_HOME,
            AccessPolicy.decide(active, "com.android.systemui", "SomeOtherActivity", 100))
    }

    @Test fun familyScreensAndAdminRemainAccessible() {
        assertEquals(AccessDecision.ALLOW, AccessPolicy.decide(FamilyState(), "com.amine.familyhome", "MainActivity", 0))
        val adult = SessionEngine.select(active, Viewer.AMINE, 100)
        assertEquals(AccessDecision.ALLOW, AccessPolicy.decide(adult, "com.google.android.youtube.tv", "WatchActivity", 100))
    }

    @Test fun parentChangesAChildsListWithoutChangingOtherChild() {
        val state = active.copy(allowed = active.allowed + (Viewer.ELIAS to setOf("be.vrt.vrtnu")))
        assertEquals(AccessDecision.CHILD_HOME, AccessPolicy.decide(state, "com.spotify.tv.android", "Main", 100))
        val sofia = SessionEngine.start(state, Viewer.SOFIA, 20, 100)
        assertEquals(AccessDecision.ALLOW, AccessPolicy.decide(sofia, "com.spotify.tv.android", "Main", 100))
    }

    @Test fun stockRedirectMatchesHomeWindowsOnly() {
        val windows = setOf("com.google.android.apps.tv.launcherx.home.HomeActivity")
        assertTrue(HomeRouting.isStockHome("com.google.android.apps.tv.launcherx", windows.first(), windows))
        assertFalse(HomeRouting.isStockHome("com.google.android.apps.mediashell", "PlayerActivity", windows))
        assertFalse(HomeRouting.isStockHome("com.google.android.apps.tv.launcherx", "SettingsActivity", windows))
        assertTrue(HomeRouting.isShortPress(100, 200, 500))
        assertFalse(HomeRouting.isShortPress(100, 800, 500))
    }
}
