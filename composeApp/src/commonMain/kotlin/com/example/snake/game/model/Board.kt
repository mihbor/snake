package com.example.snake.game.model

data class Board(
    val columns: Int = 20,
    val rows: Int = 20,
    val depth: Int = 1,
) {
    init {
        require(columns > 0) { "Board columns must be positive" }
        require(rows > 0) { "Board rows must be positive" }
        require(depth > 0) { "Board depth must be positive" }
    }

    fun contains(cell: Cell): Boolean =
        cell.column >= 0 && cell.column < columns &&
            cell.row >= 0 && cell.row < rows &&
            cell.depth >= 0 && cell.depth < depth
}