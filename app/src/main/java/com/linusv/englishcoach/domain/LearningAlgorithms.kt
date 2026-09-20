package com.linusv.englishcoach.domain

import kotlin.math.max

fun normalizeTranscript(value: String): List<String> = value
    .lowercase()
    .replace(Regex("[^a-z0-9' ]"), " ")
    .split(Regex("\\s+"))
    .filter(String::isNotBlank)

fun wordErrorRate(reference: String, hypothesis: String): Double {
    val ref = normalizeTranscript(reference)
    val hyp = normalizeTranscript(hypothesis)
    if (ref.isEmpty()) return if (hyp.isEmpty()) 0.0 else 1.0
    val distances = Array(ref.size + 1) { IntArray(hyp.size + 1) }
    for (i in 0..ref.size) distances[i][0] = i
    for (j in 0..hyp.size) distances[0][j] = j
    for (i in 1..ref.size) {
        for (j in 1..hyp.size) {
            distances[i][j] = if (ref[i - 1] == hyp[j - 1]) distances[i - 1][j - 1]
            else minOf(distances[i - 1][j] + 1, distances[i][j - 1] + 1, distances[i - 1][j - 1] + 1)
        }
    }
    return distances[ref.size][hyp.size].toDouble() / ref.size
}

fun contentAccuracy(reference: String, hypothesis: String): Int =
    (100.0 * (1.0 - wordErrorRate(reference, hypothesis)).coerceIn(0.0, 1.0)).toInt()

fun combinedScore(content: Int, pronunciation: Int, fluency: Int): Int =
    (content * .4 + pronunciation * .4 + fluency * .2).toInt().coerceIn(0, 100)

fun nextReviewInterval(label: String, currentIntervalDays: Int): Int = when (label.lowercase()) {
    "again" -> 1
    "hard" -> 3
    "good" -> when {
        currentIntervalDays <= 0 -> 7
        currentIntervalDays < 7 -> 7
        currentIntervalDays < 14 -> 14
        else -> 30
    }
    "easy" -> when {
        currentIntervalDays <= 0 -> 14
        currentIntervalDays < 14 -> 14
        else -> 30
    }
    else -> max(1, currentIntervalDays)
}
