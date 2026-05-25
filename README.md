# NeuralPulse Ecosystem

[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/)
[![Wear OS](https://img.shields.io/badge/Wear_OS-4285F4?style=flat-square&logo=wearos&logoColor=white)](https://developer.android.com/wear)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![SQLite](https://img.shields.io/badge/SQLite-003B57?style=flat-square&logo=sqlite&logoColor=white)](https://sqlite.org/)
[![SmartThings](https://img.shields.io/badge/SmartThings-15BFFF?style=flat-square&logo=smartthings&logoColor=white)](https://www.smartthings.com/)
[![FHIR](https://img.shields.io/badge/HL7_FHIR-C44F23?style=flat-square&logo=hl7&logoColor=white)](https://hl7.org/fhir/)
[![Gradle](https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=gradle&logoColor=white)](https://gradle.org/)

NeuralPulse is a premium, clinical-grade health monitoring ecosystem designed specifically for Android/Samsung Galaxy devices (smartphones, tablets) and Wear OS smartwatches (such as the Samsung Galaxy Watch series). 

The ecosystem establishes a high-performance biometrics telemetry bridge:
- **Mobile Companion App (`:app`)**: Runs natively on Android 15+ / One UI 7.0+ devices, orchestrating local edge-AI workloads, managing Room databases, and syncing clinical HL7 FHIR records via Android 16 Health Connect.
- **Watch Application (`:wear`)**: Runs natively on Wear OS 5+ / Samsung One UI 6 Watch+ smartwatches, connecting directly to the hardware sensor hub to stream raw high-frequency biometric data (PPG, ECG, EDA, and BIA).

By running real-time digital signal processing (DSP) like 4th-order Butterworth filters and statistical Kurtosis validation directly on the watch edge before transmitting via the Wearable Data Layer API, NeuralPulse ensures zero-latency, clinical-grade health tracking without draining device batteries or compromising user privacy.

---

## Technology Stack

- **Mobile Client**: Android 15 / 16 (Kotlin, Jetpack Compose, Material 3)
- **Watch Client**: Wear OS 5+ / Samsung One UI 6 Watch (Jetpack Compose for Wear OS)
- **Computer Vision & NLP**: Google MediaPipe tasks-vision (INT8 Quantized MobileNetV3) & tasks-genai (Local Gemma model)
- **Database**: Room SQLite with batched write transactions
- **System Interoperability**: Android 16 Health Connect with FHIR R4 observations schema
- **Biometric API**: Samsung Health SDK (libs/samsung-health-data-api.aar)

---

## System Architecture

The ecosystem leverages the Wearable Data Layer API to sync biometric data from the watch to the phone, resolves multi-device telemetry conflicts, processes local camera feeds, performs local language model inference, and exports validated records to the local Health Connect store.

```mermaid
graph TD
    subgraph Smartwatch Module [NeuralPulse wear Smartwatch :wear]
        PPG[Raw PPG Sensors - 25Hz] --> Filter[SignalQualityFilter<br/>Butterworth Bandpass & Kurtosis]
        EDA[Raw EDA Sensors - 1Hz] --> BioEngine[HighPerformanceBioEngine<br/>FIFO Batching & Recycling Pool]
        Filter -->|SQI Stream| BioEngine
    end

    subgraph Bluetooth Transport [Bluetooth Bus]
        BioEngine -->|Byte Payloads<br/>MessageClient| Transport[Wearable Data Layer]
    end

    subgraph Phone Companion [NeuralPulse Mobile Companion App :app]
        Transport -->|Binary Sync| Transporter[WatchDataTransporter]
        Camera[Camera Feed] -->|Bitmap Stream| Vision[HighPrecisionClassifier<br/>GPU Delegate & Consensus]
        Transporter -->|EDA / HR / SQI| Resolver[Dual-Device Resolution & Signal Gating]
        Ring[Smart Ring Telemetry] -->|EDA / Hydration / Temp| Resolver
        Resolver -->|Clean Signals| Main[MainActivity & VulnerabilityEngine]
        Resolver -->|Wrist Shift / MA| Degraded[Degraded State Fallback<br/>Resting Baseline & Mechanics]
        Degraded --> Main
        Vision -->|Scanned Food| Main
        Main -->|Buffered Telemetry| DB[(Room TelemetryDatabase)]
        Main -->|Gemma Prompt| Explain[OnDeviceExplainabilityEngine<br/>Local Gemma SLM]
        Explain -->|Systemic Recovery Budget text| Main
        Main -->|FHIR Observations| HealthConnect[HealthConnectFhirOrchestrator<br/>Android 16 Health Connect]
        Main -->|Recovery Blocker| Calendar[Google Calendar Scheduler]
        Main -->|Vocal queries| Gemini[Gemini AppFunctions Voice]
    end
```

---

## Subsystems and Edge Implementations

| Module | Feature Name | Core Technical Strategy | Technology / API |
| :--- | :--- | :--- | :--- |
| ![Wear OS](https://img.shields.io/badge/Wear_OS-4285F4?style=flat-square&logo=wearos&logoColor=white) | **4th-Order Butterworth Filter** | Bandpass filters raw PPG at 25Hz ($0.5\text{ Hz} - 4.0\text{ Hz}$) to isolate blood pulse and reject high/low noise. | ![Samsung](https://img.shields.io/badge/Samsung_SDK-0A59A4?style=flat-square&logo=samsung&logoColor=white) ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white) |
| ![Wear OS](https://img.shields.io/badge/Wear_OS-4285F4?style=flat-square&logo=wearos&logoColor=white) | **Statistical Kurtosis Gating** | Analyzes rolling 3s PPG window kurtosis ($2.8 \le K \le 5.2$) to separate biological pulses from motion noise. | ![Samsung](https://img.shields.io/badge/Samsung_SDK-0A59A4?style=flat-square&logo=samsung&logoColor=white) ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white) |
| ![Wear OS](https://img.shields.io/badge/Wear_OS-4285F4?style=flat-square&logo=wearos&logoColor=white) | **Vascular Compliance (PTT)** | Computes Pulse Transit Time delta ($150\text{ ms} - 400\text{ ms}$) between ECG R-wave and PPG peak. | ![Samsung](https://img.shields.io/badge/Samsung_SDK-0A59A4?style=flat-square&logo=samsung&logoColor=white) ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white) |
| ![Wear OS](https://img.shields.io/badge/Wear_OS-4285F4?style=flat-square&logo=wearos&logoColor=white) | **Proactive Ambient Widget** | Streams live recovery index to watch face canvas using Jetpack Glance and RemoteCompose. | ![Wear OS](https://img.shields.io/badge/Wear_OS-4285F4?style=flat-square&logo=wearos&logoColor=white) ![Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white) |
| ![Wear OS](https://img.shields.io/badge/Wear_OS-4285F4?style=flat-square&logo=wearos&logoColor=white) | **Zero-Allocation Recycling** | Employs static `BioDataHolder` recycling pool to completely eliminate GC pauses during telemetry processing. | ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white) |
| ![Wear OS](https://img.shields.io/badge/Wear_OS-4285F4?style=flat-square&logo=wearos&logoColor=white) | **Hardware FIFO Batching** | Configures hardware sensor hub to pool records in 3000ms cycles, allowing CPU sleep. | ![Samsung](https://img.shields.io/badge/Samsung_SDK-0A59A4?style=flat-square&logo=samsung&logoColor=white) |
| ![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white) | **INT8 Quantized Classifier** | Executes sub-2ms local food classification from camera frames on the device NPU/GPU. | ![Google](https://img.shields.io/badge/MediaPipe-4285F4?style=flat-square&logo=google&logoColor=white) |
| ![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white) | **Top-1% Score Gating** | Instantly rejects any single frame classification showing less than 92% confidence. | ![Google](https://img.shields.io/badge/MediaPipe-4285F4?style=flat-square&logo=google&logoColor=white) |
| ![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white) | **3-Frame Temporal Consensus** | Validates classifications by demanding 2/3 frame agreement before committing food logs to SQLite. | ![SQLite](https://img.shields.io/badge/SQLite-003B57?style=flat-square&logo=sqlite&logoColor=white) |
| ![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white) | **On-Device Gemma SLM** | Generates offline explainability summaries translating telemetry to user wellness suggestions. | ![Google](https://img.shields.io/badge/MediaPipe-4285F4?style=flat-square&logo=google&logoColor=white) |
| ![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white) | **PHR FHIR Integration** | Ingests and stores clinical-grade observation logs in HL7 FHIR format via Health Connect client. | ![FHIR](https://img.shields.io/badge/HL7_FHIR-C44F23?style=flat-square&logo=hl7&logoColor=white) ![Health Connect](https://img.shields.io/badge/Health_Connect-3DDC84?style=flat-square&logo=android&logoColor=white) |
| ![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white) | **SmartThings Sleep IoT** | Auto-triggers home climate cool-down (e.g. 19.5°C) upon multi-sensor deep sleep confirmation. | ![SmartThings](https://img.shields.io/badge/SmartThings-15BFFF?style=flat-square&logo=smartthings&logoColor=white) |
| ![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white) | **FDA Wellness Compliance** | Frames autonomic telemetry as a general wellness "Systemic Recovery Budget" (0-100) instead of medical diagnostics. | ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white) |
| ![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white) | **Graceful Signal Fallback** | Reverts to nocturnal baseline models or passive accelerometer steps when sensor contact is lost for >5m. | ![SQLite](https://img.shields.io/badge/SQLite-003B57?style=flat-square&logo=sqlite&logoColor=white) |
| ![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white) | **Conflict Resolution** | Coordinates multi-wearable inputs: Ring primary for nocturnal metrics; Watch primary for active hours. | ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white) |

---

## Project Structure

```
NeuralPulse/
├── app/                  # Android Companion Mobile App (:app)
│   └── src/main/java/com/alphahealth/monitor/
│       ├── dashboard/    # Jetpack Compose UI (One UI 6 Style Layouts)
│       ├── data/         # WatchDataTransporter, Room DB, VulnerabilityEngine
│       │   └── connect/  # HealthConnectFhirOrchestrator (Android 16 Client)
│       └── vision/       # FoodVisionEngine, HighPrecisionClassifier (MediaPipe)
├── wear/                 # Standalone Smartwatch App (NeuralPulse wear) (:wear)
│   └── src/main/java/com/alphahealth/monitor/wear/
│       ├── sensor/filter/# SignalQualityFilter (Butterworth & Kurtosis DSP)
│       ├── tracking/     # HighPerformanceBioEngine, RunningDynamicsEngine
│       └── presentation/ # WatchDashboardActivity (Circular Bezel Compose UI)
├── shared/               # Core Shared Kotlin Module (:shared)
│   └── src/main/java/com/alphahealth/monitor/shared/
│       └── SyncProtocols # Paths and keys for Bluetooth Wearable Data Client
└── web-simulator/        # standalone browser-based digital twin previewer
```

---

## Building the Ecosystem

1. **Gradle Imports**: Ensure settings include `:app`, `:wear`, and `:shared` modules in `settings.gradle.kts`.
2. **Local SDK Bindings**: Copy the proprietary Samsung SDK binaries (`samsung-health-data-api.aar` and `samsung-health-sensor-api.aar`) into the `libs/` folder inside the `:app` and `:wear` modules.
3. **Ingest Vision Model**: Place your retrained model `food_nutrition_v1.tflite` inside `app/src/main/assets/models/`.
4. **Android Developer Mode**: Turn on Developer Mode inside Samsung Health on both testing devices to enable raw SDK sensor reads.

---

## CI/CD Pipeline & Automated Release Signing

The project features a continuous integration pipeline configured in [build.yml](file:///.github/workflows/build.yml) that executes unit tests, builds debug and release APKs for both the phone companion app and the Wear OS watch app, and automatically generates GitHub Releases for pushes to the `master` branch.

### Dynamic APK Signing Architecture

To prevent build failures for contributors who do not possess the release keystore, the Gradle build scripts ([app/build.gradle.kts](file:///app/build.gradle.kts) and [wear/build.gradle.kts](file:///wear/build.gradle.kts)) use a dynamic signing configuration:
- If `release.jks` exists in the module root directory, Gradle automatically signs the release build with it.
- If `release.jks` is missing, Gradle compiles the release build without throwing an exception, outputting an unsigned release APK.
- The CI pipeline standardizes outputs into `app-release-final.apk` and `wear-release-final.apk` to handle both signed and unsigned scenarios uniformly.

### How to Configure Automated Releases & Signing

To set up fully-signed automated releases in your repository, follow these steps:

#### 1. Generate a Release Keystore
Run the following JDK tool command locally to generate a new signing keystore:
```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias neuralpulse-key
```
Note down your keystore password, key alias, and key password.

#### 2. Base64-Encode the Keystore
Encode the binary `release.jks` file to a Base64 string to store it securely in GitHub:
- **macOS/Linux**:
  ```bash
  base64 -i release.jks -o keystore.b64
  cat keystore.b64
  ```
- **Windows (PowerShell)**:
  ```powershell
  [Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks")) | Out-File -FilePath keystore.b64
  Get-Content keystore.b64
  ```

#### 3. Configure GitHub Repository Secrets
Go to your GitHub repository, navigate to **Settings > Secrets and variables > Actions**, and add the following repository secrets:

* `ANDROID_KEYSTORE_BASE64`: The full Base64-encoded string representing your keystore file.
* `ANDROID_KEYSTORE_PASSWORD`: The password set for the keystore container.
* `ANDROID_KEY_ALIAS`: The key alias (e.g., `neuralpulse-key`).
* `ANDROID_KEY_PASSWORD`: The password set for the specific key alias.

Once configured, any push to the `master` branch will trigger the pipeline, automatically decode the keystore, build signed APKs, and publish them directly to a release page on GitHub.

