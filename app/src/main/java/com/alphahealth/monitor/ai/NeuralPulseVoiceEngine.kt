package com.alphahealth.monitor.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * NeuralPulseVoiceEngine
 *
 * Real-time on-device voice recognition for the NeuralPulse health assistant.
 *
 * ARCHITECTURE — sourced from AI Edge Gallery HoldToDictateViewModel.kt:
 *   SpeechRecognizer (Android built-in, no cloud) + RecognizerIntent
 *   → EXTRA_PARTIAL_RESULTS = true → live transcription stream
 *   → onResults() → final text → onDone callback
 *   → onRmsChanged() → amplitude for animated audio visualiser
 *
 * USAGE:
 *   val voice = NeuralPulseVoiceEngine(context)
 *   voice.startListening(
 *     onPartial = { text -> liveTranscript = text },
 *     onDone    = { finalText -> sendToGemma(finalText) },
 *     onAmplitude = { amp -> updateWaveform(amp) }
 *   )
 *   voice.stopListening()  // call on button release (hold-to-talk pattern)
 *   voice.cancel()         // call when user slides away (cancel gesture)
 *
 * PERMISSION: RECORD_AUDIO must be granted before calling startListening().
 *
 * NOTE: SpeechRecognizer uses Google's on-device speech recognition model
 * (Android Speech API, local model downloaded by OS). No audio is sent to
 * any external server when the device language model is available on-device.
 */
class NeuralPulseVoiceEngine(private val context: Context) : RecognitionListener {

    private val TAG = "NeuralPulseVoice"

    // ── State ─────────────────────────────────────────────────────────────────
    data class VoiceState(
        val isListening:    Boolean = false,
        val partialText:    String  = "",
        val finalText:      String  = "",
        val amplitudeLevel: Int     = 0,   // 0–65535 for waveform visualizer
        val error:          String? = null
    )

    private val _state = MutableStateFlow(VoiceState())
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    // ── Callbacks ─────────────────────────────────────────────────────────────
    private var onPartialCallback:   ((String) -> Unit)? = null
    private var onDoneCallback:      ((String) -> Unit)? = null
    private var onAmplitudeCallback: ((Int)    -> Unit)? = null

    // ── SpeechRecognizer ──────────────────────────────────────────────────────
    private val speechRecognizer: SpeechRecognizer by lazy {
        SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(this@NeuralPulseVoiceEngine)
        }
    }

    private val recognizerIntent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE,       "en-US")
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,    1)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)  // Stream partial results live
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Begin speech recognition.
     * Call on button press-down (hold-to-talk pattern).
     *
     * @param onPartial   Called with live partial transcript during speech
     * @param onDone      Called with final confirmed transcript on release
     * @param onAmplitude Called with 0–65535 amplitude value for waveform UI
     */
    fun startListening(
        onPartial:   (String) -> Unit = {},
        onDone:      (String) -> Unit,
        onAmplitude: (Int)    -> Unit = {}
    ) {
        onPartialCallback   = onPartial
        onDoneCallback      = onDone
        onAmplitudeCallback = onAmplitude

        _state.update { it.copy(isListening = true, partialText = "", finalText = "", error = null) }
        speechRecognizer.startListening(recognizerIntent)
        Log.d(TAG, "Speech recognition started.")
    }

    /**
     * Stop listening and wait for final result.
     * Call on button release.
     */
    fun stopListening() {
        speechRecognizer.stopListening()
        _state.update { it.copy(isListening = false) }
        Log.d(TAG, "Speech recognition stopping.")
    }

    /**
     * Cancel without processing result.
     * Call when user slides finger away from button (cancel gesture).
     */
    fun cancel() {
        speechRecognizer.cancel()
        _state.update { it.copy(isListening = false, partialText = "", amplitudeLevel = 0) }
        Log.d(TAG, "Speech recognition cancelled.")
    }

    fun release() {
        speechRecognizer.destroy()
    }

    // ── RecognitionListener ───────────────────────────────────────────────────

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "Ready for speech.")
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "Speech begun.")
    }

    /**
     * Live amplitude — converted from RMS dB to 0–65535 for waveform animation.
     * Sourced from AI Edge Gallery convertRmsDbToAmplitude() function.
     */
    override fun onRmsChanged(rmsdB: Float) {
        val amplitude = convertRmsDbToAmplitude(rmsdB)
        _state.update { it.copy(amplitudeLevel = amplitude) }
        onAmplitudeCallback?.invoke(amplitude)
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        Log.d(TAG, "End of speech detected.")
    }

    /**
     * Live partial results — updates transcript in real-time as user speaks.
     * This is what makes the "live transcription" effect work.
     */
    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partial = matches?.firstOrNull() ?: ""
        _state.update { it.copy(partialText = partial) }
        onPartialCallback?.invoke(partial)
        Log.d(TAG, "Partial: $partial")
    }

    /**
     * Final confirmed result — fires on button release after stopListening().
     */
    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val finalText = matches?.firstOrNull() ?: _state.value.partialText
        _state.update { it.copy(
            isListening    = false,
            finalText      = finalText,
            partialText    = "",
            amplitudeLevel = 0
        )}
        onDoneCallback?.invoke(finalText)
        Log.d(TAG, "Final result: $finalText")
    }

    override fun onError(error: Int) {
        val errorMsg = when (error) {
            SpeechRecognizer.ERROR_AUDIO          -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT         -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK        -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH       -> "No speech detected"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
            SpeechRecognizer.ERROR_SERVER         -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error ($error)"
        }
        Log.e(TAG, "Recognition error: $errorMsg")
        _state.update { it.copy(isListening = false, error = errorMsg, amplitudeLevel = 0) }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Converts RMS dB to 0–65535 amplitude range for waveform visualizer.
     * Direct port from AI Edge Gallery HoldToDictateViewModel.kt
     */
    private fun convertRmsDbToAmplitude(rmsdB: Float): Int {
        val minDb = -2.0f
        val maxDb = 100.0f
        val clamped = rmsdB.coerceIn(minDb, maxDb)
        return ((clamped - minDb) * 65535f / (maxDb - minDb)).toInt()
    }
}
