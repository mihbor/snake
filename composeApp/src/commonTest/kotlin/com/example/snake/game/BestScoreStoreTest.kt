package com.example.snake.game

import com.example.snake.game.persistence.BestScoreStore
import com.example.snake.game.persistence.normalizeBestScore
import kotlin.test.Test
import kotlin.test.assertEquals

class BestScoreStoreTest {
    @Test
    fun negativeScoresNormalizeToZeroAndValidScoresRemainUnchanged() {
        assertEquals(0, normalizeBestScore(-1))
        assertEquals(0, normalizeBestScore(0))
        assertEquals(50, normalizeBestScore(50))
    }
}

internal class TestBestScoreStore(
    var storedScore: Int = 0,
    var readFailure: Boolean = false,
    var writeFailure: Boolean = false,
) : BestScoreStore {
    var readCount: Int = 0
        private set
    var writeCount: Int = 0
        private set
    var lastWrittenScore: Int? = null
        private set

    override fun readBestScore(): Int {
        readCount += 1
        if (readFailure) error("test read failure")
        return storedScore
    }

    override fun writeIfHigher(completedScore: Int) {
        if (writeFailure) error("test write failure")
        if (completedScore > storedScore) {
            storedScore = completedScore
            lastWrittenScore = completedScore
            writeCount += 1
        }
    }
}