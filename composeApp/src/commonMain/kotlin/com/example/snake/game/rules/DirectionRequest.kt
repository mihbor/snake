package com.example.snake.game.rules

import com.example.snake.game.model.GameState

data class DirectionRequest(
    val state: GameState,
    val result: DirectionRequestResult,
)