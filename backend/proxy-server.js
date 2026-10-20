/**
 * PolyNotes Lightweight Backend Proxy & Server-Side Gemini API Gateway
 * --------------------------------------------------------------------
 * Security Guarantees:
 * 1. GEMINI_API_KEY and EXTERNAL_API_KEY are read exclusively on the server
 *    via process.env and are NEVER exposed to the frontend or Android APK.
 * 2. All Gemini API calls run exclusively on the server side via HTTPS using
 *    the `x-goog-api-key` request header (never in URLs or client responses).
 * 3. Every response and error message passes through `redactSecrets()` so
 *    secrets can never leak through error payloads or logs.
 * 4. Enforces per-IP sliding-window rate limiting, 16 KB body size caps, and
 *    strict schema validation to prevent API abuse.
 */

const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");
const { URL } = require("node:url");

const PUBLIC_SHARED_APP_URL =
  "https://ais-pre-xkizlrcvgd746t4pvwdm6a-599575110484.asia-east1.run.app";
const DEV_SHARED_APP_URL =
  "https://ais-dev-xkizlrcvgd746t4pvwdm6a-599575110484.asia-east1.run.app";

const VALID_BOARDS = ["UBTER", "BTEUP", "HSBTE", "MSBTE"];
const VALID_BRANCHES = ["CIVIL", "MECHANICAL", "ELECTRICAL", "CSE"];
const VALID_TYPES = ["NOTES", "PRACTICALS", "PYQS", "VIVA"];

const MAX_BODY_BYTES = 16 * 1024; // 16 KB hard limit against payload abuse
const RATE_LIMIT_WINDOW_MS = 60 * 1000; // 1 minute sliding window
const DEFAULT_RATE_LIMIT_MAX = 60; // 60 req/min for read/sync endpoints
const WRITE_AND_AI_RATE_LIMIT_MAX = 10; // 10 req/min for write & Gemini AI endpoints

// In-memory store for serverless/lightweight proxy state & rate limiters
const proxyStore = {
  profiles: new Map(),
  communityResources: [],
  reports: [],
  rateLimits: new Map()
};

/**
 * Redacts any server-side secrets or Google API key patterns from strings
 * before logging or returning error messages to the client.
 */
function redactSecrets(input) {
  if (typeof input !== "string") return "";
  let sanitized = input;
  const secretsToRedact = [
    process.env.GEMINI_API_KEY,
    process.env.EXTERNAL_API_KEY,
    process.env.BACKEND_UPSTREAM_SECRET
  ].filter((s) => typeof s === "string" && s.trim().length > 0);

  for (const secret of secretsToRedact) {
    sanitized = sanitized.split(secret).join("[REDACTED_SERVER_SECRET]");
  }
  // Also redact any Google API key pattern (AIza...)
  sanitized = sanitized.replace(/AIza[0-9A-Za-z\-_]{20,}/g, "[REDACTED_API_KEY]");
  return sanitized;
}

/**
 * Per-IP / per-bucket sliding window rate limiter.
 */
function checkRateLimit(clientKey, bucket, maxRequests) {
  const now = Date.now();
  const key = `${clientKey}:${bucket}`;
  const timestamps = proxyStore.rateLimits.get(key) || [];
  const validTimestamps = timestamps.filter((ts) => now - ts < RATE_LIMIT_WINDOW_MS);

  if (validTimestamps.length >= maxRequests) {
    proxyStore.rateLimits.set(key, validTimestamps);
    return {
      allowed: false,
      remaining: 0,
      retryAfterSeconds: Math.ceil(RATE_LIMIT_WINDOW_MS / 1000)
    };
  }

  validTimestamps.push(now);
  proxyStore.rateLimits.set(key, validTimestamps);
  return {
    allowed: true,
    remaining: Math.max(0, maxRequests - validTimestamps.length),
    retryAfterSeconds: 0
  };
}

function resetRateLimits() {
  proxyStore.rateLimits.clear();
}

function validateProfilePayload(body) {
  if (!body || typeof body !== "object") return false;
  if (typeof body.name !== "string" || body.name.trim().length < 1 || body.name.length > 80) return false;
  if (typeof body.college !== "string" || body.college.trim().length < 1 || body.college.length > 120) return false;
  if (!VALID_BOARDS.includes(body.board)) return false;
  if (!VALID_BRANCHES.includes(body.branch)) return false;
  if (typeof body.semester !== "number" || body.semester < 1 || body.semester > 6) return false;
  if (body.bookmarkedIds && (!Array.isArray(body.bookmarkedIds) || body.bookmarkedIds.length > 200)) return false;
  if (body.completedTopics && (!Array.isArray(body.completedTopics) || body.completedTopics.length > 200)) return false;
  return true;
}

