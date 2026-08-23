package com.example.snake.game.persistence

import kotlinx.browser.window

class BrowserBestScoreStore : BestScoreStore {
    override fun readBestScore(): Int = runCatching {
        normalizeBestScore(window.localStorage.getItem(BEST_SCORE_KEY)?.toIntOrNull() ?: 0)
    }.getOrDefault(0)

    override fun writeIfHigher(completedScore: Int) {
        val normalizedScore = normalizeBestScore(completedScore)
        runCatching {
            val currentBest = normalizeBestScore(
                window.localStorage.getItem(BEST_SCORE_KEY)?.toIntOrNull() ?: 0,
            )
            if (normalizedScore > currentBest) {
                window.localStorage.setItem(BEST_SCORE_KEY, normalizedScore.toString())
            }
        }
    }

    private companion object {
        const val BEST_SCORE_KEY = "com.example.snake.best_score"
    }
}