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
)

class MainViewModel(
    private val auth: AuthRepository,
    private val repository: LocalLessonRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AppUiState(user = auth.currentUser()))
    val state: StateFlow<AppUiState> = _state.asStateFlow()
    val lessons = repository.lessons
    val speakingAttempts = repository.speakingAttempts
    val vocabularyCount = repository.vocabularyCount
    val dueCount = repository.dueCount

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

    fun generateLesson(topic: String) {
        val profile = _state.value.profile ?: return
        viewModelScope.launch {
            setBusy(true)
            runCatching { repository.generateLesson(profile, topic.ifBlank { "daily life" }) }
                .onSuccess { _state.value = _state.value.copy(lesson = it, error = null) }
                .onFailure { _state.value = _state.value.copy(error = "Không tạo được bài: ${it.message ?: "hãy thử lại"}") }
            setBusy(false)
        }
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

    fun scoreSpeech(targetText: String, transcript: String, audio: File?) {
        val profile = _state.value.profile ?: return
        viewModelScope.launch {
            setBusy(true)
            runCatching { repository.scoreSpeech(profile, targetText, transcript, audio) }
                .onSuccess { _state.value = _state.value.copy(speakingResult = it, error = null) }
                .onFailure { _state.value = _state.value.copy(error = "Không chấm được audio: ${it.message ?: "hãy thử lại"}") }
            audio?.delete()
            setBusy(false)
        }
    }

    fun clearSpeakingResult() { _state.value = _state.value.copy(speakingResult = null) }
    fun clearError() { _state.value = _state.value.copy(error = null) }
    private fun setBusy(value: Boolean) { _state.value = _state.value.copy(isBusy = value) }
}

class MainViewModelFactory(private val container: com.linusv.englishcoach.di.AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(container.auth, container.repository) as T
}
