# PolyNotes Security Specification & Operational Posture

## 1. API Key & Secret Management Security
1. **Google Services API Key Restriction (`app/google-services.json`)**:
   - Restricted to Android package `com.aistudio.polynotes.vtubtr` and SHA-1 certificate fingerprint `3F:2C:CD:92:61:B0:7B:BB:B5:DD:01:9D:77:18:79:8C:97:BE:79:05`.
   - Protected by Zero-Trust Firestore Security Rules (`firestore.rules`): knowing the Firebase project ID or API key grants **zero** read or write access without a valid Google Sign-In OAuth ID token (`request.auth != null && request.auth.uid == userId`).
2. **Environment & Secret Isolation (`.env` / `.env.example`)**:
   - Managed via the Android Secrets Gradle Plugin (`com.google.android.libraries.mapsplatform.secrets-gradle-plugin`).
   - `.env` and `*-debug.log` are strictly gitignored in `.gitignore`.
   - `GEMINI_API_KEY` remains commented out in `.env.example` and added to `secrets.ignoreList` so unused keys are never packaged into the APK `BuildConfig`.

## 2. Firestore Data Invariants
1. **Per-User Profile Isolation (`/users/{userId}`)**:
   - Every profile document is keyed by the authenticated Firebase user's `uid` (`request.auth.uid == userId`).
   - `userId` and `createdAt` are immutable after creation.
   - `bookmarkedIds` and `completedTopics` are bounded lists (`size() <= 200`).
2. **Per-User Custom Resources Subcollection (`/users/{userId}/resources/{resourceId}`)**:
   - Every custom submitted study resource is stored under `/users/{userId}/resources/{resourceId}` and must have `userId == request.auth.uid`.
   - `userId` and `createdAt` are immutable after creation.
   - String lengths and enum values (`NOTES`, `PRACTICALS`, `PYQS`, `VIVA`) are strictly validated via `isValidCloudStudyResource()`.

## 3. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Profile Read**: `GET /users/alice_123` with `auth = null` -> REJECTED.
2. **Cross-User Profile Read (PII / Progress Leak)**: `GET /users/alice_123` as `bob_456` -> REJECTED.
3. **Identity Spoofing on Profile Create**: `SET /users/alice_123` as `alice_123` with `{ userId: "bob_456" }` -> REJECTED.
4. **Shadow Field Injection on Profile Create**: `SET /users/alice_123` with `{ isAdmin: true }` -> REJECTED via `hasOnly()`.
5. **Immutable `createdAt` Tampering on Profile Update**: `UPDATE /users/alice_123` changing `createdAt` -> REJECTED.
6. **Immutable `userId` Tampering on Profile Update**: `UPDATE /users/alice_123` changing `userId` -> REJECTED.
7. **Unbounded Array Exhaustion ("Denial of Wallet")**: `UPDATE /users/alice_123` with `bookmarkedIds` > 200 items -> REJECTED.
8. **Value Poisoning on Semester**: `UPDATE /users/alice_123` with `semester: 99` or `"three"` -> REJECTED.
9. **Cross-User Subcollection Resource Read/List**: `LIST /users/alice_123/resources` as `bob_456` -> REJECTED.
10. **Invalid Resource URL Injection**: `CREATE /users/alice_123/resources/res_1` with `documentUrl: "javascript:alert(1)"` -> REJECTED.
11. **Invalid Resource Enum Type**: `CREATE /users/alice_123/resources/res_1` with `type: "HACKED"` -> REJECTED.
12. **Future Timestamp Spoofing**: `CREATE /users/alice_123/resources/res_1` with `createdAt` in the future (`> request.time`) -> REJECTED.
