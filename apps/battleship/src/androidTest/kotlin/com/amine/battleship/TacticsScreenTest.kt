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
class TacticsScreenTest {
    @get:Rule val screen = createComposeRule()

    @Test fun tacticsFitAllLanguagesAndBothBackPathsReturnToHelp() {
        var state by mutableStateOf(SessionState(loaded = true, screen = Screen.HELP))
        screen.setContent { BattleshipApp(state, { action ->
            if (action == GameAction.Back) state = state.copy(screen = Screen.HOME)
        }, {}) }
        AppLanguage.entries.forEachIndexed { index, language ->
            screen.runOnIdle { state = state.copy(language = language) }
            screen.waitForIdle()
            // Help starts at Back. After a tactics visit, focus returns to Tactics.
            if (index == 0) key(Key.DirectionRight)
            screen.onNodeWithTag("help_tactics").assertIsFocused()
            key(Key.DirectionCenter)
            screen.onNodeWithTag("tactics_back").assertIsFocused()
            val bounds = screen.onNodeWithTag("tactics_screen").fetchSemanticsNode().boundsInRoot
            val cards = (0..3).map { kind ->
                val card = screen.onNodeWithTag("tactics_card_$kind").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                assertTrue(card.left >= bounds.left && card.right <= bounds.right)
                assertTrue(card.top >= bounds.top && card.bottom <= bounds.bottom)
                screen.onNodeWithTag("tactics_diagram_$kind").assertIsDisplayed()
                listOf("title", "body").forEach { part ->
                    val layouts = mutableListOf<TextLayoutResult>()
                    screen.onNodeWithTag("tactics_${part}_$kind")
                        .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                    assertEquals(1, layouts.size)
                    assertFalse("${language.tag} $kind $part overflow", layouts.single().hasVisualOverflow)
                }
                card
            }
            assertEquals(cards[0].width, cards[3].width, 1f)
            assertEquals(cards[0].height, cards[3].height, 1f)
            assertTrue(cards[0].right < cards[1].left)
            assertTrue(cards[0].bottom < cards[2].top)
            capture(language.tag)
            listOf(Key.DirectionLeft, Key.DirectionRight, Key.DirectionUp, Key.DirectionDown).forEach {
                key(it)
                screen.onNodeWithTag("tactics_back").assertIsFocused()
            }
            key(Key.Back)
            screen.onNodeWithTag("help_tactics").assertIsFocused()
            screen.runOnIdle { assertEquals(Screen.HELP, state.screen) }
            key(Key.DirectionCenter)
            screen.onNodeWithTag("tactics_back").assertIsFocused()
            key(Key.DirectionCenter)
            screen.onNodeWithTag("help_tactics").assertIsFocused()
            screen.onNodeWithTag("tactics_screen").assertDoesNotExist()
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
            .executeShellCommand("screencap -p /data/local/tmp/battleship-tactics-$tag.png")
        ParcelFileDescriptor.AutoCloseInputStream(command).use { it.readBytes() }
    }
}
