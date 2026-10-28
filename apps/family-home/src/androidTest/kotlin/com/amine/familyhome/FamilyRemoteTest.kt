package com.amine.familyhome

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FamilyRemoteTest {
    @get:Rule val screen = createAndroidComposeRule<MainActivity>()

    @Test fun initialChooserShowsThreeProfiles() {
        screen.waitUntil(10_000) { screen.onAllNodesWithText("Amine").fetchSemanticsNodes().isNotEmpty() }
        screen.onNodeWithText("Amine").assertIsDisplayed()
        screen.onNodeWithText("Elias").assertIsDisplayed()
        screen.onNodeWithText("Sofia").assertIsDisplayed()
    }

    @Test fun parentCanOpenFamilyConfigurationWithPinOff() {
        screen.waitUntil(10_000) { screen.onAllNodesWithText("Amine").fetchSemanticsNodes().isNotEmpty() }
        screen.onNodeWithText("Amine").performClick()
        screen.onNodeWithText("Family configuration").performClick()
        screen.onNodeWithText("Parent PIN: off").assertIsDisplayed()
    }
}
