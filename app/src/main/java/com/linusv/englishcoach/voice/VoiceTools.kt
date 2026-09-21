package com.linusv.englishcoach.voice

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.liveGenerationConfig
import java.io.File
import java.util.Locale

class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var recognizer: SpeechRecognizer? = null
    private var output: File? = null
    private var latestTranscript: String = ""

    fun start(languageTag: String, onTranscript: (String) -> Unit): File {
        check(SpeechRecognizer.isRecognitionAvailable(context)) { "Thiết bị không có SpeechRecognizer" }
        val file = File.createTempFile("language_coach_", ".m4a", context.cacheDir)
        output = file
        latestTranscript = ""
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(16000)
            setAudioEncodingBitRate(96000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    latestTranscript = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    onTranscript(latestTranscript)
                }
                override fun onPartialResults(results: Bundle?) {
                    val partial = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    if (partial.isNotBlank()) onTranscript(partial)
                }
                override fun onError(error: Int) = Unit
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            })
        }
        return file
    }

    fun stop(): Pair<File?, String> {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
        recorder?.runCatching { stop() }
        recorder?.release()
        recorder = null
        val result = output to latestTranscript
        output = null
        return result
    }

    fun cancel() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        recorder?.runCatching { stop() }
        recorder?.release()
        recorder = null
        output?.delete()
        output = null
    }
}

class PronunciationSpeaker(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context, this)
    private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) tts.language = Locale.US
    }

    fun speak(text: String, languageTag: String = "en-US") {
        if (!ready) return
        tts.language = Locale.forLanguageTag(languageTag)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "english_coach")
    }

    fun shutdown() = tts.shutdown()
}

class PcmAudioPlayer {
    private var track: AudioTrack? = null

    fun play(pcmBytes: ByteArray) {
        stop()
        val minBuffer = AudioTrack.getMinBufferSize(24000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(24000).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(maxOf(minBuffer, pcmBytes.size))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
            .also {
                it.write(pcmBytes, 0, pcmBytes.size)
                it.play()
            }
    }

    fun stop() {
        track?.runCatching { stop() }
        track?.release()
        track = null
    }
}

@OptIn(PublicPreviewAPI::class)
class GeminiLiveConversation {
    private var session: com.google.firebase.ai.type.LiveSession? = null

    suspend fun start(languageCode: String, level: String) {
        val config = liveGenerationConfig {
            responseModality = ResponseModality.AUDIO
            speechConfig = SpeechConfig(Voice("Puck"), languageCode)
        }
        val liveModel = FirebaseAI.getInstance(FirebaseApp.getInstance(), GenerativeBackend.googleAI())
            .liveModel("gemini-3.1-flash-live-preview", config)
        session = liveModel.connect()
        session?.send("You are a friendly language coach. Practice $languageCode with a learner at level $level. Speak clearly, ask one question at a time, and correct mistakes briefly.")
        session?.startAudioConversation()
    }

    suspend fun stop() {
        session?.stopAudioConversation()
        session?.close()
        session = null
    }
}
