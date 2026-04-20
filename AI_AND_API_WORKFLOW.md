# Samvaad: Advanced AI Integration & API Logic

## 1. The Dual-Model Inference Pipeline
Samvaad employs a **Bimodal AI Architecture** through the Groq API, leveraging specialized models for transcription and cognitive interpretation.

### 1.1 Model Selection Rationale
| Module | Model | Function | Technical Rationale |
| :--- | :--- | :--- | :--- |
| **Audio** | `whisper-large-v3-turbo` | Transcription | High-throughput PCM decoding with sub-second latency. |
| **Cognitive**| `llama-3.1-8b-instant` | Evaluation | High reasoning density vs. parameter count; optimized for real-time coaching. |

---

## 2. Prompt Engineering: The "Strict Auditor" Strategy
The `LlmFeedbackEngine` utilizes a sophisticated **System Instruction Set** to enforce objectivity and eliminate "Hallucinations."

### 2.1 Truthfulness Guards
*   **Transcript Anchoring**: The model is strictly instructed to evaluate *only* the content present in the Whisper-derived transcript.
*   **The "Zero-Tolerance" Injection**: If the transcript is empty or nonsensical (e.g., noise), the system is hard-coded via the prompt to assign a `Score of 0` for Clarity and Technical Depth. This converts a probabilistic AI into a binary assessment gate.

---

## 3. JSON Object Mode: Deterministic UI Hydration
To satisfy the requirements of a **Reliable Software System**, Samvaad mandates the use of **JSON Object Mode** (`response_format: { "type": "json_object" }`).

### 3.1 Rationale for Determinism
In standard completion modes, LLMs return free-form text which can break application parsers. By enforcing JSON Mode:
1.  The output is guaranteed to be a valid JSON object.
2.  The schema follows a strict contract (Summary, Strengths, Areas to Improve, Scores).
3.  **UI Resilience**: This allows UI components like the `RadarChart` and `FeedbackCarouselAdapter` to hydrate with 100% data integrity, preventing `NullPointerException` crashes common in loose AI integrations.

---

## 4. Networking & Resilience: Retrofit + OkHttp
The communication stack is built using the **Retrofit + OkHttp** architectural standard.

*   **Multipart Requests**: Audio data is streamed as binary blocks to minimize memory overhead during large session uploads.
*   **HttpLoggingInterceptor**: Utilized during the audit phase to verify that headers, tokens, and payloads adhere to strict API security standards.
*   **Timeout Policy**: A 60-second read-write timeout is enforced to account for variable mobile network conditions, ensuring the `onFailure` callback handles network drops gracefully.
