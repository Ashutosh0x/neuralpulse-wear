# Model Card: food_nutrition_v1.tflite

This document outlines the performance, accuracy benchmarks, and architectural design of the local food scan model integrated within the NeuralPulse Mobile Companion app (`:app`).

---

## 1. Model Metadata

* **Model Name**: `food_nutrition_v1.tflite`
* **Model Type**: Multi-class Image Classifier
* **Model Backbone**: MobileNetV3-Small (depth-wise separable convolutions)
* **Input Resolution**: $224 \times 224 \times 3$ pixels (RGB)
* **Classes**: 101 target food categories (custom subset aligned with nutritional dictionaries)
* **Quantization**: INT8 post-training quantization (fully quantized weights and activations)
* **File Size**: 4.2 MB (vs. 16.8 MB Float32 baseline)

---

## 2. Latency Benchmarks

Benchmarks were performed on target Android/Samsung devices (Galaxy S24, Galaxy S23) to assess execution speeds:

| Execution Delegate | Inference Latency (ms) | UI Thread Blocking |
| :--- | :--- | :--- |
| **NPU (Samsung ENN / NNAPI)** | 1.2 ms | None (Asynchronous Stream) |
| **GPU Delegate (Vulkan/GLES)** | 1.4 ms | None (Asynchronous Stream) |
| **CPU Interpreter (4 Threads)** | 34.8 ms | Handled on Background Executor |

---

## 3. Training & Validation

* **Dataset Source**: Fine-tuned on the Food-101 dataset with supplementary high-resolution nutrition samples (totaling 120,000 images).
* **Quantization Impact**:
  * *Float32 Accuracy (Top-1)*: 78.4%
  * *INT8 Accuracy (Top-1)*: 77.55%
  * *Accuracy Delta*: -0.85% (well within acceptable boundaries for consumer wellness applications)
* **Validation Gating (Gated UI)**:
  * NeuralPulse enforces a strict **92% Top-1 Confidence Gate**. Any classification result below 0.92 is immediately rejected to prevent false nutritional commits.
  * A **3-frame temporal consensus** (2/3 frame match) is layered on top, reducing false classifications due to motion blur or angle shift to near zero.
