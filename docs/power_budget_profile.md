# Power Budget & Battery Profiling Guide

High-frequency biometric tracking (such as 25Hz PPG monitoring) and local on-device machine learning inference (MobileNet, Gemma) represent significant electrical draw. To achieve multi-day battery life, NeuralPulse implements strict power optimization and background profiling rules.

---

## 1. Standalone Smartwatch App Power Strategy (`:wear`)

### Hardware FIFO Batching
Continuous CPU wakeups for sensor readings degrade battery performance. NeuralPulse enforces hardware-level data pooling:
* **Sensor Hub Batching**: The watch app utilizes `setBatchProcessingGroup(3000)` on the Samsung BioActive sensor service.
* **Low-Power FIFO**: Raw 25Hz PPG data is collected in a low-power hardware memory cache. The main Wear OS CPU is kept in a deep sleep state for 3000ms intervals, waking up briefly to process the 75-sample buffer.
* **Impact**: Decreases sensor-related CPU wake cycles from 1500 wakes/minute to 20 wakes/minute, reducing watch telemetry power consumption by up to 72%.

### Wear OS 7 Ambient Engine Gating
* **Live Updates API**: The watch widget (`NeuralPulseGlanceWidget`) uses Jetpack Glance. Biometric streams to the watch face are throttled to update only on significant state deltas (e.g. heart rate changes of $> 3\text{ BPM}$) during ambient mode.
* **AMOLED Conservation**: The user interface is strictly bound to pure black `#000000` background canvases. This allows active pixels to remain powered off on OLED displays, minimizing display draw to $<12\text{ mA}$.

---

## 2. Companion Mobile App Power Strategy (`:app`)

### Local Language Model (Gemma) Constraints
Running a local Large Language Model (Gemma) is CPU/GPU intensive. NeuralPulse applies strict runtime constraints:
* **Battery Gate**: Local Gemma LLM inference calls are throttled when the smartphone's battery drops below 20%, falling back to lightweight template-based explanations.
* **Thermal Mitigation**: A cooldown period of 60 seconds is enforced between sequential language model prompts to prevent thermal throttling on the NPU delegate.
* **State Restriction**: Analysis reports are batched and processed preferentially when the device is connected to an external power source.

---

## 3. Doze Mode & Background Scheduling

* **Doze Mode Compliance**: The app uses `WorkManager` with a `NetworkType.CONNECTED` and `RequiresCharging` constraint for clinical FHIR syncing, allowing Android to merge network requests during system maintenance windows.
* **AlarmManager Throttling**: Precision alerts (such as Google Calendar fatigue blockers) use non-exact alarms (`setAndAllowWhileIdle`) unless a critical desaturation event is detected, ensuring background processes are kept within battery-friendly limits.
