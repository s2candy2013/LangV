package com.linusv.englishcoach.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.linusv.englishcoach.BuildConfig
import com.linusv.englishcoach.domain.combinedScore
import com.linusv.englishcoach.domain.contentAccuracy
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface LessonGenerator {
    suspend fun generateLesson(profile: LearnerProfile, topic: String): LessonPayload
    suspend fun generateLessonFromImage(profile: LearnerProfile, imageBytes: ByteArray, mimeType: String): LessonPayload =
        generateLesson(profile, "vocabulary from the selected image")
    suspend fun generateFlashcardsFromImage(profile: LearnerProfile, imageBytes: ByteArray, mimeType: String): FlashcardSet =
        FlashcardSet("Flashcards", generateLessonFromImage(profile, imageBytes, mimeType).vocabulary)
    suspend fun gradeExerciseFromImage(profile: LearnerProfile, imageBytes: ByteArray, mimeType: String): ExerciseFeedback =
        ExerciseFeedback("Bài tập", 0, emptyList(), listOf("Chưa thể chấm ảnh ở chế độ demo."), emptyList(), listOf("Hãy thử lại với ảnh rõ hơn."))
    suspend fun generateConversation(profile: LearnerProfile, topic: String): ConversationPayload =
        ConversationPayload("Hội thoại", topic, emptyList(), emptyList())
    suspend fun synthesizeSpeech(profile: LearnerProfile, text: String): ByteArray? = null
    suspend fun scoreSpeech(profile: LearnerProfile, targetText: String, transcript: String, audio: File?): PronunciationFeedback
    fun currentModelName(): String? = null
    fun selectModel(modelName: String) = Unit
    fun modelHealth(modelNames: List<String>): List<ModelHealth> = modelNames.map { ModelHealth(it) }
}

class ModelHealthStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("ai_model_health", Context.MODE_PRIVATE)

    fun recordSuccess(modelName: String, latencyMs: Long) {
        val now = System.currentTimeMillis()
        val key = key(modelName)
        val streak = if (now - preferences.getLong("${key}_lastFailureAt", 0) > WINDOW_MS) 0 else 0
        preferences.edit()
            .putInt("${key}_requests", preferences.getInt("${key}_requests", 0) + 1)
            .putInt("${key}_successes", preferences.getInt("${key}_successes", 0) + 1)
            .putLong("${key}_latency", latencyMs)
            .putLong("${key}_lastUsedAt", now)
            .putInt("${key}_streak", streak)
            .apply()
    }

    fun recordFailure(modelName: String, failure: Throwable, latencyMs: Long) {
        val now = System.currentTimeMillis()
        val key = key(modelName)
        val capacityError = isCapacityError(failure)
        val oldStreak = preferences.getInt("${key}_streak", 0)
        val streak = if (capacityError && now - preferences.getLong("${key}_lastFailureAt", 0) <= WINDOW_MS) oldStreak + 1 else if (capacityError) 1 else 0
        preferences.edit()
            .putInt("${key}_requests", preferences.getInt("${key}_requests", 0) + 1)
            .putInt("${key}_throttled", preferences.getInt("${key}_throttled", 0) + if (capacityError) 1 else 0)
            .putLong("${key}_latency", latencyMs)
            .putLong("${key}_lastUsedAt", now)
            .putLong("${key}_lastFailureAt", now)
            .putString("${key}_lastFailure", failure.message ?: failure::class.simpleName.orEmpty())
            .putInt("${key}_streak", streak)
            .apply()
    }

    fun snapshot(modelNames: List<String>): List<ModelHealth> = modelNames.distinct().map { modelName ->
        val key = key(modelName)
        val now = System.currentTimeMillis()
        val requests = preferences.getInt("${key}_requests", 0)
        val lastFailureAt = preferences.getLong("${key}_lastFailureAt", 0)
        val streak = if (now - lastFailureAt <= WINDOW_MS) preferences.getInt("${key}_streak", 0) else 0
        val throttled = preferences.getInt("${key}_throttled", 0)
        val status = when {
            requests == 0 -> "Chưa có dữ liệu"
            streak >= 2 -> "Quá tải"
            streak == 1 || (throttled > 0 && now - lastFailureAt <= WINDOW_MS) -> "Cảnh báo"
            else -> "Ổn định"
        }
        ModelHealth(
            modelName = modelName,
            requestCount = requests,
            successCount = preferences.getInt("${key}_successes", 0),
            throttledCount = throttled,
            lastLatencyMs = preferences.getLong("${key}_latency", 0).takeIf { it > 0 },
            lastFailure = preferences.getString("${key}_lastFailure", null),
            lastUsedAt = preferences.getLong("${key}_lastUsedAt", 0).takeIf { it > 0 },
            status = status,
        )
    }

    private fun key(modelName: String) = "model_${modelName.replace(Regex("[^A-Za-z0-9_.-]"), "_")}"

    private fun isCapacityError(failure: Throwable): Boolean = generateSequence(failure) { it.cause }
        .joinToString(" ") { it.message.orEmpty() }
        .lowercase()
        .let { message -> listOf("high demand", "resource exhausted", "too many requests", "rate limit", "429", "503").any(message::contains) }

    companion object { private const val WINDOW_MS = 15 * 60 * 1000L }
}

