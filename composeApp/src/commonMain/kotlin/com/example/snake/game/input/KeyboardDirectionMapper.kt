package com.example.snake.game.input

import com.example.snake.game.model.Direction

object KeyboardDirectionMapper {
    fun toDirection(key: GameKey): Direction? = when (key) {
        GameKey.ARROW_UP, GameKey.W -> Direction.UP
        GameKey.ARROW_DOWN, GameKey.S -> Direction.DOWN
        GameKey.ARROW_LEFT, GameKey.A -> Direction.LEFT
        GameKey.ARROW_RIGHT, GameKey.D -> Direction.RIGHT
        GameKey.Q -> Direction.FORWARD
        GameKey.E -> Direction.BACKWARD
        GameKey.P, GameKey.SPACE -> null
    }
}