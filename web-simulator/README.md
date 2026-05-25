# Web Simulator: Digital Twin Previewer

The `web-simulator/` directory contains an interactive, browser-based digital twin of the NeuralPulse ecosystem. This allows developers to test UX flows, biometric state transitions, and clinical data exports without requiring physical smartwatches or Android test hardware.

---

## 1. Simulated Subsystems

The simulator renders a side-by-side view of a Wear OS watch face and a companion mobile app dashboard:

* **Biomechanical Gait Simulator**: Simulates active running telemetry (ground contact time, vertical oscillation, and asymmetry metrics).
* **Clinical Report Generator**: Renders a glassmorphic preview of a secure pulmonology sleep apnea report, complete with raw desaturation traces and SHA-256 signatures.
* **Google Calendar Integration**: Triggers simulated "Fatigue Blocker" calendar invites when sympathetic exhaustion threshold metrics are breached.
* **Gemini Voice Agent Box**: Simulates voice-driven query prompts (e.g. "Check recovery status") and responses.
* **Sub-Millisecond Hardware Toggles**: Lets developers toggle GPU/NPU delegates, hardware FIFO batching, and Room database batch transactions to observe simulated performance HUD impact.

---

## 2. How to Run

No compilation, local Node.js servers, or package installation are required:

1. Locate the `web-simulator/index.html` file in your workspace.
2. Open **[index.html](file:///c:/Users/ashut/OneDrive/Documents/samsung%20health%20monitor%20app/web-simulator/index.html)** in any modern web browser (Chrome, Firefox, Safari, Edge).
3. Use the control panel at the bottom to inject simulated sensor data and trigger system-wide state changes.
