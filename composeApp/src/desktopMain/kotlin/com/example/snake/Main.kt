package com.example.snake

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.snake.game.persistence.DesktopBestScoreStore
import com.example.snake.game.ui.InputCapabilities
import com.example.snake.game.ui.SnakeApp

fun main() = application {
    val windowState = rememberWindowState(width = 520.dp, height = 920.dp)
    Window(
        onCloseRequest = ::exitApplication,
        title = "Snake",
        state = windowState,
    ) {
        SnakeApp(
            capabilities = InputCapabilities(keyboard = true, touch = false),
            bestScoreStore = DesktopBestScoreStore(),
        )
    }
}