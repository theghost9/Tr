package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Customer
import com.example.data.model.DebtPayment
import com.example.data.model.Invoice
import com.example.data.model.StoreSettings
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LowStockAmber
import com.example.ui.theme.ProfitGreen
import com.example.util.PdfInvoiceGenerator
import com.example.util.PdfInvoiceGenerator.formatMoney
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvoicesCustomersScreen(
    invoices: List<Invoice>,
    customers: List<Customer>,
    payments: List<DebtPayment>,
    settings: StoreSettings,
    initialTabIndex: Int = 0,
    onSelectInvoiceForReceipt: (Invoice) -> Unit,
    onSaveCustomer: (
        id: String,
        name: String,
        phone: String,
        address: String,
        notes: String,
        initialDebt: Double,
        existingCustomer: Customer?
    ) -> Unit,
    onRecordPayment: (Customer, Double, String) -> Unit
) {
    val currency = settings.currencySymbol.ifBlank { "$" }
    var selectedTab by remember { mutableIntStateOf(initialTabIndex) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var payingCustomer by remember { mutableStateOf<Customer?>(null) }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showAddCustomerDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_customer_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة عميل", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("سجل الفواتير (${invoices.size})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_invoices")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("العملاء والديون (${customers.size})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_customers")
                )
            }

            Surface(tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            if (selectedTab == 0) "ابحث برقم الفاتورة أو اسم العميل..."
                            else "ابحث باسم العميل أو رقم الهاتف..."
                        )
                    },
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
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            if (selectedTab == 0) {
                val filteredInvoices = remember(invoices, searchQuery) {
                    val q = searchQuery.trim()
                    if (q.isBlank()) invoices
                    else invoices.filter {
                        it.invoiceNumber.contains(q, ignoreCase = true) ||
                            it.customerName.contains(q, ignoreCase = true) ||
                            it.itemsSummary.contains(q, ignoreCase = true)
                    }
                }
                InvoicesHistoryList(
                    invoices = filteredInvoices,
                    settings = settings,
                    currency = currency,
                    onSelectInvoice = onSelectInvoiceForReceipt
                )
            } else {
                val filteredCustomers = remember(customers, searchQuery) {
                    val q = searchQuery.trim()
                    if (q.isBlank()) customers
                    else customers.filter {
                        it.name.contains(q, ignoreCase = true) ||
                            it.phone.contains(q, ignoreCase = true)
                    }
                }
                CustomersAndDebtsList(
                    customers = filteredCustomers,
                    payments = payments,
                    currency = currency,
                    onOpenPaymentDialog = { payingCustomer = it }
                )
            }
        }
    }

    if (showAddCustomerDialog) {
        CustomerFormDialog(
            currency = currency,
            onDismiss = { showAddCustomerDialog = false },
            onSave = { name, phone, address, notes, debt ->
                onSaveCustomer("", name, phone, address, notes, debt, null)
                showAddCustomerDialog = false
            }
        )
    }

    if (payingCustomer != null) {
        RecordDebtPaymentDialog(
            customer = payingCustomer!!,
            currency = currency,
            onDismiss = { payingCustomer = null },
            onConfirmPayment = { amount, notes ->
                onRecordPayment(payingCustomer!!, amount, notes)
                payingCustomer = null
            }
        )
    }
}

