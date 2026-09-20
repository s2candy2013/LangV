package com.linusv.englishcoach.data

import android.content.Context
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.linusv.englishcoach.BuildConfig
import com.linusv.englishcoach.domain.combinedScore
import com.linusv.englishcoach.domain.contentAccuracy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

interface LessonGenerator {
    suspend fun generateLesson(profile: LearnerProfile, topic: String): LessonPayload
    suspend fun scoreSpeech(targetText: String, transcript: String, audio: File?): PronunciationFeedback
}

private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

class DemoLessonGenerator : LessonGenerator {
    override suspend fun generateLesson(profile: LearnerProfile, topic: String): LessonPayload = LessonPayload(
        title = "A practical lesson about $topic",
        topic = topic,
        cefr = profile.level,
        vocabulary = listOf(
            VocabularyItem("consistent", "/kənˈsɪstənt/", "adjective", "đều đặn, nhất quán", "A consistent routine makes learning easier.", "Một thói quen đều đặn giúp việc học dễ hơn."),
            VocabularyItem("progress", "/ˈprɑːɡres/", "noun", "tiến bộ", "Small steps create real progress.", "Những bước nhỏ tạo ra tiến bộ thật sự."),
            VocabularyItem("encourage", "/ɪnˈkɜːrɪdʒ/", "verb", "khuyến khích", "My friends encourage me to keep speaking.", "Bạn bè khuyến khích tôi tiếp tục nói."),
            VocabularyItem("notice", "/ˈnoʊtɪs/", "verb", "nhận thấy", "You will notice the difference after a week.", "Bạn sẽ nhận thấy sự khác biệt sau một tuần."),
        ),
        grammar = listOf(
            GrammarPattern("I have been + V-ing", "Dùng để nói một hành động bắt đầu trong quá khứ và vẫn đang tiếp diễn.", "Không dùng động từ nguyên mẫu sau have been.", listOf("I have been practicing every morning.", "She has been reading aloud.")),
            GrammarPattern("The more..., the more...", "Dùng để diễn tả hai điều cùng tăng hoặc cùng giảm.", "Giữ cùng dạng so sánh ở hai vế.", listOf("The more you speak, the more confident you become.")),
        ),
        passage = ReadingPassage("A small daily habit", "I have been practicing English for fifteen minutes every morning. At first, I felt nervous, but a consistent routine helped me notice small improvements. My friends encourage me to speak even when I make mistakes.", "Tôi đã luyện tiếng Anh mười lăm phút mỗi sáng. Ban đầu tôi thấy lo lắng, nhưng một thói quen đều đặn giúp tôi nhận thấy những tiến bộ nhỏ. Bạn bè khuyến khích tôi nói ngay cả khi mắc lỗi.", listOf("How long does the learner practice?", "What helped the learner improve?")),
        speakingPrompts = listOf(
            SpeakingPrompt("I have been practicing English for fifteen minutes every morning.", listOf("Nối âm have_been.", "Nhấn vào practicing và fifteen.")),
            SpeakingPrompt("The more you speak, the more confident you become.", listOf("Giữ nhịp đều ở hai vế.", "Nhấn vào speak và confident.")),
        ),
    )

    override suspend fun scoreSpeech(targetText: String, transcript: String, audio: File?): PronunciationFeedback =
        PronunciationFeedback(82, 78, listOf("Hãy phát âm rõ âm cuối trong confident."), listOf("Đọc chậm hơn một chút và nhấn vào từ khóa."))
}

class FirebaseLessonGenerator : LessonGenerator {
    private val model: GenerativeModel by lazy {
        FirebaseAI.getInstance(FirebaseApp.getInstance(), GenerativeBackend.googleAI()).generativeModel(BuildConfig.GEMINI_MODEL)
    }

