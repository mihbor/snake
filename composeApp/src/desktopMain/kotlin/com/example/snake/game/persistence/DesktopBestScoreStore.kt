package com.example.snake.game.persistence

import java.util.prefs.Preferences

class DesktopBestScoreStore : BestScoreStore {
    private val preferences: Preferences? = runCatching {
        Preferences.userNodeForPackage(DesktopBestScoreStore::class.java)
    }.getOrNull()

    override fun readBestScore(): Int = runCatching {
        normalizeBestScore(preferences?.getInt(BEST_SCORE_KEY, 0) ?: 0)
    }.getOrDefault(0)

    override fun writeIfHigher(completedScore: Int) {
        val normalizedScore = normalizeBestScore(completedScore)
        runCatching {
            val currentBest = normalizeBestScore(preferences?.getInt(BEST_SCORE_KEY, 0) ?: 0)
            if (normalizedScore > currentBest) {
                preferences?.putInt(BEST_SCORE_KEY, normalizedScore)
                preferences?.flush()
            }
        }
    }

    private companion object {
        const val BEST_SCORE_KEY = "best_score"
    }
}