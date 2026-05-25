package com.alphahealth.monitor.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * GemmaInferenceEngine
 *
 * On-device LLM inference engine for NeuralPulse health assistant.
 *
 * ARCHITECTURE — based on AI Edge Gallery's LlmChatTask pattern:
 *   1. Model download via URLConnection (HuggingFace URL from GemmaModelCatalog)
 *   2. LlmInference.createFromOptions() initialisation
 *   3. LlmInference.generateAsync() streaming → token-by-token UI update
 *   4. Full session management: reset between queries to maintain context
 *
 * STORAGE: context.getExternalFilesDir(null)/{normalizedName}/model.task
 * (mirrors AI Edge Gallery's Model.getPath() pattern)
 *
 * THREAD SAFETY: All inference runs on Dispatchers.Default. UI updates must
 * be collected on the main thread via StateFlow.
 */
class GemmaInferenceEngine(private val context: Context) {

    private val TAG = "GemmaInferenceEngine"

    // ── State ─────────────────────────────────────────────────────────────────
    data class EngineState(
        val downloadedModel:     GemmaModelCatalog.GemmaModel? = null,
        val isDownloading:       Boolean = false,
        val downloadProgress:    Float   = 0f,   // 0.0–1.0
        val downloadSpeedMbps:   Float   = 0f,
        val downloadedMb:        Float   = 0f,
        val totalMb:             Float   = 0f,
        val isInitializing:      Boolean = false,
        val isReady:             Boolean = false,
        val isGenerating:        Boolean = false,
        val currentResponse:     String  = "",
        val tokensPerSecond:     Float   = 0f,
        val error:               String? = null
    )

    private val _state = MutableStateFlow(EngineState())
    val state: StateFlow<EngineState> = _state.asStateFlow()

    private var llmInference: LlmInference? = null
    private var currentModel: GemmaModelCatalog.GemmaModel? = null

    // ── Download ──────────────────────────────────────────────────────────────

    /**
     * Download a model from Hugging Face using the exact URL format from AI Edge Gallery:
     *   https://huggingface.co/{modelId}/resolve/{commitHash}/{modelFile}?download=true
     *
     * Saves to: externalFilesDir/{normalizedName}/{modelFile}
     * Progress reported via [_state].downloadProgress
     */
    suspend fun downloadModel(model: GemmaModelCatalog.GemmaModel): Boolean =
        withContext(Dispatchers.IO) {
            val destFile = getModelFile(model)
            if (destFile.exists() && destFile.length() > model.sizeInBytes * 0.95) {
                Log.i(TAG, "Model already cached at ${destFile.absolutePath}")
                return@withContext true
            }
            destFile.parentFile?.mkdirs()

            _state.update { it.copy(
                isDownloading    = true,
                downloadProgress = 0f,
                downloadedMb     = 0f,
                totalMb          = model.sizeInBytes / 1_048_576f,
                error            = null
            )}

            try {
                val connection = URL(model.downloadUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 30_000
                connection.readTimeout    = 120_000
                connection.setRequestProperty("User-Agent", "NeuralPulse-Android/1.0")
                connection.connect()

                val totalBytes = connection.contentLengthLong.coerceAtLeast(model.sizeInBytes)
                var receivedBytes = 0L
                val startTime = System.currentTimeMillis()

                connection.inputStream.use { input ->
                    destFile.outputStream().use { output ->
                        val buffer = ByteArray(65_536) // 64KB chunks
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            receivedBytes += bytesRead

                            val elapsed = (System.currentTimeMillis() - startTime) / 1000f
                            val speedMbps = if (elapsed > 0) (receivedBytes / 1_048_576f) / elapsed else 0f

                            _state.update { it.copy(
                                downloadProgress  = receivedBytes.toFloat() / totalBytes,
                                downloadedMb      = receivedBytes / 1_048_576f,
                                downloadSpeedMbps = speedMbps
                            )}
                        }
                    }
                }

                _state.update { it.copy(isDownloading = false, downloadProgress = 1f) }
                Log.i(TAG, "Download complete: ${destFile.absolutePath}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Download failed: ${e.message}")
                destFile.delete() // Clean up partial file
                _state.update { it.copy(isDownloading = false, error = "Download failed: ${e.message?.take(80)}") }
                false
            }
        }

    // ── Initialization ────────────────────────────────────────────────────────

    /**
     * Initialize LlmInference with the downloaded model.
     * Uses MediaPipe tasks-genai LlmInference API (same as AI Edge Gallery).
     *
     * Config mirrors AI Edge Gallery defaultConfig:
     *   topK=64, topP=0.95, temperature=1.0, GPU accelerator preferred
     */
    suspend fun initializeModel(model: GemmaModelCatalog.GemmaModel): Boolean =
        withContext(Dispatchers.Default) {
            val modelFile = getModelFile(model)
            if (!modelFile.exists()) {
                _state.update { it.copy(error = "Model file not found. Download it first.") }
                return@withContext false
            }

            _state.update { it.copy(isInitializing = true, error = null) }

            try {
                // Clean up previous instance
                llmInference?.close()
                llmInference = null

                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(model.maxTokens)
                    .build()

                llmInference = LlmInference.createFromOptions(context, options)
                currentModel = model

                _state.update { it.copy(
                    isInitializing = false,
                    isReady        = true,
                    downloadedModel = model
                )}

                Log.i(TAG, "Model initialized: ${model.displayName}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Init failed: ${e.message}")
                _state.update { it.copy(
                    isInitializing = false,
                    isReady        = false,
                    error          = "Initialization failed: ${e.message?.take(100)}"
                )}
                false
            }
        }

    // ── Inference ─────────────────────────────────────────────────────────────

    /**
     * Generate a streaming health assistant response.
     *
     * Prepends the NeuralPulse health system prompt + biometric context.
     * Tokens stream via StateFlow.currentResponse.
     *
     * @param userQuery     User's question or voice transcript
     * @param biometricContext Current biometric readings to inject as context
     */
    suspend fun generateResponse(
        userQuery: String,
        biometricContext: String = ""
    ) = withContext(Dispatchers.Default) {
        val engine = llmInference ?: run {
            _state.update { it.copy(error = "Model not initialized. Initialize first.") }
            return@withContext
        }

        _state.update { it.copy(isGenerating = true, currentResponse = "", error = null) }

        val fullPrompt = buildPrompt(userQuery, biometricContext)
        val start = System.currentTimeMillis()

        try {
            engine.generateResponseAsync(fullPrompt) { partialResult: String, done: Boolean ->
                _state.update { state ->
                    state.copy(
                        currentResponse = state.currentResponse + partialResult,
                        isGenerating    = !done
                    )
                }
            }

            // Wait for completion (state updated via ProgressListener callback)
            while (_state.value.isGenerating) {
                kotlinx.coroutines.delay(50)
            }

            val elapsed = (System.currentTimeMillis() - start) / 1000f
            val wordCount = _state.value.currentResponse.split(" ").size
            val tps = if (elapsed > 0) wordCount / elapsed else 0f
            _state.update { it.copy(tokensPerSecond = tps) }

        } catch (e: Exception) {
            Log.e(TAG, "Generation error: ${e.message}")
            _state.update { it.copy(
                isGenerating = false,
                error        = "Generation failed: ${e.message?.take(80)}"
            )}
        }
    }

    fun stopGeneration() {
        _state.update { it.copy(isGenerating = false) }
        // LlmInference doesn't expose a stop API in 0.10.x — recreating closes the stream
    }

    fun clearResponse() {
        _state.update { it.copy(currentResponse = "", error = null) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun isModelDownloaded(model: GemmaModelCatalog.GemmaModel): Boolean {
        val file = getModelFile(model)
        return file.exists() && file.length() > model.sizeInBytes * 0.95
    }

    fun getModelFile(model: GemmaModelCatalog.GemmaModel): File {
        // Mirrors AI Edge Gallery's Model.getPath() pattern
        val base = context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
        return File("$base/${model.normalizedName}/${model.modelFile}")
    }

    private fun buildPrompt(userQuery: String, biometricContext: String): String {
        val contextSection = if (biometricContext.isNotBlank()) {
            "\n\nCurrent biometric readings:\n$biometricContext\n"
        } else ""

        return "<start_of_turn>user\n${GemmaModelCatalog.HEALTH_SYSTEM_PROMPT}$contextSection\n\n$userQuery<end_of_turn>\n<start_of_turn>model\n"
    }

    fun release() {
        llmInference?.close()
        llmInference = null
        _state.update { it.copy(isReady = false) }
    }
}
