package com.example.snake.game.rules

import com.example.snake.game.model.GameState

data class StepTransition(
    val state: GameState,
    val outcome: StepOutcome,
)