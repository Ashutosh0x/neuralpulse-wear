package com.alphahealth.monitor.ai

/**
 * GemmaModelCatalog
 *
 * Sourced directly from the Google AI Edge Gallery open-source repository:
 * https://github.com/google-ai-edge/gallery
 *
 * URL template (confirmed from ModelAllowlist.kt line 80):
 *   https://huggingface.co/{modelId}/resolve/{commitHash}/{modelFile}?download=true
 *
 * All models are INT4 quantized for mobile deployment via MediaPipe tasks-genai.
 * Stored to: context.getExternalFilesDir(null)/gemma/{normalizedName}/{version}/
 */
object GemmaModelCatalog {

    data class GemmaModel(
        val name: String,
        val displayName: String,
        val modelId: String,          // Hugging Face repo ID
        val modelFile: String,        // filename on HF
        val commitHash: String,       // exact commit for reproducible download
        val sizeInBytes: Long,
        val estimatedPeakMemoryGb: Float,
        val maxTokens: Int = 1024,
        val description: String,
        val supportsImage: Boolean = false,
        val recommended: Boolean = false
    ) {
        /** Exact download URL used by AI Edge Gallery (ModelAllowlist.kt:80) */
        val downloadUrl: String get() =
            "https://huggingface.co/$modelId/resolve/$commitHash/$modelFile?download=true"

        val sizeGb: Float get() = sizeInBytes / 1_073_741_824f

        val normalizedName: String get() = name.replace(Regex("[^a-zA-Z0-9]"), "_")
    }

    /**
     * RECOMMENDED — smallest model, best for health assistant on-device
     * 554 MB download, ~2GB peak RAM — runs on any Android device with 4GB+ RAM
     */
    val GEMMA3_1B = GemmaModel(
        name              = "Gemma3-1B-IT-q4",
        displayName       = "Gemma 3 1B (Recommended)",
        modelId           = "litert-community/Gemma3-1B-IT",
        modelFile         = "Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task",
        commitHash        = "42d538a932e8d5b12e6b3b455f5572560bd60b2c",
        sizeInBytes       = 554_661_246L,
        estimatedPeakMemoryGb = 2.0f,
        maxTokens         = 1024,
        description       = "Gemma 3 1B — 4-bit quantized. Best for health Q&A on-device. 554 MB download.",
        recommended       = true
    )

    /**
     * BALANCED — Gemma 3n E2B (2B effective parameters), supports image + text
     * 3.1 GB download, ~5.5GB peak RAM — requires 8GB+ device
     */
    val GEMMA3N_E2B = GemmaModel(
        name              = "Gemma-3n-E2B-it-int4",
        displayName       = "Gemma 3n E2B (Multimodal)",
        modelId           = "google/gemma-3n-E2B-it-litert-preview",
        modelFile         = "gemma-3n-E2B-it-int4.task",
        commitHash        = "20250520",
        sizeInBytes       = 3_136_226_711L,
        estimatedPeakMemoryGb = 5.5f,
        maxTokens         = 4096,
        description       = "Gemma 3n E2B — multimodal (text + image). 3.1 GB download. Requires 8GB RAM.",
        supportsImage     = true
    )

    /**
     * POWER — Qwen 2.5 1.5B Q8, top-quality reasoning
     * 1.5 GB download, ~2.5GB peak RAM
     */
    val QWEN25_1B5 = GemmaModel(
        name              = "Qwen2.5-1.5B-q8",
        displayName       = "Qwen 2.5 1.5B (High Quality)",
        modelId           = "litert-community/Qwen2.5-1.5B-Instruct",
        modelFile         = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
        commitHash        = "19edb84c69a0212f29a6ef17ba0d6f278b6a1614",
        sizeInBytes       = 1_625_493_432L,
        estimatedPeakMemoryGb = 2.5f,
        maxTokens         = 1024,
        description       = "Qwen 2.5 1.5B — 8-bit quantized. Higher quality reasoning. 1.5 GB download."
    )

    /**
     * REASONING — DeepSeek R1 Distill 1.5B, chain-of-thought health reasoning
     * 1.8 GB download, ~2.5GB peak RAM
     */
    val DEEPSEEK_R1_1B5 = GemmaModel(
        name              = "DeepSeek-R1-1.5B-q8",
        displayName       = "DeepSeek R1 1.5B (Reasoning)",
        modelId           = "litert-community/DeepSeek-R1-Distill-Qwen-1.5B",
        modelFile         = "DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv4096.task",
        commitHash        = "e34bb88632342d1f9640bad579a45134eb1cf988",
        sizeInBytes       = 1_833_451_520L,
        estimatedPeakMemoryGb = 2.5f,
        maxTokens         = 1024,
        description       = "DeepSeek R1 1.5B — chain-of-thought reasoning. Best for complex health analysis. 1.8 GB."
    )

    /** All available models ordered by recommended first */
    val ALL: List<GemmaModel> = listOf(GEMMA3_1B, QWEN25_1B5, DEEPSEEK_R1_1B5, GEMMA3N_E2B)

    /**
     * System prompt for NeuralPulse health assistant context.
     * Injected before every user query.
     */
    const val HEALTH_SYSTEM_PROMPT = """You are NeuralPulse AI, an advanced on-device health intelligence assistant embedded in the NeuralPulse wearable ecosystem.

Your role:
- Analyse biometric data: HRV, SpO2, EDA (galvanic skin conductance), sleep metrics, food scan results
- Provide actionable recovery and nutrition recommendations
- Explain autonomic nervous system patterns in plain language
- Suggest lifestyle optimisations based on sensor data

Rules:
- Be concise (3-5 sentences max per response unless asked for detail)
- Never diagnose, treat, or replace medical advice — always recommend consulting a doctor for clinical decisions
- All your processing happens 100% on-device; no data leaves the phone
- Use precise numbers when given biometric context"""
}
