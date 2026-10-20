/**
 * PolyNotes Lightweight Backend Proxy & Serverless Function
 * ---------------------------------------------------------
 * Keeps all external API keys (e.g., process.env.EXTERNAL_API_KEY) strictly on
 * the server side so that ZERO API keys are ever bundled into the Android APK.
 */

const http = require("node:http");
const { URL } = require("node:url");

const VALID_BOARDS = ["UBTER", "BTEUP", "HSBTE", "MSBTE"];
const VALID_BRANCHES = ["CIVIL", "MECHANICAL", "ELECTRICAL", "CSE"];
const VALID_TYPES = ["NOTES", "PRACTICALS", "PYQS", "VIVA"];

// In-memory store for serverless/lightweight proxy state
const proxyStore = {
  profiles: new Map(),
  communityResources: [],
  reports: []
};

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

function sendJson(res, statusCode, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(statusCode, {
    "Content-Type": "application/json; charset=utf-8",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET, POST, PUT, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type, X-Client-Platform, X-Student-Id"
  });
  res.end(body);
}

function readJsonBody(req) {
  return new Promise((resolve, reject) => {
    let raw = "";
    req.on("data", (chunk) => {
      raw += chunk;
      if (raw.length > 100_000) {
        reject(new Error("Payload too large"));
      }
    });
    req.on("end", () => {
      if (!raw) return resolve({});
      try {
        resolve(JSON.parse(raw));
      } catch (err) {
        reject(new Error("Invalid JSON payload"));
      }
    });
    req.on("error", reject);
  });
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

  // Server-side secret read from environment variables (never returned to client)
  const hasServerSecretConfigured = Boolean(
    process.env.EXTERNAL_API_KEY || process.env.BACKEND_UPSTREAM_SECRET
  );

  // 1. Health & Security Posture Endpoint
  if (req.method === "GET" && pathname === "/api/v1/health") {
    return sendJson(res, 200, {
      status: "ok",
      service: "polynotes-backend-proxy",
      proxyArchitecture: "serverless-http-proxy",
      clientApiKeysRemoved: true,
      serverEnvConfigured: hasServerSecretConfigured,
      communityResourcesCount: proxyStore.communityResources.length,
      timestamp: new Date().toISOString()
    });
  }

  // 2. Fetch Community & Proxied Study Resources
  if (req.method === "GET" && pathname === "/api/v1/resources") {
    return sendJson(res, 200, {
      status: "ok",
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
        error: err.message || "Bad request"
      });
    }
  }

  // 4. Get or Sync Student Profile via Backend Proxy
  if (pathname === "/api/v1/profile") {
    const studentId = parsedUrl.searchParams.get("studentId") || req.headers["x-student-id"] || "default_student";

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
          error: err.message || "Bad request"
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
        error: err.message || "Bad request"
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
    console.log(`PolyNotes Lightweight Backend Proxy listening on http://0.0.0.0:${port}`);
  });
}

module.exports = {
  handleProxyRequest,
  createProxyServer,
  validateProfilePayload,
  validateResourcePayload
};
