package com.example.cpay_printer.models

import android.util.Log
import net.posprinter.utils.DataForSendToPrinterPos58
import java.util.Locale
import kotlin.math.min

// =========================
// Cart Item for Receipt
// =========================
data class CartItemReceipt(
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0,
    val total: Double = 0.0,
    val category: String? = null
)


// =========================
// MAIN RECEIPT
// =========================
data class PrintableReceiptMain(
    val orderId: String = "",
    val datetime: String = "",
    val businessName: String = "",
    val customerPhone: String = "",
    val customerName: String = "",
    val deliveryType: String = "",
    val address: String = "",
    val customerNote: String? = null,
    val items: List<CartItemReceipt> = emptyList(),
    val otherCharges: List<Pair<String, Double>> = emptyList(),
    val discount: Double = 0.0,
    val orderTotal: Double = 0.0
) {

    fun generateMainReceipt58(): MutableList<ByteArray> {
        val list = mutableListOf<ByteArray>()
        list.add(DataForSendToPrinterPos58.initializePrinter())
        list.add(DataForSendToPrinterPos58.selectAlignment(1))

        list.add("**** MAIN RECEIPT ****\n".toByteArray())
        list.add("Order: $orderId\n".toByteArray())
        list.add("Time: $datetime\n".toByteArray())
        list.add("Business: $businessName\n".toByteArray())
        list.add("--------------------------------\n".toByteArray())

        list.add("Customer: $customerName\nPhone: $customerPhone\n".toByteArray())

        if (!customerNote.isNullOrEmpty()) {
            list.add("NOTE: $customerNote\n".toByteArray())
            list.add("--------------------------------\n".toByteArray())
        }

        list.add("Item               Qty   Price\n".toByteArray())
        list.add("--------------------------------\n".toByteArray())

        items.forEach { item ->
            list.add(formatItem(item).toByteArray())
        }

        list.add("--------------------------------\n".toByteArray())

        otherCharges.forEach { (name, amount) ->
            list.add("$name: ${formatMoney(amount)}\n".toByteArray())
        }

        list.add("Discount: ${formatMoney(discount)}\n".toByteArray())
        list.add("TOTAL: ${formatMoney(orderTotal)}\n".toByteArray())
        list.add("--------------------------------\n".toByteArray())

        repeat(2) { list.add(DataForSendToPrinterPos58.printAndFeedLine()) }
        return list
    }

    // Column widths
    private val NAME_WIDTH_MAIN = 18
    private val QTY_WIDTH = 3
    private val PRICE_WIDTH = 7


    private fun formatItem(item: CartItemReceipt): String {
        val nameLines = wrapText(item.name, NAME_WIDTH_MAIN)
        val qtyStr = item.quantity.toString()
        val priceStr = formatMoney(item.price)

        val sb = StringBuilder()

        val firstName = nameLines.firstOrNull() ?: ""
        val namePart = firstName.padEnd(NAME_WIDTH_MAIN, ' ')
        val qtyPart = qtyStr.padStart(QTY_WIDTH, ' ')
        val pricePart = priceStr.padStart(PRICE_WIDTH, ' ')

        sb.append("$namePart $qtyPart $pricePart\n")

        if (nameLines.size > 1) {
            for (i in 1 until nameLines.size) {
                val cont = nameLines[i]
                val contLine = cont.padEnd(NAME_WIDTH_MAIN, ' ')
                sb.append("  $contLine\n")
            }
        }

        return sb.toString()
    }

    private fun formatMoney(value: Double): String =
        String.format(Locale.US, "%.2f", value)

    private fun wrapText(text: String, width: Int): List<String> {
        if (text.isEmpty()) return listOf("")
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = StringBuilder()

        for (word in words) {
            if (current.isEmpty()) {
                if (word.length <= width) current.append(word)
                else {
                    var start = 0
                    while (start < word.length) {
                        val end = min(start + width, word.length)
                        lines.add(word.substring(start, end))
                        start = end
                    }
                }
            } else {
                if (current.length + 1 + word.length <= width) {
                    current.append(" ").append(word)
                } else {
                    lines.add(current.toString())
                    current = StringBuilder(word)
                }
            }
        }

        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }
}



