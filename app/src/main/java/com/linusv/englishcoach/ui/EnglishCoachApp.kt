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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
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
import androidx.compose.material3.Text
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
import com.linusv.englishcoach.data.LearnerProfile
import com.linusv.englishcoach.data.SpeakingPrompt
import com.linusv.englishcoach.voice.PronunciationSpeaker
import com.linusv.englishcoach.voice.VoiceRecorder
import kotlinx.coroutines.launch

@Composable
fun EnglishCoachApp(container: com.linusv.englishcoach.di.AppContainer) {
    val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
            Icon(Icons.Default.RecordVoiceOver, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Text("English Coach", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Một góc học tiếng Anh riêng cho bạn", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
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
    var level by remember { mutableStateOf("A2") }
    var goal by remember { mutableStateOf("Giao tiếp hằng ngày") }
    var interests by remember { mutableStateOf("Công việc và đời sống") }
    var minutes by remember { mutableIntStateOf(15) }
    var accent by remember { mutableStateOf("US") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(24.dp))
        Text("Thiết lập lộ trình", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Chọn vài thông tin để Gemini tạo bài vừa sức.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Trình độ hiện tại", fontWeight = FontWeight.SemiBold)
        ChoiceRow(listOf("A1", "A2", "B1", "B2", "C1"), level) { level = it }
        OutlinedTextField(goal, { goal = it }, Modifier.fillMaxWidth(), label = { Text("Mục tiêu") }, minLines = 1)
        OutlinedTextField(interests, { interests = it }, Modifier.fillMaxWidth(), label = { Text("Chủ đề bạn thích") })
        Text("Thời lượng mỗi buổi: $minutes phút", fontWeight = FontWeight.SemiBold)
        ChoiceRow(listOf("10", "15", "25", "40"), minutes.toString()) { minutes = it.toInt() }
        Text("Giọng đọc mẫu", fontWeight = FontWeight.SemiBold)
        ChoiceRow(listOf("US", "UK"), accent) { accent = it }
        Button(onClick = { onSave(LearnerProfile(level = level, goal = goal, interests = interests, sessionMinutes = minutes, accent = accent, completedOnboarding = true)) }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Bắt đầu học")
        }
    }
}

@Composable
private fun ChoiceRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { option -> FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(option) }) }
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
            composable("practice") { PracticeScreen(state, viewModel, navController) }
            composable("progress") { ProgressScreen(viewModel) }
            composable("settings") { SettingsScreen(state, viewModel) }
        }
    }
}

@Composable
private fun BottomNav(navController: NavHostController) {
    val items = listOf("home" to ("Học" to Icons.Default.Home), "progress" to ("Tiến độ" to Icons.Default.BarChart), "settings" to ("Cài đặt" to Icons.Default.Settings))
    NavigationBar(Modifier.navigationBarsPadding()) {
        val current = navController.currentBackStackEntryAsState().value?.destination?.route
        items.forEach { (route, item) -> NavigationBarItem(selected = current == route, onClick = { navController.navigate(route) { launchSingleTop = true; popUpTo("home") { saveState = true } } }, icon = { Icon(item.second, item.first) }, label = { Text(item.first) }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(state: AppUiState, viewModel: MainViewModel, nav: NavHostController) {
    var topic by remember { mutableStateOf("daily life") }
    val profile = state.profile ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Xin chào${state.user?.displayName?.let { ", $it" } ?: ""}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Hôm nay mình học gì?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${profile.level} • ${profile.sessionMinutes} phút", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Tạo một bài học vừa sức, có từ vựng, ngữ pháp, đọc hiểu và luyện nói.")
                OutlinedTextField(topic, { topic = it }, Modifier.fillMaxWidth(), label = { Text("Chủ đề") })
                Button(onClick = { viewModel.generateLesson(topic); nav.navigate("lesson") }, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Icon(Icons.Default.AutoStories, null); Spacer(Modifier.width(8.dp)); Text(if (state.isBusy) "Đang tạo..." else "Tạo bài học")
                }
            }
        }
        state.lesson?.let { lesson ->
            Text("Bài gần nhất", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            LessonSummaryCard(lesson) { nav.navigate("lesson") }
        }
    }
}

@Composable
private fun LessonSummaryCard(lesson: GeneratedLesson, onOpen: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoStories, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) { Text(lesson.payload.title, fontWeight = FontWeight.Bold); Text("${lesson.payload.vocabulary.size} từ • ${lesson.payload.cefr}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { speaker.shutdown() } }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(lesson.payload.title) }, navigationIcon = { IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") } })
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("Từ vựng", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(lesson.payload.vocabulary) { word ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(word.term, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton({ speaker.speak(word.term, state.profile?.accent ?: "US") }) { Icon(Icons.Default.VolumeUp, "Nghe cách đọc") } }; Text("${word.ipa} • ${word.partOfSpeech}", color = MaterialTheme.colorScheme.primary); Text(word.meaningVi); Text(word.exampleEn, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic); Text(word.exampleVi, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
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
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(passage.title, fontWeight = FontWeight.Bold); Text(passage.textEn); OutlinedButton({ showTranslation = !showTranslation }) { Text(if (showTranslation) "Ẩn bản dịch" else "Xem bản dịch") }; if (showTranslation) Text(passage.translationVi, color = MaterialTheme.colorScheme.onSurfaceVariant); passage.questions.forEachIndexed { index, question -> Text("${index + 1}. $question", fontWeight = FontWeight.SemiBold) } } }
}

