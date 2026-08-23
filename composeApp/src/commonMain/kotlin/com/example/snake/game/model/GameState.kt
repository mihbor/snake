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
    val mode: PlayMode = PlayMode.TWO_D,
) {
    init {
        require(score >= 0) { "Score must not be negative" }
        require(board.contains(food)) { "Food must be inside the board" }
        require(food !in snake.segments) { "Food must not occupy the snake" }
        require(
            when (mode) {
                PlayMode.TWO_D -> board.depth == 1
                PlayMode.THREE_D -> board.depth >= 2
            },
        ) {
            "2D sessions require one board depth layer and 3D sessions require at least two"
        }
        require((status == SessionStatus.GAME_OVER) == (collisionCause != null)) {
            "Game-over states must have a collision cause, and other states must not"
        }
    }
}