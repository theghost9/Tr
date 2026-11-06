package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CartItem
import com.example.data.model.Customer
import com.example.data.model.DebtPayment
import com.example.data.model.Invoice
import com.example.data.model.Product
import com.example.data.model.StoreSettings
import com.example.data.repository.StoreFirestoreRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class SalesPeriodFilter(val labelAr: String) {
    TODAY("اليوم"),
    WEEK("آخر 7 أيام"),
    MONTH("هذا الشهر"),
    ALL("كل الأوقات")
}

data class DailyBarPoint(
    val dayLabel: String,
    val sales: Double,
    val profit: Double
)

data class DashboardMetrics(
    val todaySales: Double = 0.0,
    val todayNetProfit: Double = 0.0,
    val todayInvoicesCount: Int = 0,
    val todayCollected: Double = 0.0,
    val periodSales: Double = 0.0,
    val periodCost: Double = 0.0,
    val periodNetProfit: Double = 0.0,
    val periodInvoicesCount: Int = 0,
    val totalCustomerDebt: Double = 0.0,
    val totalInventoryCostValue: Double = 0.0,
    val totalInventoryRetailValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val last7DaysChart: List<DailyBarPoint> = emptyList()
)

data class PosCartState(
    val items: List<CartItem> = emptyList(),
    val selectedCustomer: Customer? = null,
    val discountInput: String = "",
    val taxRateInput: String = "0",
    val paymentMethod: String = "CASH", // CASH, CREDIT, PARTIAL, CARD
    val paidAmountInput: String = "",
    val notes: String = "",
    val isSubmitting: Boolean = false
) {
    val subtotal: Double
        get() = items.sumOf { it.lineTotal }

    val totalCost: Double
        get() = items.sumOf { it.lineCost }

    val discountAmount: Double
        get() = (discountInput.toDoubleOrNull() ?: 0.0).coerceIn(0.0, subtotal)

    val taxRatePercent: Double
        get() = (taxRateInput.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)

    val taxAmount: Double
        get() = ((subtotal - discountAmount).coerceAtLeast(0.0)) * (taxRatePercent / 100.0)

    val grandTotal: Double
        get() = (subtotal - discountAmount + taxAmount).coerceAtLeast(0.0)

    val estimatedNetProfit: Double
        get() = grandTotal - totalCost

    val effectivePaidAmount: Double
        get() = when (paymentMethod) {
            "CASH", "CARD" -> grandTotal
            "CREDIT" -> 0.0
            "PARTIAL" -> (paidAmountInput.toDoubleOrNull() ?: 0.0).coerceIn(0.0, grandTotal)
            else -> grandTotal
        }

    val remainingDebtAmount: Double
        get() = (grandTotal - effectivePaidAmount).coerceAtLeast(0.0)
}

