package com.example.snake.game.model

data class Snake(
    val segments: List<Cell>,
) {
    init {
        require(segments.isNotEmpty()) { "Snake must contain at least one segment" }
    }

    fun head(): Cell = segments.first()

    fun moveTo(nextHead: Cell): Snake =
        Snake(listOf(nextHead) + segments.dropLast(1))
}