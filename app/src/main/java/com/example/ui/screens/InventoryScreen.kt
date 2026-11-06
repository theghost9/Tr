package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.data.model.StoreSettings
import com.example.ui.components.CameraBarcodeScannerDialog
import com.example.ui.theme.DangerRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LowStockAmber
import com.example.ui.theme.ProfitGreen
import com.example.util.PdfInvoiceGenerator.formatMoney

enum class StockFilter(val label: String) {
    ALL("كل الأصناف"),
    LOW_STOCK("نواقص المخزون"),
    OUT_OF_STOCK("نافد تماماً")
}

@Composable
fun InventoryScreen(
    products: List<Product>,
    settings: StoreSettings,
    onSaveProduct: (
        id: String,
        name: String,
        barcode: String,
        category: String,
        costPrice: Double,
        sellingPrice: Double,
        stockQuantity: Long,
        minStockThreshold: Long,
        unit: String,
        existingProduct: Product?
    ) -> Unit,
    onRestockProduct: (Product, Long) -> Unit,
    onDeleteProduct: (Product) -> Unit
) {
    val currency = settings.currencySymbol.ifBlank { "$" }
    var searchQuery by remember { mutableStateOf("") }
    var stockFilter by remember { mutableStateOf(StockFilter.ALL) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showProductDialog by remember { mutableStateOf(false) }
    var restockingProduct by remember { mutableStateOf<Product?>(null) }

    val filteredProducts = remember(products, searchQuery, stockFilter) {
        products.filter { prod ->
            val matchesStock = when (stockFilter) {
                StockFilter.ALL -> true
                StockFilter.LOW_STOCK -> prod.isLowStock || prod.isOutOfStock
                StockFilter.OUT_OF_STOCK -> prod.isOutOfStock
            }
            val q = searchQuery.trim()
            val matchesQuery = q.isBlank() ||
                prod.name.contains(q, ignoreCase = true) ||
                prod.barcode.contains(q, ignoreCase = true) ||
                prod.category.contains(q, ignoreCase = true)
            matchesStock && matchesQuery
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProduct = null
                    showProductDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_new_product_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة صنف", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // Top Inventory Summary & Filter Bar
            Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "إجمالي الأصناف: ${products.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val lowCount = products.count { it.isLowStock || it.isOutOfStock }
                        Text(
                            text = "النواقص: $lowCount صنف",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (lowCount > 0) LowStockAmber else ProfitGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث بالاسم، الباركود، أو التصنيف...") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
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
                            .fillMaxWidth()
                            .testTag("inventory_search_input")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(StockFilter.entries) { filter ->
                            FilterChip(
                                selected = stockFilter == filter,
                                onClick = { stockFilter = filter },
                                label = { Text(filter.label) }
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("inventory_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                Icon(
                                    imageVector = Icons.Default.Inventory,
                                    contentDescription = null,
                                    modifier = Modifier.size(42.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "لا توجد أصناف في هذا العرض",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "اضغط على زر (إضافة صنف) لتسجيل بضاعتك وتحديد سعر الشراء والبيع وحد النواقص",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredProducts, key = { it.id }) { product ->
                        InventoryProductCard(
                            product = product,
                            currency = currency,
                            onEdit = {
                                editingProduct = product
                                showProductDialog = true
                            },
                            onRestock = { restockingProduct = product },
                            onDelete = { onDeleteProduct(product) }
                        )
                    }
                }
            }
        }
    }

    if (showProductDialog) {
        ProductFormDialog(
            existingProduct = editingProduct,
            allProducts = products,
            currency = currency,
            onDismiss = { showProductDialog = false },
            onSave = { id, name, barcode, category, cost, sell, stock, minThreshold, unit ->
                onSaveProduct(id, name, barcode, category, cost, sell, stock, minThreshold, unit, editingProduct)
                showProductDialog = false
            }
        )
    }

    if (restockingProduct != null) {
        RestockDialog(
            product = restockingProduct!!,
            onDismiss = { restockingProduct = null },
            onConfirmAddStock = { added ->
                onRestockProduct(restockingProduct!!, added)
                restockingProduct = null
            }
        )
    }
}

@Composable
private fun InventoryProductCard(
    product: Product,
    currency: String,
    onEdit: () -> Unit,
    onRestock: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when {
        product.isOutOfStock -> DangerRed
        product.isLowStock -> LowStockAmber
        else -> ProfitGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "التصنيف: ${product.category} • الباركود: ${product.barcode.ifBlank { "غير محدد" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = when {
                            product.isOutOfStock -> "نافد (0)"
                            product.isLowStock -> "منخفض (${product.stockQuantity} ${product.unit})"
                            else -> "${product.stockQuantity} ${product.unit}"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Stock Level Bar
            val targetMax = (product.minStockThreshold * 4L).coerceAtLeast(10L)
            val progress = (product.stockQuantity.toFloat() / targetMax.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            // Financial Breakdown Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("سعر الشراء", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${formatMoney(product.costPrice)} $currency",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text("سعر البيع", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${formatMoney(product.sellingPrice)} $currency",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("صافي ربح الوحدة", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "+${formatMoney(product.unitProfit)} $currency (${String.format("%.0f", product.profitMarginPercent)}%)",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = ProfitGreen
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onRestock,
                    modifier = Modifier.testTag("restock_btn_${product.id}")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("توريد كمية")
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل الصنف")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف الصنف",
                            tint = DangerRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductFormDialog(
    existingProduct: Product?,
    allProducts: List<Product>,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (
        id: String,
        name: String,
        barcode: String,
        category: String,
        costPrice: Double,
        sellingPrice: Double,
        stockQuantity: Long,
        minStockThreshold: Long,
        unit: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(existingProduct?.name ?: "") }
    var barcode by remember {
        mutableStateOf(
            existingProduct?.barcode ?: ("628" + (1000000..9999999).random().toString())
        )
    }
    var category by remember { mutableStateOf(existingProduct?.category ?: "عام") }
    var costPriceStr by remember { mutableStateOf(existingProduct?.costPrice?.toString() ?: "") }
    var sellingPriceStr by remember { mutableStateOf(existingProduct?.sellingPrice?.toString() ?: "") }
    var stockQtyStr by remember { mutableStateOf(existingProduct?.stockQuantity?.toString() ?: "10") }
    var minThresholdStr by remember { mutableStateOf(existingProduct?.minStockThreshold?.toString() ?: "5") }
    var unit by remember { mutableStateOf(existingProduct?.unit ?: "قطعة") }
    var showScanner by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingProduct == null) "إضافة صنف جديد للمخزون" else "تعديل بيانات الصنف",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الصنف *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("رقم الباركود") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_barcode_input"),
                    trailingIcon = {
                        IconButton(onClick = { showScanner = true }) {
                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "مسح الباركود")
                        }
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("التصنيف") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("الوحدة (قطعة/كجم)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPriceStr,
                        onValueChange = { costPriceStr = it },
                        label = { Text("سعر الشراء ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_cost_input")
                    )
                    OutlinedTextField(
                        value = sellingPriceStr,
                        onValueChange = { sellingPriceStr = it },
                        label = { Text("سعر البيع ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_selling_input")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockQtyStr,
                        onValueChange = { stockQtyStr = it },
                        label = { Text("الكمية الحالية") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_stock_input")
                    )
                    OutlinedTextField(
                        value = minThresholdStr,
                        onValueChange = { minThresholdStr = it },
                        label = { Text("حد تنبيه النقص") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_threshold_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costPriceStr.toDoubleOrNull() ?: 0.0
                    val sell = sellingPriceStr.toDoubleOrNull() ?: 0.0
                    val stock = stockQtyStr.toLongOrNull() ?: 0L
                    val minT = minThresholdStr.toLongOrNull() ?: 5L
                    onSave(
                        existingProduct?.id ?: "",
                        name,
                        barcode,
                        category,
                        cost,
                        sell,
                        stock,
                        minT,
                        unit
                    )
                },
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text("حفظ الصنف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )

    if (showScanner) {
        CameraBarcodeScannerDialog(
            products = allProducts,
            title = "مسح باركود الصنف",
            onBarcodeScanned = { scanned -> barcode = scanned },
            onDismiss = { showScanner = false }
        )
    }
}

@Composable
private fun RestockDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirmAddStock: (Long) -> Unit
) {
    var addedQtyStr by remember { mutableStateOf("10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("توريد مخزون: ${product.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("الكمية الحالية بالمخزون: ${product.stockQuantity} ${product.unit}")
                OutlinedTextField(
                    value = addedQtyStr,
                    onValueChange = { addedQtyStr = it },
                    label = { Text("الكمية المراد إضافتها") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = addedQtyStr.toLongOrNull() ?: 0L
                    if (qty != 0L) onConfirmAddStock(qty)
                }
            ) {
                Text("تأكيد الإضافة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