function validateResourcePayload(body) {
  if (!body || typeof body !== "object") return false;
  if (typeof body.title !== "string" || body.title.trim().length < 4 || body.title.length > 140) return false;
  if (typeof body.subjectId !== "string" || body.subjectId.trim().length < 1 || body.subjectId.length > 64) return false;
  if (typeof body.subjectName !== "string" || body.subjectName.trim().length < 1 || body.subjectName.length > 100) return false;
  if (typeof body.unit !== "number" || body.unit < 1 || body.unit > 6) return false;
  if (!VALID_TYPES.includes(body.type)) return false;
  if (
    typeof body.documentUrl !== "string" ||
    (!body.documentUrl.startsWith("http://") && !body.documentUrl.startsWith("https://")) ||
    body.documentUrl.length < 8 ||
    body.documentUrl.length > 500
  ) {
    return false;
  }
  if (typeof body.semester !== "number" || body.semester < 1 || body.semester > 6) return false;
  return true;
}

function validateAiStudyRequest(body) {
  if (!body || typeof body !== "object") return false;
  if (typeof body.topic !== "string" || body.topic.trim().length < 2 || body.topic.length > 200) return false;
  if (body.subjectName !== undefined && (typeof body.subjectName !== "string" || body.subjectName.length > 100)) {
    return false;
  }
  return true;
}

function sendJson(res, statusCode, payload, extraHeaders = {}) {
  const safeBody = redactSecrets(JSON.stringify(payload));
  res.writeHead(statusCode, {
    "Content-Type": "application/json; charset=utf-8",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET, POST, PUT, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type, X-Client-Platform, X-Student-Id",
    "X-Content-Type-Options": "nosniff",
    "Cache-Control": "no-store",
    ...extraHeaders
  });
  res.end(safeBody);
}

function readJsonBody(req) {
  return new Promise((resolve, reject) => {
    let raw = "";
    req.on("data", (chunk) => {
      raw += chunk;
      if (raw.length > MAX_BODY_BYTES) {
        reject(new Error("Payload exceeds 16KB maximum limit"));
      }
    });
    req.on("end", () => {
      if (!raw) return resolve({});
      try {
        resolve(JSON.parse(raw));
      } catch (_err) {
        reject(new Error("Invalid JSON payload"));
      }
    });
    req.on("error", (err) => reject(new Error(redactSecrets(err.message || "Request stream error"))));
  });
}

/**
 * Server-side only Gemini API helper.
 * Uses process.env.GEMINI_API_KEY exclusively on the server and never exposes
 * the key or raw upstream error details to the client.
 */
async function callServerSideGeminiSummary(topic, subjectName) {
  const apiKey = process.env.GEMINI_API_KEY;
  const cleanTopic = String(topic).trim().slice(0, 200);
  const cleanSubject = String(subjectName || "Polytechnic Engineering").trim().slice(0, 100);

  if (!apiKey || apiKey.trim().length === 0) {
    return {
      source: "server-curated-fallback",
      summary: `Key revision points for ${cleanTopic} (${cleanSubject}): focus on standard IS code definitions, core formulas, neat labelled diagrams, and step-by-step numerical derivations for diploma semester exams.`
    };
  }

  try {
    const response = await fetch(
      "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent",
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "x-goog-api-key": apiKey.trim()
        },
        body: JSON.stringify({
          contents: [
            {
              parts: [
                {
                  text: `Provide a concise, 3-bullet exam revision summary for Polytechnic Diploma subject "${cleanSubject}" on topic: "${cleanTopic}".`
                }
              ]
            }
          ],
          generationConfig: {
            maxOutputTokens: 256,
            temperature: 0.3
          }
        })
      }
    );

    if (!response.ok) {
      throw new Error(`Upstream Gemini status ${response.status}`);
    }

    const data = await response.json();
    const text =
      data?.candidates?.[0]?.content?.parts?.[0]?.text ||
      `Key revision summary generated for ${cleanTopic}.`;

    return {
      source: "server-side-gemini",
      summary: redactSecrets(text)
    };
  } catch (_err) {
    // Never leak upstream error strings or keys to the client
    return {
      source: "server-curated-fallback",
      summary: `Key revision points for ${cleanTopic} (${cleanSubject}): review core definitions, standard units, and previous year diploma exam numerical problems.`
    };
  }
}