@Composable
private fun InvoicesHistoryList(
    invoices: List<Invoice>,
    settings: StoreSettings,
    currency: String,
    onSelectInvoice: (Invoice) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.forLanguageTag("ar")) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("invoices_history_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (invoices.isEmpty()) {
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
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "لا توجد فواتير مطابقة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            items(invoices, key = { it.id }) { invoice ->
                val dateStr = dateFormat.format(invoice.createdAt?.toDate() ?: Date())
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectInvoice(invoice) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = invoice.invoiceNumber,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = if (invoice.remainingAmount > 0.0) {
                                    LowStockAmber.copy(alpha = 0.14f)
                                } else {
                                    ProfitGreen.copy(alpha = 0.14f)
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (invoice.remainingAmount > 0.0) {
                                        "متبقي: ${formatMoney(invoice.remainingAmount)} $currency"
                                    } else {
                                        "مدفوعة بالكامل"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (invoice.remainingAmount > 0.0) LowStockAmber else ProfitGreen,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "العميل: ${invoice.customerName}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = invoice.itemsSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "الإجمالي: ${formatMoney(invoice.totalAmount)} $currency",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "صافي الربح: +${formatMoney(invoice.netProfit)} $currency",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = ProfitGreen
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    PdfInvoiceGenerator.generateAndShareInvoicePdf(context, invoice, settings)
                                },
                                modifier = Modifier.testTag("export_pdf_${invoice.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مشاركة PDF")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomersAndDebtsList(
    customers: List<Customer>,
    payments: List<DebtPayment>,
    currency: String,
    onOpenPaymentDialog: (Customer) -> Unit
) {
    val totalDebt = customers.sumOf { it.currentDebt }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("customers_debts_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "إجمالي الديون المستحقة عند العملاء",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "عدد العملاء المدينين: ${customers.count { it.currentDebt > 0 }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${formatMoney(totalDebt)} $currency",
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = if (totalDebt > 0) LowStockAmber else ProfitGreen
                    )
                }
            }
        }

        items(customers, key = { it.id }) { customer ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = customer.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (customer.phone.isNotBlank()) {
                                Text(
                                    text = "هاتف: ${customer.phone}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            color = if (customer.currentDebt > 0.0) {
                                LowStockAmber.copy(alpha = 0.14f)
                            } else {
                                ProfitGreen.copy(alpha = 0.14f)
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (customer.currentDebt > 0.0) {
                                    "مدين: ${formatMoney(customer.currentDebt)} $currency"
                                } else {
                                    "خالص الرصيد"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = if (customer.currentDebt > 0.0) LowStockAmber else ProfitGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إجمالي المشتريات: ${formatMoney(customer.totalPurchases)} $currency",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (customer.currentDebt > 0.0) {
                            FilledTonalButton(
                                onClick = { onOpenPaymentDialog(customer) },
                                modifier = Modifier.testTag("pay_debt_btn_${customer.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تسجيل سداد دفعة")
                            }
                        }
                    }
                }
            }
        }

        if (payments.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "أحدث سندات القبض وتحصيل الديون",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(payments.take(6), key = { it.id }) { payment ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "سداد من: ${payment.customerName}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (payment.notes.isNotBlank()) {
                                Text(
                                    text = payment.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "+${formatMoney(payment.amount)} $currency",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = ProfitGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerFormDialog(
    currency: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var initialDebtStr by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة عميل جديد", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم العميل *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = initialDebtStr,
                    onValueChange = { initialDebtStr = it },
                    label = { Text("رصيد دين سابق ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val debt = initialDebtStr.toDoubleOrNull() ?: 0.0
                    onSave(name, phone, address, notes, debt)
                },
                modifier = Modifier.testTag("save_customer_button")
            ) {
                Text("حفظ العميل")
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
private fun RecordDebtPaymentDialog(
    customer: Customer,
    currency: String,
    onDismiss: () -> Unit,
    onConfirmPayment: (Double, String) -> Unit
) {
    var amountStr by remember { mutableStateOf(customer.currentDebt.toString()) }
    var notes by remember { mutableStateOf("دفعة سداد نقدي") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل دفعة سداد: ${customer.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "الرصيد المدين الحالي: ${formatMoney(customer.currentDebt)} $currency",
                    fontWeight = FontWeight.Bold,
                    color = LowStockAmber
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("المبلغ المحصل ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("بيان السند / ملاحظات") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    onConfirmPayment(amt, notes)
                }
            ) {
                Text("تأكيد السداد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