private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

class DemoLessonGenerator : LessonGenerator {
    override suspend fun generateLesson(profile: LearnerProfile, topic: String): LessonPayload = when (profile.languageCode) {
        "zh-CN", "zh-TW" -> chineseLesson(profile, topic)
        "ja" -> japaneseLesson(profile, topic)
        "ko" -> koreanLesson(profile, topic)
        else -> englishLesson(profile, topic)
    }

    override suspend fun scoreSpeech(profile: LearnerProfile, targetText: String, transcript: String, audio: File?): PronunciationFeedback =
        PronunciationFeedback(82, 78, listOf("Có một vài âm cần rõ hơn."), listOf("Đọc chậm hơn một chút và nhấn đúng từ khóa."))
}

private fun englishLesson(profile: LearnerProfile, topic: String) = LessonPayload(
    "A practical lesson about $topic", topic, profile.languageCode, profile.level,
    listOf(
        VocabularyItem("consistent", "/kənˈsɪstənt/", null, "adjective", "đều đặn, nhất quán", "A consistent routine makes learning easier.", "Một thói quen đều đặn giúp việc học dễ hơn."),
        VocabularyItem("progress", "/ˈprɑːɡres/", null, "noun", "tiến bộ", "Small steps create real progress.", "Những bước nhỏ tạo ra tiến bộ thật sự."),
        VocabularyItem("encourage", "/ɪnˈkɜːrɪdʒ/", null, "verb", "khuyến khích", "My friends encourage me to keep speaking.", "Bạn bè khuyến khích tôi tiếp tục nói."),
    ),
    listOf(GrammarPattern("I have been + V-ing", "Diễn tả hành động bắt đầu trong quá khứ và vẫn tiếp diễn.", "Không dùng động từ nguyên mẫu sau have been.", listOf("I have been practicing every morning."))),
    ReadingPassage("A small daily habit", "I practice for fifteen minutes every morning. A consistent routine helps me notice small improvements.", "Tôi luyện tập mười lăm phút mỗi sáng. Một thói quen đều đặn giúp tôi nhận ra những tiến bộ nhỏ.", listOf("How long does the learner practice?")),
    listOf(SpeakingPrompt("I practice for fifteen minutes every morning.", listOf("Nhấn vào practice và fifteen."))),
)

