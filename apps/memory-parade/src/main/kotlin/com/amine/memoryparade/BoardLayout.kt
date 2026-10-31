package com.amine.memoryparade

internal data class BoardLayout(val size: BoardSize) {
    val cardWidth: Float = if (size == BoardSize.EASY) 174f else 134f
    val cardHeight: Float = when (size) { BoardSize.EASY -> 150f; BoardSize.CLASSIC -> 115f; BoardSize.CHALLENGE -> 88f }
    val gap: Float = if (size == BoardSize.EASY) 14f else 10f
    private val width = size.columns * cardWidth + (size.columns - 1) * gap
    private val height = size.rows * cardHeight + (size.rows - 1) * gap
    val x = 48f + (584f - width) / 2f
    val y = 91f + (382f - height) / 2f
    fun cardX(index: Int) = x + (index % size.columns) * (cardWidth + gap)
    fun cardY(index: Int) = y + (index / size.columns) * (cardHeight + gap)
}
