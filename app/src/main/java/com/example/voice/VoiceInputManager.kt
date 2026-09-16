package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class VoiceState {
    object Idle : VoiceState()
    object Listening : VoiceState()
    data class Processing(val partialText: String) : VoiceState()
    data class Success(val text: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

class VoiceInputManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    init {
        initSpeechRecognizerIfNeeded()
    }

    private fun initSpeechRecognizerIfNeeded() {
        if (speechRecognizer == null && SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                setupListener()
            } catch (e: Exception) {
                speechRecognizer = null
            }
        }
    }

    private fun setupListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _voiceState.value = VoiceState.Listening
            }

            override fun onBeginningOfSpeech() {
                _voiceState.value = VoiceState.Listening
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                _voiceState.value = VoiceState.Processing("در حال پردازش صدا...")
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "صدایی شنیده نشد. لطفاً بلندتر صحبت کنید یا از نمونه جملات یا کیبورد استفاده کنید."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "زمان شنیدن صدا به پایان رسید. لطفاً دوباره امتحان کنید."
                    SpeechRecognizer.ERROR_AUDIO -> "خطا در میکروفون یا ضبط صدا."
                    SpeechRecognizer.ERROR_CLIENT -> "سرویس صوتی دستگاه پاسخ نداد. می‌توانید از دستیار صوتی سیستم یا کیبورد استفاده کنید."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "دسترسی به ضبط صدا (میکروفون) داده نشده است."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "سرویس گفتار در حال حاضر مشغول است. چند لحظه بعد مجدداً تلاش کنید."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "خطای اتصال به اینترنت برای پردازش آنلاین صدا. لطفاً اتصال شبکه را بررسی کنید."
                    SpeechRecognizer.ERROR_SERVER -> "خطای سرور سرویس گفتار. لطفاً دوباره تلاش کنید یا از دستیار سیستم استفاده نمایید."
                    else -> "سرویس گفتار پاسخ نداد. می‌توانید از دستیار صوتی سیستم یا نمونه‌جملات استفاده نمایید."
                }
                _voiceState.value = VoiceState.Error(errorMsg)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0]
                    _voiceState.value = VoiceState.Success(recognizedText)
                } else {
                    _voiceState.value = VoiceState.Error("متنی یافت نشد")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    _voiceState.value = VoiceState.Processing(matches[0])
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun createSpeechIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "جمله خود را بگویید (مثلاً: ۲۰ هزار تومان نان خریدم)")
        }
    }

    fun startListening() {
        initSpeechRecognizerIfNeeded()
        if (speechRecognizer == null) {
            _voiceState.value = VoiceState.Error("سرویس گفتار مستقیم در این دستگاه فعال نیست. از دستیار سیستم استفاده کنید.")
            return
        }

        try {
            _voiceState.value = VoiceState.Listening
            speechRecognizer?.startListening(createSpeechIntent())
        } catch (e: Exception) {
            _voiceState.value = VoiceState.Error("خطا در شروع ضبط صوتی: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Ignored
        }
    }

    fun simulateInput(text: String) {
        _voiceState.value = VoiceState.Success(text)
    }

    fun resetState() {
        _voiceState.value = VoiceState.Idle
    }

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignored
        }
        speechRecognizer = null
    }

    companion object {
        fun openVoiceSettings(ctx: Context) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                ctx.startActivity(intent)
            } catch (_: Exception) {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    ctx.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
    }
}
