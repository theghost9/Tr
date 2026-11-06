package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Invoice
import com.example.data.model.Product
import com.example.data.model.StoreSettings
import com.example.ui.theme.DangerRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LowStockAmber
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.DailyBarPoint
import com.example.ui.viewmodel.DashboardMetrics
import com.example.ui.viewmodel.SalesPeriodFilter
import com.example.util.PdfInvoiceGenerator.formatMoney

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    metrics: DashboardMetrics,
    selectedPeriod: SalesPeriodFilter,
    onSelectPeriod: (SalesPeriodFilter) -> Unit,
    products: List<Product>,
    recentInvoices: List<Invoice>,
    settings: StoreSettings,
    onNavigateToPos: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onQuickRestock: (Product) -> Unit,
    onSelectInvoiceForReceipt: (Invoice) -> Unit,
    onSeedStarterData: () -> Unit
) {
    val currency = settings.currencySymbol.ifBlank { "$" }
    val lowStockItems = products.filter { it.isLowStock || it.isOutOfStock }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Daily Financial Summary Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF065F46),
                                    Color(0xFF047857),
                                    Color(0xFF0F172A)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = settings.storeName.ifBlank { "متجر تاجر برو" },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "متابعة المبيعات اليومية وصافي الأرباح بدقة",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFA7F3D0)
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = "${metrics.todayInvoicesCount} فاتورة اليوم",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFFFBBF24),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HeroMetricBox(
                                modifier = Modifier.weight(1f),
                                label = "مبيعات اليوم",
                                amount = "${formatMoney(metrics.todaySales)} $currency",
                                subtext = "المحصل نقداً: ${formatMoney(metrics.todayCollected)} $currency",
                                accentColor = Color.White
                            )
                            HeroMetricBox(
                                modifier = Modifier.weight(1f),
                                label = "صافي ربح اليوم",
                                amount = "${formatMoney(metrics.todayNetProfit)} $currency",
                                subtext = "بعد خصم تكلفة البضاعة",
                                accentColor = Color(0xFF6EE7B7)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onNavigateToPos,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hero_new_invoice_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFBBF24),
                                    contentColor = Color(0xFF0F172A)
                                )
                            ) {
                                Icon(imageVector = Icons.Default.PointOfSale, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("فاتورة جديدة", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onNavigateToInventory,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hero_inventory_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(imageVector = Icons.Default.Inventory2, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("جرد المخزون")
                            }
                        }
                    }
                }
            }
        }

        // Empty-Store Starter Data Banner
        if (products.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ابدأ فوراً في تجربة النظام",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "المخزون فارغ حالياً. يمكنك إضافة أصنافك يدوياً أو تحميل باقة أصناف وعملاء نموذجية بضغطة زر لتجربة إصدار الفواتير وحساب الأرباح فوراً.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Button(
                            onClick = onSeedStarterData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("seed_starter_data_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AddBusiness, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تحميل أصناف وعملاء نموذجيين للتجربة")
                        }
                    }
                }
            }
        }

        // 2. Low-Stock & Out-of-Stock Alerts Section
        if (lowStockItems.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = LowStockAmber
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تنبيهات نقص المخزون (${lowStockItems.size} صنف)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "عرض الكل",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onNavigateToInventory() }
                            )
                        }

                        lowStockItems.take(4).forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (item.isOutOfStock) {
                                                "نفد من المخزون (0 ${item.unit})"
                                            } else {
                                                "المتبقي: ${item.stockQuantity} ${item.unit} (حد التنبيه: ${item.minStockThreshold})"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (item.isOutOfStock) DangerRed else LowStockAmber
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = { onQuickRestock(item) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("quick_restock_${item.id}")
                                    ) {
                                        Text("+10 توريد")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Period Filter & Profit Breakdown KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "تحليل الأرباح والمبيعات حسب الفترة",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SalesPeriodFilter.entries.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { onSelectPeriod(period) },
                            label = { Text(period.labelAr) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "إجمالي المبيعات (${selectedPeriod.labelAr})",
                        value = "${formatMoney(metrics.periodSales)} $currency",
                        subtitle = "عدد الفواتير: ${metrics.periodInvoicesCount}",
                        icon = Icons.Default.ReceiptLong,
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "صافي الربح (${selectedPeriod.labelAr})",
                        value = "${formatMoney(metrics.periodNetProfit)} $currency",
                        subtitle = "التكلفة: ${formatMoney(metrics.periodCost)} $currency",
                        icon = Icons.Default.TrendingUp,
                        accentColor = ProfitGreen
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "ديون العملاء (الآجل)",
                        value = "${formatMoney(metrics.totalCustomerDebt)} $currency",
                        subtitle = "مستحقات لدى العملاء",
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = if (metrics.totalCustomerDebt > 0) LowStockAmber else ProfitGreen
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "قيمة المخزون (بسعر البيع)",
                        value = "${formatMoney(metrics.totalInventoryRetailValue)} $currency",
                        subtitle = "بسعر الشراء: ${formatMoney(metrics.totalInventoryCostValue)} $currency",
                        icon = Icons.Default.Inventory2,
                        accentColor = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // 4. Last 7 Days Sales & Profit Chart
        item {
            SalesAndProfitChartCard(
                points = metrics.last7DaysChart,
                currency = currency
            )
        }

        // 5. Recent Invoices Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "أحدث الفواتير المصدرة",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (recentInvoices.isNotEmpty()) {
                    Text(
                        text = "كل الفواتير",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToInvoices() }
                    )
                }
            }
        }

        if (recentInvoices.isEmpty()) {
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
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "لا توجد فواتير مسجلة حتى الآن",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "انتقل إلى نقطة البيع لإصدار أول فاتورة ومتابعة أرباحك اليومية",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(recentInvoices.take(5), key = { it.id }) { invoice ->
                RecentInvoiceRowCard(
                    invoice = invoice,
                    currency = currency,
                    onClick = { onSelectInvoiceForReceipt(invoice) }
                )
            }
        }

        // 6. Developer Attribution Card (TRiX)
        item {
            TrixDeveloperDashboardCard()
        }
    }
}

