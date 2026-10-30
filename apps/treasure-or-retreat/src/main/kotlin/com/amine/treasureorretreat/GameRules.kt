package com.amine.treasureorretreat

import kotlin.random.Random

enum class Card(val value: Int, val label: String) {
    GOLD(40, "Gold coins"), RUBY(100, "Ruby"), EMERALD(150, "Emerald"), TRAP(0, "Temple trap")
}
enum class Phase { READY, REVEALING, RESULT, FINISHED }
enum class Outcome { NONE, BANKED, TRAPPED }

data class GameState(
    val expedition: Int = 1,
    val deck: List<Card> = emptyList(),
    val unbanked: Int = 0,
    val banked: Int = 0,
    val lastCard: Card? = null,
    val phase: Phase = Phase.READY,
    val outcome: Outcome = Outcome.NONE,
    val event: Int = 0
) {
    fun remaining(card: Card): Int = deck.count { it == card }
}

object GameRules {
    const val EXPEDITIONS = 3
    private val distribution = linkedMapOf(Card.GOLD to 8, Card.RUBY to 5, Card.EMERALD to 3, Card.TRAP to 4)
    fun deck(): List<Card> = distribution.flatMap { (card, count) -> List(count) { card } }
    fun newGame(random: Random = Random.Default): GameState = expedition(1, 0, random)
    private fun expedition(index: Int, banked: Int, random: Random): GameState {
        val cards = deck().toMutableList()
        val opening = cards.filter { it != Card.TRAP }.random(random)
        cards.remove(opening)
        return GameState(index, cards.shuffled(random), opening.value, banked, opening)
    }
    fun explore(state: GameState): GameState {
        if (state.phase != Phase.READY) return state
        if (state.deck.isEmpty()) return bank(state)
        val card = state.deck.first()
        return state.copy(
            deck = state.deck.drop(1), lastCard = card,
            unbanked = if (card == Card.TRAP) 0 else state.unbanked + card.value,
            phase = Phase.REVEALING,
            outcome = if (card == Card.TRAP) Outcome.TRAPPED else Outcome.NONE,
            event = state.event + 1
        )
    }
    fun bank(state: GameState): GameState {
        if (state.phase != Phase.READY) return state
        return state.copy(banked = state.banked + state.unbanked, unbanked = 0,
            phase = Phase.REVEALING, outcome = Outcome.BANKED, event = state.event + 1)
    }
    fun settle(state: GameState): GameState {
        if (state.phase != Phase.REVEALING) return state
        return state.copy(phase = if (state.outcome == Outcome.NONE) Phase.READY else Phase.RESULT)
    }
    fun advance(state: GameState, random: Random = Random.Default): GameState {
        if (state.phase != Phase.RESULT) return state
        return if (state.expedition == EXPEDITIONS) state.copy(phase = Phase.FINISHED)
            else expedition(state.expedition + 1, state.banked, random).copy(event = state.event + 1)
    }
}
