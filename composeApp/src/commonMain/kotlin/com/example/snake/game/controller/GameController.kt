package com.example.snake.game.controller

import com.example.snake.game.model.GameState
import com.example.snake.game.model.Direction
import com.example.snake.game.model.SessionStatus
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
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val movementClock: MovementClock = CoroutineMovementClock(),
    private val movementIntervalMillis: Long = DEFAULT_MOVEMENT_INTERVAL_MILLIS,
    private val random: Random = Random.Default,
) : AutoCloseable {
    companion object {
        const val DEFAULT_MOVEMENT_INTERVAL_MILLIS = 150L
    }

    private val _state = MutableStateFlow(readyState())
    private var movementJob: Job? = null
    private var closed = false

    val state: StateFlow<GameState> = _state.asStateFlow()

    fun startNewGame() {
        if (closed) return

        _state.value = GameRules.startNewGame(random = random)
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

    fun advanceForTest(): StepOutcome {
        if (closed) return StepOutcome.NOT_ACTIVE

        var outcome = StepOutcome.NOT_ACTIVE
        _state.update { currentState ->
            if (closed) {
                outcome = StepOutcome.NOT_ACTIVE
                currentState
            } else {
                val transition = GameRules.advance(currentState, random = random)
                outcome = transition.outcome
                transition.state
            }
        }
        return outcome
    }

    fun startClock() {
        if (closed || _state.value.status != SessionStatus.ACTIVE || movementJob?.isActive == true) return

        movementJob = scope.launch {
            movementClock.ticks(movementIntervalMillis).collect {
                advanceForTest()
            }
        }
    }

    override fun close() {
        if (closed) return

        closed = true
        movementJob?.cancel()
        movementJob = null
        scope.cancel()
    }

    private fun readyState(): GameState =
        GameRules.startNewGame(random = random).copy(status = SessionStatus.READY)
}