class StoreViewModel(
    private val repository: StoreFirestoreRepository,
    private val currentUserId: String
) : ViewModel() {

    val productsState: StateFlow<UiState<List<Product>>> = repository.observeProducts(currentUserId)
        .map<List<Product>, UiState<List<Product>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing products", error)
            emit(UiState.Error(error.localizedMessage ?: "تعذر تحميل المخزون"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val invoicesState: StateFlow<UiState<List<Invoice>>> = repository.observeInvoices(currentUserId)
        .map<List<Invoice>, UiState<List<Invoice>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing invoices", error)
            emit(UiState.Error(error.localizedMessage ?: "تعذر تحميل الفواتير"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val customersState: StateFlow<UiState<List<Customer>>> = repository.observeCustomers(currentUserId)
        .map<List<Customer>, UiState<List<Customer>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing customers", error)
            emit(UiState.Error(error.localizedMessage ?: "تعذر تحميل العملاء"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val paymentsState: StateFlow<UiState<List<DebtPayment>>> = repository.observePayments(currentUserId)
        .map<List<DebtPayment>, UiState<List<DebtPayment>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing payments", error)
            emit(UiState.Error(error.localizedMessage ?: "تعذر تحميل سندات القبض"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val settingsState: StateFlow<StoreSettings> = repository.observeSettings(currentUserId)
        .catch { error ->
            Log.w(TAG, "Error observing settings", error)
            emit(StoreSettings(userId = currentUserId))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = StoreSettings(userId = currentUserId)
        )

    private val _selectedPeriod = MutableStateFlow(SalesPeriodFilter.TODAY)
    val selectedPeriod: StateFlow<SalesPeriodFilter> = _selectedPeriod.asStateFlow()

    private val _cartState = MutableStateFlow(PosCartState())
    val cartState: StateFlow<PosCartState> = _cartState.asStateFlow()

    private val _lastCreatedInvoice = MutableStateFlow<Invoice?>(null)
    val lastCreatedInvoice: StateFlow<Invoice?> = _lastCreatedInvoice.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        productsState,
        invoicesState,
        customersState,
        _selectedPeriod
    ) { prodUi, invUi, custUi, period ->
        val products = (prodUi as? UiState.Success)?.data ?: emptyList()
        val invoices = (invUi as? UiState.Success)?.data ?: emptyList()
        val customers = (custUi as? UiState.Success)?.data ?: emptyList()

        val now = Calendar.getInstance()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val periodStartMillis = when (period) {
            SalesPeriodFilter.TODAY -> startOfToday
            SalesPeriodFilter.WEEK -> startOfToday - 6L * 24L * 3600L * 1000L
            SalesPeriodFilter.MONTH -> Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            SalesPeriodFilter.ALL -> 0L
        }

        val todayInvoices = invoices.filter { inv ->
            val time = inv.createdAt?.toDate()?.time ?: System.currentTimeMillis()
            time >= startOfToday
        }

        val periodInvoices = invoices.filter { inv ->
            val time = inv.createdAt?.toDate()?.time ?: System.currentTimeMillis()
            time >= periodStartMillis
        }

        val dayFormat = SimpleDateFormat("MM/dd", Locale.US)
        val chartPoints = (6 downTo 0).map { daysAgo ->
            val dayStart = startOfToday - daysAgo * 24L * 3600L * 1000L
            val dayEnd = dayStart + 24L * 3600L * 1000L
            val dayInvoices = invoices.filter { inv ->
                val t = inv.createdAt?.toDate()?.time ?: 0L
                t in dayStart until dayEnd
            }
            DailyBarPoint(
                dayLabel = if (daysAgo == 0) "اليوم" else dayFormat.format(Date(dayStart)),
                sales = dayInvoices.sumOf { it.totalAmount },
                profit = dayInvoices.sumOf { it.netProfit }
            )
        }

        DashboardMetrics(
            todaySales = todayInvoices.sumOf { it.totalAmount },
            todayNetProfit = todayInvoices.sumOf { it.netProfit },
            todayInvoicesCount = todayInvoices.size,
            todayCollected = todayInvoices.sumOf { it.paidAmount },
            periodSales = periodInvoices.sumOf { it.totalAmount },
            periodCost = periodInvoices.sumOf { it.totalCost },
            periodNetProfit = periodInvoices.sumOf { it.netProfit },
            periodInvoicesCount = periodInvoices.size,
            totalCustomerDebt = customers.sumOf { it.currentDebt },
            totalInventoryCostValue = products.sumOf { it.costPrice * it.stockQuantity },
            totalInventoryRetailValue = products.sumOf { it.sellingPrice * it.stockQuantity },
            lowStockCount = products.count { it.isLowStock },
            outOfStockCount = products.count { it.isOutOfStock },
            last7DaysChart = chartPoints
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DashboardMetrics()
    )

    fun setSelectedPeriod(period: SalesPeriodFilter) {
        _selectedPeriod.value = period
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun clearLastCreatedInvoice() {
        _lastCreatedInvoice.value = null
    }

    // --- Cart Management ---

    fun addToCart(product: Product) {
        if (product.stockQuantity <= 0L) {
            _statusMessage.value = "تنبيه: الصنف (${product.name}) نافد من المخزون!"
            return
        }
        _cartState.update { current ->
            val existingIndex = current.items.indexOfFirst { it.product.id == product.id }
            val updatedItems = if (existingIndex >= 0) {
                val existingItem = current.items[existingIndex]
                val nextQty = (existingItem.quantity + 1L).coerceAtMost(product.stockQuantity)
                current.items.toMutableList().apply {
                    set(existingIndex, existingItem.copy(product = product, quantity = nextQty))
                }
            } else {
                current.items + CartItem(product = product, quantity = 1L)
            }
            current.copy(items = updatedItems)
        }
    }

    fun scanBarcodeToCart(barcode: String): Product? {
        val cleanCode = barcode.trim()
        if (cleanCode.isBlank()) return null
        val products = (productsState.value as? UiState.Success)?.data ?: emptyList()
        val matched = products.firstOrNull {
            it.barcode.equals(cleanCode, ignoreCase = true) ||
                it.name.equals(cleanCode, ignoreCase = true)
        }
        if (matched != null) {
            addToCart(matched)
            _statusMessage.value = "تمت إضافة (${matched.name}) إلى الفاتورة"
        } else {
            _statusMessage.value = "لم يتم العثور على صنف بالباركود: $cleanCode"
        }
        return matched
    }

    fun updateCartItemQuantity(productId: String, delta: Long) {
        _cartState.update { current ->
            val updated = current.items.mapNotNull { item ->
                if (item.product.id == productId) {
                    val newQty = item.quantity + delta
                    when {
                        newQty <= 0L -> null
                        newQty > item.product.stockQuantity -> {
                            _statusMessage.value = "الكمية المتاحة بالمخزون (${item.product.stockQuantity}) فقط"
                            item.copy(quantity = item.product.stockQuantity)
                        }
                        else -> item.copy(quantity = newQty)
                    }
                } else {
                    item
                }
            }
            current.copy(items = updated)
        }
    }

    fun removeFromCart(productId: String) {
        _cartState.update { current ->
            current.copy(items = current.items.filterNot { it.product.id == productId })
        }
    }

    fun clearCart() {
        val defaultTax = settingsState.value.defaultTaxRate
        _cartState.value = PosCartState(
            taxRateInput = if (defaultTax > 0.0) defaultTax.toString() else "0"
        )
    }

    fun selectCustomerForCart(customer: Customer?) {
        _cartState.update { it.copy(selectedCustomer = customer) }
    }

    fun updateCartDiscount(discountStr: String) {
        _cartState.update { it.copy(discountInput = discountStr) }
    }

    fun updateCartTaxRate(taxStr: String) {
        _cartState.update { it.copy(taxRateInput = taxStr) }
    }

    fun updateCartPaymentMethod(method: String) {
        _cartState.update { it.copy(paymentMethod = method) }
    }

    fun updateCartPaidAmount(paidStr: String) {
        _cartState.update { it.copy(paidAmountInput = paidStr) }
    }

    fun updateCartNotes(notes: String) {
        _cartState.update { it.copy(notes = notes) }
    }

    fun submitInvoiceCheckout() {
        val current = _cartState.value
        if (current.items.isEmpty()) {
            _statusMessage.value = "أضف أصنافاً إلى الفاتورة أولاً"
            return
        }
        if ((current.paymentMethod == "CREDIT" || current.paymentMethod == "PARTIAL") &&
            current.selectedCustomer == null
        ) {
            _statusMessage.value = "يرجى اختيار العميل لتسجيل المبلغ الآجل على حسابه"
            return
        }

        _cartState.update { it.copy(isSubmitting = true) }

        val existingInvoicesCount = ((invoicesState.value as? UiState.Success)?.data?.size ?: 0) + 1
        val datePart = SimpleDateFormat("MMdd", Locale.US).format(Date())
        val invoiceNumber = "INV-$datePart-${1000 + existingInvoicesCount}"

        viewModelScope.launch {
            val result = repository.createInvoiceCheckout(
                invoiceNumber = invoiceNumber,
                cartItems = current.items,
                discountAmount = current.discountAmount,
                taxAmount = current.taxAmount,
                paidAmount = current.effectivePaidAmount,
                paymentMethod = current.paymentMethod,
                selectedCustomer = current.selectedCustomer,
                notes = current.notes
            )
            result.onSuccess { invoice ->
                clearCart()
                _lastCreatedInvoice.value = invoice
                _statusMessage.value = "تم إصدار الفاتورة ${invoice.invoiceNumber} وخصم المخزون بنجاح!"
            }.onFailure { err ->
                _cartState.update { it.copy(isSubmitting = false) }
                _statusMessage.value = "حدث خطأ أثناء حفظ الفاتورة: ${err.localizedMessage}"
            }
        }
    }

    // --- Inventory Actions ---

    fun saveProduct(
        id: String = "",
        name: String,
        barcode: String,
        category: String,
        costPrice: Double,
        sellingPrice: Double,
        stockQuantity: Long,
        minStockThreshold: Long,
        unit: String,
        existingProduct: Product? = null
    ) {
        if (name.isBlank()) {
            _statusMessage.value = "يرجى إدخال اسم الصنف"
            return
        }
        viewModelScope.launch {
            val result = repository.saveProduct(
                id = id,
                name = name,
                barcode = barcode,
                category = category,
                costPrice = costPrice,
                sellingPrice = sellingPrice,
                stockQuantity = stockQuantity,
                minStockThreshold = minStockThreshold,
                unit = unit,
                existingCreatedAt = existingProduct?.createdAt
            )
            result.onSuccess {
                _statusMessage.value = if (id.isBlank()) "تمت إضافة الصنف للمخزون بنجاح" else "تم تحديث بيانات الصنف"
            }.onFailure { err ->
                _statusMessage.value = "تعذر حفظ الصنف: ${err.localizedMessage}"
            }
        }
    }

    fun restockProduct(product: Product, addedQuantity: Long) {
        if (addedQuantity == 0L) return
        viewModelScope.launch {
            val newQty = (product.stockQuantity + addedQuantity).coerceAtLeast(0L)
            repository.adjustProductStock(product, newQty)
                .onSuccess {
                    _statusMessage.value = "تم تحديث مخزون (${product.name}) إلى $newQty ${product.unit}"
                }
                .onFailure { err ->
                    _statusMessage.value = "فشل تحديث الكمية: ${err.localizedMessage}"
                }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product.id)
                .onSuccess { _statusMessage.value = "تم حذف الصنف (${product.name})" }
                .onFailure { err -> _statusMessage.value = "تعذر الحذف: ${err.localizedMessage}" }
        }
    }

    // --- Customer & Debt Actions ---

    fun saveCustomer(
        id: String = "",
        name: String,
        phone: String,
        address: String,
        notes: String,
        initialDebt: Double = 0.0,
        existingCustomer: Customer? = null
    ) {
        if (name.isBlank()) {
            _statusMessage.value = "يرجى إدخال اسم العميل"
            return
        }
        viewModelScope.launch {
            repository.saveCustomer(
                id = id,
                name = name,
                phone = phone,
                address = address,
                notes = notes,
                totalPurchases = existingCustomer?.totalPurchases ?: 0.0,
                currentDebt = if (id.isBlank()) initialDebt else (existingCustomer?.currentDebt ?: initialDebt),
                existingCreatedAt = existingCustomer?.createdAt
            ).onSuccess {
                _statusMessage.value = if (id.isBlank()) "تم تسجيل العميل بنجاح" else "تم تحديث بيانات العميل"
            }.onFailure { err ->
                _statusMessage.value = "تعذر حفظ العميل: ${err.localizedMessage}"
            }
        }
    }

    fun recordCustomerDebtPayment(customer: Customer, amount: Double, notes: String) {
        if (amount <= 0.0) {
            _statusMessage.value = "أدخل مبلغ سداد صحيح أكبر من صفر"
            return
        }
        viewModelScope.launch {
            repository.recordCustomerPayment(customer, amount, notes)
                .onSuccess {
                    _statusMessage.value = "تم تسجيل سداد ${String.format(Locale.US, "%.2f", amount)} من حساب ${customer.name}"
                }
                .onFailure { err ->
                    _statusMessage.value = "فشل تسجيل الدفعة: ${err.localizedMessage}"
                }
        }
    }

    // --- Store Settings & Starter Data ---

    fun saveStoreSettings(
        storeName: String,
        storePhone: String,
        storeAddress: String,
        currencySymbol: String,
        defaultTaxRate: Double,
        receiptFooter: String
    ) {
        viewModelScope.launch {
            repository.saveSettings(
                storeName = storeName,
                storePhone = storePhone,
                storeAddress = storeAddress,
                currencySymbol = currencySymbol,
                defaultTaxRate = defaultTaxRate,
                receiptFooter = receiptFooter,
                existingSettings = settingsState.value
            ).onSuccess {
                _statusMessage.value = "تم حفظ إعدادات المحل والعملة بنجاح"
            }.onFailure { err ->
                _statusMessage.value = "تعذر حفظ الإعدادات: ${err.localizedMessage}"
            }
        }
    }

    fun seedStarterData() {
        viewModelScope.launch {
            repository.seedStarterStoreData()
                .onSuccess {
                    _statusMessage.value = "تم تحميل أصناف وعملاء نموذجيين لتجربة النظام فوراً!"
                }
                .onFailure { err ->
                    _statusMessage.value = "تعذر تحميل البيانات النموذجية: ${err.localizedMessage}"
                }
        }
    }

    private companion object {
        const val TAG = "StoreViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
