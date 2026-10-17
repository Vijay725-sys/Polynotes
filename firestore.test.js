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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

function validProfilePayload(uid, timestamp) {
  const ts = timestamp || new Date(Date.now() - 1000);
  return {
    userId: uid,
    name: "Rahul Sharma",
    college: "Govt. Polytechnic Dehradun",
    board: "UBTER",
    branch: "CIVIL",
    semester: 3,
    bookmarkedIds: ["res_01", "res_07"],
    completedTopics: ["Surveying Fundamental Principles"],
    createdAt: ts,
    updatedAt: ts,
  };
}

function validResourcePayload(uid, timestamp) {
  const ts = timestamp || new Date(Date.now() - 1000);
  return {
    userId: uid,
    subjectId: "ubter_ce_302",
    subjectName: "Concrete Technology",
    title: "Concrete Mix Design Solved Guide",
    unit: 3,
    type: "NOTES",
    documentUrl: "https://www.ubter.in",
    source: "Govt Polytechnic Roorkee",
    reviewStatus: "UNDER_REVIEW",
    uploadedAt: "Oct 2026",
    semester: 3,
    description: "IS 10262 mix design workflow and lab calculations.",
    topicsCovered: ["Mix Design", "IS 10262"],
    pageCountOrLength: "18 Pages",
    fileFormat: "PDF Document",
    createdAt: ts,
    updatedAt: ts,
  };
}

// --- SECURITY BOUNDS TESTS ---

test("Unauthenticated user: cannot read or write user profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));
});

test("Authenticated user: can create, read, and update their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  const initialTs = new Date(Date.now() - 2000);

  await assertSucceeds(profileRef.set(validProfilePayload(ALICE_UID, initialTs)));
  await assertSucceeds(profileRef.get());

  await assertSucceeds(
    profileRef.update({
      semester: 4,
      bookmarkedIds: ["res_01", "res_02", "res_07"],
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Authenticated user: cannot read or modify another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      name: "Hacked Name",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Shadow Update Test: rejects ghost fields on profile creation and update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertFails(
    profileRef.set({
      ...validProfilePayload(ALICE_UID),
      isAdmin: true,
    })
  );

  await assertSucceeds(profileRef.set(validProfilePayload(ALICE_UID)));
  await assertFails(
    profileRef.update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Authenticated user: can create, list, and delete their own cloud resources", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const resRef = aliceDb.collection("users").doc(ALICE_UID).collection("resources").doc("res_custom_1");

  await assertSucceeds(resRef.set(validResourcePayload(ALICE_UID)));
  await assertSucceeds(resRef.get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("resources").where("userId", "==", ALICE_UID).get()
  );
  await assertSucceeds(resRef.delete());
});

test("Cross-user isolation: Bob cannot read, list, or delete Alice's cloud resources", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const resRef = aliceDb.collection("users").doc(ALICE_UID).collection("resources").doc("res_custom_1");
  await assertSucceeds(resRef.set(validResourcePayload(ALICE_UID)));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("resources").doc("res_custom_1").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("resources").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("resources").doc("res_custom_1").delete()
  );
});
