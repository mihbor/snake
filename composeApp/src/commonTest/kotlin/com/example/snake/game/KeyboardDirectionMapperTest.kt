package com.example.snake.game

import androidx.compose.ui.input.key.Key
import com.example.snake.game.input.GameKey
import com.example.snake.game.input.KeyboardDirectionMapper
import com.example.snake.game.model.Direction
import com.example.snake.game.ui.toGameKey
import kotlin.test.Test
import kotlin.test.assertEquals

class KeyboardDirectionMapperTest {
    @Test
    fun mapsArrowKeysAndWASDToTheSameDirections() {
        assertEquals(Direction.UP, KeyboardDirectionMapper.toDirection(GameKey.ARROW_UP))
        assertEquals(Direction.UP, KeyboardDirectionMapper.toDirection(GameKey.W))
        assertEquals(Direction.DOWN, KeyboardDirectionMapper.toDirection(GameKey.ARROW_DOWN))
        assertEquals(Direction.DOWN, KeyboardDirectionMapper.toDirection(GameKey.S))
        assertEquals(Direction.LEFT, KeyboardDirectionMapper.toDirection(GameKey.ARROW_LEFT))
        assertEquals(Direction.LEFT, KeyboardDirectionMapper.toDirection(GameKey.A))
        assertEquals(Direction.RIGHT, KeyboardDirectionMapper.toDirection(GameKey.ARROW_RIGHT))
        assertEquals(Direction.RIGHT, KeyboardDirectionMapper.toDirection(GameKey.D))
    }

    @Test
    fun pauseKeysAreNotDirectionalInputs() {
        assertEquals(null, KeyboardDirectionMapper.toDirection(GameKey.P))
        assertEquals(null, KeyboardDirectionMapper.toDirection(GameKey.SPACE))
    }

    @Test
    fun composePauseKeysMapAtTheInputBoundary() {
        assertEquals(GameKey.P, Key.P.toGameKey())
        assertEquals(GameKey.SPACE, Key.Spacebar.toGameKey())
        assertEquals(null, Key.Enter.toGameKey())
    }
}