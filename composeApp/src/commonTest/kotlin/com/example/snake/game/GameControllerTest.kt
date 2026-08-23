package com.example.snake.game

import com.example.snake.game.controller.GameController
import com.example.snake.game.controller.CoroutineMovementClock
import com.example.snake.game.controller.MovementClock
import com.example.snake.game.model.Cell
import com.example.snake.game.model.Direction
import com.example.snake.game.model.SessionStatus
import com.example.snake.game.rules.DirectionRequestResult
import com.example.snake.game.rules.StepOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameControllerTest {
    @Test
    fun controllerStartsReadyAndIgnoresInputBeforeStart() {
        val controller = testController()

        try {
            assertEquals(SessionStatus.READY, controller.state.value.status)
            assertEquals(DirectionRequestResult.IGNORED_INACTIVE, controller.requestDirection(Direction.UP))
            assertEquals(SessionStatus.READY, controller.state.value.status)
        } finally {
            controller.close()
        }
    }

    @Test
    fun startingAClockBackedGameStartsOnlyOneClockAndAppliesTurnsOnTicks() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val clock = ManualMovementClock()
        val controller = GameController(
            scope = scope,
            movementClock = clock,
        )

        try {
            controller.startNewGame()
            controller.startNewGame()
            runCurrent()

            assertEquals(1, clock.startCount)
            assertEquals(150L, clock.lastIntervalMillis)
            assertEquals(DirectionRequestResult.ACCEPTED, controller.requestDirection(Direction.UP))
            assertEquals(Cell(10, 10), controller.state.value.snake.head())

            clock.tick()
            runCurrent()

            assertEquals(Cell(10, 9), controller.state.value.snake.head())
            assertEquals(Direction.UP, controller.state.value.currentDirection)
        } finally {
            controller.close()
        }
    }

    @Test
    fun productionClockMovesTheSnakeAfterOneMovementInterval() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val controller = GameController(
            scope = scope,
            movementClock = CoroutineMovementClock(),
        )

        try {
            controller.startNewGame()
            runCurrent()

            advanceTimeBy(GameController.DEFAULT_MOVEMENT_INTERVAL_MILLIS)
            runCurrent()

            assertEquals(Cell(11, 10), controller.state.value.snake.head())
        } finally {
            controller.close()
        }
    }

    @Test
    fun controllerPublishesCollectionStateAsOneClockTransition() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + SupervisorJob())
        val clock = ManualMovementClock()
        val controller = GameController(
            scope = scope,
            movementClock = clock,
            random = FirstThenOtherRowRandom(),
        )

        try {
            controller.startNewGame()
            runCurrent()

            repeat(9) {
                clock.tick()
                runCurrent()
            }
            assertEquals(Cell(19, 10), controller.state.value.snake.head())

            assertEquals(DirectionRequestResult.ACCEPTED, controller.requestDirection(Direction.UP))
            clock.tick()
            runCurrent()
            repeat(9) {
                clock.tick()
                runCurrent()
            }
            assertEquals(Cell(19, 0), controller.state.value.snake.head())

            assertEquals(DirectionRequestResult.ACCEPTED, controller.requestDirection(Direction.LEFT))
            repeat(19) {
                clock.tick()
                runCurrent()
            }

            val collected = controller.state.value
            assertEquals(Cell(0, 0), collected.snake.head())
            assertEquals(4, collected.snake.segments.size)
            assertEquals(10, collected.score)
            assertTrue(collected.food.row > 0)
            assertTrue(collected.food !in collected.snake.segments)
            assertTrue(collected.board.contains(collected.food))
        } finally {
            controller.close()
        }
    }

    @Test
    fun closedControllerDoesNotChangeStateOrAcceptActions() {
        val controller = testController()
        controller.startNewGame()
        val activeState = controller.state.value

        controller.close()

        assertEquals(DirectionRequestResult.IGNORED_INACTIVE, controller.requestDirection(Direction.UP))
        assertEquals(StepOutcome.NOT_ACTIVE, controller.advanceForTest())
        assertEquals(activeState, controller.state.value)
    }

    private fun testController(): GameController = GameController(
        scope = CoroutineScope(SupervisorJob()),
        movementClock = ManualMovementClock(),
    )
}

private class ManualMovementClock : MovementClock {
    private val ticks = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    var startCount: Int = 0
        private set
    var lastIntervalMillis: Long? = null
        private set

    override fun ticks(intervalMillis: Long): Flow<Unit> = flow {
        startCount += 1
        lastIntervalMillis = intervalMillis
        emitAll(ticks)
    }

    fun tick() {
        ticks.tryEmit(Unit)
    }
}

private class FirstThenOtherRowRandom : Random() {
    private var calls = 0

    override fun nextBits(bitCount: Int): Int =
        if (calls++ < 2) 0 else 395
}
