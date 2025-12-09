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


private fun wrapText(text: String, width: Int): List<String> {
    if (text.isBlank()) return listOf("")

    val result = mutableListOf<String>()
    var remaining = text.trim()

    while (remaining.length > width) {
        result.add(remaining.substring(0, width))
        remaining = remaining.substring(width)
    }

    if (remaining.isNotEmpty()) result.add(remaining)
    return result
}


// =========================
// Cart Item for Receipt
// =========================
data class CartItemReceipt(
    val name: String = "",
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

    val NAME_WIDTH_58 = 23
    val QTY_WIDTH_58 = 3

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

    // ---------- HEADER ----------
    val header =
        "Item".padEnd(NAME_WIDTH_58) +
                "  " +                     
                "Qty\n"
list.add(DataForSendToPrinterPos58.selectAlignment(0))  // <<< FIX

    list.add(header.toByteArray())
    list.add("--------------------------------\n".toByteArray())

    // ---------- ITEMS ----------
    items.forEach {
        list.add(formatItem58(it).toByteArray())
    }

    list.add("--------------------------------\n".toByteArray())

    // ---------- CHARGES ----------
    otherCharges.forEach { (name, amount) ->
        list.add("$name: ${formatMoney(amount)}\n".toByteArray())
    }

    list.add("TOTAL: ${formatMoney(orderTotal)}\n".toByteArray())
    list.add("--------------------------------\n".toByteArray())

    repeat(2) { list.add(DataForSendToPrinterPos58.printAndFeedLine()) }

    // cut
    list.add(byteArrayOf(0x1D, 0x56, 0x42, 0x00))

    return list
}


// --------------------------------
// 58mm item formatter
// --------------------------------

private fun formatItem58(item: CartItemReceipt): String {
    val NAME_WIDTH = 20
    val QTY_WIDTH = 6

    val lines = wrapText(item.name.trimStart(), NAME_WIDTH)
    val qty = item.quantity.toString().padStart(QTY_WIDTH)

    val sb = StringBuilder()

    // First line
    sb.append(lines[0].take(NAME_WIDTH).padEnd(NAME_WIDTH))
    sb.append("  ")
    sb.append(qty.take(QTY_WIDTH))
    sb.append("\n")

    // Wrapped lines — NO LEFT PADDING
    for (i in 1 until lines.size) {
        sb.append(lines[i].take(NAME_WIDTH))
        sb.append("\n")
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
          list.add(DataForSendToPrinterPos58.selectAlignment(0))  // LEFT ALIGN

        list.add("Item                             Qty \n".toByteArray())
        list.add("------------------------------------------\n".toByteArray())

        items.forEach { list.add(formatItem80(it).toByteArray()) }

        list.add("------------------------------------------\n".toByteArray())
        otherCharges.forEach { (name, amount) -> list.add("$name: ${formatMoney(amount)}\n".toByteArray()) }
        list.add("TOTAL: ${formatMoney(orderTotal)}\n".toByteArray())

        repeat(3) { list.add(DataForSendToPrinterPos58.printAndFeedLine()) }
        list.add(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
        return list
    }

    private val NAME_WIDTH_MAIN_80 = 30
    private val PRICE_WIDTH_80 = 7

 private fun formatItem80(item: CartItemReceipt): String {
    val NAME_WIDTH = NAME_WIDTH_MAIN_80   // 30
    val QTY_WIDTH = 5

    val lines = wrapText(item.name.trimStart(), NAME_WIDTH)
    val qty = item.quantity.toString().padStart(QTY_WIDTH)

    val sb = StringBuilder()

    // First line
    sb.append(lines[0].take(NAME_WIDTH).padEnd(NAME_WIDTH))
    sb.append("  ")
    sb.append(qty)
    sb.append("\n")

   
    for (i in 1 until lines.size) {
        sb.append(lines[i].take(NAME_WIDTH))
        sb.append("\n")
    }

    return sb.toString()
}



    private fun formatMoney(value: Double): String = String.format(Locale.US, "%.2f", value)

   
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
        list.add("Item                      Qty\n".toByteArray())
        list.add(boldOff())

        list.add("--------------------------------\n".toByteArray())
        list.add(boldOn())
        items.forEach { list.add(formatItem58(it).toByteArray()) }
   list.add(boldOff())
        repeat(2) { list.add(DataForSendToPrinterPos58.printAndFeedLine()) }
        list.add(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
        return list
    }

    private val NAME_KOT_58 = 23

 private fun formatItem58(item: CartItemReceipt): String {
    val NAME_WIDTH = 20
    val QTY_WIDTH = 6

    val lines = wrapText(item.name.trim(), NAME_WIDTH)
    val q = item.quantity.toString().padStart(QTY_WIDTH)

    val sb = StringBuilder()

    // First line
    sb.append(lines[0].take(NAME_WIDTH).padEnd(NAME_WIDTH))
    sb.append("  ")
    sb.append(q.take(QTY_WIDTH))
    sb.append("\n")

    // Wrapped lines — NO LEFT SPACE
    for (i in 1 until lines.size) {
        sb.append(lines[i].take(NAME_WIDTH))  
        sb.append("\n")
    }

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
        list.add("Item                              Qty\n".toByteArray())
        list.add(boldOff())

        list.add("------------------------------------------\n".toByteArray())
        items.forEach { list.add(formatItem80(it).toByteArray()) }

        repeat(2) { list.add(DataForSendToPrinterPos58.printAndFeedLine()) }
        list.add(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
        return list
    }

    private val NAME_KOT_80 = 31

  private fun formatItem80(item: CartItemReceipt): String {
    val NAME_WIDTH = NAME_KOT_80   // 31
    val QTY_WIDTH = 4

    val lines = wrapText(item.name.trimStart(), NAME_WIDTH)
    val q = item.quantity.toString().padStart(QTY_WIDTH)

    val sb = StringBuilder()

    // First line
    sb.append(lines[0].take(NAME_WIDTH).padEnd(NAME_WIDTH))
    sb.append("  ")
    sb.append(q)
    sb.append("\n")

  
    for (i in 1 until lines.size) {
        sb.append(lines[i].take(NAME_WIDTH))
        sb.append("\n")
    }

    return sb.toString()
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
