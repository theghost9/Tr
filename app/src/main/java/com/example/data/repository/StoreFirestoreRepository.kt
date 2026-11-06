package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CartItem
import com.example.data.model.Customer
import com.example.data.model.DebtPayment
import com.example.data.model.Invoice
import com.example.data.model.InvoiceLineItem
import com.example.data.model.Product
import com.example.data.model.StoreSettings
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StoreFirestoreRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول بحساب Google أولاً للوصول إلى قاعدة البيانات السحابية")
    }

    // --- Real-time Observation Flows ---

    fun observeProducts(userId: String = requireUserId()): Flow<List<Product>> = flow {
        val path = "users/$userId/products"
        emitAll(
            db.collection("users").document(userId).collection("products")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Product::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.name }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeInvoices(userId: String = requireUserId()): Flow<List<Invoice>> = flow {
        val path = "users/$userId/invoices"
        emitAll(
            db.collection("users").document(userId).collection("invoices")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Invoice::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeCustomers(userId: String = requireUserId()): Flow<List<Customer>> = flow {
        val path = "users/$userId/customers"
        emitAll(
            db.collection("users").document(userId).collection("customers")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Customer::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.currentDebt }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observePayments(userId: String = requireUserId()): Flow<List<DebtPayment>> = flow {
        val path = "users/$userId/payments"
        emitAll(
            db.collection("users").document(userId).collection("payments")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DebtPayment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeSettings(userId: String = requireUserId()): Flow<StoreSettings> = flow {
        val path = "users/$userId/settings/config"
        emitAll(
            db.collection("users").document(userId).collection("settings").document("config")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            StoreSettings::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: StoreSettings(userId = userId)
                    } else {
                        StoreSettings(userId = userId)
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    // --- Product CRUD Operations ---

    suspend fun saveProduct(
        id: String = "",
        name: String,
        barcode: String,
        category: String,
        costPrice: Double,
        sellingPrice: Double,
        stockQuantity: Long,
        minStockThreshold: Long,
        unit: String,
        existingCreatedAt: Timestamp? = null
    ): Result<String> {
        val uid = requireUserId()
        val productsRef = db.collection("users").document(uid).collection("products")
        return try {
            if (id.isBlank()) {
                val docRef = productsRef.document()
                val payload = mapOf(
                    "userId" to uid,
                    "name" to name.trim(),
                    "barcode" to barcode.trim(),
                    "category" to category.trim().ifBlank { "عام" },
                    "costPrice" to costPrice.coerceAtLeast(0.0),
                    "sellingPrice" to sellingPrice.coerceAtLeast(0.0),
                    "stockQuantity" to stockQuantity.coerceAtLeast(0L),
                    "minStockThreshold" to minStockThreshold.coerceAtLeast(0L),
                    "unit" to unit.trim().ifBlank { "قطعة" },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                Result.success(docRef.id)
            } else {
                val docRef = productsRef.document(id)
                val updatedProduct = Product(
                    id = id,
                    userId = uid,
                    name = name.trim(),
                    barcode = barcode.trim(),
                    category = category.trim().ifBlank { "عام" },
                    costPrice = costPrice.coerceAtLeast(0.0),
                    sellingPrice = sellingPrice.coerceAtLeast(0.0),
                    stockQuantity = stockQuantity.coerceAtLeast(0L),
                    minStockThreshold = minStockThreshold.coerceAtLeast(0L),
                    unit = unit.trim().ifBlank { "قطعة" },
                    createdAt = existingCreatedAt,
                    updatedAt = null
                )
                if (existingCreatedAt != null) {
                    docRef.set(updatedProduct).await()
                } else {
                    val updateMap = mapOf(
                        "name" to name.trim(),
                        "barcode" to barcode.trim(),
                        "category" to category.trim().ifBlank { "عام" },
                        "costPrice" to costPrice.coerceAtLeast(0.0),
                        "sellingPrice" to sellingPrice.coerceAtLeast(0.0),
                        "stockQuantity" to stockQuantity.coerceAtLeast(0L),
                        "minStockThreshold" to minStockThreshold.coerceAtLeast(0L),
                        "unit" to unit.trim().ifBlank { "قطعة" },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.update(updateMap).await()
                }
                Result.success(id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val op = if (id.isBlank()) OperationType.CREATE else OperationType.UPDATE
            handleFirestoreError(e, op, "users/$uid/products/$id")
            Result.failure(e)
        }
    }

    suspend fun adjustProductStock(product: Product, newQuantity: Long): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("products").document(product.id)
        return try {
            docRef.update(
                mapOf(
                    "stockQuantity" to newQuantity.coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("products").document(productId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getProductById(targetUserId: String, productId: String): Result<Product?> {
        val docRef = db.collection("users").document(targetUserId).collection("products").document(productId)
        return try {
            val snap = docRef.get().await()
            Result.success(snap.toObject(Product::class.java))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    // --- Customer & Debt Operations ---

    suspend fun saveCustomer(
        id: String = "",
        name: String,
        phone: String,
        address: String,
        notes: String,
        totalPurchases: Double = 0.0,
        currentDebt: Double = 0.0,
        existingCreatedAt: Timestamp? = null
    ): Result<String> {
        val uid = requireUserId()
        val customersRef = db.collection("users").document(uid).collection("customers")
        return try {
            if (id.isBlank()) {
                val docRef = customersRef.document()
                val payload = mapOf(
                    "userId" to uid,
                    "name" to name.trim(),
                    "phone" to phone.trim(),
                    "address" to address.trim(),
                    "totalPurchases" to totalPurchases.coerceAtLeast(0.0),
                    "currentDebt" to currentDebt.coerceAtLeast(0.0),
                    "notes" to notes.trim(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                Result.success(docRef.id)
            } else {
                val docRef = customersRef.document(id)
                val updateMap = mapOf(
                    "name" to name.trim(),
                    "phone" to phone.trim(),
                    "address" to address.trim(),
                    "totalPurchases" to totalPurchases.coerceAtLeast(0.0),
                    "currentDebt" to currentDebt.coerceAtLeast(0.0),
                    "notes" to notes.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updateMap).await()
                Result.success(id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val op = if (id.isBlank()) OperationType.CREATE else OperationType.UPDATE
            handleFirestoreError(e, op, "users/$uid/customers/$id")
            Result.failure(e)
        }
    }

    suspend fun recordCustomerPayment(
        customer: Customer,
        paymentAmount: Double,
        notes: String
    ): Result<String> {
        val uid = requireUserId()
        val paymentRef = db.collection("users").document(uid).collection("payments").document()
        val customerRef = db.collection("users").document(uid).collection("customers").document(customer.id)
        return try {
            val batch = db.batch()
            val paymentPayload = mapOf(
                "userId" to uid,
                "customerId" to customer.id,
                "customerName" to customer.name,
                "amount" to paymentAmount,
                "notes" to notes.trim(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.set(paymentRef, paymentPayload)

            val updatedDebt = (customer.currentDebt - paymentAmount).coerceAtLeast(0.0)
            batch.update(
                customerRef,
                mapOf(
                    "currentDebt" to updatedDebt,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(paymentRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, paymentRef.path)
            Result.failure(e)
        }
    }

    // --- Invoice Checkout & Atomic Inventory + Debt Update ---

    suspend fun createInvoiceCheckout(
        invoiceNumber: String,
        cartItems: List<CartItem>,
        discountAmount: Double,
        taxAmount: Double,
        paidAmount: Double,
        paymentMethod: String,
        selectedCustomer: Customer?,
        notes: String
    ): Result<Invoice> {
        val uid = requireUserId()
        require(cartItems.isNotEmpty()) { "السلة فارغة، أضف صنفاً واحداً على الأقل" }

        val invoicesRef = db.collection("users").document(uid).collection("invoices")
        val invoiceDoc = invoicesRef.document()

        val subtotal = cartItems.sumOf { it.lineTotal }.coerceAtLeast(0.0)
        val totalCost = cartItems.sumOf { it.lineCost }.coerceAtLeast(0.0)
        val totalAmount = (subtotal - discountAmount + taxAmount).coerceAtLeast(0.0)
        val netProfit = totalAmount - totalCost
        val safePaid = paidAmount.coerceIn(0.0, totalAmount)
        val remainingAmount = (totalAmount - safePaid).coerceAtLeast(0.0)
        val totalUnits = cartItems.sumOf { it.quantity }

        val lineItemMaps = cartItems.take(50).map { item ->
            InvoiceLineItem(
                productId = item.product.id,
                productName = item.product.name,
                barcode = item.product.barcode,
                unit = item.product.unit,
                quantity = item.quantity,
                costPrice = item.product.costPrice,
                unitPrice = item.customUnitPrice,
                lineTotal = item.lineTotal
            ).toMap()
        }

        val itemsSummary = cartItems.joinToString("، ") { "${it.product.name} (${it.quantity})" }
            .take(1900)

        val customerId = selectedCustomer?.id ?: ""
        val customerName = selectedCustomer?.name?.ifBlank { "عميل نقدي" } ?: "عميل نقدي"

        return try {
            val batch = db.batch()

            val invoicePayload = mapOf(
                "userId" to uid,
                "invoiceNumber" to invoiceNumber,
                "customerId" to customerId,
                "customerName" to customerName,
                "itemsSummary" to itemsSummary,
                "items" to lineItemMaps,
                "itemCount" to totalUnits,
                "subtotal" to subtotal,
                "discountAmount" to discountAmount.coerceAtLeast(0.0),
                "taxAmount" to taxAmount.coerceAtLeast(0.0),
                "totalAmount" to totalAmount,
                "totalCost" to totalCost,
                "netProfit" to netProfit,
                "paidAmount" to safePaid,
                "remainingAmount" to remainingAmount,
                "paymentMethod" to paymentMethod,
                "notes" to notes.trim().take(500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.set(invoiceDoc, invoicePayload)

            // Deduct stock quantities atomically
            for (cartItem in cartItems) {
                if (cartItem.product.id.isNotBlank()) {
                    val productRef = db.collection("users").document(uid)
                        .collection("products").document(cartItem.product.id)
                    val newStock = (cartItem.product.stockQuantity - cartItem.quantity).coerceAtLeast(0L)
                    batch.update(
                        productRef,
                        mapOf(
                            "stockQuantity" to newStock,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            // Update customer purchases & debt if customer selected
            if (selectedCustomer != null && selectedCustomer.id.isNotBlank()) {
                val customerRef = db.collection("users").document(uid)
                    .collection("customers").document(selectedCustomer.id)
                val newTotalPurchases = (selectedCustomer.totalPurchases + totalAmount).coerceAtLeast(0.0)
                val newDebt = (selectedCustomer.currentDebt + remainingAmount).coerceAtLeast(0.0)
                batch.update(
                    customerRef,
                    mapOf(
                        "totalPurchases" to newTotalPurchases,
                        "currentDebt" to newDebt,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()

            val createdInvoice = Invoice(
                id = invoiceDoc.id,
                userId = uid,
                invoiceNumber = invoiceNumber,
                customerId = customerId,
                customerName = customerName,
                itemsSummary = itemsSummary,
                items = lineItemMaps,
                itemCount = totalUnits,
                subtotal = subtotal,
                discountAmount = discountAmount,
                taxAmount = taxAmount,
                totalAmount = totalAmount,
                totalCost = totalCost,
                netProfit = netProfit,
                paidAmount = safePaid,
                remainingAmount = remainingAmount,
                paymentMethod = paymentMethod,
                notes = notes.trim(),
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            Result.success(createdInvoice)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, invoiceDoc.path)
            Result.failure(e)
        }
    }

    // --- Store Settings ---

    suspend fun saveSettings(
        storeName: String,
        storePhone: String,
        storeAddress: String,
        currencySymbol: String,
        defaultTaxRate: Double,
        receiptFooter: String,
        existingSettings: StoreSettings?
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("settings").document("config")
        return try {
            if (existingSettings?.createdAt != null) {
                docRef.update(
                    mapOf(
                        "storeName" to storeName.trim().ifBlank { "متجر تاجر برو" },
                        "storePhone" to storePhone.trim(),
                        "storeAddress" to storeAddress.trim(),
                        "currencySymbol" to currencySymbol.trim().ifBlank { "$" },
                        "defaultTaxRate" to defaultTaxRate.coerceIn(0.0, 100.0),
                        "receiptFooter" to receiptFooter.trim(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "storeName" to storeName.trim().ifBlank { "متجر تاجر برو" },
                        "storePhone" to storePhone.trim(),
                        "storeAddress" to storeAddress.trim(),
                        "currencySymbol" to currencySymbol.trim().ifBlank { "$" },
                        "defaultTaxRate" to defaultTaxRate.coerceIn(0.0, 100.0),
                        "receiptFooter" to receiptFooter.trim(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Optional Starter Store Seed Data (User-Triggered) ---

    suspend fun seedStarterStoreData(): Result<Unit> {
        val uid = requireUserId()
        val batch = db.batch()
        val productsRef = db.collection("users").document(uid).collection("products")
        val customersRef = db.collection("users").document(uid).collection("customers")

        val sampleProducts = listOf(
            mapOf(
                "userId" to uid,
                "name" to "قهوة عربية مختصة 500 جم",
                "barcode" to "6281000101",
                "category" to "مشروبات وقهوة",
                "costPrice" to 8.5,
                "sellingPrice" to 14.0,
                "stockQuantity" to 25L,
                "minStockThreshold" to 5L,
                "unit" to "عبوة",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            mapOf(
                "userId" to uid,
                "name" to "زيت زيتون بكر ممتاز 1 لتر",
                "barcode" to "6281000102",
                "category" to "مواد غذائية",
                "costPrice" to 11.0,
                "sellingPrice" to 16.5,
                "stockQuantity" to 4L,
                "minStockThreshold" to 6L,
                "unit" to "زجاجة",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            mapOf(
                "userId" to uid,
                "name" to "عسل سدر طبيعي 500 جم",
                "barcode" to "6281000103",
                "category" to "مواد غذائية",
                "costPrice" to 18.0,
                "sellingPrice" to 28.0,
                "stockQuantity" to 12L,
                "minStockThreshold" to 4L,
                "unit" to "عبوة",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            mapOf(
                "userId" to uid,
                "name" to "أرز بسمتي هندي فاخر 5 كجم",
                "barcode" to "6281000104",
                "category" to "بقوليات وحبوب",
                "costPrice" to 9.0,
                "sellingPrice" to 13.5,
                "stockQuantity" to 2L,
                "minStockThreshold" to 5L,
                "unit" to "كيس",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            mapOf(
                "userId" to uid,
                "name" to "شاي سيلاني فاخر 100 فتلة",
                "barcode" to "6281000105",
                "category" to "مشروبات وقهوة",
                "costPrice" to 3.0,
                "sellingPrice" to 5.5,
                "stockQuantity" to 40L,
                "minStockThreshold" to 10L,
                "unit" to "علبة",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        val sampleCustomers = listOf(
            mapOf(
                "userId" to uid,
                "name" to "محمود عبد الرحمن",
                "phone" to "01098765432",
                "address" to "شارع التحرير - وسط البلد",
                "totalPurchases" to 240.0,
                "currentDebt" to 45.0,
                "notes" to "عميل دائم - سداد أسبوعي",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ),
            mapOf(
                "userId" to uid,
                "name" to "شركة النور للتوريدات",
                "phone" to "01122334455",
                "address" to "المنطقة التجارية",
                "totalPurchases" to 580.0,
                "currentDebt" to 120.0,
                "notes" to "توريدات جملة",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        return try {
            sampleProducts.forEach { batch.set(productsRef.document(), it) }
            sampleCustomers.forEach { batch.set(customersRef.document(), it) }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }
}