private fun chineseLesson(profile: LearnerProfile, topic: String): LessonPayload {
    val traditional = profile.languageCode == "zh-TW"
    val hello = if (traditional) "你好，很高興認識你。" else "你好，很高兴认识你。"
    val passage = if (traditional) "我每天早上學中文十五分鐘。每天練習讓我進步。" else "我每天早上学中文十五分钟。每天练习让我进步。"
    return LessonPayload(
        if (traditional) "中文日常練習" else "中文日常练习", topic, profile.languageCode, profile.level,
        listOf(
            VocabularyItem("你好", "nǐ hǎo", "nǐ hǎo", "cụm từ", "xin chào", hello, "Xin chào, rất vui được gặp bạn."),
            VocabularyItem(if (traditional) "學習" else "学习", "xué xí", "xuéxí", "động từ", "học tập", if (traditional) "我每天學習中文。" else "我每天学习中文。", "Tôi học tiếng Trung mỗi ngày."),
            VocabularyItem(if (traditional) "進步" else "进步", "jìn bù", "jìnbù", "động từ/danh từ", "tiến bộ", if (traditional) "我每天都在進步。" else "我每天都在进步。", "Mỗi ngày tôi đều tiến bộ."),
        ),
        listOf(GrammarPattern("每天 + động từ", "Dùng 每天 để nói một hoạt động diễn ra mỗi ngày.", "Trạng từ thời gian thường đứng trước động từ.", listOf(if (traditional) "我每天學中文。" else "我每天学中文。"))),
        ReadingPassage(if (traditional) "每天學習" else "每天学习", passage, "Tôi học tiếng Trung mười lăm phút mỗi sáng. Luyện tập mỗi ngày giúp tôi tiến bộ.", listOf(if (traditional) "他每天學習多長時間？" else "他每天学习多长时间？")),
        listOf(SpeakingPrompt(hello, listOf("Chú ý thanh 3 của nǐ và hǎo.", "Đọc liền mạch, không tách từng chữ."))),
    )
}

private fun japaneseLesson(profile: LearnerProfile, topic: String) = LessonPayload(
    "毎日の日本語", topic, profile.languageCode, profile.level,
    listOf(
        VocabularyItem("こんにちは", "こんにちは", "konnichiwa", "lời chào", "xin chào", "こんにちは。はじめまして。", "Xin chào. Rất vui được gặp bạn."),
        VocabularyItem("勉強する", "べんきょうする", "benkyō suru", "động từ", "học", "毎日、日本語を勉強します。", "Mỗi ngày tôi học tiếng Nhật."),
        VocabularyItem("少し", "すこし", "sukoshi", "phó từ", "một chút", "毎日少し練習します。", "Mỗi ngày tôi luyện tập một chút."),
    ),
    listOf(GrammarPattern("N を Vます", "Trợ từ を đánh dấu tân ngữ trực tiếp.", "Viết は nhưng thường đọc là wa khi làm trợ từ.", listOf("日本語を勉強します。"))),
    ReadingPassage("毎日の練習", "私は毎朝十五分、日本語を勉強します。毎日少しずつ練習します。", "Tôi học tiếng Nhật mười lăm phút mỗi sáng. Mỗi ngày tôi luyện tập từng chút một.", listOf("毎朝、何分勉強しますか。")),
    listOf(SpeakingPrompt("毎日、日本語を勉強します。", listOf("Giữ nguyên trường âm trong べんきょう.", "Trợ từ を đọc là o."))),
)

private fun koreanLesson(profile: LearnerProfile, topic: String) = LessonPayload(
    "매일 한국어", topic, profile.languageCode, profile.level,
    listOf(
        VocabularyItem("안녕하세요", "안녕하세요", "annyeonghaseyo", "lời chào", "xin chào", "안녕하세요. 만나서 반갑습니다.", "Xin chào. Rất vui được gặp bạn."),
        VocabularyItem("공부하다", "공부하다", "gongbuhada", "động từ", "học", "매일 한국어를 공부해요.", "Mỗi ngày tôi học tiếng Hàn."),
        VocabularyItem("연습하다", "연습하다", "yeonseuphada", "động từ", "luyện tập", "아침에 발음을 연습해요.", "Tôi luyện phát âm vào buổi sáng."),
    ),
    listOf(GrammarPattern("N을/를 V", "을/를 đánh dấu tân ngữ; dùng 를 sau nguyên âm và 을 sau phụ âm.", "Không dùng cùng lúc 을 và 를.", listOf("한국어를 공부해요."))),
    ReadingPassage("매일 연습", "저는 매일 아침 십오 분 동안 한국어를 공부해요. 조금씩 연습해서 자신감이 생겨요.", "Tôi học tiếng Hàn mười lăm phút mỗi sáng. Luyện tập từng chút giúp tôi tự tin hơn.", listOf("매일 몇 분 동안 공부해요?")),
    listOf(SpeakingPrompt("매일 한국어를 공부해요.", listOf("Nối tự nhiên 한국어를.", "Không nuốt phụ âm cuối trong 매일."))),
)

