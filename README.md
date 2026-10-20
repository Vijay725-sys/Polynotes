# PolyNotes — Polytechnic Diploma Study & Revision Portal

**Every Subject. Every Note. One Place.**

PolyNotes is a hybrid **Native Jetpack Compose + Full-Screen Web App** study portal for Indian Polytechnic Diploma students (focused on **UBTER Civil Engineering**), backed by a **Lightweight Serverless Backend Proxy (`/api/v1`)** so that **zero API keys are ever hardcoded or bundled inside the Android APK**.

---

## Architecture & Security Highlights

1. **Zero Client-Side API Keys in APK**
   - All API keys and secret-injection plugins have been removed from the Android client (`app/build.gradle.kts`, `BuildConfig`, and Android resources).
   - Verified automatically in `ExampleRobolectricTest` via `AppSecurityAndHealthManager.hasZeroClientSideApiKeysInApk()`.

2. **Lightweight Backend Proxy & Serverless Function (`backend/proxy-server.js` & `api/proxy.js`)**
   - Handles external API requests on the server side using `process.env.EXTERNAL_API_KEY`.
   - Exposes `/api/v1/health`, `/api/v1/resources`, `/api/v1/profile`, and `/api/v1/report` with strict schema validation.
   - The Android app communicates with the proxy via `PolyNotesBackendProxyClient` (`OkHttpClient`) and falls back seamlessly to local `SharedPreferences` storage when offline.

3. **Automated Tests**
   - **Backend Proxy Tests (Node.js)**:
     ```bash
     node --test backend/proxy.test.js
     ```
   - **Android JVM & Robolectric Tests**:
     ```bash
     gradle :app:testDebugUnitTest
     ```
