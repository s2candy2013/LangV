package com.linusv.englishcoach.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.linusv.englishcoach.data.GeneratedLesson
import com.linusv.englishcoach.data.FlashcardSet
import com.linusv.englishcoach.data.ConversationPayload
import com.linusv.englishcoach.data.ExerciseFeedback
import com.linusv.englishcoach.data.PronunciationFeedback
import com.linusv.englishcoach.data.LearnerProfile
import com.linusv.englishcoach.data.SpeakingPrompt
import com.linusv.englishcoach.data.SupportedLanguages
import com.linusv.englishcoach.data.SupportedGeminiModels
import com.linusv.englishcoach.data.ModelHealth
import com.linusv.englishcoach.data.VocabularyProgressEntity
import com.linusv.englishcoach.data.parseLesson
import com.linusv.englishcoach.notifications.StudyReminderScheduler
import com.linusv.englishcoach.voice.PronunciationSpeaker
import com.linusv.englishcoach.voice.PcmAudioPlayer
import com.linusv.englishcoach.voice.GeminiLiveConversation
import com.linusv.englishcoach.voice.VoiceRecorder
import kotlinx.coroutines.launch

@Composable
fun EnglishCoachApp(container: com.linusv.englishcoach.di.AppContainer) {
    val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(state.profile?.remindersEnabled, state.profile?.reminderHour) {
        state.profile?.let { profile ->
            if (profile.remindersEnabled) {
                StudyReminderScheduler.schedule(context, profile.reminderHour)
                if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else StudyReminderScheduler.cancel(context)
        }
    }
    Surface(Modifier.fillMaxSize()) {
        when {
            state.user == null -> LoginScreen(state.error, state.isBusy) { (context as? Activity)?.let(viewModel::signIn) }
            state.profile?.completedOnboarding != true -> OnboardingScreen(state.isBusy) { viewModel.saveProfile(it) }
            else -> MainShell(state, viewModel)
        }
    }
}

@Composable
private fun LoginScreen(error: String?, busy: Boolean, onSignIn: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Image(painterResource(com.linusv.englishcoach.R.drawable.langv_logo_v3), "Logo LangV", Modifier.fillMaxWidth().height(88.dp))
            Text("Language Coach", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Học ngoại ngữ theo cách của người Việt", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, "Bảo mật")
                    Text("Đăng nhập Google để lưu tiến độ trên thiết bị này.")
                }
            }
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Button(onClick = onSignIn, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Đăng nhập bằng Google")
            }
            Text("Nếu chưa có Firebase credentials, nút này chạy Demo Mode để bạn xem toàn bộ UX.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun OnboardingScreen(busy: Boolean, onSave: (LearnerProfile) -> Unit) {
    var languageCode by remember { mutableStateOf("en") }
    val language = SupportedLanguages.find(languageCode)
    var level by remember { mutableStateOf("A2") }
    var goal by remember { mutableStateOf("Giao tiếp hằng ngày") }
    var interests by remember { mutableStateOf("Công việc và đời sống") }
    var minutes by remember { mutableIntStateOf(15) }
    var voiceTag by remember { mutableStateOf("en-US") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("Thiết lập lộ trình", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Chọn vài thông tin để Gemini tạo bài vừa sức.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Ngôn ngữ muốn học", fontWeight = FontWeight.SemiBold)
        LanguageChoiceRow(languageCode) { code ->
            val selected = SupportedLanguages.find(code)
            languageCode = code
            level = selected.defaultLevel
            voiceTag = selected.voices.first().tag
        }
        Text("Trình độ hiện tại • ${language.levelSystem}", fontWeight = FontWeight.SemiBold)
        ChoiceRow(language.levels, level) { level = it }
        OutlinedTextField(goal, { goal = it }, Modifier.fillMaxWidth(), label = { Text("Mục tiêu") }, minLines = 1)
        OutlinedTextField(interests, { interests = it }, Modifier.fillMaxWidth(), label = { Text("Chủ đề bạn thích") })
        Text("Thời lượng mỗi buổi: $minutes phút", fontWeight = FontWeight.SemiBold)
        ChoiceRow(listOf("10", "15", "25", "40"), minutes.toString()) { minutes = it.toInt() }
        Text("Giọng đọc mẫu", fontWeight = FontWeight.SemiBold)
        ChoiceRow(language.voices.map { it.label }, language.voices.firstOrNull { it.tag == voiceTag }?.label ?: language.voices.first().label) { label -> voiceTag = language.voices.first { it.label == label }.tag }
        Button(onClick = { onSave(LearnerProfile(languageCode = languageCode, level = level, goal = goal, interests = interests, sessionMinutes = minutes, voiceTag = voiceTag, completedOnboarding = true)) }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Bắt đầu học")
        }
    }
}

@Composable
private fun ChoiceRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        options.forEach { option -> FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(option) }) }
    }
}

