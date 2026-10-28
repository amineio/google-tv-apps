package com.amine.battleship.game.ai

import com.amine.battleship.game.model.Coordinate
import com.amine.battleship.game.model.Coordinate.Companion.BOARD_SIZE
import com.amine.battleship.game.model.ShotOutcome
import kotlin.random.Random

/** Normal difficulty: parity search, then candidate ship lines around unresolved hits. */
class HuntTargetStrategy(private val seed: Long) : ComputerOpponent {
    override fun chooseShot(knowledge: AttackKnowledge): Coordinate {
        val attacked = BooleanArray(CELL_COUNT)
        val blocked = BooleanArray(CELL_COUNT)
        val hits = BooleanArray(CELL_COUNT)
        var historySeed = seed
        for (shot in knowledge.shots) {
            require(shot.coordinate.isValid) { "Shot history contains an invalid coordinate" }
            val index = shot.coordinate.index
            attacked[index] = true
            blocked[index] = shot.outcome == ShotOutcome.MISS
            hits[index] = shot.outcome != ShotOutcome.MISS
            historySeed = historySeed * 31 + index * 3 + shot.outcome.ordinal
        }
        for (ship in knowledge.sunkShips) {
            for (cell in ship.cells) {
                require(cell.isValid) { "Sunk ship contains an invalid coordinate" }
                blocked[cell.index] = true
                hits[cell.index] = false
            }
        }
        val available = (0 until CELL_COUNT).filter { !attacked[it] }
        check(available.isNotEmpty()) { "No unattacked cells remain" }
        val random = Random(historySeed)
        if (hits.any { it }) {
            val scores = IntArray(CELL_COUNT)
            val alignedHits = IntArray(CELL_COUNT)
            for (length in knowledge.remainingShipLengths) {
                for (line in placements[length].orEmpty()) {
                    if (line.any { blocked[it] }) continue
                    val hitCount = line.count { hits[it] }
                    if (hitCount == 0) continue
                    for (position in line.indices) {
                        val index = line[position]
                        if (attacked[index]) continue
                        val touchesHit = (position > 0 && hits[line[position - 1]]) ||
                            (position < line.lastIndex && hits[line[position + 1]])
                        if (touchesHit) {
                            alignedHits[index] = maxOf(alignedHits[index], hitCount)
                            scores[index] += hitCount * hitCount
                        }
                    }
                }
            }
            val bestAlignment = available.maxOf { alignedHits[it] }
            if (bestAlignment > 0) {
                val aligned = available.filter { alignedHits[it] == bestAlignment }
                val bestScore = aligned.maxOf { scores[it] }
                val candidates = aligned.filter { scores[it] == bestScore }
                return candidates[random.nextInt(candidates.size)].coordinate
            }
        }
        // Every remaining ship has at least two cells, so one parity covers every ship.
        val parity = (seed and 1L).toInt()
        val hunt = available.filter { ((it / BOARD_SIZE + it % BOARD_SIZE) and 1) == parity }
            .ifEmpty { available }
        return hunt[random.nextInt(hunt.size)].coordinate
    }

    private val Coordinate.index: Int get() = row * BOARD_SIZE + column
    private val Int.coordinate: Coordinate get() = Coordinate(this / BOARD_SIZE, this % BOARD_SIZE)

    companion object {
        private const val CELL_COUNT = BOARD_SIZE * BOARD_SIZE
        private val placements: Map<Int, List<IntArray>> = (2..5).associateWith { length ->
            buildList {
                for (row in 0 until BOARD_SIZE) {
                    for (column in 0 until BOARD_SIZE) {
                        if (column + length <= BOARD_SIZE) add(IntArray(length) { row * BOARD_SIZE + column + it })
                        if (row + length <= BOARD_SIZE) add(IntArray(length) { (row + it) * BOARD_SIZE + column })
                    }
                }
            }
        }
    }
}
