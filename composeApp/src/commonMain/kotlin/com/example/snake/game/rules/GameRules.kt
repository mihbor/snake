package com.example.snake.game.rules

import com.example.snake.game.model.Board
import com.example.snake.game.model.Cell
import com.example.snake.game.model.CollisionCause
import com.example.snake.game.model.Direction
import com.example.snake.game.model.GameState
import com.example.snake.game.model.SessionStatus
import com.example.snake.game.model.Snake
import kotlin.random.Random

object GameRules {
    /**
     * Creates a fresh active game with the head at the integer center of the board and the body
     * extending left. The default board therefore starts at (10, 10), (9, 10), and (8, 10).
     */
    fun startNewGame(
        board: Board = Board(20, 20),
        random: Random = Random.Default,
    ): GameState {
        val head = Cell(column = board.columns / 2, row = board.rows / 2)
        val initialSegments = listOf(
            head,
            Cell(column = head.column - 1, row = head.row),
            Cell(column = head.column - 2, row = head.row),
        )
        require(initialSegments.all(board::contains)) {
            "Board is too small for the centered three-segment snake"
        }
        val initialSnake = Snake(initialSegments)
        val food = randomUnoccupiedCell(board, initialSnake, random)
            ?: throw IllegalArgumentException("Board has no free cell for food")

        return GameState(
            status = SessionStatus.ACTIVE,
            board = board,
            snake = initialSnake,
            currentDirection = Direction.RIGHT,
            pendingDirection = null,
            score = 0,
            food = food,
            collisionCause = null,
        )
    }

    /**
     * Retains the first accepted turn until the next successful step. Repeating that turn is
     * idempotent, while a different request cannot replace it before it is applied.
     */
    fun requestDirection(state: GameState, requested: Direction): DirectionRequest {
        if (state.status != SessionStatus.ACTIVE) {
            return DirectionRequest(state, DirectionRequestResult.IGNORED_INACTIVE)
        }

        if (requested == state.currentDirection.opposite()) {
            return DirectionRequest(state, DirectionRequestResult.IGNORED_REVERSAL)
        }

        val pendingDirection = state.pendingDirection
        if (pendingDirection != null) {
            if (pendingDirection == requested) {
                return DirectionRequest(state, DirectionRequestResult.ACCEPTED)
            }
            return DirectionRequest(state, DirectionRequestResult.IGNORED_PENDING_TURN)
        }

        return DirectionRequest(
            state = state.copy(pendingDirection = requested),
            result = DirectionRequestResult.ACCEPTED,
        )
    }

    fun advance(state: GameState, random: Random = Random.Default): StepTransition {
        if (state.status != SessionStatus.ACTIVE) {
            return StepTransition(state, StepOutcome.NOT_ACTIVE)
        }

        val effectiveDirection = state.pendingDirection ?: state.currentDirection
        val offset = effectiveDirection.offset()
        val nextHead = Cell(
            column = state.snake.head().column + offset.column,
            row = state.snake.head().row + offset.row,
        )
        if (!state.board.contains(nextHead)) {
            return StepTransition(
                state = gameOver(state, cause = CollisionCause.BOUNDARY),
                outcome = StepOutcome.BOUNDARY_COLLISION,
            )
        }

        // The literal current-body policy includes the tail; collision is checked before any
        // movement operation can drop it.
        if (nextHead in state.snake.segments) {
            return StepTransition(
                state = gameOver(state, cause = CollisionCause.SELF_COLLISION),
                outcome = StepOutcome.SELF_COLLISION,
            )
        }

        if (nextHead == state.food) {
            val grownSnake = state.snake.moveToAndGrow(nextHead)
            val replacementFood = randomUnoccupiedCell(state.board, grownSnake, random)
                ?: return StepTransition(state, StepOutcome.FOOD_COLLECTION_BLOCKED)

            return StepTransition(
                state = state.copy(
                    snake = grownSnake,
                    currentDirection = effectiveDirection,
                    pendingDirection = null,
                    score = state.score + 10,
                    food = replacementFood,
                ),
                outcome = StepOutcome.FOOD_COLLECTED,
            )
        }

        return StepTransition(
            state = state.copy(
                snake = state.snake.moveTo(nextHead),
                currentDirection = effectiveDirection,
                pendingDirection = null,
            ),
            outcome = StepOutcome.MOVED,
        )
    }

    private fun gameOver(
        state: GameState,
        cause: CollisionCause,
    ): GameState = state.copy(
        status = SessionStatus.GAME_OVER,
        pendingDirection = null,
        collisionCause = cause,
    )

    private fun randomUnoccupiedCell(board: Board, snake: Snake, random: Random): Cell? {
        val availableCells = mutableListOf<Cell>()
        for (row in 0 until board.rows) {
            for (column in 0 until board.columns) {
                val candidate = Cell(column = column, row = row)
                if (candidate !in snake.segments) {
                    availableCells += candidate
                }
            }
        }
        if (availableCells.isEmpty()) return null
        return availableCells[random.nextInt(availableCells.size)]
    }
}