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

test("Unauthenticated user: cannot read user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Unauthenticated user: cannot read subjects", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("subjects").get());
});

test("Authenticated user: can create own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice Student",
      email: "alice@example.com",
      college: "Stanford University",
      course: "Computer Science",
      currentSemester: 3,
      createdAt: new Date(),
    })
  );
});

test("Cross-user isolation: Bob cannot read Alice's profile", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice",
      email: "alice@example.com",
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: can create and read subject", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const subDoc = aliceDb.collection("users").doc(ALICE_UID).collection("subjects").doc("sub_math101");

  await assertSucceeds(
    subDoc.set({
      id: "sub_math101",
      userId: ALICE_UID,
      name: "Calculus I",
      code: "MATH101",
      semester: 1,
      credits: 4,
      minAttendance: 75,
      createdAt: new Date(),
    })
  );

  await assertSucceeds(subDoc.get());
});

test("Cross-user isolation: Bob cannot read or write Alice's subject", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("subjects").doc("sub_math101").set({
      id: "sub_math101",
      userId: ALICE_UID,
      name: "Calculus I",
      code: "MATH101",
      semester: 1,
      credits: 4,
      minAttendance: 75,
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("subjects").doc("sub_math101").get()
  );
});

test("Schema validation: rejects subject without required fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("subjects").doc("sub_bad").set({
      id: "sub_bad",
      userId: ALICE_UID,
      // Missing name, code, semester, credits, minAttendance
      createdAt: new Date(),
    })
  );
});
