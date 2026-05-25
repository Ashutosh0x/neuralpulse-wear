# Changelog

All notable changes to the NeuralPulse project will be documented in this file. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-05-25

### Added
* **Edge Digital Signal Processing**: 4th-order Butterworth bandpass filtering and Kurtosis validation on Wear OS (`:wear`).
* **Vascular Compliance (PTT)**: Continuous Pulse Transit Time calculation using ECG R-Wave and PPG peak delta synchronization.
* **Wear OS 7 Glance Widget**: AMOLED pure black Glance widgets using `RemoteCompose` to stream autonomic recovery indices.
* **Local Machine Learning**: Quantized MobileNetV3 food nutrition classification gate (92% score threshold) and 3-frame temporal consensus engine.
* **Android 16 Clinical Sync**: Clinical FHIR observations pipeline via `ClinicalDataOrchestrator` using the Android 16 Health Connect PHR features.
* **Home IoT Automation**: SmartThings/Matter climate integration (`SmartThingsAutomationBridge`) adjusting thermostat states upon verified sleep transitions.
* **Security Hardening**: AES-GCM local encryption via Android KeyStore, secure preferences wrapper, and `BiometricPrompt` authorization before clinical data exports.
* **Network Security Configuration**: SSL certificate pinning configurations for clinical API endpoints.
* **CI/CD Pipeline**: GitHub Actions workflow (`build.yml`) checking compiling and executing local unit tests on every PR.
* **Core Unit Test Suite**: JUnit 4 testing classes for filters, PTT calculations, consensus arrays, and dual-wearable priority rules.
* **Stub Shading Libraries**: Shaded local classes for Samsung Health, Health Connect, and MediaPipe to enable independent compiler bootstrapping.
