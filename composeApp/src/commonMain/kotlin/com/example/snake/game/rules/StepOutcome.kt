package com.example.snake.game.rules

enum class StepOutcome {
    MOVED,
    NOT_ACTIVE,
    BOUNDARY_BLOCKED,
    FOOD_COLLECTED,
    FOOD_COLLECTION_BLOCKED,
}