package com.example.snake.game

import com.example.snake.game.model.CollisionCause
import com.example.snake.game.model.GameState
import com.example.snake.game.model.PlayMode
import com.example.snake.game.model.SessionStatus
import com.example.snake.game.rules.GameRules
import com.example.snake.game.ui.bestScoreLabel
import com.example.snake.game.ui.currentScoreLabel
import com.example.snake.game.ui.modeLabel
import com.example.snake.game.ui.modeSelectionAvailable
import com.example.snake.game.ui.selectedModeLabel
import com.example.snake.game.ui.threeDControlLabel
import com.example.snake.game.ui.ThreeDControl
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun modeLabelsRemainExplicitAndStableForPlayersAndAssistiveTechnology() {
        assertEquals("Selected mode: 2D", selectedModeLabel(PlayMode.TWO_D))
        assertEquals("Selected mode: 3D", selectedModeLabel(PlayMode.THREE_D))
        assertEquals("Mode: 2D", modeLabel(PlayMode.TWO_D))
        assertEquals("Mode: 3D", modeLabel(PlayMode.THREE_D))
    }

    @Test
    fun threeDimensionalControlLabelsExposeAllSixMovementAxes() {
        assertEquals(
            listOf("Up", "Down", "Left", "Right", "Forward", "Backward"),
            ThreeDControl.entries.map(::threeDControlLabel),
        )
    }

    @Test
    fun modeSelectionIsAvailableBeforeStartAndAfterCompletionOnly() {
        assertTrue(modeSelectionAvailable(SessionStatus.READY))
        assertTrue(modeSelectionAvailable(SessionStatus.GAME_OVER))
        assertFalse(modeSelectionAvailable(SessionStatus.ACTIVE))
        assertFalse(modeSelectionAvailable(SessionStatus.PAUSED))
    }
}