// =========================
// KOT RECEIPT (Fixed)
// =========================
data class KOTPrintableReceipt(
    val orderId: String = "",
    val datetime: String = "",
    val businessName: String = "",
    val customerNote: String? = null,
    val items: List<CartItemReceipt> = emptyList(),
    val categoryName: String = ""
) {

    fun generateKOT58(): MutableList<ByteArray> {
        val list = mutableListOf<ByteArray>()
        list.add(DataForSendToPrinterPos58.initializePrinter())

        list.add("***** KOT: $categoryName *****\n".toByteArray())
        list.add("Order: $orderId\n".toByteArray())
        list.add("Time: $datetime\n".toByteArray())
        list.add("--------------------------------\n".toByteArray())

        if (!customerNote.isNullOrEmpty()) {
            list.add("NOTE: $customerNote\n".toByteArray())
            list.add("--------------------------------\n".toByteArray())
        }

        list.add("Item                     Qty\n".toByteArray())

        list.add("--------------------------------\n".toByteArray())

        items.forEach { item ->
            list.add(formatItem(item).toByteArray())
        }

        list.add("--------------------------------\n".toByteArray())
        repeat(2) { list.add(DataForSendToPrinterPos58.printAndFeedLine()) }

        return list
    }

    private val NAME_WIDTH_KOT = 20
    private val QTY_WIDTH_KOT = 6

    private fun formatItem(item: CartItemReceipt): String {
        val nameLines = wrapText(item.name, NAME_WIDTH_KOT)
        val qtyStr = item.quantity.toString()
        val sb = StringBuilder()

        val firstName = nameLines.firstOrNull() ?: ""
        val namePart = firstName.padEnd(NAME_WIDTH_KOT, ' ')
        val qtyPart = qtyStr.padStart(QTY_WIDTH_KOT, ' ')

        sb.append("$namePart $qtyPart\n")

        if (nameLines.size > 1) {
            for (i in 1 until nameLines.size) {
                val contLine = nameLines[i].padEnd(NAME_WIDTH_KOT, ' ')
                sb.append("  $contLine\n")
            }
        }

        return sb.toString()
    }

    private fun wrapText(text: String, width: Int): List<String> {
        if (text.isEmpty()) return listOf("")
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = StringBuilder()

        for (word in words) {
            if (current.isEmpty()) {
                if (word.length <= width) current.append(word)
                else {
                    var start = 0
                    while (start < word.length) {
                        val end = kotlin.math.min(start + width, word.length)
                        lines.add(word.substring(start, end))
                        start = end
                    }
                }
            } else {
                if (current.length + 1 + word.length <= width) {
                    current.append(" ").append(word)
                } else {
                    lines.add(current.toString())
                    current = StringBuilder(word)
                }
            }
        }

        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }
}



// =========================
// Combine Main + All KOTs
// =========================
data class KotPrintableReceiptV2(
    val main: PrintableReceiptMain = PrintableReceiptMain(),
    val kotSections: Map<String, List<CartItemReceipt>> = emptyMap()
) {

    fun generateMainReceipt58(): MutableList<ByteArray> = main.generateMainReceipt58()

    fun generateAllKOTs58(): List<MutableList<ByteArray>> {
        val result = mutableListOf<MutableList<ByteArray>>()

        kotSections.forEach { (category, items) ->
            val kotReceipt = KOTPrintableReceipt(
                orderId = main.orderId,
                datetime = main.datetime,
                businessName = main.businessName,
                customerNote = main.customerNote,
                items = items,
                categoryName = category
            )
            result.add(kotReceipt.generateKOT58())
        }

        return result
    }
}
