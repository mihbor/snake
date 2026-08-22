package com.example.snake.game.model

enum class Direction(
    private val columnOffset: Int,
    private val rowOffset: Int,
) {
    UP(columnOffset = 0, rowOffset = -1),
    DOWN(columnOffset = 0, rowOffset = 1),
    LEFT(columnOffset = -1, rowOffset = 0),
    RIGHT(columnOffset = 1, rowOffset = 0),
    ;

    fun opposite(): Direction = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
    }

    fun offset(): Cell = Cell(column = columnOffset, row = rowOffset)
}