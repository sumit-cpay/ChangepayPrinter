package com.example.cpay_printer.models

import android.util.Log
import net.posprinter.utils.DataForSendToPrinterPos58
import java.util.Locale
import kotlin.math.min

// =========================
// Printer formatting helpers
// =========================
fun boldOn(): ByteArray = byteArrayOf(0x1B, 0x45, 0x01)
fun boldOff(): ByteArray = byteArrayOf(0x1B, 0x45, 0x00)

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
    val orderTotal: Double = 0.0
) {

    // -------------------------------
    // 58 mm receipt
    // -------------------------------
    fun generateMainReceipt58(): MutableList<ByteArray> {
        val list = mutableListOf<ByteArray>()
        list.add(DataForSendToPrinterPos58.initializePrinter())
        list.add(DataForSendToPrinterPos58.selectAlignment(1))

        list.add("**** MAIN RECEIPT ****\n".toByteArray())
        list.add(DataForSendToPrinterPos58.selectCharacterSize(18))
        list.add("$orderId\n".toByteArray())
        list.add(DataForSendToPrinterPos58.selectCharacterSize(0))

        list.add("Time: $datetime\n".toByteArray())
        list.add("--------------------------------\n".toByteArray())
        list.add(DataForSendToPrinterPos58.selectCharacterSize(18))
        list.add("Name: $customerName\n".toByteArray())
        list.add(DataForSendToPrinterPos58.selectCharacterSize(0))
        list.add("--------------------------------\n".toByteArray())

        if (!customerNote.isNullOrEmpty()) {
            list.add("NOTE: $customerNote\n".toByteArray())
            list.add("--------------------------------\n".toByteArray())
        }

        list.add("Item               Qty   Price\n".toByteArray())
        list.add("-------------------------------\n".toByteArray())

        items.forEach { list.add(formatItem58(it).toByteArray()) }

        list.add("--------------------------------\n".toByteArray())

        otherCharges.forEach { (name, amount) -> list.add("$name: ${formatMoney(amount)}\n".toByteArray()) }
        list.add("TOTAL: ${formatMoney(orderTotal)}\n".toByteArray())
        list.add("--------------------------------\n".toByteArray())

        repeat(2) { list.add(DataForSendToPrinterPos58.printAndFeedLine()) }
        list.add(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
        return list
    }

    private fun formatItem58(item: CartItemReceipt): String {
        val NAME_WIDTH = 18
        val QTY_WIDTH = 3
        val PRICE_WIDTH = 7
        val nameLines = wrapText(item.name, NAME_WIDTH)
        val qtyStr = item.quantity.toString()
        val priceStr = formatMoney(item.price)

        val sb = StringBuilder()
        val firstName = nameLines.firstOrNull() ?: ""
        sb.append("${firstName.padEnd(NAME_WIDTH, ' ')} ${qtyStr.padStart(QTY_WIDTH, ' ')} ${priceStr.padStart(PRICE_WIDTH, ' ')}\n")

        if (nameLines.size > 1) {
            for (i in 1 until nameLines.size) {
                sb.append("  ${nameLines[i]}\n")
            }
        }

        return sb.toString()
    }

    // -------------------------------
    // 80 mm receipt
    // -------------------------------
    fun generateMainReceipt80(): MutableList<ByteArray> {
        val list = mutableListOf<ByteArray>()
        list.add(DataForSendToPrinterPos58.initializePrinter())
        list.add(DataForSendToPrinterPos58.selectAlignment(1))

        list.add("********* MAIN RECEIPT (80mm) *********\n".toByteArray())
        list.add(DataForSendToPrinterPos58.selectCharacterSize(18))
        list.add("$orderId\n".toByteArray())
        list.add(DataForSendToPrinterPos58.selectCharacterSize(0))

        list.add("Time: $datetime\n".toByteArray())
        list.add("------------------------------------------\n".toByteArray())
                list.add(DataForSendToPrinterPos58.selectCharacterSize(18))
        list.add("Name: $customerName\n".toByteArray())
                list.add(DataForSendToPrinterPos58.selectCharacterSize(0))

       list.add("-------------------------------------------\n".toByteArray())
        if (!customerNote.isNullOrEmpty()) {
            list.add("NOTE: $customerNote\n".toByteArray())
            list.add("------------------------------------------\n".toByteArray())
        }

        list.add("Item                       Qty     Price\n".toByteArray())
        list.add("------------------------------------------\n".toByteArray())

        items.forEach { list.add(formatItem80(it).toByteArray()) }

        list.add("------------------------------------------\n".toByteArray())
        otherCharges.forEach { (name, amount) -> list.add("$name: ${formatMoney(amount)}\n".toByteArray()) }
        list.add("TOTAL: ${formatMoney(orderTotal)}\n".toByteArray())

        repeat(3) { list.add(DataForSendToPrinterPos58.printAndFeedLine()) }
        list.add(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
        return list
    }

    private val NAME_WIDTH_MAIN_80 = 26
    private val PRICE_WIDTH_80 = 10

    private fun formatItem80(item: CartItemReceipt): String {
        val nameLines = wrapText(item.name, NAME_WIDTH_MAIN_80)
        val qtyStr = item.quantity.toString()
        val priceStr = formatMoney(item.price)

        val sb = StringBuilder()
        sb.append("${nameLines[0].padEnd(NAME_WIDTH_MAIN_80, ' ')} ${qtyStr.padStart(4)} ${priceStr.padStart(PRICE_WIDTH_80, ' ')}\n")

        for (i in 1 until nameLines.size) {
            sb.append("  ${nameLines[i]}\n")
        }
        return sb.toString()
    }

    private fun formatMoney(value: Double): String = String.format(Locale.US, "%.2f", value)

    private fun wrapText(text: String, width: Int): List<String> {
        if (text.isEmpty()) return listOf("")
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            if (current.isEmpty()) current.append(word)
            else if (current.length + 1 + word.length <= width) current.append(" ").append(word)
            else {
                lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }
}

// =========================
// KOT RECEIPT
// =========================
data class KOTPrintableReceipt(
    val orderId: String = "",
    val datetime: String = "",
    val businessName: String = "",
    val customerNote: String? = null,
    val items: List<CartItemReceipt> = emptyList(),
    val categoryName: String = "",
) {

    fun generateKOT58(): MutableList<ByteArray> {
        val list = mutableListOf<ByteArray>()
        list.add(DataForSendToPrinterPos58.initializePrinter())
        list.add("*** KOT: $categoryName ***\n".toByteArray())
        list.add("Order: $orderId\n".toByteArray())
        list.add("Time: $datetime\n".toByteArray())

        list.add("--------------------------------\n".toByteArray())

        if (!customerNote.isNullOrEmpty()) {
            list.add("NOTE: $customerNote\n".toByteArray())
            list.add("--------------------------------\n".toByteArray())
        }

        list.add(boldOn())
        list.add("Item                   Qty\n".toByteArray())
        list.add(boldOff())

        list.add("--------------------------------\n".toByteArray())
        items.forEach { list.add(formatItem58(it).toByteArray()) }

        repeat(2) { list.add(DataForSendToPrinterPos58.printAndFeedLine()) }
        list.add(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
        return list
    }

    private val NAME_KOT_58 = 23

    private fun formatItem58(item: CartItemReceipt): String {
        val lines = wrapText(item.name, NAME_KOT_58)
        val q = item.quantity.toString()
        val sb = StringBuilder()
        sb.append("${lines[0].padEnd(NAME_KOT_58)}  $q\n")
        for (i in 1 until lines.size) sb.append("  ${lines[i]}\n")
        return sb.toString()
    }

    fun generateKOT80(): MutableList<ByteArray> {
        val list = mutableListOf<ByteArray>()
        list.add(DataForSendToPrinterPos58.initializePrinter())
        list.add("******* KOT: $categoryName *******\n".toByteArray())
        list.add("Order: $orderId\n".toByteArray())
        list.add("Time: $datetime\n\n".toByteArray())

        if (!customerNote.isNullOrEmpty()) list.add("NOTE: $customerNote\n\n".toByteArray())

        list.add(boldOn())
        list.add("Item                             Qty\n".toByteArray())
        list.add(boldOff())

        list.add("------------------------------------------\n".toByteArray())
        items.forEach { list.add(formatItem80(it).toByteArray()) }

        repeat(2) { list.add(DataForSendToPrinterPos58.printAndFeedLine()) }
        list.add(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
        return list
    }

    private val NAME_KOT_80 = 31

    private fun formatItem80(item: CartItemReceipt): String {
        val lines = wrapText(item.name, NAME_KOT_80)
        val q = item.quantity.toString().padStart(4)
        val sb = StringBuilder()
        sb.append("${lines[0].padEnd(NAME_KOT_80)} $q\n")
        for (i in 1 until lines.size) sb.append("  ${lines[i]}\n")
        return sb.toString()
    }

    private fun wrapText(text: String, width: Int): List<String> {
        if (text.isEmpty()) return listOf("")
        val out = mutableListOf<String>()
        var line = StringBuilder()
        for (w in text.split(" ")) {
            if (line.isEmpty()) line.append(w)
            else if (line.length + w.length + 1 <= width) line.append(" ").append(w)
            else {
                out.add(line.toString())
                line = StringBuilder(w)
            }
        }
        if (line.isNotEmpty()) out.add(line.toString())
        return out
    }
}

// =========================
// Combine Main + All KOTs
// =========================
data class KotPrintableReceiptV2(
    val main: PrintableReceiptMain = PrintableReceiptMain(),
    val kotSections: Map<String, List<CartItemReceipt>> = emptyMap()
) {
    fun generateMainReceipt58() = main.generateMainReceipt58()
    fun generateMainReceipt80() = main.generateMainReceipt80()

    fun generateAllKOTs58(): List<MutableList<ByteArray>> =
        kotSections.map { (cat, items) ->
            KOTPrintableReceipt(
                orderId = main.orderId,
                datetime = main.datetime,
                businessName = main.businessName,
                customerNote = main.customerNote,
                categoryName = cat,
                items = items,
            ).generateKOT58()
        }

    fun generateAllKOTs80(): List<MutableList<ByteArray>> =
        kotSections.map { (cat, items) ->
            KOTPrintableReceipt(
                orderId = main.orderId,
                datetime = main.datetime,
                businessName = main.businessName,
                customerNote = main.customerNote,
                categoryName = cat,
                items = items,
            ).generateKOT80()
        }
}