class FirebaseLessonGenerator(context: Context) : LessonGenerator {
    private val demoFallback = DemoLessonGenerator()
    private val healthStore = ModelHealthStore(context)
    private val preferences = context.applicationContext.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
    private var selectedModelName: String
        get() = preferences.getString("model_name", BuildConfig.GEMINI_MODEL) ?: BuildConfig.GEMINI_MODEL
        set(value) { preferences.edit().putString("model_name", value).apply() }

    private fun model(): GenerativeModel = FirebaseAI.getInstance(FirebaseApp.getInstance(), GenerativeBackend.googleAI()).generativeModel(selectedModelName)

    override fun currentModelName(): String = selectedModelName

    override fun selectModel(modelName: String) {
        if (modelName.isNotBlank()) selectedModelName = modelName.trim()
    }

    override fun modelHealth(modelNames: List<String>): List<ModelHealth> = healthStore.snapshot(modelNames)

    private suspend fun <T> tracked(block: suspend () -> T): T {
        val startedAt = System.currentTimeMillis()
        return try {
            block().also { healthStore.recordSuccess(selectedModelName, System.currentTimeMillis() - startedAt) }
        } catch (failure: Throwable) {
            healthStore.recordFailure(selectedModelName, failure, System.currentTimeMillis() - startedAt)
            throw failure
        }
    }

    override suspend fun generateLessonFromImage(profile: LearnerProfile, imageBytes: ByteArray, mimeType: String): LessonPayload = withContext(Dispatchers.IO) {
        val language = SupportedLanguages.find(profile.languageCode)
        val prompt = """
            Bạn là giáo viên ${language.displayNameVi} cho người Việt. Hãy đọc nội dung chữ trong ảnh và tạo một bài học phù hợp cấp độ ${profile.level}.
            Nếu ảnh là trang sách, hãy chọn chủ đề và từ/cụm từ quan trọng nhất; nếu là flashcard, hãy mở rộng thành bài học thực hành.
            Chỉ trả về JSON hợp lệ theo đúng cấu trúc:
            {"title":"","topic":"","languageCode":"${profile.languageCode}","level":"","vocabulary":[{"term":"","pronunciation":"","romanization":"","partOfSpeech":"","meaningVi":"","exampleTarget":"","exampleVi":""}],"grammar":[{"pattern":"","explanationVi":"","commonMistake":"","examples":[""]}],"passage":{"title":"","textTarget":"","translationVi":"","questions":[""]},"speakingPrompts":[{"targetText":"","pronunciationTips":[""]}]}
            Tạo 5 từ/cụm từ, 2 cấu trúc, một bài đọc và 2 câu luyện nói. Mọi giải thích và bản dịch bằng tiếng Việt.
            Nếu ảnh không có chữ rõ ràng, hãy tạo bài học ngắn dựa trên chủ đề nhìn thấy trong ảnh.
        """.trimIndent()
        parseLesson(tracked {
            model().generateContent(content {
                text(prompt)
                inlineData(imageBytes, mimeType)
            })
        }.text ?: error("Gemini không trả về bài học từ ảnh"))
    }

    override suspend fun generateFlashcardsFromImage(profile: LearnerProfile, imageBytes: ByteArray, mimeType: String): FlashcardSet = withContext(Dispatchers.IO) {
        val language = SupportedLanguages.find(profile.languageCode)
        val prompt = """
            Đọc chữ trong ảnh và tạo bộ flashcard học ${language.displayNameVi} cho người Việt cấp độ ${profile.level}.
            Chỉ trả về JSON: {"title":"","cards":[{"term":"","pronunciation":"","romanization":"","partOfSpeech":"","meaningVi":"","exampleTarget":"","exampleVi":""}]}.
            Tạo 8 flashcard quan trọng nhất. Nghĩa, giải thích và ví dụ tiếng Việt phải bằng tiếng Việt; câu ví dụ dùng ${language.displayNameVi}.
        """.trimIndent()
        parseFlashcards(tracked { model().generateContent(content { text(prompt); inlineData(imageBytes, mimeType) }) }.text.orEmpty())
    }

