const { test, before, after } = require("node:test");
const assert = require("node:assert/strict");
const { createProxyServer } = require("./proxy-server");

let server;
let baseUrl;

before(async () => {
  process.env.EXTERNAL_API_KEY = "server_only_secret_test_key";
  server = createProxyServer();
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const address = server.address();
  baseUrl = `http://127.0.0.1:${address.port}`;
});

after(async () => {
  if (server) {
    await new Promise((resolve) => server.close(resolve));
  }
});

test("GET /api/v1/health confirms client API keys removed and never leaks server secret", async () => {
  const res = await fetch(`${baseUrl}/api/v1/health`);
  assert.equal(res.status, 200);
  const body = await res.json();
  assert.equal(body.status, "ok");
  assert.equal(body.clientApiKeysRemoved, true);
  assert.equal(body.serverEnvConfigured, true);
  assert.equal(JSON.stringify(body).includes("server_only_secret_test_key"), false);
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
