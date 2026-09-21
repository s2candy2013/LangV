package com.linusv.englishcoach.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linusv.englishcoach.auth.AuthRepository
import com.linusv.englishcoach.auth.SignedInUser
import com.linusv.englishcoach.data.GeneratedLesson
import com.linusv.englishcoach.data.LearnerProfile
import com.linusv.englishcoach.data.LocalLessonRepository
import com.linusv.englishcoach.data.SpeakingResult
import com.linusv.englishcoach.data.SupportedLanguages
import com.linusv.englishcoach.data.SupportedGeminiModels
import com.linusv.englishcoach.data.ModelHealth
import com.linusv.englishcoach.data.ConversationPayload
import com.linusv.englishcoach.data.ExerciseFeedback
import com.linusv.englishcoach.data.FlashcardSet
import java.io.File
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppUiState(
    val user: SignedInUser? = null,
    val profile: LearnerProfile? = null,
    val lesson: GeneratedLesson? = null,
    val speakingResult: SpeakingResult? = null,
    val isBusy: Boolean = false,
    val error: String? = null,
    val modelName: String = "Demo Mode",
    val modelHealth: List<ModelHealth> = emptyList(),
    val flashcards: FlashcardSet? = null,
    val conversation: ConversationPayload? = null,
    val exerciseFeedback: ExerciseFeedback? = null,
)

