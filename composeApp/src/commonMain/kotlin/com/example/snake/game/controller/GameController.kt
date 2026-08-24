package com.example.snake.game.controller

import com.example.snake.game.model.GameState
import com.example.snake.game.model.Direction
import com.example.snake.game.model.PlayMode
import com.example.snake.game.model.SessionStatus
import com.example.snake.game.persistence.BestScoreStore
import com.example.snake.game.persistence.normalizeBestScore
import com.example.snake.game.rules.DirectionRequestResult
import com.example.snake.game.rules.GameRules
import com.example.snake.game.rules.StepOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameController(
    private val bestScoreStore: BestScoreStore,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val movementClock: MovementClock = CoroutineMovementClock(),
    private val movementIntervalMillis: Long = DEFAULT_MOVEMENT_INTERVAL_MILLIS,
    private val random: Random = Random.Default,
) : AutoCloseable {
    companion object {
        const val DEFAULT_MOVEMENT_INTERVAL_MILLIS = 150L
    }

    private var bestScoreValue = readBestScore()
    private val _selectedMode = MutableStateFlow(PlayMode.TWO_D)
    private val _state = MutableStateFlow(readyState(_selectedMode.value))
    private var movementJob: Job? = null
    private var sessionGeneration = 0L
    private var closed = false

    val state: StateFlow<GameState> = _state.asStateFlow()
    val selectedMode: StateFlow<PlayMode> = _selectedMode.asStateFlow()
    val bestScore: Int
        get() = bestScoreValue

    fun selectMode(mode: PlayMode) {
        val currentState = _state.value
        if (closed ||
            (currentState.status != SessionStatus.READY && currentState.status != SessionStatus.GAME_OVER) ||
            _selectedMode.value == mode
        ) return

        _selectedMode.value = mode
        if (currentState.status == SessionStatus.READY) {
            _state.value = readyState(mode)
        }
    }

    fun startNewGame() {
        if (closed) return

        val mode = _selectedMode.value
        sessionGeneration += 1
        movementJob?.cancel()
        movementJob = null
        _state.value = GameRules.startNewGame(random = random, mode = mode)
        startClock()
    }

    fun requestDirection(direction: Direction): DirectionRequestResult {
        if (closed) return DirectionRequestResult.IGNORED_INACTIVE

        var result = DirectionRequestResult.IGNORED_INACTIVE
        _state.update { currentState ->
            if (closed) {
                result = DirectionRequestResult.IGNORED_INACTIVE
                currentState
            } else {
                val request = GameRules.requestDirection(currentState, direction)
                result = request.result
                request.state
            }
        }
        return result
    }

    fun pause() {
        if (closed || _state.value.status != SessionStatus.ACTIVE) return

        _state.update { currentState -> GameRules.pause(currentState) }
        sessionGeneration += 1
        stopMovementClock(sessionGeneration)
    }

    fun resume() {
        if (closed || _state.value.status != SessionStatus.PAUSED) return

        _state.update { currentState -> GameRules.resume(currentState) }
        sessionGeneration += 1
        startClock()
    }

    fun advanceForTest(): StepOutcome = advanceForGeneration(sessionGeneration)

    private fun advanceForGeneration(generation: Long): StepOutcome {
        if (closed || generation != sessionGeneration) return StepOutcome.NOT_ACTIVE

        var outcome = StepOutcome.NOT_ACTIVE
        var shouldStopClock = false
        _state.update { currentState ->
            if (closed || generation != sessionGeneration) {
                outcome = StepOutcome.NOT_ACTIVE
                currentState
            } else {
                val transition = GameRules.advance(currentState, random = random)
                if (currentState.status == SessionStatus.ACTIVE &&
                    transition.state.status == SessionStatus.GAME_OVER
                ) {
                    recordCompletedScore(transition.state.score)
                }
                outcome = transition.outcome
                shouldStopClock = transition.state.status == SessionStatus.GAME_OVER
                transition.state
            }
        }
        if (shouldStopClock) {
            stopMovementClock(generation)
        }
        return outcome
    }

    fun startClock() {
        if (closed ||
            _state.value.status != SessionStatus.ACTIVE ||
            movementJob?.isActive == true
        ) return

        val generation = sessionGeneration
        movementJob = scope.launch {
            movementClock.ticks(movementIntervalMillis).collect {
                advanceForGeneration(generation)
            }
        }
    }

    override fun close() {
        if (closed) return

        closed = true
        sessionGeneration += 1
        movementJob?.cancel()
        movementJob = null
        scope.cancel()
    }

    private fun stopMovementClock(generation: Long) {
        if (generation != sessionGeneration) return

        movementJob?.cancel()
        movementJob = null
    }

    private fun readyState(mode: PlayMode): GameState =
        GameRules.startNewGame(random = random, mode = mode).copy(status = SessionStatus.READY)

    private fun readBestScore(): Int = runCatching {
        normalizeBestScore(bestScoreStore.readBestScore())
    }.getOrDefault(0)

    private fun recordCompletedScore(completedScore: Int) {
        val normalizedScore = normalizeBestScore(completedScore)
        if (normalizedScore <= bestScoreValue) return

        bestScoreValue = normalizedScore
        runCatching { bestScoreStore.writeIfHigher(normalizedScore) }
    }
}