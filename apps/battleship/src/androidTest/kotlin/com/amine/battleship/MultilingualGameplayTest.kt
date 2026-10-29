package com.amine.battleship

import android.os.ParcelFileDescriptor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import com.amine.battleship.game.model.Coordinate
import com.amine.battleship.game.model.Player
import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.localization.AppLanguage
import com.amine.battleship.ui.BattleshipApp
import com.amine.battleship.viewmodel.Confirmation
import com.amine.battleship.viewmodel.Screen
import com.amine.battleship.viewmodel.SessionState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MultilingualGameplayTest {
    @get:Rule val screen = createComposeRule()
    @Test fun gameplayAndConfirmationFitEveryLanguageWithoutChangingGridSize() {
        var state by mutableStateOf(SessionState(loaded = true, screen = Screen.GAME, game = GameEngine.newGame(42)))
        screen.setContent { BattleshipApp(state, {}, {}) }
        val original = screen.onNodeWithTag("targeting_board").getUnclippedBoundsInRoot()
        AppLanguage.entries.forEach { language ->
            screen.runOnIdle { state = state.copy(language = language, game = GameEngine.newGame(42), paused = false, confirmation = null) }
            screen.onNodeWithTag("targeting_board").assertIsFocused()
            screen.onNodeWithTag("opponent_portrait").assertIsDisplayed()
            screen.onNodeWithTag("human_turn_indicator").assertIsDisplayed()
            screen.onNodeWithTag("opponent_turn_indicator").assertDoesNotExist()
            assertEquals(original, screen.onNodeWithTag("targeting_board").getUnclippedBoundsInRoot())
            capture("game-${language.tag}")
            assertTextFits(language.tag)
            screen.runOnIdle { state = state.copy(game = GameEngine.attack(GameEngine.newGame(42), Player.HUMAN, Coordinate(0, 0)).state) }
            screen.onNodeWithTag("opponent_turn_indicator").assertIsDisplayed()
            screen.onNodeWithTag("human_turn_indicator").assertDoesNotExist()
            assertTextFits("computer-${language.tag}")
            capture("computer-${language.tag}")
            screen.runOnIdle { state = state.copy(paused = true, confirmation = Confirmation.RESTART) }
            assertTextFits("confirmation-${language.tag}")
            capture("confirmation-${language.tag}")
        }
    }
    private fun assertTextFits(language: String) {
        val text = SemanticsMatcher("text layout") { it.config.contains(SemanticsActions.GetTextLayoutResult) }
        screen.onAllNodes(text).fetchSemanticsNodes().forEach { node ->
            val layouts = mutableListOf<TextLayoutResult>()
            screen.onNode(SemanticsMatcher("node ${node.id}") { it.id == node.id })
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            layouts.forEach { assertFalse("$language text overflow: ${node.config.getOrElse(SemanticsProperties.Text) { emptyList() }} lines=${it.lineCount} width=${it.didOverflowWidth} height=${it.didOverflowHeight}", it.hasVisualOverflow) }
        }
    }
    private fun capture(tag: String) {
        if (InstrumentationRegistry.getArguments().getString("captureHelp") != "true") return
        screen.waitForIdle()
        val command = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "screencap -p /data/local/tmp/battleship-help-$tag.png")
        ParcelFileDescriptor.AutoCloseInputStream(command).use { it.readBytes() }
    }
}