class MainViewModel(
    private val auth: AuthRepository,
    private val repository: LocalLessonRepository,
) : ViewModel() {
    private val initialModelName = repository.currentModelName() ?: "Demo Mode"
    private val _state = MutableStateFlow(AppUiState(
        user = auth.currentUser(),
        modelName = initialModelName,
        modelHealth = repository.modelHealth((SupportedGeminiModels.generalUse + initialModelName).distinct()),
    ))
    val state: StateFlow<AppUiState> = _state.asStateFlow()
    val lessons = repository.lessons
    val speakingAttempts = repository.speakingAttempts
    val vocabularyCount = repository.vocabularyCount
    val dueCount = repository.dueCount
    val dueVocabulary = repository.dueVocabulary
    val vocabulary = repository.vocabulary
    val studySessions = repository.studySessions
    val totalStudyMinutes = repository.totalStudyMinutes
    val contentHistory = repository.contentHistory

    init {
        if (_state.value.user != null) loadProfile()
    }

    fun signIn(activity: Activity) {
        viewModelScope.launch {
            setBusy(true)
            auth.signIn(activity).onSuccess { user ->
                _state.value = _state.value.copy(user = user, error = null)
                loadProfile()
            }.onFailure { _state.value = _state.value.copy(error = it.message ?: "Đăng nhập thất bại") }
            setBusy(false)
        }
    }

    fun signOut() {
        auth.signOut()
        _state.value = AppUiState()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val profile = repository.profile()
            val lesson = repository.latestLesson()
            _state.value = _state.value.copy(profile = profile, lesson = lesson)
        }
    }

    fun saveProfile(profile: LearnerProfile) {
        viewModelScope.launch {
            setBusy(true)
            repository.saveProfile(profile)
            _state.value = _state.value.copy(profile = profile, error = null)
            setBusy(false)
        }
    }

    fun generateLesson(topic: String, onSuccess: () -> Unit = {}) {
        val profile = _state.value.profile ?: return
        viewModelScope.launch {
            setBusy(true)
            runCatching { repository.generateLesson(profile, topic.ifBlank { "daily life" }) }
                .onSuccess {
                    _state.value = _state.value.copy(lesson = it, error = null)
                    onSuccess()
                }
                .onFailure { _state.value = _state.value.copy(error = "Không tạo được bài: ${it.message ?: "hãy thử lại"}") }
            refreshModelHealth()
            setBusy(false)
        }
    }

    fun generateLessonFromImage(imageBytes: ByteArray, mimeType: String, onSuccess: () -> Unit = {}) {
        val profile = _state.value.profile ?: return
        viewModelScope.launch {
            setBusy(true)
            runCatching { repository.generateLessonFromImage(profile, imageBytes, mimeType) }
                .onSuccess {
                    _state.value = _state.value.copy(lesson = it, error = null)
                    onSuccess()
                }
                .onFailure { _state.value = _state.value.copy(error = "Không đọc được ảnh: ${it.message ?: "hãy thử lại"}") }
            refreshModelHealth()
            setBusy(false)
        }
    }

    fun generateFlashcardsFromImage(imageBytes: ByteArray, mimeType: String, onSuccess: () -> Unit = {}) {
        val profile = _state.value.profile ?: return
        viewModelScope.launch {
            setBusy(true)
            runCatching { repository.generateFlashcardsFromImage(profile, imageBytes, mimeType) }
                .onSuccess { _state.value = _state.value.copy(flashcards = it, error = null); onSuccess() }
                .onFailure { _state.value = _state.value.copy(error = "Không tạo được flashcard: ${it.message ?: "hãy thử lại"}") }
            refreshModelHealth()
            setBusy(false)
        }
    }

    fun gradeExerciseFromImage(imageBytes: ByteArray, mimeType: String, onSuccess: () -> Unit = {}) {
        val profile = _state.value.profile ?: return
        viewModelScope.launch {
            setBusy(true)
            runCatching { repository.gradeExerciseFromImage(profile, imageBytes, mimeType) }
                .onSuccess { _state.value = _state.value.copy(exerciseFeedback = it, error = null); onSuccess() }
                .onFailure { _state.value = _state.value.copy(error = "Không chấm được bài: ${it.message ?: "hãy thử lại"}") }
            refreshModelHealth()
            setBusy(false)
        }
    }

    fun generateConversation(topic: String, onSuccess: () -> Unit = {}) {
        val profile = _state.value.profile ?: return
        viewModelScope.launch {
            setBusy(true)
            runCatching { repository.generateConversation(profile, topic.ifBlank { "daily conversation" }) }
                .onSuccess { _state.value = _state.value.copy(conversation = it, error = null); onSuccess() }
                .onFailure { _state.value = _state.value.copy(error = "Không tạo được hội thoại: ${it.message ?: "hãy thử lại"}") }
            refreshModelHealth()
            setBusy(false)
        }
    }

    fun synthesizeSpeech(text: String, onAudio: (ByteArray) -> Unit, onFailure: (Throwable) -> Unit = {}) {
        val profile = _state.value.profile ?: return
        viewModelScope.launch {
            runCatching { repository.synthesizeSpeech(profile, text) }
                .onSuccess { audio -> if (audio != null) onAudio(audio) else onFailure(IllegalStateException("Gemini TTS không trả về audio")) }
                .onFailure(onFailure)
        }
    }

    fun switchLevel(level: String) {
        val current = _state.value.profile ?: return
        val language = SupportedLanguages.find(current.languageCode)
        if (level !in language.levels || current.level == level) return
        viewModelScope.launch {
            setBusy(true)
            runCatching {
                val updated = current.copy(level = level)
                repository.saveProfile(updated)
                updated
            }.onSuccess { updated ->
                // The existing lesson was generated for the old level.
                _state.value = _state.value.copy(profile = updated, lesson = null, speakingResult = null, error = null)
            }.onFailure { _state.value = _state.value.copy(error = "Không đổi được cấp độ: ${it.message ?: "hãy thử lại"}") }
            setBusy(false)
        }
    }

    fun switchModel(modelName: String) {
        if (modelName.isBlank() || modelName == _state.value.modelName) return
        repository.selectModel(modelName)
        _state.value = _state.value.copy(modelName = modelName, error = null)
        refreshModelHealth()
    }

    fun switchLanguage(languageCode: String) {
        val current = _state.value.profile ?: return
        if (current.languageCode == languageCode) return
        viewModelScope.launch {
            setBusy(true)
            runCatching { repository.switchLanguage(languageCode, current) }
                .onSuccess { (profile, lesson) -> _state.value = _state.value.copy(profile = profile, lesson = lesson, speakingResult = null, error = null) }
                .onFailure { _state.value = _state.value.copy(error = "Không chuyển được ngôn ngữ: ${it.message ?: "hãy thử lại"}") }
            setBusy(false)
        }
    }

    fun loadLatestLesson() {
        viewModelScope.launch { _state.value = _state.value.copy(lesson = repository.latestLesson()) }
    }

    fun openLesson(id: Long, onLoaded: () -> Unit = {}) {
        viewModelScope.launch {
            repository.lesson(id)?.let { lesson ->
                _state.value = _state.value.copy(lesson = lesson, speakingResult = null)
                onLoaded()
            }
        }
    }

    fun scoreSpeech(targetText: String, transcript: String, audio: File?) {
        val profile = _state.value.profile ?: return
        viewModelScope.launch {
            setBusy(true)
            runCatching { repository.scoreSpeech(profile, targetText, transcript, audio) }
                .onSuccess {
                    _state.value = _state.value.copy(speakingResult = it, error = null)
                    repository.saveStudySession(profile.languageCode, profile.sessionMinutes, "speaking")
                }
                .onFailure { _state.value = _state.value.copy(error = "Không chấm được audio: ${it.message ?: "hãy thử lại"}") }
            refreshModelHealth()
            audio?.delete()
            setBusy(false)
        }
    }

    fun clearSpeakingResult() { _state.value = _state.value.copy(speakingResult = null) }
    fun reviewVocabulary(term: String, label: String) {
        val languageCode = _state.value.profile?.languageCode ?: return
        viewModelScope.launch {
            repository.reviewVocabulary(languageCode, term, label)
            repository.saveStudySession(languageCode, 2, "review")
        }
    }

    fun toggleFavorite(term: String, favorite: Boolean) {
        val languageCode = _state.value.profile?.languageCode ?: return
        viewModelScope.launch { repository.setFavorite(languageCode, term, favorite) }
    }

    fun completeStudySession(minutes: Int, activity: String) {
        val languageCode = _state.value.profile?.languageCode ?: return
        viewModelScope.launch { repository.saveStudySession(languageCode, minutes, activity) }
    }

    fun updateDailyGoal(minutes: Int) {
        val current = _state.value.profile ?: return
        val updated = current.copy(dailyGoalMinutes = minutes.coerceIn(5, 120))
        viewModelScope.launch { repository.saveProfile(updated); _state.value = _state.value.copy(profile = updated) }
    }

    fun updateReminders(enabled: Boolean, hour: Int = _state.value.profile?.reminderHour ?: 20) {
        val current = _state.value.profile ?: return
        val updated = current.copy(remindersEnabled = enabled, reminderHour = hour.coerceIn(0, 23))
        viewModelScope.launch { repository.saveProfile(updated); _state.value = _state.value.copy(profile = updated) }
    }
    fun clearError() { _state.value = _state.value.copy(error = null) }
    fun reportError(message: String) { _state.value = _state.value.copy(error = message) }
    fun refreshModelHealth() {
        val names = (SupportedGeminiModels.generalUse + _state.value.modelName).distinct()
        _state.value = _state.value.copy(modelHealth = repository.modelHealth(names))
    }
    private fun setBusy(value: Boolean) { _state.value = _state.value.copy(isBusy = value) }
}

class MainViewModelFactory(private val container: com.linusv.englishcoach.di.AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(container.auth, container.repository) as T
}