@Composable
private fun PracticeScreen(state: AppUiState, viewModel: MainViewModel, nav: NavHostController) {
    val lesson = state.lesson ?: return
    val prompt = lesson.payload.speakingPrompts.firstOrNull() ?: return
    val context = LocalContext.current
    val recorder = remember { VoiceRecorder(context) }
    val speaker = remember { PronunciationSpeaker(context) }
    var recording by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf("") }
    var audioFile by remember { mutableStateOf<java.io.File?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) { audioFile = recorder.start { transcript = it }; recording = true } }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { if (recording) recorder.cancel(); speaker.shutdown() } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") }; Text("Luyện nói", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Đọc câu này", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold); Text(prompt.targetText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); IconButton({ speaker.speak(prompt.targetText, state.profile?.accent ?: "US") }, Modifier.size(52.dp).semantics { role = Role.Button }) { Icon(Icons.Default.VolumeUp, "Nghe câu mẫu", Modifier.size(30.dp)) } } }
        Text("Mẹo: ${prompt.pronunciationTips.joinToString(" • ")}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (transcript.isNotBlank()) Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Bạn đang nói", fontWeight = FontWeight.Bold); Text(transcript) } }
        Button(onClick = { if (recording) { val result = recorder.stop(); audioFile = result.first; transcript = result.second.ifBlank { transcript }; recording = false; viewModel.scoreSpeech(prompt.targetText, transcript, audioFile) } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) { audioFile = recorder.start { transcript = it }; recording = true } else permission.launch(Manifest.permission.RECORD_AUDIO) }, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth().height(56.dp)) { Icon(if (recording) Icons.Default.CheckCircle else Icons.Default.Mic, null); Spacer(Modifier.width(8.dp)); Text(if (recording) "Dừng và chấm bài" else "Bắt đầu ghi âm") }
        if (state.isBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.speakingResult?.let { result ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (result.overall >= 75) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Điểm ước lượng: ${result.overall}/100", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Nội dung ${result.contentAccuracy} • Phát âm ${result.pronunciation} • Độ trôi chảy ${result.fluency}"); result.feedback?.issues?.forEach { Text("• $it") }; result.feedback?.tips?.forEach { Text("Gợi ý: $it") }; Text(if (result.isComplete) "Đã phân tích transcript + audio" else "Chỉ có điểm transcript vì Gemini chưa phản hồi", fontSize = 12.sp) } }
        }
    }
}

@Composable
private fun ProgressScreen(viewModel: MainViewModel) {
    val attempts by viewModel.speakingAttempts.collectAsStateWithLifecycle(initialValue = emptyList())
    val vocabulary by viewModel.vocabularyCount.collectAsStateWithLifecycle(initialValue = 0)
    val due by viewModel.dueCount.collectAsStateWithLifecycle(initialValue = 0)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { Text("Tiến độ", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("Từ đang học", vocabulary.toString(), Modifier.weight(1f)); StatCard("Đến hạn", due.toString(), Modifier.weight(1f)); StatCard("Lượt nói", attempts.size.toString(), Modifier.weight(1f)) }; Text("Các lượt luyện nói gần đây", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); if (attempts.isEmpty()) Text("Chưa có lượt nào. Mở một bài học và thử đọc thành tiếng nhé.", color = MaterialTheme.colorScheme.onSurfaceVariant) else attempts.take(10).forEach { attempt -> Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.GraphicEq, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(attempt.targetText, maxLines = 2); Text("${attempt.overall}/100 • ${attempt.transcript}", color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(if (attempt.overall >= 75) "Đạt" else "Luyện thêm", color = if (attempt.overall >= 75) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) } } } }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) { Card(modifier) { Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text(label, fontSize = 12.sp, textAlign = TextAlign.Center) } } }

@Composable
private fun SettingsScreen(state: AppUiState, viewModel: MainViewModel) { Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) { Text("Cài đặt", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(state.user?.email.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Gemini Flash sẽ dùng Firebase AI Logic khi project đã được cấu hình."); OutlinedButton(onClick = viewModel::signOut, Modifier.fillMaxWidth().height(50.dp)) { Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Đăng xuất") } } }

@Composable
private fun EmptyState(title: String, message: String, action: () -> Unit) { Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center); Button(action) { Text("Quay lại") } } } }
