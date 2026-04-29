# Samvaad AI: Technical File & Function Ledger

This document serves as a "Quick Reference" for the technical architecture of the Samvaad platform. Every major component is mapped to its architectural role and mission-critical functions.

---

## 1. Core UI Components (Presentation Layer)
| File Name | Architectural Role | Mission-Critical Function |
| :--- | :--- | :--- |
| `LiveSessionActivity.java` | Main Interview Orchestrator | `onSensorChanged()`: Enforces the 70°-85° posture gate. |
| `HomeFragment.java` | Dashboard & Analytics | `loadBestScores()`: Hydrates the Radar Chart from Firestore. |
| `ScenariosFragment.java` | Content Management | `onScenarioSelected()`: Initializes the session metadata. |

---

## 2. Business Logic & Processors (Domain Layer)
| File Name | Architectural Role | Mission-Critical Function |
| :--- | :--- | :--- |
| `ScoringEngine.java` | Qualitative Evaluator | `calculateWeightedScore()`: Computes multidimensional results. |
| `LlmFeedbackEngine.java` | AI Orchestrator | `generateFeedback()`: Manages Llama-3 API requests. |
| `LevelSystem.java` | Gamification Logic | `getNextLevel()`: Calculates leveling XP based on best-scores. |

---

## 3. Data & Communication (Infrastructure Layer)
| File Name | Architectural Role | Mission-Critical Function |
| :--- | :--- | :--- |
| `SessionRepository.java` | Firestore Abstraction | `saveSession()`: Manages Atomic Merge of Drafts to Final. |
| `GroqApiClient.java` | API Client | `createChatCompletion()`: Direct interface to Llama/Whisper. |
| `SessionWorker.java` | Background Sync | `doWork()`: Triggers notifications after process completion. |
| `RandomAccessFile` (API) | Binary Audio Streaming | `writeAudioData()`: Low-level PCM disk streaming. |

---

## 4. Rapid-Fire Function Defense (VIVA Cheat Sheet)

| Function Name | What it does? | Why it exists? |
| :--- | :--- | :--- |
| `purgeLocalVideo()` | Deletes .mp4 and .wav files. | To maintain storage efficiency following Robert Pressman’s principles. |
| `startSoundWaveAnimation()` | Triggers real-time SoundWave UI. | To provide immediate visual feedback of microphone health. |
| `setJsonResponse()` | Forces AI to output JSON. | To ensure deterministic UI hydration and prevent app crashes. |
| `createNotificationChannel()` | Inits System Tray channel. | Requirement for Android 8.0+ for Delivering background results. |
| `checkAndSeed()` | Populates initial scenarios. | To ensure the app is functional and decoupled from hardcoded strings. |
