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

test("Unauthenticated user cannot read or write products", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("products").get()
  );
});

test("Authenticated user can create and query their own products with matching userId filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const productRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("products")
    .doc("prod_1");

  await assertSucceeds(
    productRef.set({
      userId: ALICE_UID,
      name: "سماعة بلوتوث",
      barcode: "622100001",
      category: "إلكترونيات",
      costPrice: 15.0,
      sellingPrice: 25.0,
      stockQuantity: 20,
      minStockThreshold: 5,
      unit: "قطعة",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("products")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});

test("Authenticated user cannot read or modify another user's products", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("products")
      .doc("bob_prod")
      .set({
        userId: BOB_UID,
        name: "شاحن سريع",
        barcode: "622100002",
        category: "إلكترونيات",
        costPrice: 8.0,
        sellingPrice: 14.0,
        stockQuantity: 10,
        minStockThreshold: 3,
        unit: "قطعة",
        createdAt: new Date(),
        updatedAt: new Date(),
      });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("products")
      .doc("bob_prod")
      .get()
  );
});

test("Shadow update with ghost field on product is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const productRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("products")
    .doc("prod_ghost");

  await assertFails(
    productRef.set({
      userId: ALICE_UID,
      name: "منتج",
      barcode: "123",
      category: "عام",
      costPrice: 10,
      sellingPrice: 15,
      stockQuantity: 5,
      minStockThreshold: 2,
      unit: "قطعة",
      isVerifiedAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated user can create valid invoice and customer", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("customers")
      .doc("cust_1")
      .set({
        userId: ALICE_UID,
        name: "أحمد محمود",
        phone: "01012345678",
        address: "القاهرة",
        totalPurchases: 100.0,
        currentDebt: 25.0,
        notes: "عميل مميز",
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("invoices")
      .doc("inv_1")
      .set({
        userId: ALICE_UID,
        invoiceNumber: "INV-1001",
        customerId: "cust_1",
        customerName: "أحمد محمود",
        itemsSummary: "سماعة بلوتوث x2",
        items: [
          {
            productId: "prod_1",
            productName: "سماعة بلوتوث",
            quantity: 2,
            costPrice: 15.0,
            unitPrice: 25.0,
            lineTotal: 50.0,
          },
        ],
        itemCount: 2,
        subtotal: 50.0,
        discountAmount: 0.0,
        taxAmount: 0.0,
        totalAmount: 50.0,
        totalCost: 30.0,
        netProfit: 20.0,
        paidAmount: 25.0,
        remainingAmount: 25.0,
        paymentMethod: "PARTIAL",
        notes: "",
        createdAt: now,
        updatedAt: now,
      })
  );
});
