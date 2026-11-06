package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.StoreSettings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(
    currentSettings: StoreSettings,
    onDismiss: () -> Unit,
    onSave: (
        storeName: String,
        storePhone: String,
        storeAddress: String,
        currencySymbol: String,
        defaultTaxRate: Double,
        receiptFooter: String
    ) -> Unit
) {
    var storeName by remember { mutableStateOf(currentSettings.storeName) }
    var storePhone by remember { mutableStateOf(currentSettings.storePhone) }
    var storeAddress by remember { mutableStateOf(currentSettings.storeAddress) }
    var currencySymbol by remember { mutableStateOf(currentSettings.currencySymbol.ifBlank { "$" }) }
    var taxRateStr by remember { mutableStateOf(currentSettings.defaultTaxRate.toString()) }
    var receiptFooter by remember { mutableStateOf(currentSettings.receiptFooter) }

    val presetCurrencies = listOf(
        "$" to "دولار ($)",
        "ج.م" to "جنيه (ج.م)",
        "ر.س" to "ريال (ر.س)",
        "د.إ" to "درهم (د.إ)",
        "د.ك" to "دينار (د.ك)",
        "€" to "يورو (€)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "إعدادات المحل والعملة والفاتورة",
                style = MaterialTheme.typography.titleLarge,
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
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("اسم المحل التجاري (يظهر في الفاتورة)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_store_name_input")
                )

                OutlinedTextField(
                    value = storePhone,
                    onValueChange = { storePhone = it },
                    label = { Text("رقم هاتف المحل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = storeAddress,
                    onValueChange = { storeAddress = it },
                    label = { Text("عنوان المحل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "اختر العملة الافتراضية:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetCurrencies.forEach { (symbol, label) ->
                        FilterChip(
                            selected = currencySymbol == symbol,
                            onClick = { currencySymbol = symbol },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = currencySymbol,
                    onValueChange = { currencySymbol = it },
                    label = { Text("رمز العملة المخصص") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_currency_input")
                )

                OutlinedTextField(
                    value = taxRateStr,
                    onValueChange = { taxRateStr = it },
                    label = { Text("نسبة الضريبة الافتراضية (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = receiptFooter,
                    onValueChange = { receiptFooter = it },
                    label = { Text("عبارة أسفل الفاتورة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val context = androidx.compose.ui.platform.LocalContext.current
                val fbUrl = "https://www.facebook.com/share/1CWeZYT8pj/"
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "تم بناء التطبيق من خلال TRiX",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "صفحة فيسبوك: $fbUrl",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        androidx.compose.material3.OutlinedButton(
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_trix_facebook_button")
                        ) {
                            Text("فتح صفحة TRiX على فيسبوك")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tax = taxRateStr.toDoubleOrNull() ?: 0.0
                    onSave(
                        storeName,
                        storePhone,
                        storeAddress,
                        currencySymbol,
                        tax,
                        receiptFooter
                    )
                },
                modifier = Modifier.testTag("save_settings_button")
            ) {
                Text("حفظ الإعدادات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
