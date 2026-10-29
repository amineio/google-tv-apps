package com.amine.battleship

import android.os.ParcelFileDescriptor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amine.battleship.localization.AppLanguage
import com.amine.battleship.ui.BattleshipApp
import com.amine.battleship.viewmodel.GameAction
import com.amine.battleship.viewmodel.Screen
import com.amine.battleship.viewmodel.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HelpScreenTest {
    @get:Rule val screen = createComposeRule()

    @Test fun illustratedHelpFitsAllLanguagesAndRemoteBackReturnsToTheMenu() {
        var state by mutableStateOf(SessionState(loaded = true))
        screen.setContent { BattleshipApp(state, { action ->
            state = when (action) {
                GameAction.Help -> state.copy(screen = Screen.HELP)
                GameAction.Back -> state.copy(screen = Screen.HOME)
                else -> state
            }
        }, {}) }
        AppLanguage.entries.forEach { language ->
            screen.runOnIdle { state = state.copy(language = language, screen = Screen.HOME) }
            screen.waitForIdle()
            capture("home-${language.tag}")
            key(Key.DirectionDown)
            key(Key.DirectionCenter)
            screen.onNodeWithTag("help_back").assertIsFocused()
            val help = screen.onNodeWithTag("help_screen").fetchSemanticsNode().boundsInRoot
            val cards = listOf("aim", "fire", "sink").map { kind ->
                val card = screen.onNodeWithTag("help_card_$kind").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                assertTrue(card.left >= help.left && card.right <= help.right)
                assertTrue(card.top >= help.top && card.bottom <= help.bottom)
                listOf("title", "body").forEach { part ->
                    val layouts = mutableListOf<TextLayoutResult>()
                    screen.onNodeWithTag("help_${part}_$kind")
                        .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                    assertEquals(1, layouts.size)
                    assertFalse("${language.tag} $kind $part overflow", layouts.single().hasVisualOverflow)
                }
                card
            }
            assertEquals(cards[0].width, cards[1].width, 1f)
            assertEquals(cards[0].width, cards[2].width, 1f)
            assertEquals(cards[0].top, cards[2].top, 1f)
            assertTrue(screen.onNodeWithTag("help_back").fetchSemanticsNode().boundsInRoot.top > cards.last().bottom)
            capture("help-${language.tag}")
            key(Key.DirectionRight)
            screen.onNodeWithTag("help_tactics").assertIsFocused()
            key(Key.DirectionLeft)
            listOf(Key.DirectionLeft, Key.DirectionUp, Key.DirectionDown).forEach {
                key(it)
                screen.onNodeWithTag("help_back").assertIsFocused()
            }
            key(Key.Back)
            screen.onNodeWithTag("help_screen").assertDoesNotExist()
            screen.runOnIdle { assertEquals(Screen.HOME, state.screen) }
            screen.runOnIdle { state = state.copy(screen = Screen.SETTINGS) }
            screen.waitForIdle()
            capture("settings-${language.tag}")
            key(Key.Back)
        }
    }

    private fun key(key: Key) {
        if (key == Key.Back) {
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            screen.waitForIdle()
        } else screen.onAllNodes(isRoot()).onLast().performKeyInput { pressKey(key) }
    }

    private fun capture(tag: String) {
        if (InstrumentationRegistry.getArguments().getString("captureHelp") != "true") return
        screen.waitForIdle()
        val command = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /data/local/tmp/battleship-help-$tag.png")
        ParcelFileDescriptor.AutoCloseInputStream(command).use { it.readBytes() }
    }
}
