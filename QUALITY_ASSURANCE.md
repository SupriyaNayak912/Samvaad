# Samvaad AI: Quality Assurance & Testing Strategy

## 1. Testing Methodology
Samvaad utilizes a **V-Model Development Approach**, where every architectural requirement corresponds to a specific verification stage. The testing is bifurcated into automated verification of logic and human-centered validation of hardware constraints.

---

## 2. White-Box Testing (Structural Verification)
White-box testing focuses on the logic and mathematical correctness of the platform's "Cognitive Engine."

*   **Scoring Logic Validation**: The `ScoringEngine.java` is tested against synthetic telemetry data to ensure that weighted scores (e.g., resilience weighting vs. clarity weighting) adhere to the project's assessment rubric.
*   **Audio PCM Audit**: Automated verification of the `RandomAccessFile` write cycles to ensure that binary audio data is streamed correctly and the WAVE Header is injected without corrupting the file structure.

---

## 3. Black-Box Testing (Behavioral Verification)
Black-box testing focuses on the user experience and hardware-response reliability.

*   **Kinetic Gate Sensitivity**: Functional testing of the accelerometer pitch logic. The "Pass/Fail" criteria are based on whether the `Begin Session` button strictly gates access within the 70°–85° range across varying phone models.
*   **AI Resilience (Chaos Engine)**: Behavior-driven testing where the system is intentionally "Stress-Tested" with rapid AI interruptions. The goal is to verify that the `Telemetry.recoveryTimeMs` is captured with millisecond accuracy.

---

## 4. Integration & Regression Testing
*   **Groq API Handoff**: A core integration test that validates the synchronous handoff between Whisper (Transcription) and Llama-3 (Analysis).
*   **Media-Data Sync**: Verifying that the document `status` in Firestore updates atomically only *after* the local media has been successfully processed.

---

## 5. Performance Benchmarking
Following Robert Pressman’s **Efficiency** attribute, the app is benchmarked for:
1.  **Latency**: Time from "Finish" click to "Analysis Complete" notification (~3.5 to 5.0 seconds).
2.  **Memory Footprint**: Monitoring the `VideoCapture` heap usage to ensure no memory leaks occur during extended 10-minute sessions.
