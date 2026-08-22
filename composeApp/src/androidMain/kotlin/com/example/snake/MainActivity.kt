package com.example.snake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.snake.game.ui.InputCapabilities
import com.example.snake.game.ui.SnakeApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SnakeApp(InputCapabilities(keyboard = false, touch = true))
        }
    }
}