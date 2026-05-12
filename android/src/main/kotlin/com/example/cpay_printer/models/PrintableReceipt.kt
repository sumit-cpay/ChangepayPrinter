package com.example.cpay_printer.models

import com.google.gson.annotations.SerializedName
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.ByteMatrix
import com.google.zxing.qrcode.encoder.Encoder
import com.google.zxing.qrcode.encoder.QRCode
import net.posprinter.utils.DataForSendToPrinterPos58
import net.posprinter.utils.DataForSendToPrinterPos80
import java.lang.Integer.min
import java.util.EnumMap
import kotlin.math.ceil
import kotlin.math.roundToInt




class PrintableReceipt(
    @SerializedName("address")
    val address: String?,
    @SerializedName("datetime")
    val datetime: String,
    @SerializedName("delivery_type")
    val deliveryType: String,
    @SerializedName("items")
    val items: List<CartItem>,
    @SerializedName("order_id")
    val orderId: String,
    @SerializedName("daily_token_number")
val dailyTokenNumber: String?,
  @SerializedName("table_number")
    val tableNumber: Int?, 
    @SerializedName("order_total")
    val orderTotal: Double,
    @SerializedName("other_charges")
    val otherCharges: List<OtherCharge>,
    @SerializedName("printer_id")
    val printerId: String,
    @SerializedName("business_name")
    val businessName: String,
    @SerializedName("customer_phone")
    val customerPhone: String,
    @SerializedName("customer_name")
    val customerName: String,
    @SerializedName("customer_note")
    val customerNote: String?,
    ) {



         // Helper to wrap long item names
    // -----------------------------
    fun wrapText(text: String, width: Int): List<String> {
        if (text.isEmpty()) return listOf("")
        val words = text.split(" ")
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

    public fun generatePrintableByteArrayForPaperWidth58(qrCodeText: String? = null): MutableList<ByteArray> {
        val list: MutableList<ByteArray> = java.util.ArrayList()
        list.add(DataForSendToPrinterPos58.initializePrinter())
        list.add(DataForSendToPrinterPos58.selectAlignment(1))
      list.add(DataForSendToPrinterPos58.selectCharacterSize(18))

val displayOrderNo =
    if (!dailyTokenNumber.isNullOrEmpty())
        "$orderId-$dailyTokenNumber"
    else
        orderId

list.add(displayOrderNo.encodeToByteArray())
list.add(DataForSendToPrinterPos58.printAndFeedLine())


list.add(byteArrayOf(0x1B, 0x21, 0x00))
        list.add(datetime.encodeToByteArray())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())
list.add(DataForSendToPrinterPos58.selectCharacterSize(1))

list.add(byteArrayOf(0x1B, 0x21, 0x00))
        list.add(businessName.encodeToByteArray())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())
list.add(DataForSendToPrinterPos58.selectCharacterSize(1))



val phone = if (!customerPhone.isNullOrEmpty()) {
    if (customerPhone.startsWith("+91") && customerPhone.length > 3)
        "+91-" + customerPhone.substring(3)
    else if (customerPhone.length == 10)
        "+91-$customerPhone"
    else customerPhone
} else ""
if (deliveryType != "DINE IN") {
    list.add(DataForSendToPrinterPos58.selectAlignment(1))    
    list.add(DataForSendToPrinterPos58.selectCharacterSize(18))     
    list.add(byteArrayOf(0x1B, 0x45, 0x01))
    list.add("$phone\n".encodeToByteArray())
    list.add(byteArrayOf(0x1B, 0x45, 0x00))
    list.add(DataForSendToPrinterPos58.selectCharacterSize(0))    
}  


// --- Customer Name (Same as your first receipt class) ---
list.add(DataForSendToPrinterPos58.selectCharacterSize(18))   
list.add(DataForSendToPrinterPos58.selectAlignment(1))       
list.add("$customerName\n".encodeToByteArray())
list.add(DataForSendToPrinterPos58.selectCharacterSize(0))   
if (tableNumber != null && tableNumber > 0) {
    list.add(DataForSendToPrinterPos58.selectAlignment(1))
    list.add(DataForSendToPrinterPos58.selectCharacterSize(18))
    list.add("Table No: $tableNumber\n".encodeToByteArray())
    list.add(DataForSendToPrinterPos58.selectCharacterSize(0))
}



        list.add(DataForSendToPrinterPos58.initializePrinter())
        list.add(DataForSendToPrinterPos58.selectCharacterSize(1))



        if (      qrCodeText != null &&
    deliveryType != "DINE IN" &&
    deliveryType != "SELF PICK UP" &&
    deliveryType != "BILL_PAYMENT") {
        list.add("--------------------------------".encodeToByteArray())

            list.add(DataForSendToPrinterPos58.initializePrinter())
            list.add(DataForSendToPrinterPos58.printAndFeedLine())
            list.add(DataForSendToPrinterPos58.selectAlignment(1))
            list.add(qrCodeDataToByteArray(qrCodeText, 250)!!)
            list.add(DataForSendToPrinterPos58.printAndFeedLine())
        list.add("--------------------------------".encodeToByteArray())

        }
       list.add(DataForSendToPrinterPos58.selectCharacterSize(0))

        list.add(DataForSendToPrinterPos58.printAndFeedLine())
      //  list.add("Items      Qty    Price   Total  ".encodeToByteArray())
      list.add("Items               Qty    Total".encodeToByteArray())
        list.add("--------------------------------".encodeToByteArray())

        list.add(DataForSendToPrinterPos58.initializePrinter())

       for (item in items) {
    list.add(byteArrayOf(0x1B, 0x45, 0x01))

    
    list.add(addOrderItemToPrintableString(item).encodeToByteArray())
        list.add(byteArrayOf(0x1B, 0x45, 0x00))


    
    item.addons?.forEach { addon ->
        val addonText =
            if (addon.price != null && addon.price > 0)
                "   + ${addon.name} (${addon.price})\n"
            else
                "   + ${addon.name}\n"

        list.add(addonText.encodeToByteArray())
    }

   
    list.add("--------------------------------\n".encodeToByteArray())
}

        list.add(DataForSendToPrinterPos58.initializePrinter())
        list.add(DataForSendToPrinterPos58.selectAlignment(2))
        list.add("\n".encodeToByteArray())

        if (customerNote != null) {
            list.add(DataForSendToPrinterPos58.initializePrinter())
            list.add(DataForSendToPrinterPos58.selectAlignment(2))
            list.add("Note: $customerNote".encodeToByteArray())
            list.add("\n".encodeToByteArray())
            list.add("--------------------------------".encodeToByteArray())
            list.add("\n".encodeToByteArray())
        }


val filteredCharges = otherCharges.filter {
    !it.name.equals("EXTRA", ignoreCase = true)
}

val extraTotal = otherCharges
    .filter { it.name.equals("EXTRA", ignoreCase = true) }
    .sumOf { it.value }

val finalTotal = orderTotal - extraTotal


for (charge in filteredCharges) {
    list.add(DataForSendToPrinterPos58.selectAlignment(2))
    list.add("${charge.name} ${charge.value}\n".encodeToByteArray())
}

        list.add("--------------------------------".encodeToByteArray())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())
        list.add("Rs. ${"%.2f".format(finalTotal)}".encodeToByteArray())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())
        list.add("--------------------------------".encodeToByteArray())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())
        list.add(DataForSendToPrinterPos58.selectOrCancelBoldModel(1))
        list.add(deliveryType.encodeToByteArray())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())
        list.add("-------------------------------".encodeToByteArray())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())

        if (address != null) {
            list.add(DataForSendToPrinterPos58.initializePrinter())
            list.add(DataForSendToPrinterPos58.selectCharacterSize(2))
              val addressLines = wrapText(address, 32)

    for (line in addressLines) {
        list.add((line + "\n").encodeToByteArray())
    }
            list.add(DataForSendToPrinterPos58.printAndFeedLine())
        }

        list.add(DataForSendToPrinterPos58.printAndFeedLine())


        val data = byteArrayOf(27, 109)
        list.add(data)
        return list
    }

