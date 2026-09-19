package com.example.speech

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.model.StakeoutGuidance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * Text-to-speech feedback manager providing hands-free auditory stakeout cues
 * (e.g., 'Moving closer', 'Stop, point reached', 'Approaching point', 'Turn right').
 */
class StakeoutSpeechManager(
    context: Context,
    initialSettings: StakeoutVoiceSettings = StakeoutVoiceSettings()
) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null

    private val evaluator = StakeoutCueEvaluator()

    private val _voiceSettings = MutableStateFlow(initialSettings)
    val voiceSettings: StateFlow<StakeoutVoiceSettings> = _voiceSettings.asStateFlow()

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _lastSpokenCue = MutableStateFlow<String>("")
    val lastSpokenCue: StateFlow<String> = _lastSpokenCue.asStateFlow()

    private val _cueHistory = MutableStateFlow<List<String>>(emptyList())
    val cueHistory: StateFlow<List<String>> = _cueHistory.asStateFlow()

    init {
        try {
            tts = TextToSpeech(appContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            val result = engine.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default device locale
                engine.language = Locale.getDefault()
            }

            // Configure audio attributes for Navigation/Guidance
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                engine.setAudioAttributes(audioAttributes)
            }

            engine.setSpeechRate(_voiceSettings.value.speechRate)
            engine.setPitch(1.0f)

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })

            _isTtsReady.value = true
            Log.i(TAG, "TextToSpeech engine initialized successfully.")
        } else {
            Log.w(TAG, "TextToSpeech initialization returned failure status: $status")
            _isTtsReady.value = false
        }
    }

    /**
     * Ingests real-time stakeout guidance and utters appropriate auditory feedback.
     */
    fun onGuidanceUpdated(guidance: StakeoutGuidance?) {
        val settings = _voiceSettings.value
        if (!settings.isEnabled || guidance == null) return

        val cue = evaluator.evaluate(guidance, settings) ?: return
        speak(cue.text, isUrgent = cue.isUrgent)
    }

    /**
     * Speaks the given phrase via TTS.
     *
     * @param text The sentence to speak
     * @param isUrgent If true, interrupts any current speech immediately (QUEUE_FLUSH)
     */
    fun speak(text: String, isUrgent: Boolean = false) {
        if (!_voiceSettings.value.isEnabled) return

        _lastSpokenCue.value = text
        _cueHistory.value = (_cueHistory.value + text).takeLast(10)

        val engine = tts
        if (engine == null || !_isTtsReady.value) {
            // Audio engine not yet ready or unavailable
            return
        }

        val queueMode = if (isUrgent) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val utteranceId = UUID.randomUUID().toString()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val params = Bundle()
            engine.speak(text, queueMode, params, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = utteranceId
            @Suppress("DEPRECATION")
            engine.speak(text, queueMode, params)
        }
    }

    fun testSpeech() {
        speak("Testing voice feedback. Stop, point reached! Twenty centimeters.", isUrgent = true)
    }

    fun toggleVoice(): Boolean {
        val newEnabled = !_voiceSettings.value.isEnabled
        _voiceSettings.value = _voiceSettings.value.copy(isEnabled = newEnabled)
        if (!newEnabled) {
            stopSpeech()
            evaluator.reset()
        } else {
            speak("Voice stakeout guidance enabled.", isUrgent = true)
        }
        return newEnabled
    }

    fun setVoiceEnabled(enabled: Boolean) {
        _voiceSettings.value = _voiceSettings.value.copy(isEnabled = enabled)
        if (!enabled) {
            stopSpeech()
            evaluator.reset()
        }
    }

    fun setVoiceMode(mode: StakeoutVoiceMode) {
        _voiceSettings.value = _voiceSettings.value.copy(mode = mode)
    }

    fun setSpeechRate(rate: Float) {
        val clampedRate = rate.coerceIn(0.75f, 2.0f)
        _voiceSettings.value = _voiceSettings.value.copy(speechRate = clampedRate)
        tts?.setSpeechRate(clampedRate)
    }

    fun setCadenceSeconds(seconds: Int) {
        val clamped = seconds.coerceIn(1, 10)
        _voiceSettings.value = _voiceSettings.value.copy(cadenceSeconds = clamped)
    }

    fun stopSpeech() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
    }

    fun resetState() {
        evaluator.reset()
    }

    fun shutdown() {
        try {
            stopSpeech()
            tts?.shutdown()
            tts = null
            _isTtsReady.value = false
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down TextToSpeech", e)
        }
    }

    companion object {
        private const val TAG = "StakeoutSpeech"
    }
}
