package com.example.snake.game.rules

enum class DirectionRequestResult {
    ACCEPTED,
    IGNORED_INACTIVE,
    IGNORED_REVERSAL,
    IGNORED_PENDING_TURN,
    IGNORED_UNSUPPORTED_MODE,
}