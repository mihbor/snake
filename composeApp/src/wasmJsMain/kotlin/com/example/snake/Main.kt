package com.example.snake

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.example.snake.game.persistence.BrowserBestScoreStore
import com.example.snake.game.ui.InputCapabilities
import com.example.snake.game.ui.SnakeApp
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        SnakeApp(
            capabilities = InputCapabilities(keyboard = true, touch = true),
            bestScoreStore = BrowserBestScoreStore(),
        )
    }
}