# Biometrics Signal Processing Engine (Wear OS)

To achieve clinical-grade biometric accuracy, raw smartwatch sensor streams are filtered on the edge to eliminate motion artifacts (MA) before they are dispatched to the companion app.

---

## 1. 4th-Order Butterworth Bandpass Filter

The PPG sensor operates at a sampling frequency ($f_s$) of $25\text{ Hz}$. Motion artifacts typically introduce low-frequency drift ($< 0.5\text{ Hz}$) and high-frequency noise ($> 4.0\text{ Hz}$). We implement a 4th-order Butterworth bandpass filter to isolate the target physiological band ($0.5\text{ Hz} - 4.0\text{ Hz}$, equivalent to $30 - 240\text{ BPM}$).

### Difference Equation
The filter is implemented using a linear constant-coefficient difference equation:

$$
y[n] = \sum_{k=0}^{4} b_k x[n-k] - \sum_{l=1}^{4} a_l y[n-l]
$$

Where:
- $x[n]$ is the raw input PPG signal at index $n$.
- $y[n]$ is the filtered output PPG signal at index $n$.
- $b$ and $a$ are the filter coefficients optimized for the $25\text{ Hz}$ sampling rate.

### Filter Coefficients
The coefficients used in `SignalQualityFilter.kt` are:

```kotlin
val b = doubleArrayOf(0.0039, 0.0, -0.0078, 0.0, 0.0039)
val a = doubleArrayOf(1.0, -3.5185, 4.6793, -2.7875, 0.6272)
```

---

## 2. Statistical Signal Quality Index (SQI)

Raw PPG signal contours follow specific physiological profiles. When motion noise disrupts the sensor's optical contact, the signal contours flatten, clip, or experience erratic spikes, altering their statistical distribution.

We evaluate the Signal Quality Index (SQI) by calculating the **Kurtosis** of a rolling 3-second window (75 samples).

### Mathematical Definition of Kurtosis
Kurtosis measures the "tailedness" of the probability distribution of the signal:

$$
\text{Kurtosis} = \frac{\frac{1}{N} \sum_{i=1}^{N} (x_i - \mu)^4}{\sigma^4}
$$

Where:
- $\mu$ is the sample mean: $\mu = \frac{1}{N} \sum_{i=1}^{N} x_i$
- $\sigma$ is the standard deviation: $\sigma = \sqrt{\frac{1}{N} \sum_{i=1}^{N} (x_i - \mu)^2}$
- $N = 75$ (number of samples in the 3-second window).

### Physiological Gating
- A clean, resting PPG waveform exhibits a kurtosis value in the range of **$2.8 \le \text{Kurtosis} \le 5.2$**.
- If physical movement creates high-frequency jitter or offsets, the kurtosis shifts outside this range.
- The watch evaluates the SQI score:
  - **$\text{SQI} = 1.0$ (Clinical Grade)**: Kurtosis is within $2.8$ to $5.2$. Telemetry is marked as highly reliable.
  - **$\text{SQI} = 0.0$ (Noisy / Invalid)**: Kurtosis falls outside the range. The UI displays a warning asking the user to adjust the watch band fit.

---

## 3. Pulse Transit Time (PTT) and Vascular Compliance

To estimate vascular compliance and arterial stiffness dynamically on the edge, the system tracks the temporal relationship between electrical cardiac activation and peripheral mechanical pressure waves.

### Mathematical Formulation
Pulse Transit Time ($\text{PTT}$) is defined as the time interval between the initiation of the ventricular contraction (demarcated by the ECG R-Wave peak) and the arrival of the corresponding pulse wave at the wrist (demarcated by the PPG systolic peak):

$$
\text{PTT} = t_{\text{PPG\_Peak}} - t_{\text{ECG\_RWave}}
$$

Where:
- $t_{\text{ECG\_RWave}}$ is the epoch timestamp (in milliseconds) of the ECG R-Wave peak.
- $t_{\text{PPG\_Peak}}$ is the epoch timestamp (in milliseconds) of the optical PPG systolic peak.

### Physiological Boundaries & Artifact Rejection
Under normal physiological conditions, the Pulse Transit Time for a mature cardiovascular system ranges from $150\text{ ms}$ to $400\text{ ms}$:

$$
150\text{ ms} \le \text{PTT} \le 400\text{ ms}
$$

- **Normal Compliance**: PTT values within the physiological boundary ($150 - 400\text{ ms}$) are smoothed and utilized for estimating relative changes in vascular compliance and sympathetic nervous system tone.
- **Vascular Stiffness / Blood Pressure Shifts**: A downward trend in PTT represents increased pulse wave velocity ($\text{PWV} \propto \frac{1}{\text{PTT}}$), suggesting vascular constriction or elevated blood pressure.
- **Artifact Rejection**: Any measured temporal delta outside this range ($\Delta t < 150\text{ ms}$ or $\Delta t > 400\text{ ms}$) is rejected by the engine (returning `-1L`), as it typically represents a sensor shift, synchronization delay, or ectopic beat.

---

## 4. Graceful Signal Degradation (The Real-World "Wrist Shift")

During physical movement (such as high-intensity workouts or sleeping on one's arm), the smartwatch optical sensor may shift, losing skin contact. This results in continuous noise (`SQI = 0.0`) or flatlining data. 

To prevent downstream diagnostic calculations (like the Vulnerability Engine) from stalling or crashing due to missing data, we implement a **Degraded State Mode**:
- **Continuous Monitoring**: If the Kurtosis validity check fails repeatedly for more than **5 minutes**, the downstream engine flags a Degraded Signal state.
- **Biometric Fallback**: The algorithm gracefully falls back to parsing resting sleep baseline metrics collected during sleep cycles (defaulting to resting baseline EDA of $1.8\text{ uS}$ and heart rate of $65\text{ BPM}$).
- **Mechanical Tracking**: The system pivots to tracking passive mechanical data via the watch's accelerometer and gyroscope to estimate calorie burns and step dynamics, ignoring the noisy optical streams until a clean PPG signal returns (validated when Kurtosis returns to the $2.8 - 5.2$ range).
