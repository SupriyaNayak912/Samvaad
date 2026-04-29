# Samvaad: Advanced Database Specification & Data Integrity Audit

## 1. Hybrid Storage Strategy: Media vs. Structured Metadata
Samvaad utilizes a **Bimodal Persistence Model** to handle the high-throughput requirements of media and the low-latency querying needs of structured user metrics.

### 1.1 Data Ledger & Type Definition
| Entity | Storage Type | Logic Provider | Retention |
| :--- | :--- | :--- | :--- |
| **User State** | NoSQL (Firestore) | `UserRepository` | Permanent |
| **Session Audits**| NoSQL (Firestore) | `SessionRepository` | Permanent |
| **Video Stream** | Internal File (mp4) | `VideoCapture` | Session-bound |
| **Audio PCM** | Internal File (wav) | `RandomAccessFile` | Session-bound |

---

## 2. NoSQL Schema & Cardinality (Google Firestore)
The cloud database follows a **User-Centric Hierarchical Schema**, designed for maximum data locality and zero-latency retrieval.

### 2.1 Collection Hierarchy
*   **`/users/{uid}`**: The primary account document. Stores `User.java` properties.
*   **`/users/{uid}/sessions/{sid}`**: A nested sub-collection adhering to **1:N Cardinality**. Every interview attempt is an atomic document containing the `SessionMetrics.java` object.
    *   **Advantage**: This structure allows for **Per-User Security Rules**, ensuring that UID-level isolation is enforced at the database level, not just the application level.

---

## 3. Data Integrity: The "Atomic Draft-to-Final" Pattern
To satisfy Pressman’s **Reliability** attribute, Samvaad implements an incremental persistence mechanism.

1.  **Draft State**: At `t=0` of a session, a document is initialized with `status: "IN_PROGRESS"`.
2.  **Incremental Telemetry**: WPM and Silence metrics are computed in memory but synchronized only after the final result is ready to minimize network overhead.
3.  **Finalization Merge**: Upon session conclusion, the `SessionRepository` performs a "Deep Merge," updating raw telemetry with the AI-generated qualitative verdict and marking the status as `COMPLETED`.

---

## 4. Schema Evolution & Future-Proofing
The NoSQL architecture is designed to handle **Schema Drift**. 
*   **Property Mapping**: Use of `@PropertyName` and `@Exclude` annotations in `SessionMetrics.java` ensures that if new fields (e.g., "Gaze Tracking") are added in the future, old session documents remain perfectly parseable with default null values.
*   **Audit Accuracy**: All temporal fields use the `com.google.firebase.Timestamp` object, which provides microsecond precision for chronological auditing.

---

## 5. Local Resource Management (Efficiency)
To prevent "Internal Storage Bloat," the app follows a strict **Ephemeral Resource Policy**. 
*   **Purge Logic**: Once the `SessionWorker` confirms that the session metadata and AI results are safely persisted to the cloud, the `purgeLocalVideo()` method is triggered via an asynchronous cleanup thread to reclaim gigabytes of disk space.