@Composable
private fun TrixDeveloperDashboardCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val fbUrl = "https://www.facebook.com/share/1CWeZYT8pj/"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_trix_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "تم بناء التطبيق من خلال TRiX",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "تواصل معنا وتابعنا عبر صفحتنا الرسمية على فيسبوك",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = fbUrl,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = {
                    try {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(fbUrl)
                        )
                        context.startActivity(intent)
                    } catch (_: Exception) {
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1877F2),
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("dashboard_trix_facebook_button")
            ) {
                Text("فيسبوك TRiX", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HeroMetricBox(
    modifier: Modifier = Modifier,
    label: String,
    amount: String,
    subtext: String,
    accentColor: Color
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFD1FAE5)
            )
            Text(
                text = amount,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFCBD5E1),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SalesAndProfitChartCard(
    points: List<DailyBarPoint>,
    currency: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مؤشر المبيعات وصافي الربح (آخر 7 أيام)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(color = Color(0xFF059669), label = "المبيعات")
                    LegendDot(color = Color(0xFFFBBF24), label = "الربح")
                }
            }

            val maxVal = (points.maxOfOrNull { maxOf(it.sales, it.profit) } ?: 1.0).coerceAtLeast(1.0)
            val primaryBarColor = Color(0xFF059669)
            val profitBarColor = Color(0xFFFBBF24)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                points.forEach { pt ->
                    val salesRatio = (pt.sales / maxVal).toFloat().coerceIn(0.04f, 1f)
                    val profitRatio = (pt.profit.coerceAtLeast(0.0) / maxVal).toFloat().coerceIn(0.04f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .width(12.dp)
                                    .fillMaxHeight()
                            ) {
                                val barH = size.height * salesRatio
                                drawRoundRect(
                                    color = primaryBarColor,
                                    topLeft = Offset(0f, size.height - barH),
                                    size = Size(size.width, barH),
                                    cornerRadius = CornerRadius(6f, 6f)
                                )
                            }
                            Canvas(
                                modifier = Modifier
                                    .width(12.dp)
                                    .fillMaxHeight()
                            ) {
                                val barH = size.height * profitRatio
                                drawRoundRect(
                                    color = profitBarColor,
                                    topLeft = Offset(0f, size.height - barH),
                                    size = Size(size.width, barH),
                                    cornerRadius = CornerRadius(6f, 6f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = pt.dayLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecentInvoiceRowCard(
    invoice: Invoice,
    currency: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        text = invoice.invoiceNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = if (invoice.remainingAmount > 0.0) {
                            LowStockAmber.copy(alpha = 0.15f)
                        } else {
                            ProfitGreen.copy(alpha = 0.15f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (invoice.remainingAmount > 0.0) "متبقي آجل" else "خالص",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (invoice.remainingAmount > 0.0) LowStockAmber else ProfitGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "${invoice.customerName} • ${invoice.itemsSummary}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatMoney(invoice.totalAmount)} $currency",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "ربح: +${formatMoney(invoice.netProfit)} $currency",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = ProfitGreen
                )
            }
        }
    }
}
