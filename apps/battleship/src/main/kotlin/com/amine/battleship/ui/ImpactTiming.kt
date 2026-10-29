package com.amine.battleship.ui

import com.amine.battleship.game.model.ShotOutcome

/** The session and renderer use one deadline, including a final sinking shot. */
internal fun impactDurationMillis(outcome: ShotOutcome): Long = when (outcome) {
    ShotOutcome.MISS -> 1100L
    ShotOutcome.HIT -> 1500L
    ShotOutcome.SUNK -> 2500L
}

internal fun impactProgress(startedAtNanos: Long, nowNanos: Long, outcome: ShotOutcome): Float =
    ((nowNanos - startedAtNanos).coerceAtLeast(0L).toDouble() / (impactDurationMillis(outcome) * 1_000_000L))
        .coerceIn(0.0, 1.0).toFloat()
