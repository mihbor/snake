package com.example.snake.game

import com.example.snake.game.model.Board
import com.example.snake.game.model.Cell
import com.example.snake.game.model.Direction
import com.example.snake.game.model.GameState
import com.example.snake.game.model.SessionStatus
import com.example.snake.game.model.Snake
import com.example.snake.game.rules.DirectionRequestResult
import com.example.snake.game.rules.GameRules
import com.example.snake.game.rules.StepOutcome
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GameRulesTest {
    @Test
    fun boardValidatesDimensionsAndCoordinates() {
        assertFailsWith<IllegalArgumentException> { Board(columns = 0, rows = 20) }
        assertFailsWith<IllegalArgumentException> { Board(columns = 20, rows = -1) }

        val board = Board(columns = 3, rows = 2)

        assertTrue(board.contains(Cell(0, 0)))
        assertTrue(board.contains(Cell(2, 1)))
        assertTrue(!board.contains(Cell(-1, 0)))
        assertTrue(!board.contains(Cell(3, 0)))
        assertTrue(!board.contains(Cell(0, 2)))
    }

    @Test
    fun directionsExposeOppositesAndOneCellOffsets() {
        assertEquals(Direction.DOWN, Direction.UP.opposite())
        assertEquals(Direction.UP, Direction.DOWN.opposite())
        assertEquals(Direction.RIGHT, Direction.LEFT.opposite())
        assertEquals(Direction.LEFT, Direction.RIGHT.opposite())

        assertEquals(Cell(0, -1), Direction.UP.offset())
        assertEquals(Cell(0, 1), Direction.DOWN.offset())
        assertEquals(Cell(-1, 0), Direction.LEFT.offset())
        assertEquals(Cell(1, 0), Direction.RIGHT.offset())
    }

    @Test
    fun snakeKeepsHeadFirstOrderingWhenMoving() {
        assertFailsWith<IllegalArgumentException> { Snake(emptyList()) }

        val snake = Snake(listOf(Cell(2, 3), Cell(1, 3), Cell(0, 3)))

        assertEquals(Cell(2, 3), snake.head())
        assertEquals(
            listOf(Cell(2, 4), Cell(2, 3), Cell(1, 3)),
            snake.moveTo(Cell(2, 4)).segments,
        )
    }

    @Test
    fun startingCreatesActiveGameWithDeterministicSeed() {
        val state = GameRules.startNewGame(random = Random(0))

        assertEquals(SessionStatus.ACTIVE, state.status)
        assertEquals(Board(20, 20), state.board)
        assertEquals(
            listOf(Cell(10, 10), Cell(9, 10), Cell(8, 10)),
            state.snake.segments,
        )
        assertEquals(Direction.RIGHT, state.currentDirection)
        assertEquals(null, state.pendingDirection)
        assertEquals(0, state.score)
        assertTrue(state.snake.segments.distinct().size == 3)
        assertTrue(state.snake.segments.all(state.board::contains))
        assertTrue(state.food !in state.snake.segments)
        assertTrue(state.board.contains(state.food))
        assertEquals(state, GameRules.startNewGame(random = Random(0)))
    }

    @Test
    fun freshGamesCanPlaceFoodBelowTheTopRow() {
        val foods = (0 until 32).map { seed ->
            GameRules.startNewGame(random = Random(seed)).food
        }

        assertTrue(foods.any { it.row > 0 }, "Food should be able to spawn below the top row")
    }

    @Test
    fun largerBoardsUseTheSameCenteredRightMovingArrangement() {
        val state = GameRules.startNewGame(Board(columns = 10, rows = 6))

        assertEquals(
            listOf(Cell(5, 3), Cell(4, 3), Cell(3, 3)),
            state.snake.segments,
        )
    }

    @Test
    fun tooSmallBoardIsRejectedForInitialSnake() {
        assertFailsWith<IllegalArgumentException> {
            GameRules.startNewGame(Board(columns = 3, rows = 20))
        }
    }

    @Test
    fun advanceMovesExactlyOneCellAndPreservesLengthAndScore() {
        val initial = GameRules.startNewGame()

        val transition = GameRules.advance(initial)

        assertEquals(StepOutcome.MOVED, transition.outcome)
        assertEquals(
            listOf(Cell(11, 10), Cell(10, 10), Cell(9, 10)),
            transition.state.snake.segments,
        )
        assertEquals(Direction.RIGHT, transition.state.currentDirection)
        assertEquals(null, transition.state.pendingDirection)
        assertEquals(0, transition.state.score)
        assertEquals(initial.food, transition.state.food)
    }

    @Test
    fun gameStateRejectsFoodOutsideTheBoardOrOnTheSnake() {
        val board = Board()
        val snake = Snake(listOf(Cell(10, 10), Cell(9, 10), Cell(8, 10)))

        assertFailsWith<IllegalArgumentException> {
            GameState(
                status = SessionStatus.ACTIVE,
                board = board,
                snake = snake,
                currentDirection = Direction.RIGHT,
                pendingDirection = null,
                score = 0,
                food = Cell(-1, 0),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            GameState(
                status = SessionStatus.ACTIVE,
                board = board,
                snake = snake,
                currentDirection = Direction.RIGHT,
                pendingDirection = null,
                score = 0,
                food = snake.head(),
            )
        }
    }

    @Test
    fun collectingFoodGrowsSnakeAwardsTenPointsAndReplacesFood() {
        val initial = GameRules.startNewGame().copy(food = Cell(11, 10))

        val transition = GameRules.advance(initial)

        assertEquals(StepOutcome.FOOD_COLLECTED, transition.outcome)
        assertEquals(
            listOf(
                Cell(11, 10),
                Cell(10, 10),
                Cell(9, 10),
                Cell(8, 10),
            ),
            transition.state.snake.segments,
        )
        assertEquals(10, transition.state.score)
        assertEquals(Direction.RIGHT, transition.state.currentDirection)
        assertEquals(null, transition.state.pendingDirection)
        assertTrue(transition.state.food !in transition.state.snake.segments)
        assertTrue(transition.state.board.contains(transition.state.food))
    }

    @Test
    fun collectingFoodTwiceAccumulatesGrowthAndScore() {
        val initial = GameRules.startNewGame().copy(food = Cell(11, 10))
        val afterFirst = GameRules.advance(initial).state.copy(food = Cell(12, 10))

        val transition = GameRules.advance(afterFirst)

        assertEquals(StepOutcome.FOOD_COLLECTED, transition.outcome)
        assertEquals(5, transition.state.snake.segments.size)
        assertEquals(
            listOf(
                Cell(12, 10),
                Cell(11, 10),
                Cell(10, 10),
                Cell(9, 10),
                Cell(8, 10),
            ),
            transition.state.snake.segments,
        )
        assertEquals(20, transition.state.score)
        assertTrue(transition.state.food !in transition.state.snake.segments)
    }

    @Test
    fun pendingTurnCanCollectFoodOnTheFollowingStep() {
        val initial = GameRules.startNewGame().copy(food = Cell(10, 9))
        val requested = GameRules.requestDirection(initial, Direction.UP)

        val transition = GameRules.advance(requested.state)

        assertEquals(DirectionRequestResult.ACCEPTED, requested.result)
        assertEquals(StepOutcome.FOOD_COLLECTED, transition.outcome)
        assertEquals(Direction.UP, transition.state.currentDirection)
        assertEquals(null, transition.state.pendingDirection)
        assertEquals(10, transition.state.score)
        assertEquals(Cell(10, 9), transition.state.snake.head())
        assertEquals(4, transition.state.snake.segments.size)
    }

    @Test
    fun collectionIsBlockedWhenGrowthLeavesNoReplacementCell() {
        val initial = GameRules.startNewGame(Board(columns = 4, rows = 1))

        val transition = GameRules.advance(initial)

        assertEquals(StepOutcome.FOOD_COLLECTION_BLOCKED, transition.outcome)
        assertEquals(initial, transition.state)
        assertEquals(SessionStatus.ACTIVE, transition.state.status)
    }

    @Test
    fun noNewInputContinuesInTheLastValidDirection() {
        val initial = GameRules.startNewGame()

        val afterFirstStep = GameRules.advance(initial).state
        val afterSecondStep = GameRules.advance(afterFirstStep).state

        assertEquals(Cell(12, 10), afterSecondStep.snake.head())
        assertEquals(Direction.RIGHT, afterSecondStep.currentDirection)
    }

    @Test
    fun validDirectionsAreAppliedOnTheFollowingStep() {
        for (direction in listOf(Direction.UP, Direction.DOWN, Direction.RIGHT)) {
            val initial = GameRules.startNewGame()
            val request = GameRules.requestDirection(initial, direction)

            assertEquals(DirectionRequestResult.ACCEPTED, request.result)
            assertEquals(initial.snake, request.state.snake)
            assertEquals(direction, request.state.pendingDirection)

            val transition = GameRules.advance(request.state)

            assertEquals(StepOutcome.MOVED, transition.outcome)
            assertEquals(direction, transition.state.currentDirection)
            assertEquals(null, transition.state.pendingDirection)
            assertEquals(
                Cell(
                    10 + direction.offset().column,
                    10 + direction.offset().row,
                ),
                transition.state.snake.head(),
            )
        }
    }

    @Test
    fun immediateReversalIsIgnoredWithoutChangingOrEndingTheGame() {
        val initial = GameRules.startNewGame()

        val request = GameRules.requestDirection(initial, Direction.LEFT)

        assertEquals(DirectionRequestResult.IGNORED_REVERSAL, request.result)
        assertEquals(initial, request.state)

        val transition = GameRules.advance(request.state)

        assertEquals(StepOutcome.MOVED, transition.outcome)
        assertEquals(Cell(11, 10), transition.state.snake.head())
        assertEquals(SessionStatus.ACTIVE, transition.state.status)
    }

    @Test
    fun pendingTurnIsIdempotentAndConflictingInputIsIgnored() {
        val initial = GameRules.startNewGame()

        val firstRequest = GameRules.requestDirection(initial, Direction.UP)
        val repeatedRequest = GameRules.requestDirection(firstRequest.state, Direction.UP)
        val conflictingRequest = GameRules.requestDirection(firstRequest.state, Direction.DOWN)

        assertEquals(DirectionRequestResult.ACCEPTED, firstRequest.result)
        assertEquals(DirectionRequestResult.ACCEPTED, repeatedRequest.result)
        assertEquals(firstRequest.state, repeatedRequest.state)
        assertEquals(DirectionRequestResult.IGNORED_PENDING_TURN, conflictingRequest.result)
        assertEquals(firstRequest.state, conflictingRequest.state)

        val afterTurn = GameRules.advance(firstRequest.state).state
        val reversal = GameRules.requestDirection(afterTurn, Direction.DOWN)

        assertEquals(DirectionRequestResult.IGNORED_REVERSAL, reversal.result)
        assertEquals(afterTurn, reversal.state)
    }

    @Test
    fun inactiveDirectionAndAdvanceAreNoOps() {
        val ready = GameState(
            status = SessionStatus.READY,
            board = Board(),
            snake = Snake(listOf(Cell(10, 10), Cell(9, 10), Cell(8, 10))),
            currentDirection = Direction.RIGHT,
            pendingDirection = null,
            score = 0,
            food = Cell(0, 0),
        )

        val request = GameRules.requestDirection(ready, Direction.UP)
        val transition = GameRules.advance(ready)

        assertEquals(DirectionRequestResult.IGNORED_INACTIVE, request.result)
        assertEquals(ready, request.state)
        assertEquals(StepOutcome.NOT_ACTIVE, transition.outcome)
        assertEquals(ready, transition.state)
    }

    @Test
    fun boundaryAttemptDoesNotWrapOrDiscardPendingDirection() {
        val initial = GameRules.startNewGame(Board(columns = 5, rows = 1)).copy(food = Cell(4, 0))
        val atRightEdge = GameRules.advance(initial).state
        val pendingAtEdge = GameRules.requestDirection(atRightEdge, Direction.UP).state

        val transition = GameRules.advance(pendingAtEdge)

        assertEquals(StepOutcome.BOUNDARY_BLOCKED, transition.outcome)
        assertEquals(pendingAtEdge, transition.state)
        assertEquals(Cell(3, 0), transition.state.snake.head())
        assertEquals(Direction.UP, transition.state.pendingDirection)
    }
}