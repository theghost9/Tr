package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Customer
import com.example.data.model.Invoice
import com.example.data.model.Product
import com.example.data.model.StoreSettings
import com.example.ui.components.CameraBarcodeScannerDialog
import com.example.ui.theme.DangerRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LowStockAmber
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.PosCartState
import com.example.util.PdfInvoiceGenerator
import com.example.util.PdfInvoiceGenerator.formatMoney

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PosScreen(
    products: List<Product>,
    customers: List<Customer>,
    cartState: PosCartState,
    settings: StoreSettings,
    onAddToCart: (Product) -> Unit,
    onScanBarcode: (String) -> Product?,
    onUpdateCartQty: (String, Long) -> Unit,
    onRemoveFromCart: (String) -> Unit,
    onClearCart: () -> Unit,
    onSelectCustomer: (Customer?) -> Unit,
    onQuickAddCustomer: (String, String) -> Unit,
    onUpdateDiscount: (String) -> Unit,
    onUpdateTaxRate: (String) -> Unit,
    onUpdatePaymentMethod: (String) -> Unit,
    onUpdatePaidAmount: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onSubmitCheckout: () -> Unit
) {
    val currency = settings.currencySymbol.ifBlank { "$" }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showCheckoutSheet by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    val categories = remember(products) {
        listOf("الكل") + products.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { prod ->
            val matchesCat = selectedCategory == "الكل" || prod.category == selectedCategory
            val q = searchQuery.trim()
            val matchesSearch = q.isBlank() ||
                prod.name.contains(q, ignoreCase = true) ||
                prod.barcode.contains(q, ignoreCase = true)
            matchesCat && matchesSearch
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Search & Barcode Bar
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث باسم الصنف أو الباركود...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "مسح")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pos_search_input")
                    )

                    FilledIconButton(
                        onClick = { showBarcodeScanner = true },
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("pos_scan_barcode_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "مسح الباركود بالكاميرا"
                        )
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) }
                        )
                    }
                }
            }
        }

        // Products List for Fast Tapping
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("pos_products_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredProducts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "لا توجد أصناف مطابقة للبحث",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "أضف أصنافاً من قسم المخزون أو جرب البحث بكلمة أخرى",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    val inCartQty = cartState.items.firstOrNull { it.product.id == product.id }?.quantity ?: 0L
                    PosProductItemCard(
                        product = product,
                        inCartQuantity = inCartQty,
                        currency = currency,
                        onAdd = { onAddToCart(product) },
                        onDecrement = { onUpdateCartQty(product.id, -1L) }
                    )
                }
            }
        }

        // Sticky Live Cart Summary Bar at Bottom
        if (cartState.items.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Text(
                                    text = "${cartState.items.sumOf { it.quantity }} وحدات",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "الربح المتوقع: +${formatMoney(cartState.estimatedNetProfit)} $currency",
                                style = MaterialTheme.typography.labelMedium,
                                color = ProfitGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "الإجمالي: ${formatMoney(cartState.grandTotal)} $currency",
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onClearCart,
                            modifier = Modifier.testTag("clear_cart_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "إفراغ السلة",
                                tint = DangerRed
                            )
                        }
                        Button(
                            onClick = { showCheckoutSheet = true },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("open_checkout_sheet_button")
                        ) {
                            Icon(imageVector = Icons.Default.ShoppingCartCheckout, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مراجعة وإصدار الفاتورة", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showBarcodeScanner) {
        CameraBarcodeScannerDialog(
            products = products,
            title = "مسح باركود الصنف للإضافة للفاتورة",
            onBarcodeScanned = { code ->
                onScanBarcode(code)
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }

    if (showCheckoutSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCheckoutSheet = false }
        ) {
            CheckoutBottomSheetContent(
                cartState = cartState,
                customers = customers,
                currency = currency,
                onUpdateCartQty = onUpdateCartQty,
                onRemoveFromCart = onRemoveFromCart,
                onSelectCustomer = onSelectCustomer,
                onOpenNewCustomerDialog = { showAddCustomerDialog = true },
                onUpdateDiscount = onUpdateDiscount,
                onUpdateTaxRate = onUpdateTaxRate,
                onUpdatePaymentMethod = onUpdatePaymentMethod,
                onUpdatePaidAmount = onUpdatePaidAmount,
                onUpdateNotes = onUpdateNotes,
                onConfirmCheckout = {
                    showCheckoutSheet = false
                    onSubmitCheckout()
                }
            )
        }
    }

    if (showAddCustomerDialog) {
        QuickAddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, phone ->
                onQuickAddCustomer(name, phone)
                showAddCustomerDialog = false
            }
        )
    }
}