@Composable
private fun LanguageChoiceRow(selectedCode: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        SupportedLanguages.all.forEach { language ->
            FilterChip(selected = language.code == selectedCode, onClick = { onSelect(language.code) }, label = { Text(language.shortName) })
        }
    }
}

@Composable
private fun MainShell(state: AppUiState, viewModel: MainViewModel) {
    val navController = rememberNavController()
    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(state.error) { state.error?.let { snackbarHost.showSnackbar(it); viewModel.clearError() } }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = { BottomNav(navController) },
    ) { padding ->
        NavHost(navController, startDestination = "home", Modifier.padding(padding)) {
            composable("home") { HomeScreen(state, viewModel, navController) }
            composable("lesson") { LessonScreen(state, viewModel, navController) }
            composable("conversation") { ConversationScreen(state, navController) }
            composable("flashcards") { FlashcardsScreen(state, navController) }
            composable("vocabulary") { VocabularyScreen(viewModel) }
            composable("review") { ReviewScreen(state, viewModel, navController) }
            composable("history") { HistoryScreen(viewModel, navController) }
            composable("quiz") { QuickQuizScreen(viewModel, navController) }
            composable("exercise") { ExerciseScreen(state, navController) }
            composable("live") { LiveConversationScreen(state, navController) }
            composable("practice") { PracticeScreen(state, viewModel, navController) }
            composable("progress") { ProgressScreen(state, viewModel) }
            composable("settings") { SettingsScreen(state, viewModel) }
        }
    }
}

@Composable
private fun BottomNav(navController: NavHostController) {
    val items = listOf(
        "home" to ("Học" to Icons.Default.Home),
        "review" to ("Ôn tập" to Icons.Default.AutoStories),
        "vocabulary" to ("Từ vựng" to Icons.Default.MenuBook),
        "progress" to ("Tiến độ" to Icons.Default.BarChart),
        "settings" to ("Cài đặt" to Icons.Default.Settings),
    )
    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        val current = navController.currentBackStackEntryAsState().value?.destination?.route
        items.forEach { (route, item) -> NavigationBarItem(selected = current == route, onClick = { navController.navigate(route) { launchSingleTop = true; popUpTo("home") { saveState = true } } }, icon = { Icon(item.second, item.first) }, label = { Text(item.first) }) }
    }
}

