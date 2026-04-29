# Samvaad AI: Technical Architecture & Logical Audit

Samvaad is a high-fidelity AI interview simulator designed to eliminate ambiguity in professional performance assessment. This document serves as the **Technical Defense Manual** for academic review, proving the robustness of the backend, database design, and AI-driven evaluation logic.

---

## 🏛️ 1. Core Philosophy: "The Strict Auditor"

Unlike basic simulators that use hardcoded math, Samvaad operates on a **Human-like Cognitive Pipeline**. We reject "Simple Leniency." If a candidate fails to participate meaningfully (e.g., provides short, one-word, or nonsense responses like 'booing'), the system is architected to detect this and assign a **Zero Score**.

### 🛡️ AI Truthfulness Guards
*   **Transcript-Anchored Evaluation**: The LLM (Llama-3-70B) is strictly instructed to evaluate content depth.
*   **Anti-Hallucination Trigger**: If the transcription from Groq Whisper is empty or nonsensical, the evaluation logic defaults to a "Non-Responsive" state, preventing the AI from generating "fake" encouragement.

---

## 🏗️ 2. Professional Database Architecture (Elite Sub-DB Schema)

The database is built using a **Hierarchical User-Centric Design** to ensure data locality, rapid access, and professional-grade security. This nested structure is superior to "flat" designs as it natively handles user privacy and query optimization.

### 📋 Unified Path: `/users/{uid}/sessions/`
Every session is an isolated document containing:
1.  **Immediate Persistence**: Stored "that very second" the session starts with a `status: IN_PROGRESS` flag. 
2.  **Telemetry Map**: Raw WPM, Silence Count, Chaos Hits, and Face Stability checks.
3.  **AI Verdict**: The definitive overall score and blunt feedback.
4.  **Heatmap Data**: Persistent Tracking of daily activity in `/users/{uid}/stats/heatmap` for historical audits.

---

## 📊 3. Performance Visualization: The Power Radar

The Dashboard features a **Performance Radar Chart**, which is the "Single Source of Truth" for readiness. It distinguishes between:
*   **Technical Depth**: Derived from the logical substance of the transcript.
*   **Communication Clarity**: Derived from pacing, filler words, and vocal confidence.
*   **Presence & Resilience**: Derived from sensor stability and chaos-recovery data.

---

## 🔍 4. Logical Event Audit (The Evidence)

| Event | Logic Location | Database Impact |
| :--- | :--- | :--- |
| **Session Init** | `LiveSessionActivity:L901` | **NEW** document created in Firestore immediately. |
| **Speech Generation**| `LiveSessionActivity:L501` | **PRE-FLIGHT** logic ensures voice engine is initialized. |
| **AI Analysis** | `LlmFeedbackEngine:L24` | **STRICT** Evaluation: Penalizes empty transcripts. |
| **Data Finalization**| `SessionRepository:L70` | Updates Draft ⮕ `COMPLETED`; Increments Heatmap. |

---

## 🏛️ 5. Invigilator Defense: Common Technical Queries

**Q: Why use sub-collections for sessions instead of a flat list?**
**A:** Sub-collections (`/users/uid/sessions`) are optimized for per-user security rules and query speed. It prevents "Mega-Collection" overhead and ensures a student can only access their own data. This is the **Industry Standard** for private metrics.

**Q: How does the app handle network failure during AI analysis?**
**A:** The app uses a "Retain Draft" strategy. If the analysis fails, the session remains in `IN_PROGRESS` state in Firestore with raw telemetry preserved. This proves the attempt happened even if the network dropped.

**Q: Why is there no "Mathematical Fallback" for scores?**
**A:** To eliminate ambiguity. A professional interview is a human experience; mathematical heuristics (like simple WPM) are insufficient for a "Verdict." We mandate AI Interpretation to ensure the highest evaluation standards.