    override suspend fun gradeExerciseFromImage(profile: LearnerProfile, imageBytes: ByteArray, mimeType: String): ExerciseFeedback = withContext(Dispatchers.IO) {
        val language = SupportedLanguages.find(profile.languageCode)
        val prompt = """
            Bạn là giáo viên ${language.displayNameVi}. Hãy đọc ảnh bài tập của học viên và chấm bài ở cấp độ ${profile.level}.
            Nếu ảnh có đáp án viết tay, hãy cố gắng đọc; nếu không đủ rõ, nêu chính xác phần không đọc được.
            Chỉ trả về JSON: {"title":"","score":0,"answers":[""],"mistakes":[""],"explanations":[""],"nextSteps":[""]}.
            Điểm 0-100. Tất cả nội dung bằng tiếng Việt.
        """.trimIndent()
        parseExerciseFeedback(tracked { model().generateContent(content { text(prompt); inlineData(imageBytes, mimeType) }) }.text.orEmpty())
    }

    override suspend fun generateConversation(profile: LearnerProfile, topic: String): ConversationPayload = withContext(Dispatchers.IO) {
        val language = SupportedLanguages.find(profile.languageCode)
        val prompt = """
            Tạo hội thoại thực tế bằng ${language.displayNameVi} cho người Việt cấp độ ${profile.level}, chủ đề: $topic.
            Chỉ trả về JSON: {"title":"","topic":"","lines":[{"speaker":"A","targetText":"","translationVi":""}],"tips":[""]}.
            Tạo 8-12 lượt thoại ngắn. Bản dịch và mẹo bằng tiếng Việt. Câu thoại phù hợp để luyện nói.
        """.trimIndent()
        parseConversation(tracked { model().generateContent(prompt) }.text.orEmpty())
    }

    @OptIn(PublicPreviewAPI::class)
    override suspend fun synthesizeSpeech(profile: LearnerProfile, text: String): ByteArray? = withContext(Dispatchers.IO) {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.AUDIO)
            speechConfig = SpeechConfig(Voice("Kore"), profile.voiceTag)
        }
        val ttsModel = FirebaseAI.getInstance(FirebaseApp.getInstance(), GenerativeBackend.googleAI())
            .generativeModel("gemini-3.1-flash-tts-preview", config)
        val response = ttsModel.generateContent("Đọc tự nhiên, rõ ràng và phù hợp cho người học: $text")
        response.candidates.firstOrNull()?.content?.parts?.firstOrNull { it is InlineDataPart }?.let { (it as InlineDataPart).inlineData }
    }

    override suspend fun generateLesson(profile: LearnerProfile, topic: String): LessonPayload = withContext(Dispatchers.IO) {
        val language = SupportedLanguages.find(profile.languageCode)
        val prompt = """
            Bạn là giáo viên ${language.displayNameVi} cho người Việt. Chỉ trả về JSON hợp lệ theo đúng cấu trúc:
            {"title":"","topic":"","languageCode":"${profile.languageCode}","level":"","vocabulary":[{"term":"","pronunciation":"","romanization":"","partOfSpeech":"","meaningVi":"","exampleTarget":"","exampleVi":""}],"grammar":[{"pattern":"","explanationVi":"","commonMistake":"","examples":[""]}],"passage":{"title":"","textTarget":"","translationVi":"","questions":[""]},"speakingPrompts":[{"targetText":"","pronunciationTips":[""]}]}
            Tạo 5 từ/cụm từ, 2 cấu trúc, một bài đọc phù hợp cấp độ và 2 câu luyện nói.
            Mọi lời giải thích, nghĩa, bản dịch và mẹo phải bằng tiếng Việt; ví dụ và câu hỏi đọc hiểu dùng ${language.displayNameVi}.
            Với tiếng Trung ghi pinyin có dấu thanh; tiếng Nhật ghi kana và romaji; tiếng Hàn ghi cách đọc Hangul và romanization; tiếng Anh ghi IPA.
            Cấp độ: ${profile.level}. Mục tiêu: ${profile.goal}. Sở thích: ${profile.interests}. Chủ đề: $topic.
        """.trimIndent()
        runCatching {
            var lastFailure: Throwable? = null
            repeat(3) { attempt ->
                try {
                    return@runCatching parseLesson(tracked { model().generateContent(prompt) }.text ?: error("Gemini không trả về bài học"))
                } catch (failure: Throwable) {
                    lastFailure = failure
                    if (!isCapacityError(failure) || attempt == 2) throw failure
                    delay(700L * (attempt + 1))
                }
            }
            throw lastFailure ?: error("Không thể tạo bài học")
        }.getOrElse { failure ->
            if (isCapacityError(failure)) {
                // Keep lesson creation usable while Gemini is rate-limited or overloaded.
                demoFallback.generateLesson(profile, topic)
            } else {
                throw failure
            }
        }
    }

    private fun isCapacityError(failure: Throwable): Boolean {
        val message = generateSequence(failure) { it.cause }
            .joinToString(" ") { it.message.orEmpty() }
            .lowercase()
        return listOf("high demand", "resource exhausted", "too many requests", "rate limit", "429")
            .any(message::contains)
    }

    override suspend fun scoreSpeech(profile: LearnerProfile, targetText: String, transcript: String, audio: File?): PronunciationFeedback = withContext(Dispatchers.IO) {
        val language = SupportedLanguages.find(profile.languageCode)
        val audioBytes = audio?.takeIf(File::exists)?.readBytes()
        val prompt = content {
            text("""
                Đánh giá phát âm ${language.displayNameVi} của người Việt. Câu mẫu: "$targetText". Transcript: "$transcript".
                Chỉ trả về JSON: {"pronunciation":0,"fluency":0,"issues":[""],"tips":[""]}.
                Điểm là số nguyên 0-100. Giải thích và mẹo ngắn gọn bằng tiếng Việt. Đây là ước lượng AI, không phải chứng nhận ngữ âm.
            """.trimIndent())
            if (audioBytes != null) inlineData(audioBytes, "audio/mp4")
        }
        parseFeedback(tracked { model().generateContent(prompt) }.text ?: error("Gemini không trả về điểm"))
    }
}

