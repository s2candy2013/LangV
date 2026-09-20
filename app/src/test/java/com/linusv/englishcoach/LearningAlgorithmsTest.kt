package com.linusv.englishcoach

import com.linusv.englishcoach.domain.combinedScore
import com.linusv.englishcoach.domain.contentAccuracy
import com.linusv.englishcoach.domain.nextReviewInterval
import com.linusv.englishcoach.domain.normalizeTranscript
import com.linusv.englishcoach.domain.wordErrorRate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningAlgorithmsTest {
    @Test fun normalizeRemovesPunctuation() = assertEquals(listOf("hello", "world"), normalizeTranscript("Hello, world!"))
    @Test fun exactTranscriptHasZeroError() = assertEquals(0.0, wordErrorRate("hello world", "Hello world"), 0.001)
    @Test fun missingWordReducesAccuracy() = assertTrue(contentAccuracy("hello brave world", "hello world") < 100)
    @Test fun scoreUsesExpectedWeights() = assertEquals(80, combinedScore(80, 80, 80))
    @Test fun reviewIntervalsFollowPlan() { assertEquals(1, nextReviewInterval("Again", 7)); assertEquals(3, nextReviewInterval("Hard", 7)); assertEquals(7, nextReviewInterval("Good", 0)); assertEquals(14, nextReviewInterval("Easy", 7)); assertEquals(30, nextReviewInterval("Easy", 14)) }
}
