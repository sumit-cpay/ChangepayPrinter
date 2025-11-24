package com.example.cpay_printer.models

import net.posprinter.utils.DataForSendToPrinterPos58

// =========================
// Cart Item for Receipt
// =========================
data class CartItemReceipt(
    val name: String,
    val price: Double,
    val quantity: Int,
    val total: Double,
    val category: String? = null,
)

// =========================
// MAIN RECEIPT
// =========================
data class PrintableReceiptMain(
    val orderId: String,
    val datetime: String,
    val businessName: String,
    val customerPhone: String,
    val customerName: String,
    val deliveryType: String,
    val address: String,
    val customerNote: String?,
    val items: List<CartItemReceipt>,
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

        list.add("Item               Qty   Price   Total\n".toByteArray())
        list.add("--------------------------------\n".toByteArray())

        items.forEach { item ->
            list.add(formatItem(item).toByteArray())
        }

        list.add("--------------------------------\n".toByteArray())
        otherCharges.forEach {
            list.add("${it.first}: ${it.second}\n".toByteArray())
        }

        list.add("Discount: $discount\n".toByteArray())
        list.add("TOTAL: $orderTotal\n".toByteArray())
        list.add("\n\n\n".toByteArray())

        return list
    }

    private fun formatItem(item: CartItemReceipt): String {
        val name = item.name.padEnd(18, ' ')
        val qty = item.quantity.toString().padEnd(4, ' ')
        val price = item.price.toString().padEnd(7, ' ')
        val total = item.total.toString()
        return "$name $qty $price $total\n"
    }
}

// =========================
// KOT RECEIPT
// =========================
data class KOTPrintableReceipt(
    val orderId: String,
    val datetime: String,
    val businessName: String,
    val customerNote: String?,
    val items: List<CartItemReceipt>,
    val categoryName: String
) {

    fun generateKOT58(halfCut: Boolean): MutableList<ByteArray> {
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

        list.add("Item                 Qty\n".toByteArray())
        list.add("--------------------------------\n".toByteArray())

        items.forEach { item ->
            list.add(formatItem(item).toByteArray())
        }

        list.add("--------------------------------\n".toByteArray())

        // HALF CUT OPTION
        if (halfCut) {
           list.add(DataForSendToPrinterPos58.printAndFeedLine(5))
list.add(DataForSendToPrinterPos58.selectCutPagerModeAndCutPager(66, 1))

        } else {
            list.add("\n\n\n".toByteArray())
        }

        return list
    }

    private fun formatItem(item: CartItemReceipt): String {
        val name = item.name.padEnd(20, ' ')
        val qty = item.quantity.toString()
        return "$name $qty\n"
    }
}

// =========================
// Combines Main + All KOTs
// =========================
data class KotPrintableReceiptV2(
    val main: PrintableReceiptMain,
    val kotSections: Map<String, List<CartItemReceipt>>
) {

    fun generateAllKOTs58(halfCut: Boolean): List<MutableList<ByteArray>> {
        val list = mutableListOf<MutableList<ByteArray>>()

        kotSections.forEach { (category, items) ->
            val kot = KOTPrintableReceipt(
                orderId = main.orderId,
                datetime = main.datetime,
                businessName = main.businessName,
                customerNote = main.customerNote,
                items = items,
                categoryName = category
            )

            list.add(kot.generateKOT58(halfCut))
        }

        return list
    }

    fun generateMainReceipt58(): MutableList<ByteArray> {
        return main.generateMainReceipt58()
    }
}
