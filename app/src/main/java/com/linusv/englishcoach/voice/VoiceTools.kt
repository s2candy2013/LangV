package com.linusv.englishcoach.voice

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.io.File
import java.util.Locale

class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var recognizer: SpeechRecognizer? = null
    private var output: File? = null
    private var latestTranscript: String = ""

    fun start(onTranscript: (String) -> Unit): File {
        check(SpeechRecognizer.isRecognitionAvailable(context)) { "Thiết bị không có SpeechRecognizer" }
        val file = File.createTempFile("english_coach_", ".m4a", context.cacheDir)
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
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
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

    fun speak(text: String, accent: String = "US") {
        if (!ready) return
        tts.language = if (accent == "UK") Locale.UK else Locale.US
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "english_coach")
    }

    fun shutdown() = tts.shutdown()
}
