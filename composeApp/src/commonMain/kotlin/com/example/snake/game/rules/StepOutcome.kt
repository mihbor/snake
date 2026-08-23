package com.example.snake.game.rules

enum class StepOutcome {
    MOVED,
    NOT_ACTIVE,
    BOUNDARY_COLLISION,
    SELF_COLLISION,
    FOOD_COLLECTED,
    FOOD_COLLECTION_BLOCKED,
    UNSUPPORTED_MODE,
}