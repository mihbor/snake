package com.example.snake.game.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.example.snake.game.controller.GameController

@Composable
fun SnakeApp(capabilities: InputCapabilities) {
    val controller = remember { GameController() }
    val state by controller.state.collectAsState()

    DisposableEffect(controller) {
        onDispose { controller.close() }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            GameScreen(
                state = state,
                capabilities = capabilities,
                onStart = controller::startNewGame,
                onDirection = controller::requestDirection,
                modifier = Modifier,
            )
        }
    }
}