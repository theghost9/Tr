package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.Invoice
import com.example.data.model.StoreSettings
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfInvoiceGenerator {

    fun generateAndShareInvoicePdf(
        context: Context,
        invoice: Invoice,
        settings: StoreSettings
    ): Result<File> {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 420
            val lineItems = invoice.parsedLineItems()
            val pageHeight = (540 + lineItems.size * 34).coerceAtLeast(620)

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#065F46")
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 96f, headerPaint)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(settings.storeName.ifBlank { "متجر تاجر برو" }, pageWidth / 2f, 40f, titlePaint)

            val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#D1FAE5")
                textSize = 11f
                textAlign = Paint.Align.CENTER
            }
            val contactLine = listOf(settings.storePhone, settings.storeAddress)
                .filter { it.isNotBlank() }
                .joinToString(" | ")
                .ifBlank { "فاتورة مبيعات ضريبية / إيصال رسمي" }
            canvas.drawText(contactLine, pageWidth / 2f, 65f, subHeaderPaint)

            val dateFormatter = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.forLanguageTag("ar"))
            val dateStr = dateFormatter.format(invoice.createdAt?.toDate() ?: Date())

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E293B")
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#334155")
                textSize = 12f
                textAlign = Paint.Align.LEFT
            }

            var y = 125f
            canvas.drawText("رقم الفاتورة: ${invoice.invoiceNumber}", pageWidth - 24f, y, labelPaint)
            canvas.drawText(dateStr, 24f, y, valuePaint)

            y += 24f
            canvas.drawText("العميل: ${invoice.customerName}", pageWidth - 24f, y, labelPaint)
            val methodAr = when (invoice.paymentMethod) {
                "CASH" -> "نقدي"
                "CREDIT" -> "آجل (ذمم)"
                "PARTIAL" -> "دفع جزئي"
                "CARD" -> "بطاقة / شبكة"
                else -> invoice.paymentMethod
            }
            canvas.drawText("الدفع: $methodAr", 24f, y, valuePaint)

            y += 18f
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CBD5E1")
                strokeWidth = 1.5f
            }
            canvas.drawLine(24f, y, pageWidth - 24f, y, dividerPaint)

            // Table Header
            y += 24f
            val tableHeaderBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F1F5F9")
                style = Paint.Style.FILL
            }
            canvas.drawRect(24f, y - 16f, pageWidth - 24f, y + 10f, tableHeaderBg)

            canvas.drawText("الصنف", pageWidth - 32f, y, labelPaint)
            canvas.drawText("الكمية x السعر", pageWidth / 2f + 20f, y, labelPaint)
            canvas.drawText("الإجمالي", 32f, y, valuePaint)

            y += 26f
            val itemPaintRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 11.5f
                textAlign = Paint.Align.RIGHT
            }
            val itemPaintLeft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                textSize = 11.5f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }

            val currency = settings.currencySymbol.ifBlank { "$" }

            for (item in lineItems) {
                canvas.drawText(item.productName.take(28), pageWidth - 32f, y, itemPaintRight)
                canvas.drawText(
                    "${item.quantity} × ${formatMoney(item.unitPrice)}",
                    pageWidth / 2f + 20f,
                    y,
                    itemPaintRight
                )
                canvas.drawText(
                    "${formatMoney(item.lineTotal)} $currency",
                    32f,
                    y,
                    itemPaintLeft
                )
                y += 28f
            }

            canvas.drawLine(24f, y, pageWidth - 24f, y, dividerPaint)
            y += 24f

            canvas.drawText("الإجمالي الفرعي:", pageWidth - 32f, y, labelPaint)
            canvas.drawText("${formatMoney(invoice.subtotal)} $currency", 32f, y, itemPaintLeft)

            if (invoice.discountAmount > 0.0) {
                y += 22f
                canvas.drawText("الخصم:", pageWidth - 32f, y, labelPaint)
                canvas.drawText("-${formatMoney(invoice.discountAmount)} $currency", 32f, y, itemPaintLeft)
            }

            if (invoice.taxAmount > 0.0) {
                y += 22f
                canvas.drawText("الضريبة:", pageWidth - 32f, y, labelPaint)
                canvas.drawText("+${formatMoney(invoice.taxAmount)} $currency", 32f, y, itemPaintLeft)
            }

            y += 28f
            val totalBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#ECFDF5")
                style = Paint.Style.FILL
            }
            canvas.drawRect(24f, y - 18f, pageWidth - 24f, y + 12f, totalBoxPaint)

            val grandTotalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#065F46")
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val grandTotalValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#065F46")
                textSize = 14f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("الإجمالي النهائي المطلوب:", pageWidth - 32f, y, grandTotalPaint)
            canvas.drawText("${formatMoney(invoice.totalAmount)} $currency", 32f, y, grandTotalValuePaint)

            y += 28f
            canvas.drawText("المبلغ المدفوع:", pageWidth - 32f, y, labelPaint)
            canvas.drawText("${formatMoney(invoice.paidAmount)} $currency", 32f, y, itemPaintLeft)

            y += 22f
            canvas.drawText("المبلغ المتبقي (آجل):", pageWidth - 32f, y, labelPaint)
            canvas.drawText("${formatMoney(invoice.remainingAmount)} $currency", 32f, y, itemPaintLeft)

            y += 40f
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#64748B")
                textSize = 10.5f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                settings.receiptFooter.ifBlank { "شكراً لتعاملكم معنا" },
                pageWidth / 2f,
                y,
                footerPaint
            )
            y += 18f
            canvas.drawText(
                "تم بناء التطبيق من خلال TRiX | fb.com/share/1CWeZYT8pj",
                pageWidth / 2f,
                y,
                footerPaint
            )

            pdfDocument.finishPage(page)

            val invoicesDir = File(context.cacheDir, "invoices").apply { mkdirs() }
            val safeNum = invoice.invoiceNumber.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val pdfFile = File(invoicesDir, "invoice_${safeNum}.pdf")
            FileOutputStream(pdfFile).use { output ->
                pdfDocument.writeTo(output)
            }
            pdfDocument.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "فاتورة ${invoice.invoiceNumber} - ${settings.storeName}"
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    buildTextReceipt(invoice, settings)
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "مشاركة أو طباعة الفاتورة PDF"))
            Result.success(pdfFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareTextReceipt(context: Context, invoice: Invoice, settings: StoreSettings) {
        val text = buildTextReceipt(invoice, settings)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(sendIntent, "مشاركة إيصال الفاتورة"))
    }

    fun buildTextReceipt(invoice: Invoice, settings: StoreSettings): String {
        val currency = settings.currencySymbol.ifBlank { "$" }
        val sb = StringBuilder()
        sb.appendLine("🧾 *${settings.storeName.ifBlank { "متجر تاجر برو" }}*")
        if (settings.storePhone.isNotBlank()) sb.appendLine("📞 ${settings.storePhone}")
        sb.appendLine("────────────────────")
        sb.appendLine("رقم الفاتورة: ${invoice.invoiceNumber}")
        sb.appendLine("العميل: ${invoice.customerName}")
        sb.appendLine("────────────────────")
        invoice.parsedLineItems().forEach { item ->
            sb.appendLine("• ${item.productName} (${item.quantity} × ${formatMoney(item.unitPrice)}) = ${formatMoney(item.lineTotal)} $currency")
        }
        sb.appendLine("────────────────────")
        if (invoice.discountAmount > 0) {
            sb.appendLine("الخصم: -${formatMoney(invoice.discountAmount)} $currency")
        }
        if (invoice.taxAmount > 0) {
            sb.appendLine("الضريبة: +${formatMoney(invoice.taxAmount)} $currency")
        }
        sb.appendLine("*الإجمالي النهائي: ${formatMoney(invoice.totalAmount)} $currency*")
        sb.appendLine("المدفوع: ${formatMoney(invoice.paidAmount)} $currency")
        if (invoice.remainingAmount > 0) {
            sb.appendLine("المتبقي (آجل): ${formatMoney(invoice.remainingAmount)} $currency")
        }
        sb.appendLine("────────────────────")
        sb.appendLine(settings.receiptFooter.ifBlank { "شكراً لتعاملكم معنا" })
        sb.appendLine("تم بناء التطبيق من خلال TRiX")
        sb.appendLine("https://www.facebook.com/share/1CWeZYT8pj/")
        return sb.toString()
    }

    fun formatMoney(amount: Double): String {
        return String.format(Locale.US, "%.2f", amount)
    }
}
