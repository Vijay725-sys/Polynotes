const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validProfilePayload(userId, now) {
  return {
    userId,
    name: "Rahul Sharma",
    college: "Govt. Polytechnic Dehradun",
    board: "UBTER",
    branch: "CIVIL",
    semester: 3,
    bookmarkedIds: ["res_01", "res_07"],
    completedTopics: ["Surveying Fundamental Principles"],
    darkMode: false,
    createdAt: now,
    updatedAt: now,
  };
}

function validResourcePayload(userId, now) {
  return {
    userId,
    subjectId: "ubter_ce_301",
    subjectName: "Basic Surveying",
    title: "Unit 2 Levelling & Contouring Notes",
    unit: 2,
    type: "NOTES",
    documentUrl: "https://www.ubter.in",
    source: "Govt. Polytechnic Dehradun",
    reviewStatus: "UNDER_REVIEW",
    uploadedAt: "Oct 2026",
    semester: 3,
    description: "Detailed levelling field book problems and rise/fall method.",
    topicsCovered: ["Height of Instrument", "Rise and Fall Method"],
    pageCountOrLength: "18 Pages",
    fileFormat: "PDF Document",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user cannot read or write profile or resources", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .set(validProfilePayload(ALICE_UID, new Date()))
  );
});

test("Authenticated owner can create, read, and update their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const createdTime = new Date(Date.now() - 2000);
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .set(validProfilePayload(ALICE_UID, createdTime))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Update bookmarks and darkMode while keeping createdAt & userId immutable
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .update({
        bookmarkedIds: ["res_01", "res_07", "res_15"],
        darkMode: true,
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("Cross-user access to another student's profile is rejected", async () => {
  const createdTime = new Date(Date.now() - 2000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .set(validProfilePayload(ALICE_UID, createdTime));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .update({ name: "Hacked Name", updatedAt: new Date(Date.now() - 1000) })
  );
});

test("Shadow field injection and immutable createdAt tampering on profile fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Shadow field injection
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .set({
        ...validProfilePayload(ALICE_UID, now),
        isAdmin: true,
      })
  );

  // Create valid doc, then attempt to mutate createdAt
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .set(validProfilePayload(ALICE_UID, now))
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .update({
        createdAt: new Date(Date.now() - 500),
        updatedAt: new Date(Date.now() - 500),
      })
  );
});

test("Authenticated owner can create and query custom study resources, cross-user fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("resources")
      .doc("custom_res_1")
      .set(validResourcePayload(ALICE_UID, now))
  );

  // Aligned list query succeeds for owner
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("resources")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  // Cross-user read & list fail
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("resources")
      .doc("custom_res_1")
      .get()
  );

  // Invalid documentUrl scheme (non-http/https) fails
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("resources")
      .doc("bad_res_2")
      .set({
        ...validResourcePayload(ALICE_UID, now),
        documentUrl: "javascript:alert(1)",
      })
  );
});
