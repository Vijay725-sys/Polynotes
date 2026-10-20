# PolyNotes — Indian Polytechnic Diploma Study Portal

**Tagline:** *Every Subject. Every Note. One Place.*

PolyNotes is a mobile-first Android + Full-Screen Web application designed for Indian Polytechnic Diploma students (initially focused on **UBTER Civil Engineering**).

---

## 1. Resolution of All 10 Architectural & Operational Issues

1. **Zero-Friction Setup (Replaces Multi-Container Sharded Cluster Complexity)**:
   - Instead of requiring Docker Compose, Mongo sharded clusters, replica set initialization, and manual user scripts, PolyNotes runs on a **Serverless + Local-First Hybrid Architecture** using **Firebase Firestore Enterprise** (`ai-studio-android-polynote-1715a246-8014-49be-9f8f-e84126d1b49e`) with automatic on-device `SharedPreferences` and memory/disk fallback.
2. **Production-Grade Secret & API Key Security (`.env` & `google-services.json`)**:
   - Managed via the **Android Secrets Gradle Plugin** (`com.google.android.libraries.mapsplatform.secrets-gradle-plugin`).
   - `.env` and `*-debug.log` are enforced in `.gitignore`.
   - `GEMINI_API_KEY` is commented out in `.env.example` and added to `secrets.ignoreList` in `app/build.gradle.kts` so unused keys are never bundled into `BuildConfig`.
   - The Firebase API key in `app/google-services.json` is locked to Android package `com.aistudio.polynotes.vtubtr` + SHA-1 certificate (`3F:2C:CD:92:61:B0:7B:BB:B5:DD:01:9D:77:18:79:8C:97:BE:79:05`) and guarded by hardened Zero-Trust `firestore.rules`.
3. **Two-Tier Automated Test Suite & CI Readiness**:
   - **Tier 1 (JavaScript Rules Unit Tests)**: `firestore.test.js` validates all 8 Pillars of Hardened Firestore Rules and Dirty Dozen payloads in ~1s.
   - **Tier 2 (Android JVM Robolectric Tests)**: `PolyNotesRepositoryRuleTest.kt` and `ExampleRobolectricTest.kt` validate Kotlin repository CRUD, real-time flows, cross-user isolation, unauthenticated access rejection, and contract validation.
4. **Right-Sized Infrastructure**:
   - Uses managed Firestore collections (`/users/{userId}` and `/users/{userId}/resources/{resourceId}`) + local offline fallback instead of over-engineered sharded clusters.
5. **Strict Full-Stack API Contract Validation**:
   - `firebase-blueprint.json`, `Models.kt` (`UserProfileDoc`, `CloudStudyResource`), `AppSecurityAndHealthManager.validateProfileContract()` / `validateResourceContract()`, and `firestore.rules` enforce identical field names, types, bounds, and nullability across Android, WebView JS Bridge (`PolyNotesWebBridge`), and Firestore.
6. **Production Readiness & Runtime Health Checks**:
   - `AppSecurityAndHealthManager.runHealthCheck()` executes live health checks on startup and surfaces a **System Health & API Key Security Card** in the Profile tab.
7. **Documented Security Posture**:
   - Full security specification and adversarial test matrix documented in [`security_spec.md`](./security_spec.md).
8. **Zero Single-Point-of-Failure External Coupling**:
   - If network connectivity is unavailable, all 16 verified UBTER Civil Engineering resources, bookmarks, and topic completion progress continue functioning seamlessly from local storage.
9. **Structured JSON Observability & Error Logging**:
   - Every Firestore operation routes exceptions through `handleFirestoreError()` in `FirestoreErrorHandler.kt`, emitting structured JSON diagnostics (`error`, `operationType`, `path`, `authInfo`).
10. **1-Command Quick Start & Smoke Verification**:
    - Build & compile APK: `gradle assembleDebug`
    - Run Tier 1 Security Rules tests: `FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js`
    - Run Tier 2 Robolectric JVM tests: `gradle :app:testDebugUnitTest`
