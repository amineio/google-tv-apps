package com.amine.memoryparade

import kotlin.random.Random

enum class Animal(val label: String) {
    FOX("Fox"), PANDA("Panda"), RABBIT("Rabbit"), OWL("Owl"), DEER("Deer"), BEAR("Bear"),
    SQUIRREL("Squirrel"), RACCOON("Raccoon")
}
enum class BoardSize(val key: String, val rows: Int, val columns: Int) {
    EASY("easy", 2, 3), CLASSIC("classic", 3, 4), CHALLENGE("challenge", 4, 4);
    val cards get() = rows * columns
    val pairs get() = cards / 2
}
enum class Direction { UP, DOWN, LEFT, RIGHT }
data class GameState(
    val cards: List<Animal>,
    val first: Int? = null,
    val second: Int? = null,
    val matched: Set<Int> = emptySet(),
    val parade: List<Animal> = emptyList(),
    val moves: Int = 0,
    val lastMatch: List<Int> = emptyList(),
    val size: BoardSize = BoardSize.CLASSIC,
) {
    val comparing get() = first != null && second != null
    val completed get() = matched.size == cards.size
    val pairs get() = parade.size
    val visible get() = matched + listOfNotNull(first, second)
    val isMatch get() = comparing && cards[first!!] == cards[second!!]
}
object GameRules {
    fun newGame(random: Random = Random.Default, size: BoardSize = BoardSize.CLASSIC): GameState {
        val animals = Animal.entries.shuffled(random).take(size.pairs)
        return GameState((animals + animals).shuffled(random), size = size)
    }
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
    fun move(index: Int, direction: Direction, size: BoardSize = BoardSize.CLASSIC): Int = when (direction) {
        Direction.UP -> if (index >= size.columns) index - size.columns else index
        Direction.DOWN -> if (index < size.cards - size.columns) index + size.columns else index
        Direction.LEFT -> if (index % size.columns > 0) index - 1 else index
        Direction.RIGHT -> if (index % size.columns < size.columns - 1) index + 1 else index
    }
    fun bestScore(previous: Int, result: Int, size: BoardSize = BoardSize.CLASSIC): Int = when {
        result < size.pairs -> previous
        previous == 0 -> result
        else -> minOf(previous, result)
    }
}