fun parseLesson(raw: String): LessonPayload {
    val source = extractJson(raw)
    return runCatching { json.decodeFromString<LessonPayload>(source) }.getOrElse {
        val old = json.decodeFromString<LegacyLessonPayload>(source)
        LessonPayload(old.title, old.topic, "en", old.cefr, old.vocabulary.map { item ->
            VocabularyItem(item.term, item.ipa, null, item.partOfSpeech, item.meaningVi, item.exampleEn, item.exampleVi)
        }, old.grammar, ReadingPassage(old.passage.title, old.passage.textEn, old.passage.translationVi, old.passage.questions), old.speakingPrompts)
    }
}

fun parseFeedback(raw: String): PronunciationFeedback = json.decodeFromString(extractJson(raw))

fun parseFlashcards(raw: String): FlashcardSet = json.decodeFromString(extractJson(raw))

fun parseExerciseFeedback(raw: String): ExerciseFeedback = json.decodeFromString(extractJson(raw))

fun parseConversation(raw: String): ConversationPayload = json.decodeFromString(extractJson(raw))

private fun extractJson(raw: String): String {
    val start = raw.indexOf('{')
    val end = raw.lastIndexOf('}')
    require(start >= 0 && end > start) { "Phản hồi AI không chứa JSON" }
    return raw.substring(start, end + 1)
}

@Serializable
private data class LegacyVocabularyItem(val term: String, val ipa: String, val partOfSpeech: String, val meaningVi: String, val exampleEn: String, val exampleVi: String)
@Serializable
private data class LegacyReadingPassage(val title: String, val textEn: String, val translationVi: String, val questions: List<String>)
@Serializable
private data class LegacyLessonPayload(val title: String, val topic: String, val cefr: String, val vocabulary: List<LegacyVocabularyItem>, val grammar: List<GrammarPattern>, val passage: LegacyReadingPassage, val speakingPrompts: List<SpeakingPrompt>)

@OptIn(ExperimentalCoroutinesApi::class)
class LocalLessonRepository(private val dao: LearnerDao, private val generator: LessonGenerator) {
    private val activeLanguage = MutableStateFlow("en")
    val lessons = activeLanguage.flatMapLatest(dao::lessons)
    val speakingAttempts = activeLanguage.flatMapLatest(dao::speakingAttempts)
    val vocabularyCount = activeLanguage.flatMapLatest(dao::vocabularyCount)
    val dueCount = activeLanguage.flatMapLatest { dao.dueCount(it, System.currentTimeMillis()) }

    fun currentModelName(): String? = generator.currentModelName()

    fun selectModel(modelName: String) = generator.selectModel(modelName)

