package com.amine.memoryparade

import kotlin.random.Random

enum class Animal(val label: String) { FOX("Fox"), PANDA("Panda"), RABBIT("Rabbit"), OWL("Owl"), DEER("Deer"), BEAR("Bear") }
enum class Direction { UP, DOWN, LEFT, RIGHT }
data class GameState(
    val cards: List<Animal>,
    val first: Int? = null,
    val second: Int? = null,
    val matched: Set<Int> = emptySet(),
    val parade: List<Animal> = emptyList(),
    val moves: Int = 0,
    val lastMatch: List<Int> = emptyList(),
) {
    val comparing get() = first != null && second != null
    val completed get() = matched.size == 12
    val pairs get() = parade.size
    val visible get() = matched + listOfNotNull(first, second)
    val isMatch get() = comparing && cards[first!!] == cards[second!!]
}
object GameRules {
    fun newGame(random: Random = Random.Default) = GameState((Animal.entries + Animal.entries).shuffled(random))
    fun flip(state: GameState, index: Int): GameState {
        if (index !in state.cards.indices || state.completed || state.comparing || index in state.visible) return state
        return if (state.first == null) state.copy(first = index) else state.copy(second = index, moves = state.moves + 1)
    }
    fun resolve(state: GameState): GameState {
        if (!state.comparing) return state
        return if (state.isMatch) state.copy(
            matched = state.matched + listOf(state.first!!, state.second!!),
            lastMatch = listOf(state.first!!, state.second!!),
            parade = state.parade + state.cards[state.first], first = null, second = null,
        ) else state.copy(first = null, second = null)
    }
    fun move(index: Int, direction: Direction): Int = when (direction) {
        Direction.UP -> if (index >= 4) index - 4 else index
        Direction.DOWN -> if (index < 8) index + 4 else index
        Direction.LEFT -> if (index % 4 > 0) index - 1 else index
        Direction.RIGHT -> if (index % 4 < 3) index + 1 else index
    }
    fun bestScore(previous: Int, result: Int): Int = when {
        result < 6 -> previous
        previous == 0 -> result
        else -> minOf(previous, result)
    }
}
