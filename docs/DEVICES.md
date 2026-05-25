# Supported Hardware & Emulator Configuration (DEVICES.md)

NeuralPulse is a production-grade, clinical biometrics tracking ecosystem. To ensure accessibility for open-source contributors who do not possess Samsung-specific wearable hardware, the app contains a unified **Hardware Abstraction Layer (HAL)**.

---

## 1. Supported Devices & Hardware Specifications

The high-frequency sensor streams (Butterworth DSP filters and kurtosis gating) require target hardware support inside the wearable's micro-sensor hub.

### Validated Wearable Targets
- **Samsung Galaxy Watch 7 / Watch Ultra** (One UI 6 Watch, Wear OS 5, BioActive Sensor v2)
- **Samsung Galaxy Watch 6 / Watch 6 Classic** (One UI 5 Watch, Wear OS 4, BioActive Sensor)
- **Samsung Galaxy Watch 5 / Watch 5 Pro** (One UI 4.5 Watch, Wear OS 3.5, BioActive Sensor)
- **Samsung Galaxy Watch 4 / Watch 4 Classic** (Wear OS 3.0, BioActive Sensor)

### Minimum Operating System Requirements
- **Wear OS smartwatch (`:wear`)**: Wear OS 3.0+ (API Level 30+).
- **Mobile companion app (`:app`)**: Android 15+ (API Level 35+) / Samsung One UI 7.0+.

---

## 2. Sensor APIs & Requirements

| Metric | Sensor Subsystem | Android/Samsung API | Required SDK / Version |
| :--- | :--- | :--- | :--- |
| **EDA Conductance** | Electrodermal Sweat Nodes | `SamsungHealthSensorType.EDA` | Samsung Health SDK v1.1.0+ |
| **Cell Hydration** | Bioelectrical Impedance (BIA) | `SamsungHealthSensorType.BIA` | Samsung Health SDK v1.1.0+ |
| **Heart Rate (PPG)** | Photoplethysmogram Hub | `SamsungHealthSensorType.PPG` | Samsung Health SDK v1.1.0+ |
| **Sleep Apnea Logs** | SpO2 Oxygen Desaturation | `HealthConnectClient.PHR` | Android 16 Health Connect PHR |

---

## 3. Contributing Without Hardware (Biometric Emulator HAL)

Contributors without a Samsung Galaxy Watch can build, test, and debug the entire ecosystem by engaging the pre-recorded biometric emulator fixtures.

### How the Biosensor HAL Works
The sensor pipeline is abstracts behind the `WearableSensorBridge` interface:
```kotlin
interface WearableSensorBridge {
    fun streamBioTelemetry(): Flow<TelemetrySignal>
    fun getActiveDeviceSource(): String
}
```

### Engaging the Playback Emulator
By default, when debugging in the emulator, the app instantiates the `MockWearableSensorBridge`. This reads prerecorded stress-drift biometrics arrays representing:
- Live skin sweat spikes (stress drift)
- Core tissue hydration shifts
- Real clinical heart rate anomalies

To compile and verify the app, simply toggle the simulation buttons inside the **Ecosystem Command Tab** control array. This executes the entire mathematical downstream (Vulnerability Index weighting and clinical alerts) without checking for actual Bluetooth hardware.
