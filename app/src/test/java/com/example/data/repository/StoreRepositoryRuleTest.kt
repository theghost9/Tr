package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class StoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAndObserveProducts_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = StoreFirestoreRepository(firestore)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveProduct(
                name = "قهوة مختصة",
                barcode = "628111",
                category = "مشروبات",
                costPrice = 10.0,
                sellingPrice = 16.0,
                stockQuantity = 30L,
                minStockThreshold = 5L,
                unit = "عبوة"
            )
        }
        assertTrue(saveResult.isSuccess)
        val productId = saveResult.getOrThrow()

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProducts(uid).first { list -> list.any { it.id == productId } }
        }
        assertTrue(observed.any { it.id == productId && it.name == "قهوة مختصة" })
    }

    @Test
    fun createInvoiceCheckout_deductsInventoryAndCalculatesProfit() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = StoreFirestoreRepository(firestore)

        val productId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveProduct(
                name = "سماعة لاسلكية",
                barcode = "628222",
                category = "إلكترونيات",
                costPrice = 20.0,
                sellingPrice = 35.0,
                stockQuantity = 10L,
                minStockThreshold = 3L,
                unit = "قطعة"
            ).getOrThrow()
        }

        val product = Product(
            id = productId,
            userId = uid,
            name = "سماعة لاسلكية",
            barcode = "628222",
            category = "إلكترونيات",
            costPrice = 20.0,
            sellingPrice = 35.0,
            stockQuantity = 10L,
            minStockThreshold = 3L,
            unit = "قطعة"
        )

        val invoiceResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createInvoiceCheckout(
                invoiceNumber = "INV-2026-01",
                cartItems = listOf(CartItem(product = product, quantity = 2L)),
                discountAmount = 0.0,
                taxAmount = 0.0,
                paidAmount = 70.0,
                paymentMethod = "CASH",
                selectedCustomer = null,
                notes = "بيع نقدي"
            )
        }
        assertTrue(invoiceResult.isSuccess)
        val invoice = invoiceResult.getOrThrow()
        assertEquals(70.0, invoice.totalAmount, 0.01)
        assertEquals(30.0, invoice.netProfit, 0.01)

        val updatedProduct = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getProductById(uid, productId).getOrThrow()
        }
        assertNotNull(updatedProduct)
        assertEquals(8L, updatedProduct!!.stockQuantity)
    }

    @Test
    fun getProductById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = StoreFirestoreRepository(firestore)
        val productId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveProduct(
                name = "منتج خاص",
                barcode = "999",
                category = "عام",
                costPrice = 5.0,
                sellingPrice = 9.0,
                stockQuantity = 15L,
                minStockThreshold = 3L,
                unit = "قطعة"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = StoreFirestoreRepository(firestore)
        val bobResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getProductById(aliceUid, productId)
        }
        assertTrue(bobResult.isFailure)
        val ex = bobResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(ex)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex!!.code)
    }

    @Test
    fun observeProducts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = StoreFirestoreRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeProducts(aliceUid).first()
            }
            fail("Expected exception for unauthenticated user")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_store@test.com"
        const val BOB_EMAIL = "bob_store@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
