package com.example.snake.game.model

enum class PlayMode(private val displayLabel: String) {
    TWO_D("2D"),
    THREE_D("3D"),
    ;

    fun label(): String = displayLabel
}