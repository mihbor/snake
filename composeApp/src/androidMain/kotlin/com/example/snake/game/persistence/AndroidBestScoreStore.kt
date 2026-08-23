package com.example.snake.game.persistence

import android.content.Context

class AndroidBestScoreStore(context: Context) : BestScoreStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun readBestScore(): Int = runCatching {
        normalizeBestScore(preferences.getInt(BEST_SCORE_KEY, 0))
    }.getOrDefault(0)

    override fun writeIfHigher(completedScore: Int) {
        val normalizedScore = normalizeBestScore(completedScore)
        runCatching {
            val currentBest = normalizeBestScore(preferences.getInt(BEST_SCORE_KEY, 0))
            if (normalizedScore > currentBest) {
                preferences.edit()
                    .putInt(BEST_SCORE_KEY, normalizedScore)
                    .commit()
            }
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "snake_preferences"
        const val BEST_SCORE_KEY = "best_score"
    }
}