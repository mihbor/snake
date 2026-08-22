package com.example.snake.game.model

data class GameState(
    val status: SessionStatus,
    val board: Board,
    val snake: Snake,
    val currentDirection: Direction,
    val pendingDirection: Direction?,
    val score: Int,
) {
    init {
        require(score >= 0) { "Score must not be negative" }
    }
}