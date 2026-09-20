package com.linusv.englishcoach.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
data class VocabularyItem(
    val term: String,
    val ipa: String,
    val partOfSpeech: String,
    val meaningVi: String,
    val exampleEn: String,
    val exampleVi: String,
)

@Serializable
data class GrammarPattern(
    val pattern: String,
    val explanationVi: String,
    val commonMistake: String,
    val examples: List<String>,
)

@Serializable
data class ReadingPassage(
    val title: String,
    val textEn: String,
    val translationVi: String,
    val questions: List<String>,
)

@Serializable
data class SpeakingPrompt(
    val targetText: String,
    val pronunciationTips: List<String>,
)

@Serializable
data class LessonPayload(
    val title: String,
    val topic: String,
    val cefr: String,
    val vocabulary: List<VocabularyItem>,
    val grammar: List<GrammarPattern>,
    val passage: ReadingPassage,
    val speakingPrompts: List<SpeakingPrompt>,
)

@Serializable
data class PronunciationFeedback(
    val pronunciation: Int,
    val fluency: Int,
    val issues: List<String>,
    val tips: List<String>,
)

@Serializable
data class LearnerProfile(
    val id: Int = 1,
    val level: String = "A2",
    val goal: String = "Giao tiếp hằng ngày",
    val interests: String = "Công việc và đời sống",
    val sessionMinutes: Int = 15,
    val accent: String = "US",
    val completedOnboarding: Boolean = false,
)

@Serializable
data class SpeakingResult(
    val contentAccuracy: Int,
    val pronunciation: Int,
    val fluency: Int,
    val overall: Int,
    val transcript: String,
    val feedback: PronunciationFeedback? = null,
    val isComplete: Boolean = true,
)

@Serializable
data class GeneratedLesson(
    val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val payload: LessonPayload,
)

@Entity(tableName = "profiles")
data class LearnerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val level: String,
    val goal: String,
    val interests: String,
    val sessionMinutes: Int,
    val accent: String,
    val completedOnboarding: Boolean,
)

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val title: String,
    val topic: String,
    val level: String,
    val payloadJson: String,
)

@Entity(tableName = "vocabulary_progress")
data class VocabularyProgressEntity(
    @PrimaryKey val term: String,
    val intervalDays: Int = 0,
    val nextReviewAt: Long = 0,
    val reviewCount: Int = 0,
)

@Entity(tableName = "speaking_attempts")
data class SpeakingAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val targetText: String,
    val transcript: String,
    val contentAccuracy: Int,
    val pronunciation: Int,
    val fluency: Int,
    val overall: Int,
    val feedbackJson: String,
)
