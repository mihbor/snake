package com.example.snake.game.model

enum class Direction(
    private val columnOffset: Int,
    private val rowOffset: Int,
    private val depthOffset: Int,
) {
    UP(columnOffset = 0, rowOffset = -1, depthOffset = 0),
    DOWN(columnOffset = 0, rowOffset = 1, depthOffset = 0),
    LEFT(columnOffset = -1, rowOffset = 0, depthOffset = 0),
    RIGHT(columnOffset = 1, rowOffset = 0, depthOffset = 0),
    FORWARD(columnOffset = 0, rowOffset = 0, depthOffset = -1),
    BACKWARD(columnOffset = 0, rowOffset = 0, depthOffset = 1),
    ;

    fun opposite(): Direction = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
        FORWARD -> BACKWARD
        BACKWARD -> FORWARD
    }

    fun offset(): Cell = Cell(column = columnOffset, row = rowOffset, depth = depthOffset)
}