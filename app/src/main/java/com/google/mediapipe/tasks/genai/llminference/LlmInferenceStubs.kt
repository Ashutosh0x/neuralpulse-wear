package com.google.mediapipe.tasks.genai.llminference

import android.content.Context

class LlmInference private constructor() {

    fun generateResponse(prompt: String): String {
        return "Stub response"
    }

    fun close() {}

    class LlmInferenceOptions private constructor(
        val modelPath: String,
        val maxTokens: Int
    ) {
        class Builder {
            private var modelPath: String = ""
            private var maxTokens: Int = 512

            fun setModelPath(path: String): Builder {
                this.modelPath = path
                return this
            }

            fun setMaxTokens(tokens: Int): Builder {
                this.maxTokens = tokens
                return this
            }

            fun build(): LlmInferenceOptions {
                return LlmInferenceOptions(modelPath, maxTokens)
            }
        }

        companion object {
            fun builder(): Builder {
                return Builder()
            }
        }
    }

    companion object {
        fun createFromOptions(context: Context, options: LlmInferenceOptions): LlmInference {
            return LlmInference()
        }
    }
}