@Composable
private fun HistoryScreen(viewModel: MainViewModel, nav: NavHostController) {
    val lessons by viewModel.lessons.collectAsStateWithLifecycle(initialValue = emptyList())
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") }; Text("Lịch sử bài học", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        if (lessons.isEmpty()) {
            EmptyState("Chưa có bài học", "Các bài học bạn tạo sẽ được lưu ở đây.") { nav.navigate("home") }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(lessons, key = { it.id }) { lesson ->
                    Card(onClick = { viewModel.openLesson(lesson.id) { nav.navigate("lesson") } }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoStories, "Bài học", tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) { Text(lesson.title, fontWeight = FontWeight.Bold); Text("${lesson.topic} • ${lesson.level}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Text("Mở", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewScreen(state: AppUiState, viewModel: MainViewModel, nav: NavHostController) {
    val due by viewModel.dueVocabulary.collectAsStateWithLifecycle(initialValue = emptyList())
    var index by remember(due) { mutableIntStateOf(0) }
    var revealed by remember(due, index) { mutableStateOf(false) }
    val current = due.getOrNull(index)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Ôn tập hôm nay", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(if (due.isEmpty()) "Bạn đã hoàn thành các từ đến hạn." else "${index + 1}/${due.size} từ đến hạn", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { nav.navigate("quiz") }, modifier = Modifier.fillMaxWidth()) { Text("Làm quiz nhanh từ kho từ vựng") }
        if (current == null) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tuyệt vời!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Hãy tạo bài học mới hoặc quay lại vào ngày mai để tiếp tục ôn tập.")
                    Button(onClick = { nav.navigate("home") }) { Text("Tạo bài học") }
                }
            }
        } else {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nhớ nghĩa của từ này?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(current.term, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    if (revealed) {
                        if (current.meaningVi.isNotBlank()) Text(current.meaningVi, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                        if (current.exampleTarget.isNotBlank()) Text(current.exampleTarget, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, textAlign = TextAlign.Center)
                        Text("Hãy tự nói nghĩa hoặc đặt một câu trước khi chọn kết quả.", textAlign = TextAlign.Center)
                    }
                    OutlinedButton(onClick = { revealed = !revealed }) { Text(if (revealed) "Ẩn gợi ý" else "Hiện gợi ý") }
                }
            }
            Text("Bạn nhớ từ này đến đâu?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Again" to "Quên", "Hard" to "Khó", "Good" to "Nhớ", "Easy" to "Dễ").forEach { (label, text) ->
                    OutlinedButton(onClick = {
                        viewModel.reviewVocabulary(current.term, label)
                        if (index + 1 < due.size) index++ else index = due.size
                        revealed = false
                    }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) { Text(text) }
                }
            }
            Text("Từ này sẽ được lên lịch lại tự động theo mức độ ghi nhớ.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickQuizScreen(viewModel: MainViewModel, nav: NavHostController) {
    val vocabulary by viewModel.vocabulary.collectAsStateWithLifecycle(initialValue = emptyList())
    var index by remember(vocabulary) { mutableIntStateOf(0) }
    var score by remember(vocabulary) { mutableIntStateOf(0) }
    var answered by remember(vocabulary) { mutableStateOf(false) }
    var selectedTerm by remember(vocabulary) { mutableStateOf<String?>(null) }
    val questions = remember(vocabulary) { vocabulary.filter { it.meaningVi.isNotBlank() }.take(10) }
    val current = questions.getOrNull(index)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") }; Text("Quiz nhanh", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        if (current == null) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Kết quả", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("$score/${questions.size}", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("Quiz giúp phát hiện những từ cần đưa vào ôn tập nhiều hơn.", textAlign = TextAlign.Center)
                    Button(onClick = { nav.navigate("review") }) { Text("Ôn lại từ khó") }
                }
            }
        } else {
            Text("Câu ${index + 1}/${questions.size} • Điểm $score", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nghĩa nào đúng với từ này?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(current.term, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                }
            }
            val options = remember(current.term, vocabulary) { (listOf(current) + vocabulary.filter { it.term != current.term }.shuffled().take(3)).shuffled() }
            options.forEach { option ->
                OutlinedButton(onClick = {
                    if (!answered) { selectedTerm = option.term; if (option.term == current.term) score++; answered = true }
                }, modifier = Modifier.fillMaxWidth(), enabled = !answered) { Text(option.meaningVi.ifBlank { option.term }) }
            }
            if (answered) {
                Text(if (selectedTerm == current.term) "Chính xác! ${current.meaningVi}" else "Đáp án đúng: ${current.meaningVi}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Button(onClick = { index++; answered = false; selectedTerm = null }, modifier = Modifier.fillMaxWidth()) { Text(if (index + 1 == questions.size) "Xem kết quả" else "Câu tiếp theo") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VocabularyScreen(viewModel: MainViewModel) {
    val lessons by viewModel.lessons.collectAsStateWithLifecycle(initialValue = emptyList())
    val progress by viewModel.vocabulary.collectAsStateWithLifecycle(initialValue = emptyList())
    var query by remember { mutableStateOf("") }
    val words = remember(lessons) {
        lessons
            .flatMap { lesson -> runCatching { parseLesson(lesson.payloadJson).vocabulary }.getOrDefault(emptyList()) }
            .distinctBy { it.term.trim().lowercase() }
    }
    val filteredWords = words.filter { word ->
        query.isBlank() || listOf(word.term, word.meaningVi, word.exampleTarget)
            .any { it.contains(query.trim(), ignoreCase = true) }
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text("Từ vựng", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            if (words.isEmpty()) "Các từ mới trong bài học sẽ được lưu ở đây."
            else "${words.size} từ đang được lưu",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tìm từ vựng") },
            singleLine = true,
        )
        if (filteredWords.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (words.isEmpty()) "Chưa có từ vựng" else "Không tìm thấy từ phù hợp",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        if (words.isEmpty()) "Hãy tạo một bài học để bắt đầu lưu từ mới." else "Thử tìm bằng từ tiếng Anh hoặc nghĩa tiếng Việt.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filteredWords, key = { it.term }) { word ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(word.term, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                val favorite = progress.firstOrNull { it.term == word.term }?.isFavorite == true
                                IconButton(onClick = { viewModel.toggleFavorite(word.term, !favorite) }) { Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "${if (favorite) "Bỏ lưu" else "Lưu"} ${word.term}", tint = if (favorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
                                Text(word.partOfSpeech, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            }
                            Text(
                                listOfNotNull(word.pronunciation, word.romanization?.takeIf { it.isNotBlank() })
                                    .joinToString(" • "),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(word.meaningVi, fontWeight = FontWeight.SemiBold)
                            Text(word.exampleTarget, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            Text(word.exampleVi, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationScreen(state: AppUiState, nav: NavHostController) {
    val conversation = state.conversation
    if (conversation == null) { EmptyState("Chưa có hội thoại", "Quay về trang Học để tạo hội thoại.") { nav.navigate("home") }; return }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") }; Text(conversation.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        Text("Chủ đề: ${conversation.topic}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        conversation.lines.forEach { line ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (line.speaker.equals("A", true)) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(line.speaker, fontWeight = FontWeight.Bold); Text(line.targetText, style = MaterialTheme.typography.titleMedium); Text(line.translationVi, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        Text("Mẹo luyện nói", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        conversation.tips.forEach { Text("• $it") }
    }
}

@Composable
private fun FlashcardsScreen(state: AppUiState, nav: NavHostController) {
    val flashcards = state.flashcards
    if (flashcards == null) { EmptyState("Chưa có flashcard", "Quay về trang Học để tạo flashcard.") { nav.navigate("home") }; return }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") }; Text(flashcards.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        Text("${flashcards.cards.size} thẻ được tạo từ ảnh", color = MaterialTheme.colorScheme.onSurfaceVariant)
        flashcards.cards.forEach { card ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(card.term, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(listOfNotNull(card.pronunciation, card.romanization, card.partOfSpeech).joinToString(" • "), color = MaterialTheme.colorScheme.primary); Text(card.meaningVi); Text(card.exampleTarget, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic); Text(card.exampleVi, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        }
    }
}

@Composable
private fun ExerciseScreen(state: AppUiState, nav: NavHostController) {
    val feedback = state.exerciseFeedback
    if (feedback == null) { EmptyState("Chưa có kết quả", "Quay về trang Học để chấm bài từ ảnh.") { nav.navigate("home") }; return }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") }; Text(feedback.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        Card(colors = CardDefaults.cardColors(containerColor = if (feedback.score >= 75) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Điểm ước lượng: ${feedback.score}/100", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Kết quả do AI đọc từ ảnh; hãy kiểm tra lại nếu ảnh mờ.") } }
        FeedbackSection("Đáp án đọc được", feedback.answers)
        FeedbackSection("Lỗi cần sửa", feedback.mistakes)
        FeedbackSection("Giải thích", feedback.explanations)
        FeedbackSection("Bước tiếp theo", feedback.nextSteps)
    }
}

@Composable
private fun FeedbackSection(title: String, items: List<String>) {
    if (items.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    items.forEach { Card(Modifier.fillMaxWidth()) { Text(it, Modifier.padding(14.dp)) } }
}

@Composable
private fun LiveConversationScreen(state: AppUiState, nav: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val live = remember { GeminiLiveConversation() }
    var active by remember { mutableStateOf(false) }
    var starting by remember { mutableStateOf(false) }
    var liveError by remember { mutableStateOf<String?>(null) }
    val profile = state.profile
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted || profile == null) return@rememberLauncherForActivityResult
        starting = true
        scope.launch { runCatching { live.start(profile.voiceTag, profile.level) }.onSuccess { active = true }.onFailure { liveError = it.message ?: "Không thể kết nối Gemini Live" }.also { starting = false } }
    }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { scope.launch { live.stop() } } }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") }; Text("Hội thoại realtime", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(if (active) "Gemini đang lắng nghe" else "Luyện nói trực tiếp với Gemini", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Nói tự nhiên, Gemini sẽ trả lời bằng giọng nói và sửa lỗi ngắn gọn.") } }
        if (starting) LinearProgressIndicator(Modifier.fillMaxWidth())
        Button(onClick = {
            if (active) { scope.launch { live.stop(); active = false } }
            else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                profile?.let { starting = true; liveError = null; scope.launch { runCatching { live.start(it.voiceTag, it.level) }.onSuccess { active = true }.onFailure { liveError = it.message ?: "Không thể kết nối Gemini Live" }.also { starting = false } } }
            } else permission.launch(Manifest.permission.RECORD_AUDIO)
        }, enabled = !starting && profile != null, modifier = Modifier.fillMaxWidth().height(56.dp)) { Icon(Icons.Default.Mic, null); Spacer(Modifier.width(8.dp)); Text(if (active) "Dừng hội thoại" else "Bắt đầu nói chuyện") }
        liveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Gemini Live là tính năng preview; cần mạng ổn định và có thể dùng quota riêng.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(state: AppUiState, viewModel: MainViewModel, nav: NavHostController) {
    var topic by remember { mutableStateOf("daily life") }
    var imageAction by remember { mutableStateOf("lesson") }
    val profile = state.profile ?: return
    val due by viewModel.dueCount.collectAsStateWithLifecycle(initialValue = 0)
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val result = runCatching {
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Không thể đọc ảnh")
            require(bytes.size <= 10 * 1024 * 1024) { "Ảnh quá lớn, hãy chọn ảnh dưới 10MB" }
            bytes to mimeType
        }
        result.onSuccess { (bytes, mimeType) ->
            when (imageAction) {
                "flashcards" -> viewModel.generateFlashcardsFromImage(bytes, mimeType) { nav.navigate("flashcards") }
                "exercise" -> viewModel.gradeExerciseFromImage(bytes, mimeType) { nav.navigate("exercise") }
                else -> viewModel.generateLessonFromImage(bytes, mimeType) { nav.navigate("lesson") }
            }
        }.onFailure { viewModel.reportError(it.message ?: "Không thể đọc ảnh") }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Xin chào${state.user?.displayName?.let { ", $it" } ?: ""}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Hôm nay mình học gì?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Ngôn ngữ đang học", fontWeight = FontWeight.SemiBold)
        LanguageChoiceRow(profile.languageCode, viewModel::switchLanguage)
        Text("Cấp độ hiện tại • ${SupportedLanguages.find(profile.languageCode).levelSystem}", fontWeight = FontWeight.SemiBold)
        ChoiceRow(SupportedLanguages.find(profile.languageCode).levels, profile.level, viewModel::switchLevel)
        Card(onClick = { nav.navigate("review") }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (due > 0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.AutoStories, "Ôn tập", tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(if (due > 0) "Bạn có $due từ cần ôn" else "Ôn tập hôm nay", fontWeight = FontWeight.Bold)
                    Text(if (due > 0) "Dành vài phút để củng cố trí nhớ." else "Các từ mới sẽ xuất hiện ở đây khi đến hạn.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Mở", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        if (state.isBusy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Đang chuẩn bị nội dung học… bạn có thể tiếp tục chờ ở màn hình này.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${SupportedLanguages.find(profile.languageCode).displayNameVi} • ${profile.level} • ${profile.sessionMinutes} phút", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Tạo một bài học vừa sức, có từ vựng, ngữ pháp, đọc hiểu và luyện nói.")
                OutlinedTextField(topic, { topic = it }, Modifier.fillMaxWidth(), label = { Text("Chủ đề") })
                Button(onClick = { viewModel.generateLesson(topic) { nav.navigate("lesson") } }, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Icon(Icons.Default.AutoStories, null); Spacer(Modifier.width(8.dp)); Text(if (state.isBusy) "Đang tạo..." else "Tạo bài học")
                }
                OutlinedButton(onClick = { imageAction = "lesson"; imagePicker.launch("image/*") }, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Icon(Icons.Default.Image, null); Spacer(Modifier.width(8.dp)); Text("Tạo bài học từ ảnh")
                }
                OutlinedButton(onClick = { imageAction = "flashcards"; imagePicker.launch("image/*") }, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Icon(Icons.Default.AutoStories, null); Spacer(Modifier.width(8.dp)); Text("Tạo flashcard từ ảnh")
                }
                OutlinedButton(onClick = { imageAction = "exercise"; imagePicker.launch("image/*") }, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Icon(Icons.Default.CheckCircle, null); Spacer(Modifier.width(8.dp)); Text("Chấm bài tập từ ảnh")
                }
                OutlinedButton(onClick = { viewModel.generateConversation(topic) { nav.navigate("conversation") } }, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Icon(Icons.Default.RecordVoiceOver, null); Spacer(Modifier.width(8.dp)); Text("Tạo hội thoại theo chủ đề")
                }
                OutlinedButton(onClick = { nav.navigate("live") }, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Icon(Icons.Default.Mic, null); Spacer(Modifier.width(8.dp)); Text("Hội thoại realtime với Gemini")
                }
                Text("Chọn ảnh trang sách hoặc flashcard; Gemini sẽ đọc nội dung và tạo bài theo cấp độ ${profile.level}.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        state.lesson?.let { lesson ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Bài gần nhất", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { nav.navigate("history") }) { Text("Xem lịch sử") }
            }
            LessonSummaryCard(lesson) { nav.navigate("lesson") }
        }
    }
}

@Composable
private fun LessonSummaryCard(lesson: GeneratedLesson, onOpen: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoStories, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) { Text(lesson.payload.title, fontWeight = FontWeight.Bold); Text("${lesson.payload.vocabulary.size} từ • ${lesson.payload.level}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Icon(Icons.Default.PlayArrow, "Mở bài")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonScreen(state: AppUiState, viewModel: MainViewModel, nav: NavHostController) {
    val lesson = state.lesson
    if (lesson == null) { EmptyState("Chưa có bài học", "Quay về trang Học để tạo bài đầu tiên.") { nav.navigate("home") }; return }
    val context = LocalContext.current
    val speaker = remember(context) { PronunciationSpeaker(context) }
    val pcmPlayer = remember { PcmAudioPlayer() }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { pcmPlayer.stop(); speaker.shutdown() } }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(lesson.payload.title) }, navigationIcon = { IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") } }, actions = { IconButton(enabled = !state.isBusy, onClick = { viewModel.generateLesson(lesson.payload.topic) }) { Icon(Icons.Default.Refresh, "Tạo lại bài học") } })
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("Từ vựng", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(lesson.payload.vocabulary) { word ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(word.term, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton({ viewModel.synthesizeSpeech(word.term, { pcmPlayer.play(it) }, { speaker.speak(word.term, state.profile?.voiceTag ?: "en-US") }) }) { Icon(Icons.Default.VolumeUp, "Nghe cách đọc") } }; Text(listOfNotNull(word.pronunciation, word.romanization?.takeIf { it.isNotBlank() }, word.partOfSpeech).joinToString(" • "), color = MaterialTheme.colorScheme.primary); Text(word.meaningVi); Text(word.exampleTarget, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic); Text(word.exampleVi, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            }
            item { Text("Cấu trúc câu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(lesson.payload.grammar) { grammar -> Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(grammar.pattern, fontWeight = FontWeight.Bold); Text(grammar.explanationVi); Text("Ví dụ: ${grammar.examples.joinToString(" • ")}"); Text("Lưu ý: ${grammar.commonMistake}", color = MaterialTheme.colorScheme.error) } } }
            item { Text("Đọc hiểu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            item { PassageCard(lesson.payload.passage) }
            item { Text("Luyện nói", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(lesson.payload.speakingPrompts) { prompt -> Card(onClick = { nav.navigate("practice") }, Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.RecordVoiceOver, null, tint = MaterialTheme.colorScheme.tertiary); Spacer(Modifier.width(12.dp)); Text(prompt.targetText, Modifier.weight(1f)); Icon(Icons.Default.PlayArrow, "Luyện nói") } } }
        }
    }
}

@Composable
private fun PassageCard(passage: com.linusv.englishcoach.data.ReadingPassage) {
    var showTranslation by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(passage.title, fontWeight = FontWeight.Bold); Text(passage.textTarget); OutlinedButton({ showTranslation = !showTranslation }) { Text(if (showTranslation) "Ẩn bản dịch" else "Xem bản dịch") }; if (showTranslation) Text(passage.translationVi, color = MaterialTheme.colorScheme.onSurfaceVariant); passage.questions.forEachIndexed { index, question -> Text("${index + 1}. $question", fontWeight = FontWeight.SemiBold) } } }
}

@Composable
private fun PracticeScreen(state: AppUiState, viewModel: MainViewModel, nav: NavHostController) {
    val lesson = state.lesson ?: return
    val prompt = lesson.payload.speakingPrompts.firstOrNull() ?: return
    val context = LocalContext.current
    val recorder = remember { VoiceRecorder(context) }
    val speaker = remember { PronunciationSpeaker(context) }
    val pcmPlayer = remember { PcmAudioPlayer() }
    var recording by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf("") }
    var audioFile by remember { mutableStateOf<java.io.File?>(null) }
    var voiceError by remember { mutableStateOf<String?>(null) }
    val voiceTag = state.profile?.voiceTag ?: "en-US"
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) { voiceError = null; audioFile = recorder.start(voiceTag, { transcript = it }, { voiceError = it }); recording = true } else voiceError = "Cần cấp quyền microphone để luyện nói." }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { if (recording) recorder.cancel(); pcmPlayer.stop(); speaker.shutdown() } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") }; Text("Luyện nói", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Đọc câu này", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold); Text(prompt.targetText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); IconButton({ viewModel.synthesizeSpeech(prompt.targetText, { pcmPlayer.play(it) }, { speaker.speak(prompt.targetText, voiceTag) }) }, Modifier.size(52.dp).semantics { role = Role.Button }) { Icon(Icons.Default.VolumeUp, "Nghe câu mẫu", Modifier.size(30.dp)) } } }
        Text("Mẹo: ${prompt.pronunciationTips.joinToString(" • ")}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (transcript.isNotBlank()) Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Bạn đang nói", fontWeight = FontWeight.Bold); Text(transcript) } }
        Button(onClick = { if (recording) { val result = recorder.stop(); audioFile = result.first; transcript = result.second.ifBlank { transcript }; recording = false; viewModel.scoreSpeech(prompt.targetText, transcript, audioFile) } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) { voiceError = null; audioFile = recorder.start(voiceTag, { transcript = it }, { voiceError = it }); recording = true } else permission.launch(Manifest.permission.RECORD_AUDIO) }, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth().height(56.dp)) { Icon(if (recording) Icons.Default.CheckCircle else Icons.Default.Mic, null); Spacer(Modifier.width(8.dp)); Text(if (recording) "Dừng và chấm bài" else "Bắt đầu ghi âm") }
        voiceError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.isBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.speakingResult?.let { result ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (result.overall >= 75) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Điểm ước lượng: ${result.overall}/100", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Nội dung ${result.contentAccuracy} • Phát âm ${result.pronunciation} • Độ trôi chảy ${result.fluency}"); result.feedback?.issues?.forEach { Text("• $it") }; result.feedback?.tips?.forEach { Text("Gợi ý: $it") }; Text(if (result.isComplete) "Đã phân tích transcript + audio" else "Chỉ có điểm transcript vì Gemini chưa phản hồi", fontSize = 12.sp) } }
        }
    }
}

