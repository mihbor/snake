package com.example.snake.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.snake.game.input.GameKey
import com.example.snake.game.input.KeyboardDirectionMapper
import com.example.snake.game.model.CollisionCause
import com.example.snake.game.model.Direction
import com.example.snake.game.model.GameState
import com.example.snake.game.model.SessionStatus

@Composable
fun GameScreen(
    state: GameState,
    bestScore: Int,
    capabilities: InputCapabilities,
    onStart: () -> Unit,
    onDirection: (Direction) -> Unit,
    modifier: Modifier = Modifier,
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.status, capabilities.keyboard) {
        if (capabilities.keyboard && state.status == SessionStatus.ACTIVE) {
            focusRequester.requestFocus()
        }
    }

    val keyboardModifier = if (capabilities.keyboard) {
        Modifier.onPreviewKeyEvent { event ->
            handleKeyboardEvent(event, state, onDirection, onPause)
        }
    } else {
        Modifier
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable(enabled = capabilities.keyboard && state.status == SessionStatus.ACTIVE)
            .then(keyboardModifier),
    ) {
        // A scrollable Column is measured with an unbounded height. Calculate the board size
        // from the window constraints before entering that scrollable layout so the initial
        // action and the active controls remain reachable in the default desktop window.
        val availableHeight = if (maxHeight.value.isFinite()) maxHeight else maxWidth
        val boardFraction = when (state.status) {
            SessionStatus.READY -> 0.44f
            SessionStatus.ACTIVE -> if (capabilities.touch) 0.34f else 0.58f
            SessionStatus.PAUSED -> if (capabilities.touch) 0.34f else 0.58f
            SessionStatus.GAME_OVER -> 0.58f
        }
        val boardSize = maxOf(1.dp, minOf(maxWidth - 32.dp, availableHeight * boardFraction))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Snake", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = currentScoreLabel(state),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics {
                    contentDescription = currentScoreLabel(state)
                },
            )
            Text(
                text = bestScoreLabel(bestScore),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics {
                    contentDescription = bestScoreLabel(bestScore)
                },
            )
            Spacer(modifier = Modifier.size(8.dp))

            when (state.status) {
                SessionStatus.READY -> {
                    Text("Ready to play")
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(onClick = onStart) {
                        Text("Start New Game")
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(controlHint(capabilities))
                }

                SessionStatus.ACTIVE -> {
                    Text("Direction: ${state.currentDirection.name.lowercase()}")
                    Spacer(modifier = Modifier.size(4.dp))
                    val pendingDirection = state.pendingDirection
                    Text(
                        // Keep a one-line feedback slot allocated even when it is empty so a
                        // pending turn cannot move the board when the state changes.
                        text = pendingDirection?.let {
                            "Turn accepted: ${it.name.lowercase()}"
                        } ?: "\u00A0",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = pendingDirection?.let {
                            Modifier.semantics {
                                contentDescription = "Accepted turn: ${it.name.lowercase()}"
                            }
                        } ?: Modifier,
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Button(
                        onClick = onPause,
                        modifier = Modifier.semantics {
                            contentDescription = "Pause game"
                            role = Role.Button
                        },
                    ) {
                        Text("Pause")
                    }
                    if (capabilities.touch) {
                        Spacer(modifier = Modifier.size(12.dp))
                        DirectionControls(
                            selectedDirection = state.pendingDirection,
                            onDirection = onDirection,
                        )
                    }
                    if (capabilities.keyboard) {
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Use Arrow keys or W/A/S/D to steer. Press P or Space to pause.")
                    }
                }

                SessionStatus.PAUSED -> {
                    Text(
                        text = "Paused",
                        modifier = Modifier.semantics {
                            contentDescription = "Paused"
                            stateDescription = "Paused"
                        },
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(
                        onClick = onResume,
                        modifier = Modifier.semantics {
                            contentDescription = "Resume game"
                            role = Role.Button
                        },
                    ) {
                        Text("Resume")
                    }
                }

                SessionStatus.GAME_OVER -> {
                    Text(
                        text = "Game Over",
                        modifier = Modifier.semantics {
                            contentDescription = "Game over"
                            stateDescription = "Game over"
                        },
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    state.collisionCause?.let { cause ->
                        Text(
                            text = collisionMessage(cause),
                            modifier = Modifier.semantics {
                                contentDescription = collisionMessage(cause)
                            },
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(
                        onClick = onStart,
                        modifier = Modifier.semantics {
                            contentDescription = "Restart game"
                            role = Role.Button
                        },
                    ) {
                        Text("Restart Game")
                    }
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            SnakeBoard(state, Modifier.size(boardSize))
        }
    }
}

internal fun currentScoreLabel(state: GameState): String = when (state.status) {
    SessionStatus.READY, SessionStatus.ACTIVE, SessionStatus.PAUSED ->
        "Current score: ${state.score}"
    SessionStatus.GAME_OVER -> "Final score: ${state.score}"
}

internal fun bestScoreLabel(bestScore: Int): String = "Best score: $bestScore"

@Composable
private fun SnakeBoard(state: GameState, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .padding(8.dp)
            .semantics {
                contentDescription =
                    "Bounded ${state.board.columns} by ${state.board.rows} snake board with " +
                        "${state.snake.segments.size} segments and one food item"
            },
    ) {
        val cellWidth = size.width / state.board.columns
        val cellHeight = size.height / state.board.rows
        val boardColor = Color(0xFFE8F0E8)
        val gridColor = Color(0xFFB7C8B7)

        drawRect(color = boardColor)
        for (column in 0..state.board.columns) {
            val x = column * cellWidth
            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        }
        for (row in 0..state.board.rows) {
            val y = row * cellHeight
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }

        drawCircle(
            color = Color(0xFFD32F2F),
            center = Offset(
                x = (state.food.column + 0.5f) * cellWidth,
                y = (state.food.row + 0.5f) * cellHeight,
            ),
            radius = minOf(cellWidth, cellHeight) * 0.32f,
        )

        state.snake.segments.forEachIndexed { index, cell ->
            val inset = 1.5f
            drawRoundRect(
                color = if (index == 0) Color(0xFF1B5E20) else Color(0xFF43A047),
                topLeft = Offset(
                    x = cell.column * cellWidth + inset,
                    y = cell.row * cellHeight + inset,
                ),
                size = Size(
                    width = cellWidth - (inset * 2),
                    height = cellHeight - (inset * 2),
                ),
                cornerRadius = CornerRadius(4f, 4f),
            )
        }
    }
}

@Composable
private fun DirectionControls(
    selectedDirection: Direction?,
    onDirection: (Direction) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DirectionButton(Direction.UP, selectedDirection, onDirection)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DirectionButton(Direction.LEFT, selectedDirection, onDirection)
            Spacer(modifier = Modifier.size(64.dp))
            DirectionButton(Direction.RIGHT, selectedDirection, onDirection)
        }
        DirectionButton(Direction.DOWN, selectedDirection, onDirection)
    }
}

@Composable
private fun DirectionButton(
    direction: Direction,
    selectedDirection: Direction?,
    onDirection: (Direction) -> Unit,
) {
    val selected = selectedDirection == direction
    Button(
        onClick = { onDirection(direction) },
        modifier = Modifier
            .sizeIn(minWidth = 64.dp, minHeight = 48.dp)
            .semantics {
                contentDescription = "Move ${direction.name.lowercase()}"
                role = Role.Button
                stateDescription = if (selected) "Selected" else "Not selected"
            },
        colors = if (selected) {
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        } else {
            ButtonDefaults.buttonColors()
        },
    ) {
        Text(direction.label())
    }
}

private fun controlHint(capabilities: InputCapabilities): String = when {
    capabilities.keyboard && capabilities.touch -> "Use Arrow keys, W/A/S/D, or the directional controls"
    capabilities.keyboard -> "Use Arrow keys or W/A/S/D to steer"
    capabilities.touch -> "Use the directional controls to steer"
    else -> "Choose a supported control surface to steer"
}

internal fun collisionMessage(cause: CollisionCause): String = when (cause) {
    CollisionCause.BOUNDARY -> "The snake hit the board boundary"
    CollisionCause.SELF_COLLISION -> "The snake hit its body"
}

private fun handleKeyboardEvent(
    event: KeyEvent,
    state: GameState,
    onDirection: (Direction) -> Unit,
    onPause: () -> Unit,
): Boolean {
    if (state.status != SessionStatus.ACTIVE ||
        event.type != KeyEventType.KeyDown
    ) {
        return false
    }

    val gameKey = event.key.toGameKey() ?: return false
    if (gameKey == GameKey.P || gameKey == GameKey.SPACE) {
        onPause()
        return true
    }

    val direction = KeyboardDirectionMapper.toDirection(gameKey) ?: return false
    onDirection(direction)
    return true
}

internal fun Key.toGameKey(): GameKey? = when (this) {
    Key.DirectionUp -> GameKey.ARROW_UP
    Key.DirectionDown -> GameKey.ARROW_DOWN
    Key.DirectionLeft -> GameKey.ARROW_LEFT
    Key.DirectionRight -> GameKey.ARROW_RIGHT
    Key.W -> GameKey.W
    Key.A -> GameKey.A
    Key.S -> GameKey.S
    Key.D -> GameKey.D
    Key.P -> GameKey.P
    Key.Spacebar -> GameKey.SPACE
    else -> null
}

private fun Direction.label(): String = when (this) {
    Direction.UP -> "Up"
    Direction.DOWN -> "Down"
    Direction.LEFT -> "Left"
    Direction.RIGHT -> "Right"
}
