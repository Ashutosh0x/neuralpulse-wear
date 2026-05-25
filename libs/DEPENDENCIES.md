# Dependency Guide & Bootstrapping

NeuralPulse utilizes a combination of open-source and proprietary wearable SDKs. To ensure that compilation is independent of binary distribution limitations (such as CI/CD environments missing access to local vendor libraries), the codebase incorporates a local API stubbing architecture.

---

## 1. Proprietary SDK Dependencies

For full production builds deployed to hardware, copy the Samsung Health SDK packages into their respective `libs/` folders:

| Module | Required Binary Library | Source | Target Path |
| :--- | :--- | :--- | :--- |
| `:app` | `samsung-health-data-api.aar` (v1.1.0) | Samsung Developer Portal | `app/libs/samsung-health-data-api.aar` |
| `:wear` | `samsung-health-data-api.aar` (v1.1.0) | Samsung Developer Portal | `wear/libs/samsung-health-data-api.aar` |
| `:wear` | `samsung-health-sensor-api.aar` (v1.2.0) | Samsung Developer Portal | `wear/libs/samsung-health-sensor-api.aar` |

---

## 2. Compilation Stubs (No-AAR Build)

If the proprietary `.aar` binaries are missing from the `libs/` folders, the project compiles cleanly anyway. This is achieved by shading the public packages of the proprietary APIs inside the source sets:

- **Samsung BioActive API**: Located in `:wear` under `wear/src/main/java/com/samsung/android/service/health/tracking/`.
- **Samsung Data API**: Located in `:app` under `app/src/main/java/com/samsung/android/health/data/`.
- **Health Connect Client / PHR**: Located in `:app` under `app/src/main/java/androidx/health/connect/client/`.
- **MediaPipe LLM Task**: Located in `:app` under `app/src/main/java/com/google/mediapipe/tasks/genai/llminference/`.

These local stubs match the class hierarchies and public methods signature of the SDK, allowing the Kotlin compiler to compile references without raw library assets.

---

## 3. Production Deployment Build Toggle

To swap between compile-time stubs and real binaries for production packaging:

1. Copy the AAR files into `libs/` as listed in section 1.
2. Uncomment the binary dependencies inside `app/build.gradle.kts` and `wear/build.gradle.kts`:
   ```kotlin
   // Uncomment for production build
   implementation(files("libs/samsung-health-data-api.aar"))
   ```
3. Exclude/delete the matching stub packages from compilation to prevent double-declaration errors:
   - Exclude the stub packages from the source sets in Gradle using `android.sourceSets` directives, or delete the folders before compiling the release package.
