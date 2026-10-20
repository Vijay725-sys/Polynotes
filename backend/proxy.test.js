const { test, before, after, beforeEach } = require("node:test");
const assert = require("node:assert/strict");
const { createProxyServer, redactSecrets, resetRateLimits } = require("./proxy-server");

let server;
let baseUrl;

before(async () => {
  process.env.GEMINI_API_KEY = "AIzaSyTestServerOnlySecretKey1234567890";
  process.env.EXTERNAL_API_KEY = "server_only_secret_test_key";
  server = createProxyServer();
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const address = server.address();
  baseUrl = `http://127.0.0.1:${address.port}`;
});

beforeEach(() => {
  resetRateLimits();
});

after(async () => {
  if (server) {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("GET /api/v1/health confirms client API keys removed and never leaks GEMINI_API_KEY", async () => {
  const res = await fetch(`${baseUrl}/api/v1/health`);
  assert.equal(res.status, 200);
  const body = await res.json();
  assert.equal(body.status, "ok");
  assert.equal(body.clientApiKeysRemoved, true);
  assert.equal(body.geminiServerSideOnly, true);
  assert.equal(body.rateLimitingEnabled, true);
  assert.equal(body.serverEnvConfigured, true);
  const serialized = JSON.stringify(body);
  assert.equal(serialized.includes("server_only_secret_test_key"), false);
  assert.equal(serialized.includes("AIzaSyTestServerOnlySecretKey1234567890"), false);
});

test("redactSecrets strips GEMINI_API_KEY and AIza patterns from error messages", () => {
  const rawError =
    "Upstream failed for key=AIzaSyTestServerOnlySecretKey1234567890 and token=server_only_secret_test_key";
  const sanitized = redactSecrets(rawError);
  assert.equal(sanitized.includes("AIzaSyTestServerOnlySecretKey1234567890"), false);
  assert.equal(sanitized.includes("server_only_secret_test_key"), false);
  assert.equal(sanitized.includes("[REDACTED_SERVER_SECRET]"), true);
});

test("POST /api/v1/ai/summarize validates input and never leaks GEMINI_API_KEY on error", async () => {
  const invalidRes = await fetch(`${baseUrl}/api/v1/ai/summarize`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ topic: "" })
  });
  assert.equal(invalidRes.status, 400);

  const validRes = await fetch(`${baseUrl}/api/v1/ai/summarize`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      topic: "Bending Moment & Shear Force Diagrams",
      subjectName: "Strength of Materials"
    })
  });
  assert.equal(validRes.status, 200);
  const body = await validRes.json();
  assert.equal(body.status, "ok");
  assert.equal(typeof body.summary, "string");
  assert.equal(JSON.stringify(body).includes("AIzaSyTestServerOnlySecretKey1234567890"), false);
});

test("POST /api/v1/resources validates schema and stores valid resource", async () => {
  const validRes = await fetch(`${baseUrl}/api/v1/resources`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      title: "Unit 2 Levelling & Contouring Field Manual",
      subjectId: "ubter_ce_301",
      subjectName: "Basic Surveying",
      unit: 2,
      type: "NOTES",
      documentUrl: "https://www.ubter.in",
      source: "Govt Polytechnic Dehradun",
      semester: 3,
      description: "Rise and fall method solved examples"
    })
  });
  assert.equal(validRes.status, 201);
  const created = await validRes.json();
  assert.equal(created.status, "ok");
  assert.equal(created.resource.title, "Unit 2 Levelling & Contouring Field Manual");

  // Invalid URL scheme (javascript:) must be rejected with 400
  const invalidRes = await fetch(`${baseUrl}/api/v1/resources`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      title: "Malicious Resource",
      subjectId: "ubter_ce_301",
      subjectName: "Basic Surveying",
      unit: 1,
      type: "NOTES",
      documentUrl: "javascript:alert(1)",
      semester: 3
    })
  });
  assert.equal(invalidRes.status, 400);
});

test("PUT and GET /api/v1/profile syncs student profile and bookmarks without client API keys", async () => {
  const putRes = await fetch(`${baseUrl}/api/v1/profile?studentId=stu_101`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      name: "Rahul Sharma",
      college: "Govt. Polytechnic Dehradun",
      board: "UBTER",
      branch: "CIVIL",
      semester: 3,
      bookmarkedIds: ["res_01", "res_05"],
      completedTopics: ["Surveying Fundamental Principles"],
      darkMode: true
    })
  });
  assert.equal(putRes.status, 200);

  const getRes = await fetch(`${baseUrl}/api/v1/profile?studentId=stu_101`);
  assert.equal(getRes.status, 200);
  const data = await getRes.json();
  assert.equal(data.profile.name, "Rahul Sharma");
  assert.deepEqual(data.profile.bookmarkedIds, ["res_01", "res_05"]);
});

test("Rate limiter blocks excessive write/AI requests with HTTP 429", async () => {
  let hit429 = false;
  for (let i = 0; i < 12; i++) {
    const res = await fetch(`${baseUrl}/api/v1/report`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        resourceTitle: "Test Resource",
        reason: "Broken link check"
      })
    });
    if (res.status === 429) {
      hit429 = true;
      break;
    }
  }
  assert.equal(hit429, true);
});