@Composable
private fun ProgressScreen(state: AppUiState, viewModel: MainViewModel) {
    val attempts by viewModel.speakingAttempts.collectAsStateWithLifecycle(initialValue = emptyList())
    val vocabulary by viewModel.vocabularyCount.collectAsStateWithLifecycle(initialValue = 0)
    val due by viewModel.dueCount.collectAsStateWithLifecycle(initialValue = 0)
    val totalMinutes by viewModel.totalStudyMinutes.collectAsStateWithLifecycle(initialValue = 0)
    val sessions by viewModel.studySessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val todayStart = remember { java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() }
    val todayMinutes = sessions.filter { it.createdAt >= todayStart }.sumOf { it.minutes }
    val goalMinutes = state.profile?.dailyGoalMinutes ?: 15
    val commonIssues = attempts.flatMap { attempt -> runCatching { kotlinx.serialization.json.Json.decodeFromString<PronunciationFeedback>(attempt.feedbackJson).issues }.getOrDefault(emptyList()) }
        .filter { it.isNotBlank() }.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }.take(5)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { Text("Tiến độ", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Mỗi lượt học được lưu trên thiết bị để bạn theo dõi tiến bộ.", color = MaterialTheme.colorScheme.onSurfaceVariant); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("Từ đang học", vocabulary.toString(), Modifier.weight(1f)); StatCard("Đến hạn", due.toString(), Modifier.weight(1f)); StatCard("Lượt nói", attempts.size.toString(), Modifier.weight(1f)) }; Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Mục tiêu hôm nay", fontWeight = FontWeight.Bold); Text("$todayMinutes/$goalMinutes phút", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }; LinearProgressIndicator(progress = { (todayMinutes.toFloat() / goalMinutes.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth()); Text("Tổng cộng: ${totalMinutes} phút • ${sessions.size} phiên", color = MaterialTheme.colorScheme.onSurfaceVariant) } }; if (commonIssues.isNotEmpty()) { Text("Lỗi phát âm cần ưu tiên", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); commonIssues.forEach { (issue, count) -> Card(Modifier.fillMaxWidth()) { Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(issue, Modifier.weight(1f)); Text("${count} lần", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) } } } }; Text("Các lượt luyện nói gần đây", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); if (attempts.isEmpty()) Text("Chưa có lượt nào. Mở một bài học và thử đọc thành tiếng nhé.", color = MaterialTheme.colorScheme.onSurfaceVariant) else attempts.take(10).forEach { attempt -> Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.GraphicEq, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(attempt.targetText, maxLines = 2); Text("${attempt.overall}/100 • ${attempt.transcript}", color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(if (attempt.overall >= 75) "Đạt" else "Luyện thêm", color = if (attempt.overall >= 75) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) } } } }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) { Card(modifier) { Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text(label, fontSize = 12.sp, textAlign = TextAlign.Center) } } }

