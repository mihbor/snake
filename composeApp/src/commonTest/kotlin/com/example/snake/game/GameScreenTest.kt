package com.example.snake.game

import com.example.snake.game.model.CollisionCause
import com.example.snake.game.model.GameState
import com.example.snake.game.model.SessionStatus
import com.example.snake.game.rules.GameRules
import com.example.snake.game.ui.bestScoreLabel
import com.example.snake.game.ui.currentScoreLabel
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class GameScreenTest {
    private val activeState = GameRules.startNewGame(random = Random(0)).copy(score = 20)

    @Test
    fun currentScoreLabelIdentifiesNonTerminalAttempts() {
        assertEquals("Current score: 20", currentScoreLabel(activeState))
        assertEquals(
            "Current score: 20",
            currentScoreLabel(activeState.copy(status = SessionStatus.READY)),
        )
        assertEquals(
            "Current score: 20",
            currentScoreLabel(activeState.copy(status = SessionStatus.PAUSED)),
        )
    }

    @Test
    fun finalScoreAndBestScoreLabelsRemainDistinct() {
        val gameOverState: GameState = activeState.copy(
            status = SessionStatus.GAME_OVER,
            collisionCause = CollisionCause.BOUNDARY,
        )

        assertEquals("Final score: 20", currentScoreLabel(gameOverState))
        assertEquals("Best score: 50", bestScoreLabel(50))
    }
}