    fun modelHealth(modelNames: List<String>): List<ModelHealth> = generator.modelHealth(modelNames)

    suspend fun profile(): LearnerProfile? = dao.profile()?.toModel()?.also { activeLanguage.value = it.languageCode }

    suspend fun saveProfile(profile: LearnerProfile) {
        dao.saveProfile(profile.toEntity())
        activeLanguage.value = profile.languageCode
    }

    suspend fun switchLanguage(languageCode: String, current: LearnerProfile): Pair<LearnerProfile, GeneratedLesson?> {
        val language = SupportedLanguages.find(languageCode)
        val profile = dao.profile(languageCode)?.toModel() ?: current.copy(
            languageCode = language.code,
            level = language.defaultLevel,
            voiceTag = language.voices.first().tag,
            completedOnboarding = true,
        )
        saveProfile(profile)
        return profile to latestLesson(languageCode)
    }

    suspend fun generateLesson(profile: LearnerProfile, topic: String): GeneratedLesson {
        val payload = generator.generateLesson(profile, topic)
        val entity = LessonEntity(0, System.currentTimeMillis(), payload.title, payload.topic, payload.level, json.encodeToString(payload), profile.languageCode)
        val id = dao.insertLesson(entity)
        payload.vocabulary.forEach { dao.saveVocabulary(VocabularyProgressEntity(profile.languageCode, it.term, 0, System.currentTimeMillis(), 0)) }
        return GeneratedLesson(id, entity.createdAt, payload)
    }

    suspend fun generateLessonFromImage(profile: LearnerProfile, imageBytes: ByteArray, mimeType: String): GeneratedLesson {
        val payload = generator.generateLessonFromImage(profile, imageBytes, mimeType)
        val entity = LessonEntity(0, System.currentTimeMillis(), payload.title, payload.topic, payload.level, json.encodeToString(payload), profile.languageCode)
        val id = dao.insertLesson(entity)
        payload.vocabulary.forEach { dao.saveVocabulary(VocabularyProgressEntity(profile.languageCode, it.term, 0, System.currentTimeMillis(), 0)) }
        return GeneratedLesson(id, entity.createdAt, payload)
    }

    suspend fun generateFlashcardsFromImage(profile: LearnerProfile, imageBytes: ByteArray, mimeType: String): FlashcardSet =
        generator.generateFlashcardsFromImage(profile, imageBytes, mimeType)

    suspend fun gradeExerciseFromImage(profile: LearnerProfile, imageBytes: ByteArray, mimeType: String): ExerciseFeedback =
        generator.gradeExerciseFromImage(profile, imageBytes, mimeType)

    suspend fun generateConversation(profile: LearnerProfile, topic: String): ConversationPayload =
        generator.generateConversation(profile, topic)

    suspend fun synthesizeSpeech(profile: LearnerProfile, text: String): ByteArray? = generator.synthesizeSpeech(profile, text)

    suspend fun latestLesson(languageCode: String = activeLanguage.value): GeneratedLesson? = dao.latestLesson(languageCode)?.let {
        GeneratedLesson(it.id, it.createdAt, parseLesson(it.payloadJson))
    }

    suspend fun scoreSpeech(profile: LearnerProfile, targetText: String, transcript: String, audio: File?): SpeakingResult {
        val content = contentAccuracy(targetText, transcript, profile.languageCode)
        val feedback = runCatching { generator.scoreSpeech(profile, targetText, transcript, audio) }.getOrNull()
        val pronunciation = feedback?.pronunciation ?: content
        val fluency = feedback?.fluency ?: content
        val result = SpeakingResult(content, pronunciation, fluency, combinedScore(content, pronunciation, fluency), transcript, feedback, feedback != null)
        dao.insertSpeakingAttempt(SpeakingAttemptEntity(0, System.currentTimeMillis(), targetText, transcript, content, pronunciation, fluency, result.overall, feedback?.let(json::encodeToString) ?: "{}", profile.languageCode))
        return result
    }
}

private fun LearnerProfileEntity.toModel() = LearnerProfile(languageCode, level, goal, interests, sessionMinutes, voiceTag, completedOnboarding)
private fun LearnerProfile.toEntity() = LearnerProfileEntity(languageCode, level, goal, interests, sessionMinutes, voiceTag, completedOnboarding, true)