@Composable
private fun SettingsScreen(state: AppUiState, viewModel: MainViewModel) {
    // General-use models support the text/JSON lesson flow and audio input used by this app.
    // Image, TTS and Live models are intentionally excluded because they need different APIs.
    val modelOptions = (SupportedGeminiModels.generalUse + state.modelName).distinct()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Cài đặt", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(state.user?.email.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.profile?.let { profile ->
            Text("Đang học: ${SupportedLanguages.find(profile.languageCode).displayNameVi} • ${profile.level}")
            Text("Mục tiêu mỗi ngày: ${profile.dailyGoalMinutes} phút", fontWeight = FontWeight.SemiBold)
            ChoiceRow(listOf("10", "15", "25", "40", "60"), profile.dailyGoalMinutes.toString()) { viewModel.updateDailyGoal(it.toInt()) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Nhắc học mỗi ngày", fontWeight = FontWeight.SemiBold)
                    Text("Nhận thông báo nhắc ôn tập trên thiết bị.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Switch(checked = profile.remindersEnabled, onCheckedChange = { viewModel.updateReminders(it) })
            }
            if (profile.remindersEnabled) {
                Text("Giờ nhắc: ${profile.reminderHour}:00", fontWeight = FontWeight.SemiBold)
                ChoiceRow(listOf("7", "9", "12", "18", "20", "21"), profile.reminderHour.toString()) { viewModel.updateReminders(true, it.toInt()) }
            }
        }
        Text("Model AI", fontWeight = FontWeight.SemiBold)
        Text("Đổi sang Flash Lite nếu model hiện tại thường báo quá tải.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        ChoiceRow(modelOptions, state.modelName, viewModel::switchModel)
        Text("Model đang dùng: ${state.modelName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Tình trạng model", fontWeight = FontWeight.SemiBold)
            OutlinedButton(onClick = viewModel::refreshModelHealth) { Text("Làm mới") }
        }
        val currentHealth = state.modelHealth.firstOrNull { it.modelName == state.modelName }
        currentHealth?.let { ModelHealthCard(it) }
        val alternative = state.modelHealth.firstOrNull { it.modelName != state.modelName && it.status == "Ổn định" }
        if (currentHealth?.status == "Quá tải" && alternative != null) {
            Text("Gợi ý chuyển sang ${alternative.modelName}", color = MaterialTheme.colorScheme.primary)
            OutlinedButton(onClick = { viewModel.switchModel(alternative.modelName) }, Modifier.fillMaxWidth()) { Text("Dùng model này") }
        }
        Text("Gemini được gọi qua Firebase AI Logic khi project đã được cấu hình.")
        OutlinedButton(onClick = viewModel::signOut, Modifier.fillMaxWidth().height(50.dp)) { Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Đăng xuất") }
    }
}

@Composable
private fun ModelHealthCard(health: ModelHealth) {
    val statusColor = when (health.status) {
        "Ổn định" -> MaterialTheme.colorScheme.primary
        "Cảnh báo" -> MaterialTheme.colorScheme.tertiary
        "Quá tải" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(health.status, color = statusColor, fontWeight = FontWeight.Bold)
            Text("${health.modelName} • ${health.requestCount} request • ${health.throttledCount} lần quá tải")
            health.lastLatencyMs?.let { Text("Phản hồi gần nhất: ${it}ms", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text("Dữ liệu trên thiết bị này, không phải quota project chính xác.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String, action: () -> Unit) { Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center); Button(action) { Text("Quay lại") } } } }