    override suspend fun generateLesson(profile: LearnerProfile, topic: String): LessonPayload = withContext(Dispatchers.IO) {
        val prompt = """
            You are an English teacher for a Vietnamese learner. Return ONLY valid JSON matching this shape:
            {"title":"","topic":"","cefr":"","vocabulary":[{"term":"","ipa":"","partOfSpeech":"","meaningVi":"","exampleEn":"","exampleVi":""}],"grammar":[{"pattern":"","explanationVi":"","commonMistake":"","examples":[""]}],"passage":{"title":"","textEn":"","translationVi":"","questions":[""]},"speakingPrompts":[{"targetText":"","pronunciationTips":[""]}]}
            Generate 5 useful vocabulary items, 2 grammar patterns, one 90-120 word passage and 2 speaking prompts.
            Learner level: ${profile.level}. Goal: ${profile.goal}. Interests: ${profile.interests}. Accent: ${profile.accent}. Topic: $topic.
            Keep English natural, Vietnamese explanations concise, and content appropriate for the CEFR level.
        """.trimIndent()
        parseLesson(model.generateContent(prompt).text ?: error("Gemini returned an empty lesson"))
    }

    override suspend fun scoreSpeech(targetText: String, transcript: String, audio: File?): PronunciationFeedback = withContext(Dispatchers.IO) {
        val audioBytes = audio?.takeIf(File::exists)?.readBytes()
        val prompt = content {
            text("""
                Evaluate this English learner's pronunciation. Target sentence: "$targetText". Speech recognizer transcript: "$transcript".
                Return ONLY JSON: {"pronunciation":0,"fluency":0,"issues":[""],"tips":[""]}.
                Scores must be integers 0-100. Give practical Vietnamese tips. This is an approximate AI estimate, not phoneme certification.
            """.trimIndent())
            if (audioBytes != null) inlineData(audioBytes, "audio/mp4")
        }
        parseFeedback(model.generateContent(prompt).text ?: error("Gemini returned an empty score"))
    }
}

fun parseLesson(raw: String): LessonPayload = json.decodeFromString(extractJson(raw))
fun parseFeedback(raw: String): PronunciationFeedback = json.decodeFromString(extractJson(raw))

private fun extractJson(raw: String): String {
    val start = raw.indexOf('{')
    val end = raw.lastIndexOf('}')
    require(start >= 0 && end > start) { "AI response did not contain JSON" }
    return raw.substring(start, end + 1)
}

class LocalLessonRepository(
    private val dao: LearnerDao,
    private val generator: LessonGenerator,
) {
    val lessons = dao.lessons()
    val speakingAttempts = dao.speakingAttempts()
    val vocabularyCount = dao.vocabularyCount()
    val dueCount = dao.dueCount(System.currentTimeMillis())

    suspend fun profile(): LearnerProfile? = dao.profile()?.toModel()
    suspend fun saveProfile(profile: LearnerProfile) = dao.saveProfile(profile.toEntity())

    suspend fun generateLesson(profile: LearnerProfile, topic: String): GeneratedLesson {
        val payload = generator.generateLesson(profile, topic)
        val entity = LessonEntity(0, System.currentTimeMillis(), payload.title, payload.topic, payload.cefr, json.encodeToString(payload))
        val id = dao.insertLesson(entity)
        payload.vocabulary.forEach { dao.saveVocabulary(VocabularyProgressEntity(it.term, 0, System.currentTimeMillis(), 0)) }
        return GeneratedLesson(id, entity.createdAt, payload)
    }

    suspend fun latestLesson(): GeneratedLesson? = dao.latestLesson()?.let { GeneratedLesson(it.id, it.createdAt, json.decodeFromString(it.payloadJson)) }

    suspend fun scoreSpeech(targetText: String, transcript: String, audio: File?): SpeakingResult {
        val content = contentAccuracy(targetText, transcript)
        val feedback = runCatching { generator.scoreSpeech(targetText, transcript, audio) }.getOrNull()
        val pronunciation = feedback?.pronunciation ?: content
        val fluency = feedback?.fluency ?: content
        val result = SpeakingResult(content, pronunciation, fluency, combinedScore(content, pronunciation, fluency), transcript, feedback, feedback != null)
        dao.insertSpeakingAttempt(SpeakingAttemptEntity(0, System.currentTimeMillis(), targetText, transcript, content, pronunciation, fluency, result.overall, feedback?.let(json::encodeToString) ?: "{}"))
        return result
    }
}

private fun LearnerProfileEntity.toModel() = LearnerProfile(id, level, goal, interests, sessionMinutes, accent, completedOnboarding)
private fun LearnerProfile.toEntity() = LearnerProfileEntity(id, level, goal, interests, sessionMinutes, accent, completedOnboarding)
