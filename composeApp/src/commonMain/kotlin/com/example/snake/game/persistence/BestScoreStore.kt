package com.example.snake.game.persistence

interface BestScoreStore {
    fun readBestScore(): Int

    fun writeIfHigher(completedScore: Int)
}

fun normalizeBestScore(value: Int): Int = value.coerceAtLeast(0)