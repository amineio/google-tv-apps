package com.amine.treasureorretreat

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GameRulesTest {
    @Test fun deckContainsExactlyTheSpecifiedCards() {
        val deck = GameRules.deck()
        assertEquals(20, deck.size)
        assertEquals(mapOf(Card.GOLD to 8, Card.RUBY to 5, Card.EMERALD to 3, Card.TRAP to 4), deck.groupingBy { it }.eachCount())
    }
    @Test fun openingIsFairTreasureAndRemovedExactlyOnce() {
        repeat(200) { seed ->
            val game = GameRules.newGame(Random(seed))
            assertNotEquals(Card.TRAP, game.lastCard)
            assertEquals(19, game.deck.size)
            assertEquals(GameRules.deck().groupingBy { it }.eachCount(), (game.deck + game.lastCard!!).groupingBy { it }.eachCount())
            assertEquals(game.lastCard!!.value, game.unbanked)
            assertEquals(4, game.remaining(Card.TRAP))
        }
    }
    @Test fun seededGamesAreReproducibleAndDifferentSeedsVary() {
        assertEquals(GameRules.newGame(Random(42)), GameRules.newGame(Random(42)))
        assertNotEquals(GameRules.newGame(Random(42)).deck, GameRules.newGame(Random(43)).deck)
    }
    @Test fun drawConsumesOneCardAndBlocksAdditionalDrawUntilSettled() {
        val state = GameRules.newGame(Random(3)).copy(deck = listOf(Card.RUBY, Card.GOLD), unbanked = 40)
        val drawn = GameRules.explore(state)
        assertEquals(140, drawn.unbanked)
        assertEquals(listOf(Card.GOLD), drawn.deck)
        assertEquals(Phase.REVEALING, drawn.phase)
        assertEquals(drawn, GameRules.explore(drawn))
        assertEquals(drawn, GameRules.bank(drawn))
        assertEquals(Phase.READY, GameRules.settle(drawn).phase)
    }
    @Test fun trapLosesOnlyTheCurrentExpedition() {
        val state = GameRules.newGame(Random(0)).copy(deck = listOf(Card.TRAP), unbanked = 250, banked = 300)
        val drawn = GameRules.explore(state)
        assertEquals(0, drawn.unbanked)
        assertEquals(300, drawn.banked)
        assertEquals(Outcome.TRAPPED, drawn.outcome)
        assertEquals(Phase.RESULT, GameRules.settle(drawn).phase)
    }
    @Test fun bankingTransfersExactlyOnce() {
        val state = GameRules.newGame(Random(0)).copy(unbanked = 250, banked = 300)
        val banked = GameRules.bank(state)
        assertEquals(550, banked.banked)
        assertEquals(0, banked.unbanked)
        assertEquals(Outcome.BANKED, banked.outcome)
        assertEquals(banked, GameRules.bank(banked))
    }
    @Test fun threeAcknowledgedExpeditionsFinishWithoutAutomaticRestart() {
        val random = Random(17)
        var state = GameRules.newGame(random)
        repeat(3) { expedition ->
            assertEquals(expedition + 1, state.expedition)
            state = GameRules.settle(GameRules.bank(state))
            state = GameRules.advance(state, random)
        }
        assertEquals(Phase.FINISHED, state.phase)
        assertEquals(state, GameRules.advance(state, random))
        assertTrue(state.banked > 0)
    }
    @Test fun restartResetsScoreAndProgress() {
        val state = GameRules.newGame(Random(5))
        assertEquals(1, state.expedition)
        assertEquals(0, state.banked)
        assertEquals(Outcome.NONE, state.outcome)
    }
    @Test fun emptyDeckSafelyBanksAndNeverDrawsMissingCard() {
        val state = GameRules.newGame(Random(1)).copy(deck = emptyList(), unbanked = 200)
        assertEquals(200, GameRules.explore(state).banked)
        assertEquals(Outcome.BANKED, GameRules.explore(state).outcome)
    }
    @Test fun allTreasureValuesAreCorrect() {
        assertEquals(40, Card.GOLD.value)
        assertEquals(100, Card.RUBY.value)
        assertEquals(150, Card.EMERALD.value)
        assertEquals(0, Card.TRAP.value)
    }
}
