package com.example.snake

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.snake.game.ui.InputCapabilities
import com.example.snake.game.ui.SnakeApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Snake",
    ) {
        SnakeApp(InputCapabilities(keyboard = true, touch = false))
    }
}