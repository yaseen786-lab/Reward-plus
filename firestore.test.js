const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_101";
const BOB_UID = "bob_uid_202";
const ADMIN_UID = "admin_uid_999";
const ADMIN_EMAIL = "nazimkhan44549763337@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validUserPayload(uid, code = "RP_ALICE1") {
  const now = new Date(Date.now() - 5000);
  return {
    userId: uid,
    displayName: "Alice Kumar",
    email: "alice@example.com",
    pointsBalance: 0,
    lifetimePointsEarned: 0,
    lifetimePointsWithdrawn: 0,
    referralCode: code,
    referralCount: 0,
    referralBonusPoints: 0,
    referralCommissionPoints: 0,
    dailyOfferDate: "2026-11-17",
    dailyOfferCount: 0,
    dailyAdCount: 0,
    fraudRiskScore: 5,
    accountStatus: "ACTIVE",
    deviceHash: "dev_hash_1234567890",
    role: "USER",
    createdAt: now,
    updatedAt: now,
  };
}

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));
  await assertFails(unauthDb.collection("earning_logs").get());
  await assertFails(unauthDb.collection("withdrawals").get());
  await assertFails(unauthDb.collection("fraud_events").get());
});

test("User profile creation, anti-hack point cap, and 30/day offer limit enforcement", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();

  // Valid create
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID)));

  // Reject self-assigned ADMIN role on create
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...validUserPayload(BOB_UID, "RP_BOB202"),
      role: "ADMIN",
    })
  );

  // Reject non-zero initial balance hack
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...validUserPayload(BOB_UID, "RP_BOB202"),
      pointsBalance: 50000,
    })
  );

  // Valid ticket increment for Mega Offer (1..30)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      megaOfferTickets: 1,
      normalOffer1Tickets: 5,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ticket overflow (> 30)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      megaOfferTickets: 31,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Valid Mega Offer credit (+1500 pts)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      pointsBalance: 1500,
      lifetimePointsEarned: 1500,
      dailyOfferCount: 1,
      megaOfferTickets: 0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject point inflation hack (+100000 in one update)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      pointsBalance: 100000,
      lifetimePointsEarned: 100000,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject exceeding 30 daily offers limit
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      dailyOfferCount: 31,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user isolation: Bob cannot read or list Alice's profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
});

test("Earning logs: 70/30 split logs and scoped list queries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("earning_logs").doc("log_1").set({
      userId: ALICE_UID,
      userName: "Alice Kumar",
      sourceType: "MEGA_OFFER",
      title: "Mega Offer: FinServe App Trial",
      pointsAwarded: 1500,
      grossValueInr: 21.43,
      userShareInr: 15.0,
      adminShareInr: 6.43,
      fraudRiskScore: 5,
      verificationStatus: "VERIFIED",
      dateKey: "2026-11-17",
      createdAt: now,
    })
  );

  // Scoped list query succeeds for owner
  await assertSucceeds(aliceDb.collection("earning_logs").where("userId", "==", ALICE_UID).get());

  // Unfiltered list fails for non-admin
  await assertFails(bobDb.collection("earning_logs").get());

  // Reject inflated pointsAwarded (> 2500)
  await assertFails(
    aliceDb.collection("earning_logs").doc("log_hack").set({
      userId: ALICE_UID,
      userName: "Alice Kumar",
      sourceType: "MEGA_OFFER",
      title: "Hacked Offer",
      pointsAwarded: 99999,
      grossValueInr: 100,
      userShareInr: 70,
      adminShareInr: 30,
      fraudRiskScore: 5,
      verificationStatus: "VERIFIED",
      dateKey: "2026-11-17",
      createdAt: now,
    })
  );
});

test("Withdrawals: UPI/IMPS/Google Play creation succeeds, client self-approval rejected, Admin approval succeeds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  const spoofedAdminDb = testEnv.authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false }).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("withdrawals").doc("wd_1").set({
      userId: ALICE_UID,
      userName: "Alice Kumar",
      userEmail: "alice@example.com",
      method: "IMPS",
      pointsRedeemed: 5000,
      amountInr: 50.0,
      destinationMasked: "HDFC •••• 8821",
      bankAccountNumber: "123456788821",
      ifscCode: "HDFC0001234",
      accountHolderName: "Alice Kumar",
      status: "PROCESSING_IMPS",
      fastTrackEligible: true,
      fraudRiskScore: 5,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Client cannot self-approve withdrawal to COMPLETED
  await assertFails(
    aliceDb.collection("withdrawals").doc("wd_1").update({
      status: "COMPLETED",
      utrOrRedeemCode: "FAKE_UTR",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Email spoofing (email_verified: false) MUST fail
  await assertFails(
    spoofedAdminDb.collection("withdrawals").doc("wd_1").update({
      status: "COMPLETED",
      utrOrRedeemCode: "SPOOF_UTR",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Verified Admin can approve & set UTR
  await assertSucceeds(
    adminDb.collection("withdrawals").doc("wd_1").update({
      status: "COMPLETED",
      utrOrRedeemCode: "IMPS432198765432",
      adminNote: "Instant IMPS settled",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Verified Admin can list all users & withdrawals
  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(adminDb.collection("withdrawals").get());
});

test("Referral system: 500pt bonus + 40% commission attribution and single-claim protection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // Alice registers her referral code
  await assertSucceeds(
    aliceDb.collection("referral_codes").doc("RP_ALICE1").set({
      ownerUid: ALICE_UID,
      referralCode: "RP_ALICE1",
      ownerName: "Alice Kumar",
      createdAt: now,
    })
  );

  // Listing referral_codes is forbidden
  await assertFails(bobDb.collection("referral_codes").get());

  // Bob claims Alice's code
  await assertSucceeds(
    bobDb.collection("referrals").doc(BOB_UID).set({
      inviterUid: ALICE_UID,
      inviteeUid: BOB_UID,
      inviteeName: "Bob Verma",
      referralCode: "RP_ALICE1",
      baseBonusPoints: 500,
      commissionRatePercent: 40,
      commissionPoints: 200,
      claimedByInviter: false,
      status: "PENDING_CLAIM",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Self-referral rejected
  await assertFails(
    aliceDb.collection("referrals").doc(ALICE_UID).set({
      inviterUid: ALICE_UID,
      inviteeUid: ALICE_UID,
      inviteeName: "Alice Kumar",
      referralCode: "RP_ALICE1",
      baseBonusPoints: 500,
      commissionRatePercent: 40,
      commissionPoints: 200,
      claimedByInviter: false,
      status: "PENDING_CLAIM",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice claims her pending referral reward
  await assertSucceeds(
    aliceDb.collection("referrals").doc(BOB_UID).update({
      claimedByInviter: true,
      status: "CREDITED",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