/**
 * Serverless / HTTP request handler.
 * Reads server-side API keys from process.env without ever exposing them to clients.
 */
async function handleProxyRequest(req, res) {
  if (req.method === "OPTIONS") {
    return sendJson(res, 200, { ok: true });
  }

  const parsedUrl = new URL(req.url || "/", "http://localhost");
  const pathname = parsedUrl.pathname;
  const clientIp =
    req.headers["x-forwarded-for"]?.split(",")[0]?.trim() ||
    req.socket?.remoteAddress ||
    "local-client";

  const isWriteOrAi =
    req.method === "POST" || req.method === "PUT" || pathname.startsWith("/api/v1/ai/");
  const bucket = isWriteOrAi ? "write_ai" : "read";
  const maxAllowed = isWriteOrAi ? WRITE_AND_AI_RATE_LIMIT_MAX : DEFAULT_RATE_LIMIT_MAX;

  const rateStatus = checkRateLimit(clientIp, bucket, maxAllowed);
  if (!rateStatus.allowed) {
    return sendJson(
      res,
      429,
      {
        status: "error",
        error: "Rate limit exceeded. Please wait before making additional requests."
      },
      {
        "Retry-After": String(rateStatus.retryAfterSeconds),
        "X-RateLimit-Remaining": "0"
      }
    );
  }

  // Server-side secret boolean check (boolean only — never returns the key value)
  const hasServerSecretConfigured = Boolean(
    process.env.GEMINI_API_KEY ||
      process.env.EXTERNAL_API_KEY ||
      process.env.BACKEND_UPSTREAM_SECRET
  );

  // 0. Public Shareable Web Portal HTML route (GET / or GET /public)
  if (req.method === "GET" && (pathname === "/" || pathname === "/public")) {
    const htmlPath = path.resolve(__dirname, "../app/src/main/assets/polynotes_webapp.html");
    if (fs.existsSync(htmlPath)) {
      const html = fs.readFileSync(htmlPath, "utf8");
      res.writeHead(200, {
        "Content-Type": "text/html; charset=utf-8",
        "Access-Control-Allow-Origin": "*",
        "Cache-Control": "public, max-age=300"
      });
      return res.end(html);
    }
  }

  // 1. Health & Security Posture Endpoint
  if (req.method === "GET" && pathname === "/api/v1/health") {
    return sendJson(res, 200, {
      status: "ok",
      service: "polynotes-backend-proxy",
      proxyArchitecture: "serverless-http-proxy",
      clientApiKeysRemoved: true,
      geminiServerSideOnly: true,
      rateLimitingEnabled: true,
      publicAccessEnabled: true,
      publicAppUrl: PUBLIC_SHARED_APP_URL,
      serverEnvConfigured: hasServerSecretConfigured,
      communityResourcesCount: proxyStore.communityResources.length,
      timestamp: new Date().toISOString()
    });
  }

  // 1b. Public Share Metadata Endpoint
  if (req.method === "GET" && pathname === "/api/v1/share") {
    return sendJson(res, 200, {
      status: "ok",
      publicAccess: true,
      allowAnonymousRead: true,
      publicAppUrl: PUBLIC_SHARED_APP_URL,
      devAppUrl: DEV_SHARED_APP_URL,
      shareTitle: "PolyNotes — Public Polytechnic Study Portal",
      shareMessage:
        "Access free UBTER & Polytechnic Diploma study notes, practical manuals, PYQs & viva guides on PolyNotes: " +
        PUBLIC_SHARED_APP_URL
    });
  }

  // 1c. Server-Side Exclusive Gemini Study Summary Endpoint
  if (req.method === "POST" && pathname === "/api/v1/ai/summarize") {
    try {
      const body = await readJsonBody(req);
      if (!validateAiStudyRequest(body)) {
        return sendJson(res, 400, {
          status: "error",
          error: "Invalid study topic request. Provide a topic between 2 and 200 characters."
        });
      }
      const result = await callServerSideGeminiSummary(body.topic, body.subjectName);
      return sendJson(res, 200, {
        status: "ok",
        source: result.source,
        summary: result.summary
      });
    } catch (err) {
      return sendJson(res, 400, {
        status: "error",
        error: redactSecrets(err.message || "Invalid request")
      });
    }
  }

  // 2. Fetch Community & Proxied Study Resources
  if (req.method === "GET" && pathname === "/api/v1/resources") {
    return sendJson(res, 200, {
      status: "ok",
      publicAccess: true,
      resources: proxyStore.communityResources
    });
  }

  // 3. Submit Study Resource via Backend Proxy
  if (req.method === "POST" && pathname === "/api/v1/resources") {
    try {
      const body = await readJsonBody(req);
      if (!validateResourcePayload(body)) {
        return sendJson(res, 400, {
          status: "error",
          error: "Schema validation failed for resource payload"
        });
      }

      const created = {
        id: body.id || `proxy_res_${Date.now()}`,
        subjectId: body.subjectId.trim(),
        subjectName: body.subjectName.trim(),
        title: body.title.trim(),
        unit: Number(body.unit),
        type: body.type,
        documentUrl: body.documentUrl.trim(),
        source: (body.source || "Student Contributor").trim(),
        reviewStatus: "UNDER_REVIEW",
        uploadedAt: body.uploadedAt || "Oct 2026",
        semester: Number(body.semester),
        description: (body.description || "").trim(),
        topicsCovered: Array.isArray(body.topicsCovered)
          ? body.topicsCovered.slice(0, 20)
          : [`${body.title.trim()} — Core Concepts`],
        pageCountOrLength: body.pageCountOrLength || "Community Submission"
      };

      proxyStore.communityResources.unshift(created);
      return sendJson(res, 201, {
        status: "ok",
        resource: created
      });
    } catch (err) {
      return sendJson(res, 400, {
        status: "error",
        error: redactSecrets(err.message || "Bad request")
      });
    }
  }

  // 4. Get or Sync Student Profile via Backend Proxy
  if (pathname === "/api/v1/profile") {
    const studentId =
      parsedUrl.searchParams.get("studentId") || req.headers["x-student-id"] || "default_student";

    if (req.method === "GET") {
      const existing = proxyStore.profiles.get(studentId) || null;
      return sendJson(res, 200, {
        status: "ok",
        studentId,
        profile: existing
      });
    }

    if (req.method === "PUT" || req.method === "POST") {
      try {
        const body = await readJsonBody(req);
        if (!validateProfilePayload(body)) {
          return sendJson(res, 400, {
            status: "error",
            error: "Schema validation failed for profile payload"
          });
        }

        const saved = {
          studentId,
          name: body.name.trim(),
          college: body.college.trim(),
          board: body.board,
          branch: body.branch,
          semester: Number(body.semester),
          bookmarkedIds: Array.isArray(body.bookmarkedIds) ? body.bookmarkedIds : [],
          completedTopics: Array.isArray(body.completedTopics) ? body.completedTopics : [],
          darkMode: Boolean(body.darkMode),
          updatedAt: new Date().toISOString()
        };
        proxyStore.profiles.set(studentId, saved);
        return sendJson(res, 200, {
          status: "ok",
          profile: saved
        });
      } catch (err) {
        return sendJson(res, 400, {
          status: "error",
          error: redactSecrets(err.message || "Bad request")
        });
      }
    }
  }

  // 5. Report Resource Issue via Backend Proxy
  if (req.method === "POST" && pathname === "/api/v1/report") {
    try {
      const body = await readJsonBody(req);
      if (!body.resourceTitle || !body.reason) {
        return sendJson(res, 400, {
          status: "error",
          error: "Missing resourceTitle or reason"
        });
      }
      const reportEntry = {
        id: `rep_${Date.now()}`,
        resourceTitle: String(body.resourceTitle).slice(0, 140),
        reason: String(body.reason).slice(0, 300),
        createdAt: new Date().toISOString()
      };
      proxyStore.reports.push(reportEntry);
      return sendJson(res, 201, {
        status: "ok",
        report: reportEntry
      });
    } catch (err) {
      return sendJson(res, 400, {
        status: "error",
        error: redactSecrets(err.message || "Bad request")
      });
    }
  }

  return sendJson(res, 404, {
    status: "error",
    error: `Unknown proxy route: ${req.method} ${pathname}`
  });
}

function createProxyServer() {
  return http.createServer((req, res) => {
    handleProxyRequest(req, res);
  });
}

if (require.main === module) {
  const port = Number(process.env.PORT || 8787);
  const server = createProxyServer();
  server.listen(port, "0.0.0.0", () => {
    console.log(`PolyNotes Public Shareable Backend Proxy listening on http://0.0.0.0:${port}`);
  });
}

module.exports = {
  handleProxyRequest,
  createProxyServer,
  validateProfilePayload,
  validateResourcePayload,
  validateAiStudyRequest,
  redactSecrets,
  resetRateLimits,
  PUBLIC_SHARED_APP_URL
};
