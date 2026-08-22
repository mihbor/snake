package com.example.snake.game.controller

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

interface MovementClock {
    fun ticks(intervalMillis: Long): Flow<Unit>
}

class CoroutineMovementClock : MovementClock {
    override fun ticks(intervalMillis: Long): Flow<Unit> = flow {
        require(intervalMillis > 0) { "Movement interval must be positive" }

        while (currentCoroutineContext().isActive) {
            delay(intervalMillis)
            emit(Unit)
        }
    }
}