package com.example.snake.game.model

data class Cell(
    val column: Int,
    val row: Int,
    val depth: Int = 0,
)