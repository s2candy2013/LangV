package com.linusv.englishcoach.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

data class VoiceOption(val tag: String, val label: String)

data class LearningLanguage(
    val code: String,
    val displayNameVi: String,
    val shortName: String,
    val levelSystem: String,
    val levels: List<String>,
    val defaultLevel: String,
    val voices: List<VoiceOption>,
)

object SupportedLanguages {
    val all = listOf(
        LearningLanguage("en", "Tiếng Anh", "Anh", "CEFR", listOf("A1", "A2", "B1", "B2", "C1"), "A2", listOf(VoiceOption("en-US", "Mỹ"), VoiceOption("en-GB", "Anh"))),
        LearningLanguage("zh-CN", "Tiếng Trung (Giản thể)", "Trung giản thể", "HSK", listOf("HSK 1", "HSK 2", "HSK 3", "HSK 4", "HSK 5", "HSK 6"), "HSK 1", listOf(VoiceOption("zh-CN", "Phổ thông"))),
        LearningLanguage("zh-TW", "Tiếng Trung (Phồn thể)", "Trung phồn thể", "TOCFL", listOf("A1", "A2", "B1", "B2", "C1", "C2"), "A1", listOf(VoiceOption("zh-TW", "Đài Loan"))),
        LearningLanguage("ja", "Tiếng Nhật", "Nhật", "JLPT", listOf("N5", "N4", "N3", "N2", "N1"), "N5", listOf(VoiceOption("ja-JP", "Tokyo"))),
        LearningLanguage("ko", "Tiếng Hàn", "Hàn", "TOPIK", listOf("TOPIK 1", "TOPIK 2", "TOPIK 3", "TOPIK 4", "TOPIK 5", "TOPIK 6"), "TOPIK 1", listOf(VoiceOption("ko-KR", "Seoul"))),
    )

    fun find(code: String): LearningLanguage = all.firstOrNull { it.code == code } ?: all.first()
}

@Serializable
data class VocabularyItem(
    val term: String,
    val pronunciation: String,
    val romanization: String? = null,
    val partOfSpeech: String,
    val meaningVi: String,
    val exampleTarget: String,
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
    val textTarget: String,
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
    val languageCode: String,
    val level: String,
    val vocabulary: List<VocabularyItem>,
    val grammar: List<GrammarPattern>,
    val passage: ReadingPassage,
    val speakingPrompts: List<SpeakingPrompt>,
)

@Serializable
data class FlashcardSet(
    val title: String,
    val cards: List<VocabularyItem>,
)

@Serializable
data class ConversationLine(
    val speaker: String,
    val targetText: String,
    val translationVi: String,
)

@Serializable
data class ConversationPayload(
    val title: String,
    val topic: String,
    val lines: List<ConversationLine>,
    val tips: List<String>,
)

@Serializable
data class ExerciseFeedback(
    val title: String,
    val score: Int,
    val answers: List<String>,
    val mistakes: List<String>,
    val explanations: List<String>,
    val nextSteps: List<String>,
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
    val languageCode: String = "en",
    val level: String = "A2",
    val goal: String = "Giao tiếp hằng ngày",
    val interests: String = "Công việc và đời sống",
    val sessionMinutes: Int = 15,
    val voiceTag: String = "en-US",
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

data class ModelHealth(
    val modelName: String,
    val requestCount: Int = 0,
    val successCount: Int = 0,
    val throttledCount: Int = 0,
    val lastLatencyMs: Long? = null,
    val lastFailure: String? = null,
    val lastUsedAt: Long? = null,
    val status: String = "Chưa có dữ liệu",
)

object SupportedGeminiModels {
    // Models compatible with the current text/JSON lesson flow and audio input.
    val generalUse = listOf(
        "gemini-3.1-pro-preview",
        "gemini-3.8-flash",
        "gemini-3.7-flash",
        "gemini-3.6-flash",
        "gemini-3.5-flash",
        "gemini-3.5-flash-lite",
        "gemini-3.1-flash-lite",
        "gemini-3-flash-preview",
    )
}

@Entity(tableName = "profiles")
data class LearnerProfileEntity(
    @PrimaryKey val languageCode: String,
    val level: String,
    val goal: String,
    val interests: String,
    val sessionMinutes: Int,
    val voiceTag: String,
    val completedOnboarding: Boolean,
    val isActive: Boolean,
)

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val title: String,
    val topic: String,
    val level: String,
    val payloadJson: String,
    val languageCode: String,
)

@Entity(tableName = "vocabulary_progress", primaryKeys = ["languageCode", "term"])
data class VocabularyProgressEntity(
    val languageCode: String,
    val term: String,
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
    val languageCode: String,
)
