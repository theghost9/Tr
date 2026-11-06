package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val barcode: String = "",
    val category: String = "عام",
    val costPrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val stockQuantity: Long = 0L,
    val minStockThreshold: Long = 5L,
    val unit: String = "قطعة",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isOutOfStock: Boolean
        get() = stockQuantity <= 0L

    val isLowStock: Boolean
        get() = stockQuantity in 1L..minStockThreshold

    val unitProfit: Double
        get() = sellingPrice - costPrice

    val profitMarginPercent: Double
        get() = if (sellingPrice > 0.0) ((sellingPrice - costPrice) / sellingPrice) * 100.0 else 0.0
}

@IgnoreExtraProperties
data class InvoiceLineItem(
    val productId: String = "",
    val productName: String = "",
    val barcode: String = "",
    val unit: String = "قطعة",
    val quantity: Long = 1L,
    val costPrice: Double = 0.0,
    val unitPrice: Double = 0.0,
    val lineTotal: Double = 0.0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "productId" to productId,
        "productName" to productName,
        "barcode" to barcode,
        "unit" to unit,
        "quantity" to quantity,
        "costPrice" to costPrice,
        "unitPrice" to unitPrice,
        "lineTotal" to lineTotal
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): InvoiceLineItem {
            return InvoiceLineItem(
                productId = map["productId"] as? String ?: "",
                productName = map["productName"] as? String ?: "",
                barcode = map["barcode"] as? String ?: "",
                unit = map["unit"] as? String ?: "قطعة",
                quantity = (map["quantity"] as? Number)?.toLong() ?: 1L,
                costPrice = (map["costPrice"] as? Number)?.toDouble() ?: 0.0,
                unitPrice = (map["unitPrice"] as? Number)?.toDouble() ?: 0.0,
                lineTotal = (map["lineTotal"] as? Number)?.toDouble() ?: 0.0
            )
        }
    }
}

@IgnoreExtraProperties
data class Invoice(
    @DocumentId val id: String = "",
    val userId: String = "",
    val invoiceNumber: String = "",
    val customerId: String = "",
    val customerName: String = "عميل نقدي",
    val itemsSummary: String = "",
    val items: List<Map<String, Any>> = emptyList(),
    val itemCount: Long = 0L,
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val totalCost: Double = 0.0,
    val netProfit: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val paymentMethod: String = "CASH",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedLineItems(): List<InvoiceLineItem> =
        items.map { InvoiceLineItem.fromMap(it) }
}

@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val totalPurchases: Double = 0.0,
    val currentDebt: Double = 0.0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DebtPayment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val amount: Double = 0.0,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StoreSettings(
    @DocumentId val id: String = "config",
    val userId: String = "",
    val storeName: String = "متجر تاجر برو",
    val storePhone: String = "",
    val storeAddress: String = "",
    val currencySymbol: String = "$",
    val defaultTaxRate: Double = 0.0,
    val receiptFooter: String = "شكراً لتعاملكم معنا - نسعد بخدمتكم دائماً",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CartItem(
    val product: Product,
    val quantity: Long = 1L,
    val customUnitPrice: Double = product.sellingPrice
) {
    val lineTotal: Double
        get() = customUnitPrice * quantity

    val lineCost: Double
        get() = product.costPrice * quantity

    val lineProfit: Double
        get() = lineTotal - lineCost
}