@Composable
private fun PosProductItemCard(
    product: Product,
    inCartQuantity: Long,
    currency: String,
    onAdd: () -> Unit,
    onDecrement: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !product.isOutOfStock, onClick = onAdd)
            .testTag("pos_product_card_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (inCartQuantity > 0L) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        color = when {
                            product.isOutOfStock -> DangerRed.copy(alpha = 0.14f)
                            product.isLowStock -> LowStockAmber.copy(alpha = 0.14f)
                            else -> ProfitGreen.copy(alpha = 0.14f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = when {
                                product.isOutOfStock -> "نفد"
                                product.isLowStock -> "مخزون منخفض: ${product.stockQuantity}"
                                else -> "متوفر: ${product.stockQuantity} ${product.unit}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                product.isOutOfStock -> DangerRed
                                product.isLowStock -> LowStockAmber
                                else -> ProfitGreen
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "السعر: ${formatMoney(product.sellingPrice)} $currency",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "ربح الوحدة: +${formatMoney(product.unitProfit)} $currency",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = ProfitGreen
                    )
                }
            }

            if (inCartQuantity > 0L) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onDecrement,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "إنقاص")
                    }
                    Text(
                        text = inCartQuantity.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = onAdd,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "زيادة",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            } else {
                FilledIconButton(
                    onClick = onAdd,
                    enabled = !product.isOutOfStock,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة للفاتورة")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CheckoutBottomSheetContent(
    cartState: PosCartState,
    customers: List<Customer>,
    currency: String,
    onUpdateCartQty: (String, Long) -> Unit,
    onRemoveFromCart: (String) -> Unit,
    onSelectCustomer: (Customer?) -> Unit,
    onOpenNewCustomerDialog: () -> Unit,
    onUpdateDiscount: (String) -> Unit,
    onUpdateTaxRate: (String) -> Unit,
    onUpdatePaymentMethod: (String) -> Unit,
    onUpdatePaidAmount: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onConfirmCheckout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "تفاصيل الفاتورة وإتمام البيع",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Cart Items List
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cartState.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.product.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${item.quantity} × ${formatMoney(item.customUnitPrice)} = ${formatMoney(item.lineTotal)} $currency",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onUpdateCartQty(item.product.id, -1L) }) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = "إنقاص")
                            }
                            Text(
                                text = "${item.quantity}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { onUpdateCartQty(item.product.id, 1L) }) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "زيادة")
                            }
                            IconButton(onClick = { onRemoveFromCart(item.product.id) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "حذف",
                                    tint = DangerRed
                                )
                            }
                        }
                    }
                }
            }
        }

        // Customer Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "العميل (اختياري للنقدي / إلزامي للآجل):",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onOpenNewCustomerDialog) {
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("عميل جديد")
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = cartState.selectedCustomer == null,
                    onClick = { onSelectCustomer(null) },
                    label = { Text("عميل نقدي مباشر") }
                )
            }
            items(customers, key = { it.id }) { cust ->
                FilterChip(
                    selected = cartState.selectedCustomer?.id == cust.id,
                    onClick = { onSelectCustomer(cust) },
                    label = {
                        Text(
                            if (cust.currentDebt > 0) {
                                "${cust.name} (عليه: ${formatMoney(cust.currentDebt)})"
                            } else {
                                cust.name
                            }
                        )
                    }
                )
            }
        }

        // Discount & Tax Inputs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = cartState.discountInput,
                onValueChange = onUpdateDiscount,
                label = { Text("قيمة الخصم ($currency)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("checkout_discount_input")
            )
            OutlinedTextField(
                value = cartState.taxRateInput,
                onValueChange = onUpdateTaxRate,
                label = { Text("نسبة الضريبة (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("checkout_tax_input")
            )
        }

        // Payment Method Selector
        Text(
            text = "طريقة الدفع:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val methods = listOf(
                "CASH" to "نقدي بالكامل",
                "CARD" to "بطاقة / شبكة",
                "PARTIAL" to "دفع جزئي والباقي آجل",
                "CREDIT" to "آجل بالكامل (على الحساب)"
            )
            methods.forEach { (code, label) ->
                FilterChip(
                    selected = cartState.paymentMethod == code,
                    onClick = { onUpdatePaymentMethod(code) },
                    label = { Text(label) }
                )
            }
        }

        if (cartState.paymentMethod == "PARTIAL") {
            OutlinedTextField(
                value = cartState.paidAmountInput,
                onValueChange = onUpdatePaidAmount,
                label = { Text("المبلغ المدفوع الآن ($currency)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("checkout_paid_amount_input")
            )
        }

        OutlinedTextField(
            value = cartState.notes,
            onValueChange = onUpdateNotes,
            label = { Text("ملاحظات الفاتورة (اختياري)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Totals & Profit Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SummaryRow("الإجمالي الفرعي:", "${formatMoney(cartState.subtotal)} $currency")
                if (cartState.discountAmount > 0.0) {
                    SummaryRow("الخصم:", "-${formatMoney(cartState.discountAmount)} $currency")
                }
                if (cartState.taxAmount > 0.0) {
                    SummaryRow("الضريبة (${cartState.taxRatePercent}%):", "+${formatMoney(cartState.taxAmount)} $currency")
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                SummaryRow(
                    label = "الإجمالي النهائي المطلوب:",
                    value = "${formatMoney(cartState.grandTotal)} $currency",
                    bold = true
                )
                SummaryRow(
                    label = "المدفوع:",
                    value = "${formatMoney(cartState.effectivePaidAmount)} $currency"
                )
                if (cartState.remainingDebtAmount > 0.0) {
                    SummaryRow(
                        label = "المتبقي (يرحل لحساب العميل):",
                        value = "${formatMoney(cartState.remainingDebtAmount)} $currency",
                        valueColor = LowStockAmber,
                        bold = true
                    )
                }
                SummaryRow(
                    label = "صافي ربح هذه الفاتورة:",
                    value = "+${formatMoney(cartState.estimatedNetProfit)} $currency",
                    valueColor = ProfitGreen,
                    bold = true
                )
            }
        }

        Button(
            onClick = onConfirmCheckout,
            enabled = !cartState.isSubmitting && cartState.items.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("confirm_issue_invoice_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "تأكيد وإصدار الفاتورة (${formatMoney(cartState.grandTotal)} $currency)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    bold: Boolean = false,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = valueColor
        )
    }
}

@Composable
private fun QuickAddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة عميل سريع") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم العميل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name, phone)
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun InvoiceReceiptDialog(
    invoice: Invoice,
    settings: StoreSettings,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currency = settings.currencySymbol.ifBlank { "$" }
    val lineItems = remember(invoice) { invoice.parsedLineItems() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ProfitGreen,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "فاتورة رقم ${invoice.invoiceNumber}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = settings.storeName.ifBlank { "متجر تاجر برو" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("العميل: ${invoice.customerName}", fontWeight = FontWeight.Bold)
                HorizontalDivider()
                lineItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.productName} (${item.quantity} × ${formatMoney(item.unitPrice)})",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${formatMoney(item.lineTotal)} $currency",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                HorizontalDivider()
                SummaryRow("الإجمالي النهائي:", "${formatMoney(invoice.totalAmount)} $currency", bold = true)
                SummaryRow("المدفوع:", "${formatMoney(invoice.paidAmount)} $currency")
                if (invoice.remainingAmount > 0.0) {
                    SummaryRow(
                        "المتبقي (آجل):",
                        "${formatMoney(invoice.remainingAmount)} $currency",
                        valueColor = LowStockAmber,
                        bold = true
                    )
                }
                SummaryRow(
                    "صافي الربح المحقق:",
                    "+${formatMoney(invoice.netProfit)} $currency",
                    valueColor = ProfitGreen,
                    bold = true
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        PdfInvoiceGenerator.shareTextReceipt(context, invoice, settings)
                    },
                    modifier = Modifier.testTag("share_text_receipt_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إيصال نصي")
                }
                Button(
                    onClick = {
                        PdfInvoiceGenerator.generateAndShareInvoicePdf(context, invoice, settings)
                    },
                    modifier = Modifier.testTag("share_pdf_receipt_button")
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مشاركة / طباعة PDF")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