public fun generatePrintableByteArrayForPaperWidth80(qrCodeText: String? = null): MutableList<ByteArray> {
    val list: MutableList<ByteArray> = java.util.ArrayList()
    
    // Header
    
    list.add(DataForSendToPrinterPos80.selectAlignment(1))
   list.add(DataForSendToPrinterPos80.selectCharacterSize(18))

val displayOrderNo =
    if (!dailyTokenNumber.isNullOrEmpty())
        "$orderId-$dailyTokenNumber"
    else
        orderId

list.add(displayOrderNo.encodeToByteArray())
list.add(DataForSendToPrinterPos80.printAndFeedLine())


    list.add(DataForSendToPrinterPos80.selectCharacterSize(1))
    list.add(datetime.encodeToByteArray())
    list.add(DataForSendToPrinterPos80.printAndFeedLine())

    list.add(DataForSendToPrinterPos80.selectCharacterSize(1))
    list.add(businessName.encodeToByteArray())
    list.add(DataForSendToPrinterPos80.printAndFeedLine())

val phone = if (!customerPhone.isNullOrEmpty()) {
    if (customerPhone.startsWith("+91") && customerPhone.length > 3)
        "+91-" + customerPhone.substring(3)
    else if (customerPhone.length == 10)
        "+91-$customerPhone"
    else customerPhone
} else ""
if (deliveryType != "DINE IN") {
list.add(DataForSendToPrinterPos80.selectCharacterSize(18))  
list.add("$phone\n".encodeToByteArray())
list.add(DataForSendToPrinterPos80.selectCharacterSize(0))   
}
 // --- Customer Name (Same look as 58mm version) ---
list.add(DataForSendToPrinterPos80.selectCharacterSize(18))  
list.add(DataForSendToPrinterPos80.selectAlignment(1))        
list.add(" $customerName\n".encodeToByteArray())
list.add(DataForSendToPrinterPos80.selectCharacterSize(0))  
if (tableNumber != null && tableNumber > 0) {
    list.add(DataForSendToPrinterPos58.selectAlignment(1))
    list.add(DataForSendToPrinterPos58.selectCharacterSize(18))
    list.add("Table No: $tableNumber\n".encodeToByteArray())
    list.add(DataForSendToPrinterPos58.selectCharacterSize(0))
} 


    list.add(DataForSendToPrinterPos80.initializePrinter())
    list.add(DataForSendToPrinterPos80.selectCharacterSize(0))
            list.add(DataForSendToPrinterPos80.printAndFeedLine())



    // QR Code
    if (   qrCodeText != null &&
    deliveryType != "DINE IN" &&
    deliveryType != "SELF PICK UP" &&
    deliveryType != "BILL_PAYMENT" ) {
        list.add(DataForSendToPrinterPos80.initializePrinter())
        list.add(DataForSendToPrinterPos80.selectAlignment(1))
        list.add(qrCodeDataToByteArray(qrCodeText, 250)!!)
        list.add(DataForSendToPrinterPos80.printAndFeedLine())
    }

    list.add("------------------------------------------------".encodeToByteArray())
    list.add(DataForSendToPrinterPos80.printAndFeedLine())

    // -----------------------------
    // Items Table Header
    // -----------------------------
    val ITEM_NAME_WIDTH = 24
    val QTY_WIDTH = 4
    val PRICE_WIDTH = 8
    val TOTAL_WIDTH = 8

    val headerLine = "Items".padEnd(ITEM_NAME_WIDTH, ' ') +
            "Qty".padStart(QTY_WIDTH, ' ') +
            "Price".padStart(PRICE_WIDTH, ' ') +
            "Total".padStart(TOTAL_WIDTH, ' ') + "\n"
    list.add(headerLine.encodeToByteArray())
    list.add("------------------------------------------------".encodeToByteArray())

    // -----------------------------
   

    // -----------------------------
    // Add items (responsive)
    // -----------------------------
  for (item in items) {

    val nameLines = wrapText(item.name.trimStart(), ITEM_NAME_WIDTH)

    val qtyStr   = item.quantity.toString().padStart(QTY_WIDTH)
    val priceStr = "%.2f".format(item.price).padStart(PRICE_WIDTH)
    val totalStr = "%.2f".format(item.total).padStart(TOTAL_WIDTH)

    
    val firstLine =
        nameLines[0].take(ITEM_NAME_WIDTH).padEnd(ITEM_NAME_WIDTH) +
        qtyStr +
        priceStr +
        totalStr +
        "\n"
list.add(byteArrayOf(0x1B, 0x45, 0x01))

    list.add(firstLine.encodeToByteArray())
list.add(byteArrayOf(0x1B, 0x45, 0x00))
   
    for (i in 1 until nameLines.size) {
        val wrapLine =
            nameLines[i].take(ITEM_NAME_WIDTH).padEnd(ITEM_NAME_WIDTH) +
            " ".repeat(QTY_WIDTH + PRICE_WIDTH + TOTAL_WIDTH) +
            "\n"

        list.add(wrapLine.encodeToByteArray())
    }
  item.addons?.forEach { addon ->

    val addonText =
        if (addon.price != null && addon.price > 0)
            "+ ${addon.name} (${addon.price})"
        else
            "+ ${addon.name}"

    val safeAddon = addonText.take(ITEM_NAME_WIDTH)

    val line =
        safeAddon.padEnd(ITEM_NAME_WIDTH) + // ONLY NAME COLUMN FILLED
        " ".repeat(QTY_WIDTH + PRICE_WIDTH + TOTAL_WIDTH) + "\n"

    list.add(line.encodeToByteArray())
}
 list.add("------------------------------------------------".encodeToByteArray())

}



    list.add(DataForSendToPrinterPos80.initializePrinter())
 list.add(DataForSendToPrinterPos80.printAndFeedLine())


    // -----------------------------
    // Customer Note
    // -----------------------------
    if (customerNote != null) {
        list.add(DataForSendToPrinterPos80.initializePrinter())
        list.add(DataForSendToPrinterPos80.selectAlignment(2))
        list.add("Note: $customerNote".encodeToByteArray())
        list.add("\n".encodeToByteArray())
        list.add("--------------------------------".encodeToByteArray())
        list.add("\n".encodeToByteArray())
    }

    // -----------------------------
    // Other Charges
    // -----------------------------
val filteredCharges = otherCharges.filter {
    !it.name.equals("EXTRA", ignoreCase = true)
}

val extraTotal = otherCharges
    .filter { it.name.equals("EXTRA", ignoreCase = true) }
    .sumOf { it.value }

val finalTotal = orderTotal - extraTotal


for (charge in filteredCharges) {
    list.add(DataForSendToPrinterPos80.selectAlignment(2))
    list.add("${charge.name} ${charge.value}\n".encodeToByteArray())
    list.add("------------------------------------------------".encodeToByteArray())
}


    list.add(DataForSendToPrinterPos80.printAndFeedLine())
    list.add(DataForSendToPrinterPos80.selectAlignment(2))
    list.add("Rs. ${"%.2f".format(finalTotal)}".encodeToByteArray())
    list.add(DataForSendToPrinterPos80.printAndFeedLine())
    list.add("--------------------------------".encodeToByteArray())
    list.add(DataForSendToPrinterPos80.printAndFeedLine())
    list.add(DataForSendToPrinterPos80.selectAlignment(2))
    list.add(DataForSendToPrinterPos80.selectOrCancelBoldModel(1))
    list.add(deliveryType.encodeToByteArray())
    list.add(DataForSendToPrinterPos80.printAndFeedLine())
    list.add("--------------------------------".encodeToByteArray())
    list.add(DataForSendToPrinterPos80.printAndFeedLine())

    // -----------------------------
    // Address
    // -----------------------------
    if (address != null) {
        list.add(DataForSendToPrinterPos80.initializePrinter())
        list.add(DataForSendToPrinterPos80.selectCharacterSize(2))
val addressLines = wrapText(address, 48)

    for (line in addressLines) {
        list.add((line + "\n").encodeToByteArray())
    }
        }

 


    list.add(byteArrayOf(0x1D, 0x56, 0x42, 0x00))

    return list
}

    private fun qrCodeDataToByteArray(data: String?, size: Int): ByteArray? {
        var byteMatrix: ByteMatrix? = null
        try {
            val hints = EnumMap<EncodeHintType, Any>(
                EncodeHintType::class.java
            )
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            val code: QRCode = Encoder.encode(data, ErrorCorrectionLevel.L, hints)
            byteMatrix = code.matrix
        } catch (e: WriterException) {
            e.printStackTrace()
            return null
        }
        if (byteMatrix == null) {
            return null
        }
        val width = byteMatrix.width
        val height = byteMatrix.height
        val coefficient = (size.toFloat() / width.toFloat()).roundToInt()
        val imageWidth = width * coefficient
        val imageHeight = height * coefficient
        val bytesByLine = ceil((imageWidth.toFloat() / 8f).toDouble()).toInt()
        var i = 8
        if (coefficient < 1) {
            return initGSv0Command(0, 0)
        }
        val imageBytes = initGSv0Command(bytesByLine, imageHeight)
        for (y in 0 until height) {
            val lineBytes = ByteArray(bytesByLine)
            var x = -1
            var multipleX = coefficient
            var isBlack = false
            for (j in 0 until bytesByLine) {
                var b = 0
                for (k in 0..7) {
                    if (multipleX == coefficient) {
                        isBlack = ++x < width && byteMatrix[x, y].toInt() == 1
                        multipleX = 0
                    }
                    if (isBlack) {
                        b = b or (1 shl 7 - k)
                    }
                    ++multipleX
                }
                lineBytes[j] = b.toByte()
            }
            for (multipleY in 0 until coefficient) {
                if (imageBytes != null) {
                    System.arraycopy(lineBytes, 0, imageBytes, i, lineBytes.size)
                }
                i += lineBytes.size
            }
        }
        return imageBytes
    }

    private fun initGSv0Command(bytesByLine: Int, bitmapHeight: Int): ByteArray? {
        val xH = bytesByLine / 256
        val xL = bytesByLine - xH * 256
        val yH = bitmapHeight / 256
        val yL = bitmapHeight - yH * 256
        val imageBytes = ByteArray(8 + bytesByLine * bitmapHeight)
        imageBytes[0] = 0x1D
        imageBytes[1] = 0x76
        imageBytes[2] = 0x30
        imageBytes[3] = 0x00
        imageBytes[4] = xL.toByte()
        imageBytes[5] = xH.toByte()
        imageBytes[6] = yL.toByte()
        imageBytes[7] = yH.toByte()
        return imageBytes
    }
 fun addOrderItemToPrintableString(orderItem: CartItem): String {
    val ITEM_NAME_WIDTH = 20
    val ITEM_QTY_WIDTH = 5
   // val ITEM_PRICE_WIDTH = 7
    val ITEM_TOTAL_WIDTH = 7

    val startIndexed = mutableListOf(0, 0, 0)
    var printableOrderItemString = ""

    while (true) {
        if (startIndexed[0] == orderItem.name.length &&
            startIndexed[1] == orderItem.quantity.toString().length &&
        //    startIndexed[2] == orderItem.price.toString().length &&
            startIndexed[2] == orderItem.total.toString().length
        ) break

        // ---- NAME ----
        val endIndex1 = min(startIndexed[0] + ITEM_NAME_WIDTH - 1, orderItem.name.length)
        val name = orderItem.name.substring(startIndexed[0], endIndex1)
        startIndexed[0] = endIndex1
        printableOrderItemString += name + " ".repeat(ITEM_NAME_WIDTH - name.length)

        // ---- QTY ----
        val endIndex2 = min(startIndexed[1] + ITEM_QTY_WIDTH - 1, orderItem.quantity.toString().length)
        val quantity = orderItem.quantity.toString().substring(startIndexed[1], endIndex2)
        startIndexed[1] = endIndex2
        printableOrderItemString += quantity + " ".repeat(ITEM_QTY_WIDTH - quantity.length)

        // ---- PRICE ----
      //  val endIndex3 = min(startIndexed[2] + ITEM_PRICE_WIDTH - 1, orderItem.price.toString().length)
      //  val price = orderItem.price.toString().substring(startIndexed[2], endIndex3)
      //  startIndexed[2] = endIndex3
     //   printableOrderItemString += price + " ".repeat(ITEM_PRICE_WIDTH - price.length)

        // ---- TOTAL (CENTER aligned) ----
     //   val endIndex4 = min(startIndexed[3] + ITEM_TOTAL_WIDTH - 1, orderItem.total.toString().length)
     //   val total = orderItem.total.toString().substring(startIndexed[3], endIndex4)
      //  startIndexed[3] = endIndex4

      val endIndex3 = min(startIndexed[2] + ITEM_TOTAL_WIDTH - 1, orderItem.total.toString().length)
val total = orderItem.total.toString().substring(startIndexed[2], endIndex3)
startIndexed[2] = endIndex3

        val spaceLeft = (ITEM_TOTAL_WIDTH - total.length) / 2
        val spaceRight = ITEM_TOTAL_WIDTH - total.length - spaceLeft

        printableOrderItemString += " ".repeat(spaceLeft) + total + " ".repeat(spaceRight)

        printableOrderItemString += '\n'
    }

    return printableOrderItemString
}

}

data class CartItem(
    @SerializedName("name")
    val name: String,
    @SerializedName("price")
    val price: Double,
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("total")
    val total: Double,
    @SerializedName("addons")
    val addons: List<Addon>? = emptyList()
)

data class OtherCharge(
    @SerializedName("name")
    val name: String,
    @SerializedName("value")
    val value: Double,
)


data class Addon(
    @SerializedName("name")
    val name: String,

    @SerializedName("price")
    val price: Double?
)