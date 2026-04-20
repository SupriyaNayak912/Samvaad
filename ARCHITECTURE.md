# Samvaad: Advanced Architecture & Engineering Specification

## 1. Structural Paradigm: Layered MVC/MVVM Hybrid
Samvaad is engineered using a robust **Layered Architecture**, ensuring high cohesion and low coupling. This implementation adheres to the **Repository Pattern**, which abstracts the data-access layer from the business logic, fulfilling Robert Pressman’s principles of modularity.

### 1.1 Architectural Layers
| Layer | Responsibility | Primary Classes |
| :--- | :--- | :--- |
| **Presentation** | UI States, Navigation, Input Handling | `MainActivity`, `LiveSessionActivity`, `HomeFragment` |
| **Domain** | Business Logic, Scoring, Content Seeding | `ScoringEngine`, `DatabaseSeeder`, `LevelSystem` |
| **Repository** | Data Abstraction, Local/Remote Handoff | `SessionRepository`, `ScenarioRepository` |
| **Data Service** | Low-level IO, API Clients, NoSQL Drivers | `GroqApiClient`, `NotificationHelper`, `Firestore` |

---

## 2. Design Patterns & SOLID Compliance
The codebase leverages enterprise-grade design patterns to ensure long-term maintainability.

*   **Repository Pattern**: Utilized in `SessionRepository` to provide a clean API to the UI while managing complex operations between Firestore (Cloud) and `RandomAccessFile` (Local).
*   **Singleton Pattern**: Applied to `GroqApiClient` and `RetrofitClient` to ensure a single instance of the network stack, optimizing resource consumption.
*   **Strategy Pattern**: The `ScoringEngine` implements a multi-pillar strategy to calculate `overallScore` based on weighted inputs from Pace, Resilience, and Clarity.
*   **Single Responsibility Principle (SRP)**: Each class is strictly decoupled. For example, `NotificationHelper` has no awareness of session contents; it only receives a trigger to display an alert.

---

## 3. Concurrency Strategy: High-Fidelity Thread Isolation
To prevent "Main Thread Starvation" during real-time audio and sensor processing, Samvaad employs a **Non-Blocking Threaded Pipeline**.

### 3.1 Audio Processing Pipeline
1.  **UI Thread**: Manages the CameraX preview and ML Kit Face Detection.
2.  **Audio Executor (Background)**: A dedicated `ExecutorService` that captures PCM buffer data at 16kHz. This thread performs the RMS/dB math and writes to the disk via `RandomAccessFile`.
3.  **WPM Handler**: A background `Looper/Handler` that calculates "Words Per Minute" every 500ms without interrupting the visual feedback loop.

---

## 4. Hardware Integration: Cyber-Physical Logic
Samvaad enforces a **Kinetic State Machine** that gates application behavior based on raw physical telemetry.

### 4.1 Accelerometer Gatekeeper (Pitch Constraint)
*   **Algorithm**: The device pitch is derived in `onSensorChanged` via: 
    `Pitch = atan2(y, z) * (180/PI)`
*   **Gate Logic**: A professional interview posture is enforced between **70° and 85°**. Deviations trigger a "Posture Reset" interrupt, pausing the session and video recording to maintain data quality.

### 4.2 ML Kit Face Logic
The app utilizes **Firebase ML Kit Face Detection** as a biometric validator. The `ImageAnalysis.Analyzer` performs real-time vertex tracking to ensure the user is centered and attentive, fulfilling the "Presence & Composure" evaluation metric.
