# PolyNotes Security Specification — Server-Side Gemini & Zero-Client-Key Architecture

## 1. Zero Client-Side API Keys in Android APK & Public Sharing Readiness
- **Client-Side Key Removal**: All client-side secret plugins (`secrets-gradle-plugin` and `google-services`) have been removed from both root `build.gradle.kts` and `app/build.gradle.kts`.
- **`.env.example` Public Sharing Compliance**: All variables in `.env.example` (`# GEMINI_API_KEY=`, `# EXTERNAL_API_KEY=`, `# PORT=8787`) are commented out so zero secrets are injected into `BuildConfig` or the APK binary, allowing unrestricted public sharing in Google AI Studio.
- **Runtime & Unit Test Verification**: `AppSecurityAndHealthManager.hasZeroClientSideApiKeysInApk()` inspects `BuildConfig` via reflection to verify that zero `GEMINI`, `API_KEY`, `SECRET`, or `TOKEN` fields exist in the compiled APK.

## 2. Server-Side Exclusive Gemini API & Proxy Architecture (`backend/proxy-server.js` & `api/proxy.js`)
- **Server-Side Secret Storage**: `GEMINI_API_KEY` is stored and accessed exclusively on the backend proxy via `process.env.GEMINI_API_KEY`.
- **Header-Only Upstream Auth**: When calling `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent`, the backend passes the key via the `x-goog-api-key` HTTP header rather than URL query parameters.
- **Automatic Secret Redaction (`redactSecrets`)**: Every JSON response and error string on the server passes through `redactSecrets()`, which strips any occurrence of `process.env.GEMINI_API_KEY`, `process.env.EXTERNAL_API_KEY`, or any `AIza...` pattern so the frontend can never retrieve the secret via API responses or error messages.

## 3. Request Validation, Rate Limiting & Abuse Safeguards
- **Per-IP Sliding-Window Rate Limiter**:
  - Read endpoints (`GET /api/v1/health`, `GET /api/v1/resources`, `GET /api/v1/profile`): max 60 requests / minute per IP.
  - Write & AI endpoints (`POST /api/v1/ai/summarize`, `POST /api/v1/resources`, `PUT /api/v1/profile`, `POST /api/v1/report`): max 10 requests / minute per IP, returning `HTTP 429 Too Many Requests` with `Retry-After` headers.
- **Payload Size Cap**: Enforces a hard `16 KB` (`16,384` bytes) request body limit (`MAX_BODY_BYTES`).
- **Schema & URL Validation**: Rejects `javascript:`/`data:` URLs and enforces strict field length and enum bounds across all endpoints.
