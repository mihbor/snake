package com.example.snake.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.snake.game.model.Direction
import com.example.snake.game.model.GameState

@Composable
fun GameScreen(
    state: GameState,
    capabilities: InputCapabilities,
    onStart: () -> Unit,
    onDirection: (Direction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.status, capabilities.keyboard) {
        if (capabilities.keyboard && state.status == com.example.snake.game.model.SessionStatus.ACTIVE) {
            focusRequester.requestFocus()
        }
    }

    val keyboardModifier = if (capabilities.keyboard) {
        Modifier.onPreviewKeyEvent { event ->
            handleKeyboardEvent(event, state, onDirection)
        }
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable(enabled = capabilities.keyboard)
            .then(keyboardModifier)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Snake", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = "Score: ${state.score}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { contentDescription = "Current score: ${state.score}" },
        )
        Spacer(modifier = Modifier.size(8.dp))
        SnakeBoard(state)
        Spacer(modifier = Modifier.size(12.dp))

        if (state.status == com.example.snake.game.model.SessionStatus.READY) {
            Text("Ready to play")
            Spacer(modifier = Modifier.size(8.dp))
            Button(onClick = onStart) {
                Text("Start New Game")
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(controlHint(capabilities))
        } else {
            Text("Direction: ${state.currentDirection.name.lowercase()}")
            state.pendingDirection?.let { pendingDirection ->
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = "Turn accepted: ${pendingDirection.name.lowercase()}",
                    modifier = Modifier.semantics {
                        contentDescription = "Accepted turn: ${pendingDirection.name.lowercase()}"
                    },
                )
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
                Text("Use Arrow keys or W/A/S/D")
            }
        }
    }
}

@Composable
private fun SnakeBoard(state: GameState) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(8.dp)
            .semantics {
                contentDescription =
                    "Bounded ${state.board.columns} by ${state.board.rows} snake board with " +
                        "${state.snake.segments.size} segments"
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
        Text(direction.symbol())
    }
}

private fun controlHint(capabilities: InputCapabilities): String = when {
    capabilities.keyboard && capabilities.touch -> "Use Arrow keys, W/A/S/D, or the directional controls"
    capabilities.keyboard -> "Use Arrow keys or W/A/S/D to steer"
    capabilities.touch -> "Use the directional controls to steer"
    else -> "Choose a supported control surface to steer"
}

private fun handleKeyboardEvent(
    event: KeyEvent,
    state: GameState,
    onDirection: (Direction) -> Unit,
): Boolean {
    if (state.status != com.example.snake.game.model.SessionStatus.ACTIVE ||
        event.type != KeyEventType.KeyDown
    ) {
        return false
    }

    val direction = KeyboardDirectionMapper.toDirection(event.key.toGameKey() ?: return false) ?: return false
    onDirection(direction)
    return true
}

private fun Key.toGameKey(): GameKey? = when (this) {
    Key.DirectionUp -> GameKey.ARROW_UP
    Key.DirectionDown -> GameKey.ARROW_DOWN
    Key.DirectionLeft -> GameKey.ARROW_LEFT
    Key.DirectionRight -> GameKey.ARROW_RIGHT
    Key.W -> GameKey.W
    Key.A -> GameKey.A
    Key.S -> GameKey.S
    Key.D -> GameKey.D
    else -> null
}

private fun Direction.symbol(): String = when (this) {
    Direction.UP -> "↑"
    Direction.DOWN -> "↓"
    Direction.LEFT -> "←"
    Direction.RIGHT -> "→"
}