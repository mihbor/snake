package com.example.snake.game.model

data class Board(
    val columns: Int = 20,
    val rows: Int = 20,
) {
    init {
        require(columns > 0) { "Board columns must be positive" }
        require(rows > 0) { "Board rows must be positive" }
    }

    fun contains(cell: Cell): Boolean =
        cell.column >= 0 && cell.column < columns && cell.row >= 0 && cell.row < rows
}