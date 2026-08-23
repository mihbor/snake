package com.example.snake.game.model

data class GameState(
    val status: SessionStatus,
    val board: Board,
    val snake: Snake,
    val currentDirection: Direction,
    val pendingDirection: Direction?,
    val score: Int,
    val food: Cell,
    val collisionCause: CollisionCause? = null,
) {
    init {
        require(score >= 0) { "Score must not be negative" }
        require(board.contains(food)) { "Food must be inside the board" }
        require(food !in snake.segments) { "Food must not occupy the snake" }
        require((status == SessionStatus.GAME_OVER) == (collisionCause != null)) {
            "Game-over states must have a collision cause, and other states must not"
        }
    }
}