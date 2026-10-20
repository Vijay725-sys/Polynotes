# PolyNotes Security Specification — Backend Proxy & Zero-Client-Key Architecture

## 1. Zero Client-Side API Keys in Android APK
- **Client-Side Key Removal**: All API keys (`google-services.json`, `Secrets Gradle Plugin`, and `BuildConfig` key fields) have been completely removed from the Android client codebase and compiled APK.
- **Verification**: `AppSecurityAndHealthManager.hasZeroClientSideApiKeysInApk()` inspects `BuildConfig` at runtime and in unit tests to guarantee zero `API_KEY`, `SECRET`, or `TOKEN` fields are embedded in the binary.

## 2. Lightweight Backend Proxy & Serverless Function (`backend/proxy-server.js` & `api/proxy.js`)
- **Server-Side Secret Isolation**: All external API keys are read strictly on the backend proxy via `process.env.EXTERNAL_API_KEY` and are never returned in HTTP responses or bundled in client assets.
- **Endpoints**:
  - `GET /api/v1/health`: Health & security posture check confirming server-side environment variable configuration and zero client-side keys.
  - `GET /api/v1/resources`: Returns curated and community-submitted diploma study resources.
  - `POST /api/v1/resources`: Validates resource schema invariants before storing/forwarding via the server-side proxy.
  - `GET /api/v1/profile` & `PUT /api/v1/profile`: Syncs student profile, bookmarks, and topic completion progress without client-side API keys.
  - `POST /api/v1/report`: Forwards resource issue reports via the backend proxy.

## 3. Full-Stack Schema Contract & Input Validation
- Both the Android client (`AppSecurityAndHealthManager`) and the Node.js Backend Proxy (`validateProfilePayload`, `validateResourcePayload`) enforce identical bounds:
  - `title`: 4–140 chars
  - `documentUrl`: `http://` or `https://` only (blocks `javascript:` / XSS payloads)
  - `board`: `UBTER`, `BTEUP`, `HSBTE`, `MSBTE`
  - `branch`: `CIVIL`, `MECHANICAL`, `ELECTRICAL`, `CSE`
  - `semester` / `unit`: `1..6`
