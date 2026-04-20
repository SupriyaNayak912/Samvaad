# Samvaad AI: Security & Data Privacy Audit

## 1. Security-by-Design Philosophy
Samvaad adheres to the principle of **Least Privilege** and **Defense in Depth**. The application is architected to minimize the attack surface and protect user metrics from unauthorized access.

---

## 2. API Key Management & Obfuscation
A critical security vulnerability in mobile development is the leakage of API keys in Version Control Systems (VCS).

*   **Logic**: Sensitive credentials (e.g., `GROQ_API_KEY`, `FIREBASE_API_KEY`) are stored in the `local.properties` file, which is explicitly ignored by `.gitignore`.
*   **Mechanism**: The `build.gradle` script late-binds these keys into the `BuildConfig` class during the compilation phase. This ensures that the keys are never stored as plain text in the codebase, preventing unauthorized consumption from the source repository.

---

## 3. Data Isolation: Firebase Security Identity
Security is enforced at the **Identity Layer** rather than the Application Layer.

*   **Hierarchical Isolation**: User data is stored in the `/users/{uid}/` path, where `{uid}` is the immutable Firebase Authentication ID.
*   **Security Rules (Draft)**: The system is designed to use Firestore Security Rules that validate:
    `allow read, write: if request.auth != null && request.auth.uid == userId;`
*   **Advantage**: This ensures that even if a malicious user captures the API request, they cannot "horizontally wander" into another candidate's private session metadata.

---

## 4. Resource Privacy: Ephemeral Media Storage
To protect the candidate’s visual and auditory privacy:
1.  **Local Encryption**: Video and audio recordings are stored in the app's **Internal Private Storage** (`context.getFilesDir()`), making them inaccessible to other applications or the system gallery.
2.  **Permanent Destruction**: Upon successful cloud synchronization, the `purgeLocalVideo()` method performs a hard delete of the `.mp4` and `.wav` binary files. No permanent local copies of the raw interview are retained on the device.

---

## 5. Network Privacy: TLS Encryption
All communication with the Groq API and Firebase Firestore is performed over **TLS 1.2/1.3 (HTTPS)**. 
*   **Interceptors**: The `HttpLoggingInterceptor` is strictly disabled in "Release Builds" to prevent sensitive transcripts from appearing in system logs (Logcat) during production use.
