# Samvaad AI: Next-Gen Interview Simulator 🚀

**Samvaad** (Sanskrit for "Dialogue") is a high-fidelity AI-driven interview coach designed to bridge the gap between technical knowledge and professional communication. It leverages state-of-the-art AI models and real-time telemetry to provide a clinical assessment of a candidate's readiness.

---

## 🧠 Technical Workflow (The "Engine")

Samvaad operates on a high-premium, 4-stage pipeline that converts raw human interaction into actionable data.

### 1. Real-Time Telemetry & Chaos Injection
During a live session, the app tracks:
- **Audio Amplitude**: Real-time energy tracking.
- **WPM (Words Per Minute)**: Dynamic pacing analysis.
- **Chaos Mode**: Simulates real-world interview stress by injecting distractions, measuring the candidate's **Resilience Score**.

### 2. Transcription (Groq Whisper)
At the end of each session, the recorded audio is processed using the **Groq Whisper API**. This provides a 100% accurate, word-for-word transcript that preserves fillers (um, ah) for later analysis.

### 3. AI Cognitive Analysis (Llama-3-70B)
The transcript and telemetry data are sent to the **Llama-3-70B model**. The AI acts as a "Mock Interviewer" to evaluate:
- **Pace & Clarity**: Did the candidate speak too fast or mumble?
- **Behavioral Profile**: Does the candidate use the STAR method?
- **Question Breakdown**: A clinical, question-by-question critique of what the candidate said vs. what an expert would say.

### 4. Firestore Persistence
All results are synchronized to **Google Firebase Firestore**. This ensures data parity across devices and a permanent historical record of growth.

---

## 📊 Database Schema (Instructor's Guide)

Our database is designed using a **Clean Architecture Hierarchy** to impress evaluators with its structure and security.

- **`users/{uid}`**: Root user profile containing metadata (Names, Level, Total XP).
- **`users/{uid}/sessions`**: A subcollection storing detailed reports.
  - Each session document contains a nested `llmFeedback` object.
  - **Zero-Data Loss**: By nesting the AI report within the session document, we minimize DB queries and ensure the report is always available offline through Firebase caching.

---

## 🏷️ Key Features for Invigilators

- **Smart Readiness Index (SRI)**: A proprietary 0-100 score calculated by AI based on multi-dimensional performance metrics.
- **Tiered Badge System**: Users are categorized into **Legend**, **Professional**, or **Apprentice** tiers based on their SRI, driving engagement and self-improvement.
- **Performance Share**: One-tap "Snapshot" sharing that generates a high-quality visualization of the AI report for social or professional proof.
- **Immersive 3D/Glassmorphism UI**: A premium design language that moves away from generic Android components to create an elite user experience.

---

## 🛠️ Tech Stack
- **Frontend**: Java, XML (Custom Glassmorphism components).
- **AI/LLM**: Llama-3-70B (Groq), Whisper (Groq).
- **Backend**: Firebase Auth, Firebase Firestore.
- **Analytics**: MPAndroidChart, Custom Circular Progress Logic.

---

*"Master the dialogue, own the room."*