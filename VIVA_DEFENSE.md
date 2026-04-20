# Samvaad Project: Viva Voce Defense Cheat Sheet

This document serves as a high-level technical summary and justification for established design decisions, tailored for academic examiners.

---

## 1. Architectural Decisions
### Q1: Why use a 3-tier Layered Architecture?
**Answer**: It follows the principle of **Separation of Concerns**. By decoupling the UI (View), Repository (Logic), and Persistence (Data), the system becomes modular. If we decide to swap Firestore for a local SQLite database, we only modify the Data Layer—the rest of the app remains untouched.

### Q2: Why use `WorkManager` instead of a standard `Service` for background sync?
**Answer**: `WorkManager` is the recommended Android API for **Persistent Work**. It is "Battery-Aware" and guarantees execution even if the app process is killed or the device reboots. This is critical for session synchronization, ensuring that academic performance data is never lost due to process termination.

---

## 2. Hardware and Sensors
### Q3: What is the purpose of the 70°–85° pitch constraint?
**Answer**: This represents a "Professional Constraint." It ensures the candidate is seated upright with the device at eye level, replicating the ergonomic setup of a real-world video interview. Technically, it serves as a hardware gatekeeper for data quality; if the angle is poor, the camera cannot capture the face properly, leading to invalid results.

### Q4: How is UI freezing prevented during high-frequency sensor updates?
**Answer**: All sensor math and audio amplitude calculations are performed in a dedicated **Thread Pool (ExecutorService)**. The results are posted back to the Main Thread via a `Handler/Looper` only when a UI update is required. This maintains a smooth 60fps frame rate for the CameraX preview.

---

## 3. Data and Persistence
### Q5: Why store media locally and metadata on the cloud?
**Answer**: This is a **Resource Optimization** strategy. Streaming raw high-definition video to the cloud in real-time is bandwidth-expensive and prone to failure. By recording locally using `RandomAccessFile` and `VideoCapture`, we ensure 100% data fidelity. Metadata (scores, transcripts) is lightweight and sent to Firestore to enable cross-device progress tracking.

### Q6: What is the significance of the "Draft Save" at session start?
**Answer**: This is an implementation of **Defensive Programming**. By creating a document with the status `IN_PROGRESS` immediately, we ensure the system has an "Audit Trail" of every session attempt, even if the student’s phone crashes mid-interview.

---

## 4. Artificial Intelligence Integration
### Q7: Why utilize the 8B model over larger models (e.g., GPT-4 or 70B)?
**Answer**: Latency is a primary constraint for user experience. The Groq-hosted `llama-3.1-8b` offers **sub-second inference**, allowing the "AI Coaching Summary" to be generated almost instantly once the session ends, while still providing high-quality qualitative analysis.

### Q8: What role does "JSON Object Mode" play in your system?
**Answer**: It bridges the gap between probabilistic AI and deterministic Software Engineering. By forcing the LLM to return valid JSON, we can reliably parse the response into Java POJOs (like the `LlmFeedback` class) to populate our UI components (Radar Charts, Insight Cards) without application crashes.
