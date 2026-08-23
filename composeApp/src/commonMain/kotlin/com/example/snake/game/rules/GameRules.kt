package com.example.snake.game.rules

import com.example.snake.game.model.Board
import com.example.snake.game.model.Cell
import com.example.snake.game.model.CollisionCause
import com.example.snake.game.model.Direction
import com.example.snake.game.model.GameState
import com.example.snake.game.model.PlayMode
import com.example.snake.game.model.SessionStatus
import com.example.snake.game.model.Snake
import kotlin.random.Random

object GameRules {
    /**
     * Creates a fresh active game with the head at the integer center of the board and the body
     * extending left. The default board therefore starts at (10, 10), (9, 10), and (8, 10).
     */
    fun startNewGame(
        board: Board? = null,
        random: Random = Random.Default,
        mode: PlayMode = PlayMode.TWO_D,
    ): GameState {
        val effectiveBoard = board ?: defaultBoard(mode)
        require(
            when (mode) {
                PlayMode.TWO_D -> effectiveBoard.depth == 1
                PlayMode.THREE_D -> effectiveBoard.depth >= 2
            },
        ) {
            "2D sessions require one board depth layer and 3D sessions require at least two"
        }

        val head = Cell(
            column = effectiveBoard.columns / 2,
            row = effectiveBoard.rows / 2,
            depth = if (mode == PlayMode.TWO_D) 0 else effectiveBoard.depth / 2,
        )
        val initialSegments = listOf(
            head,
            Cell(column = head.column - 1, row = head.row, depth = head.depth),
            Cell(column = head.column - 2, row = head.row, depth = head.depth),
        )
        require(initialSegments.all(effectiveBoard::contains)) {
            "Board is too small for the centered three-segment snake"
        }
        val initialSnake = Snake(initialSegments)
        val food = randomUnoccupiedCell(effectiveBoard, initialSnake, random)
            ?: throw IllegalArgumentException("Board has no free cell for food")

        return GameState(
            status = SessionStatus.ACTIVE,
            board = effectiveBoard,
            snake = initialSnake,
            currentDirection = Direction.RIGHT,
            pendingDirection = null,
            score = 0,
            food = food,
            collisionCause = null,
            mode = mode,
        )
    }

    fun pause(state: GameState): GameState = if (state.status == SessionStatus.ACTIVE) {
        state.copy(status = SessionStatus.PAUSED)
    } else {
        state
    }

    fun resume(state: GameState): GameState = if (state.status == SessionStatus.PAUSED) {
        state.copy(status = SessionStatus.ACTIVE)
    } else {
        state
    }

    /**
     * Retains the first accepted turn until the next successful step. Repeating that turn is
     * idempotent, while a different request cannot replace it before it is applied.
     */
    fun requestDirection(state: GameState, requested: Direction): DirectionRequest {
        if (state.status != SessionStatus.ACTIVE) {
            return DirectionRequest(state, DirectionRequestResult.IGNORED_INACTIVE)
        }

        if (state.mode == PlayMode.THREE_D) {
            return DirectionRequest(state, DirectionRequestResult.IGNORED_UNSUPPORTED_MODE)
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

        if (state.mode == PlayMode.THREE_D) {
            return StepTransition(state, StepOutcome.UNSUPPORTED_MODE)
        }

        val effectiveDirection = state.pendingDirection ?: state.currentDirection
        val offset = effectiveDirection.offset()
        val nextHead = Cell(
            column = state.snake.head().column + offset.column,
            row = state.snake.head().row + offset.row,
            depth = state.snake.head().depth + offset.depth,
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
        for (depth in 0 until board.depth) {
            for (row in 0 until board.rows) {
                for (column in 0 until board.columns) {
                    val candidate = Cell(column = column, row = row, depth = depth)
                    if (candidate !in snake.segments) {
                        availableCells += candidate
                    }
                }
            }
        }
        if (availableCells.isEmpty()) return null
        return availableCells[random.nextInt(availableCells.size)]
    }

    private fun defaultBoard(mode: PlayMode): Board = when (mode) {
        PlayMode.TWO_D -> Board(columns = 20, rows = 20, depth = 1)
        PlayMode.THREE_D -> Board(columns = 20, rows = 20, depth = 3)
    }
}