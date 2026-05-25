package com.example.cpay_printer
import com.google.gson.reflect.TypeToken

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat.getSystemService
import com.example.cpay_printer.models.BluetoothPrinter
import com.example.cpay_printer.models.OfflineOrderLabel
import com.example.cpay_printer.models.PrintableReceipt
import com.google.gson.Gson
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import net.posprinter.posprinterface.IMyBinder
import net.posprinter.posprinterface.ProcessData
import net.posprinter.posprinterface.TaskCallback
import net.posprinter.service.PosprinterService
import net.posprinter.utils.BitmapProcess
import net.posprinter.utils.BitmapToByteData
import net.posprinter.utils.DataForSendToPrinterPos58
import net.posprinter.utils.DataForSendToPrinterTSC
import kotlin.math.log
import com.example.cpay_printer.models.KotPrintableReceiptV2
import com.example.cpay_printer.models.PrintableReceiptMain
import com.example.cpay_printer.models.KOTPrintableReceipt
import com.example.cpay_printer.models.CartItemReceipt



/** CpayPrinterPlugin */
class CpayPrinterPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel

  private var activity: Activity? = null
  private lateinit var context: Context
  private var bluetoothAdapter: BluetoothAdapter? = null
  private var bluetoothManager: BluetoothManager? = null

  private var bluetoothPrintBinder: IMyBinder? = null
  private var thermalPrinterDevices = mutableSetOf<BluetoothDevice>()
  private var connectedThermalPrinter: BluetoothDevice? = null
  private var bluetoothServiceConnection: ServiceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, service: IBinder) {
      bluetoothPrintBinder = service as IMyBinder
      logger("onServiceConnected(name: ComponentName, service: IBinder)")
    }

    override fun onServiceDisconnected(name: ComponentName) {
      logger("onServiceDisconnected(name: ComponentName)")
    }
  }
  private fun logger(text: Any) {
    Log.d("ThermalPrinter", "$text")
  }

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    logger("onAttachedToEngine")
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "cpay_printer")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
    //bind service，get imyBinder
    val intent: Intent = Intent(context, PosprinterService::class.java)
    context.bindService(intent, bluetoothServiceConnection, Context.BIND_AUTO_CREATE)
  }

  private fun initialise() {
    if (bluetoothAdapter != null) {
      return
    }
    logger("Initialising Bluetooth manager and adapter")
   bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

if (bluetoothAdapter == null) {
    Toast.makeText(context, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
    return
}

if (!bluetoothAdapter!!.isEnabled) {
    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    activity?.startActivityForResult(enableBtIntent, 1)
}


  }

  private fun getAllBluetoothPairedDevices(call: MethodCall, result: Result) {

    val adapter = BluetoothAdapter.getDefaultAdapter()

    if (adapter == null) {
        result.success(emptyList<Map<String, Any>>())
        return
    }

    if (!adapter.isEnabled) {
        result.error("BLUETOOTH_OFF", "Bluetooth is disabled", null)
        return
    }

    // Android 12+ permission ONLY
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            result.error("PERMISSION", "BLUETOOTH_CONNECT denied", null)
            return
        }
    }

    val bondedDevices = adapter.bondedDevices
    logger("Bonded devices count: ${bondedDevices.size}")

    val printers = mutableListOf<Map<String, Any>>()

   thermalPrinterDevices.clear()

for (device in bondedDevices) {
    logger("Found device: ${device.name} - ${device.address}")

    thermalPrinterDevices.add(device) // ⭐ REQUIRED

    printers.add(
        BluetoothPrinter(
            device.address,
            device.name ?: "Unknown"
        ).toJson()
    )
}


    result.success(printers)
}


  private fun connectToBluetoothPrinterByAddress(call: MethodCall, result: Result) {
    val address = call.argument<String>("bluetooth_printer_address")
    if (connectedThermalPrinter?.address == address) {
      result.success(true)
      return;
    }

    try {
      val selectedPrinter = thermalPrinterDevices.first { bluetoothDevice ->  bluetoothDevice.address == address }
      logger("Found printer by address $address, trying to connect")
      if (bluetoothAdapter != null && bluetoothAdapter!!.isDiscovering) {
        bluetoothAdapter!!.cancelDiscovery()
      }
      if (bluetoothPrintBinder == null) {
        logger("myBinder is null, connection failed")
      }
      bluetoothPrintBinder!!.ConnectBtPort(address, object : TaskCallback {
        override fun OnSucceed() {
          logger("Connection successful TaskCallback")
          connectedThermalPrinter = selectedPrinter
          result.success(true)
        }

        override fun OnFailed() {
          logger("Connection failed onFailed() TaskCallback")
          connectedThermalPrinter = null
          result.success(false)
        }
      })
    } catch (error: Error) {
      logger("Error connecting printer to address $address: $error")
      result.error(
        "NOT FOUND",
        "Unable to connect to the printer with $address",
        "Error occurred while connecting to the printer with address $address. Make sure printer is on, and paired with the device"
      )
      result.success(false)
    }

  }

  private fun isConnectedToBluetoothThermalPrinter(call: MethodCall, result: Result) {
    val address = call.argument<String>("bluetooth_printer_address")
    logger("Checking connection status with printer $address")
    if (connectedThermalPrinter == null) {
      return result.success(false)
    }
    result.success(connectedThermalPrinter?.address == address)
  }

  private fun disconnectBluetoothThermalPrinter(call: MethodCall, result: Result) {
    if (connectedThermalPrinter == null) {
      return
    }
    if (bluetoothPrintBinder == null) {
      logger("myBinder is null, disconnectBluetoothThermalPrinter()")
    }
    bluetoothPrintBinder!!.RemovePrinter(connectedThermalPrinter?.name, object : TaskCallback {
      override fun OnSucceed() {
        connectedThermalPrinter = null
        result.success(true)
      }
      override fun OnFailed() {
        result.success(false)
      }
    })
  }

  private fun printStringWithBluetoothPrinter(call: MethodCall, result: Result) {
    val s = call.argument<String>("printable_string") ?: return
    logger("called printStringWithBluetoothPrinter() with print ${connectedThermalPrinter?.address} with payload $s")
    if (connectedThermalPrinter != null) {
      bluetoothPrintBinder?.WriteSendData(object : TaskCallback {
        override fun OnSucceed() {
          logger("printStringWithBluetoothPrinter() successfully sent data for printing")
          result.success(true)
        }

        override fun OnFailed() {
          logger("printStringWithBluetoothPrinter() failed to send data for printing")
          result.success(false)
        }
      }, ProcessData {
        val list: MutableList<ByteArray> = java.util.ArrayList()
        list.add(DataForSendToPrinterPos58.initializePrinter())
        list.add(s.encodeToByteArray())
        list.add(DataForSendToPrinterPos58.printAndFeedLine())
        list
      })
    } else {
      result.error("NO PRINTER FOUND", "connect to printer before print", "Try to connect to printer before printing.")
    }
  }



private fun printReceiptV2WithBluetoothPrinter(call: MethodCall, result: Result) {

    val kotReceiptMap = call.argument<Map<String, Any>>("printReceiptV2")
    val kotEnabled = call.argument<Boolean>("kot_enabled") ?: false
    val paperWidth = call.argument<Double>("paper_width") ?: 58.0

    if (kotReceiptMap == null) {
        result.error("INVALID_ARGUMENT", "No receipt data provided", null)
        return
    }

    if (connectedThermalPrinter == null) {
        result.error("NO_PRINTER", "Connect to printer before printing", null)
        return
    }

    try {
        val gson = Gson()

        fun stringFromMap(map: Map<String, Any>, vararg keys: String): String {
            for (key in keys) {
                val v = map[key] ?: continue
                val s = when (v) {
                    is String -> v.trim()
                    else -> v.toString().trim()
                }
                if (s.isNotEmpty()) return s
            }
            return ""
        }

        val mainMap = kotReceiptMap["main"]
        val mainReceipt = if (mainMap != null) {
            gson.fromJson(
                gson.toJson(mainMap),
                PrintableReceiptMain::class.java
            )
        } else {
            PrintableReceiptMain()
        }

        // Order metadata at payload root (used for KOT when main receipt is disabled/empty)
        val orderId = stringFromMap(kotReceiptMap, "orderId", "order_id")
            .ifBlank { mainReceipt.orderId }
        val datetime = com.example.cpay_printer.models.PrinterUtils.formatReceiptDateTime(
            stringFromMap(kotReceiptMap, "datetime").ifBlank { mainReceipt.datetime }
        )
        val businessName = stringFromMap(kotReceiptMap, "businessName", "business_name")
            .ifBlank { mainReceipt.businessName }
        val customerNoteTop = stringFromMap(kotReceiptMap, "customerNote", "customer_note")
        val customerNote: String? = when {
            customerNoteTop.isNotEmpty() -> customerNoteTop
            !mainReceipt.customerNote.isNullOrBlank() -> mainReceipt.customerNote
            else -> null
        }

        val rawKotSections = kotReceiptMap["kotSections"] as? Map<*, *> ?: emptyMap<String, Any>()
        val safeKotSections = mutableMapOf<String, List<CartItemReceipt>>()

        for ((key, value) in rawKotSections) {
            val items: List<CartItemReceipt> = gson.fromJson(
                gson.toJson(value),
                Array<CartItemReceipt>::class.java
            ).toList()

            safeKotSections[key.toString()] = items
        }

        Log.d("CpayPrinter", "KOT orderId=$orderId sections=${safeKotSections.size}")

        val kotReceipt = KotPrintableReceiptV2(
            orderId = orderId,
            datetime = datetime,
            businessName = businessName,
            customerNote = customerNote,
            main = mainReceipt,
            kotSections = safeKotSections
        )

        bluetoothPrintBinder?.WriteSendData(object : TaskCallback {
            override fun OnSucceed() {
                result.success(true)
            }

            override fun OnFailed() {
                result.error("PRINT_FAILED", "Failed to print", null)
            }
        }, ProcessData {

            val list = mutableListOf<ByteArray>()

          // ---------- MAIN RECEIPT ----------
val mainReceiptBytes =
    if (paperWidth == 80.0) {
        kotReceipt.generateMainReceipt80()
    } else {
        kotReceipt.generateMainReceipt58()
    }

// Print ONLY if main receipt exists and has content
if (!mainReceiptBytes.isNullOrEmpty()) {
    list.addAll(mainReceiptBytes)
}

            for ((category, items) in kotReceipt.kotSections) {

                val kot = KOTPrintableReceipt(
                    orderId = kotReceipt.orderId,
datetime = kotReceipt.datetime,
businessName = kotReceipt.businessName,
customerNote = kotReceipt.customerNote,
                    items = items,
                    categoryName = category
                )

                if (paperWidth == 80.0) {
                    list.addAll(kot.generateKOT80())
                } else {
                    list.addAll(kot.generateKOT58())
                }

                repeat(2) { list.add(DataForSendToPrinterPos58.printAndFeedLine()) }
            }

            list
        })

    } catch (e: Exception) {
        e.printStackTrace()
        result.error("PRINT_FAILED", e.message, null)
    }
}








  private fun printReceiptWithBluetoothPrinter(call: MethodCall, result: Result) {
    val printableReceiptMap = call.argument<Map<String, Any>>("printable_receipt")
    val qrCodeText = call.argument<String?>("qr_code_text")
    val paperWidth = call.argument<Double>("paper_width")
    val gson = Gson()
    val printableReceipt = gson.fromJson(gson.toJson(printableReceiptMap), PrintableReceipt::class.java)
    logger("printReceiptWithBluetoothPrinter() with $printableReceiptMap, $qrCodeText with $connectedThermalPrinter")
    if (connectedThermalPrinter == null) {
      result.error("NO PRINTER FOUND", "connect to printer before print", "Try to connect to printer before printing.")
      return
    }
    bluetoothPrintBinder?.WriteSendData(object : TaskCallback {
      override fun OnSucceed() {
        logger("printReceiptWithBluetoothPrinter() successfully sent data for printing")
        result.success(true)
      }

      override fun OnFailed() {
        logger("printReceiptWithBluetoothPrinter() failed to send data for printing")
        result.error("Failed to print receipt", "Unknown", "Unknown")
      }
    }, ProcessData {
      if (paperWidth == 58.0) {
        printableReceipt.generatePrintableByteArrayForPaperWidth58(qrCodeText)
      } else {
        printableReceipt.generatePrintableByteArrayForPaperWidth80(qrCodeText)
      }
    })
  }
  fun bitmapFromArray(pixels2d: Array<IntArray>): Bitmap? {
    val width = pixels2d.size
    val height = pixels2d[0].size
    val pixels = IntArray(width * height)
    var pixelsIndex = 0
    for (i in 0 until width) {
      for (j in 0 until height) {
        pixels[pixelsIndex] = pixels2d[i][j]
        pixelsIndex++
      }
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
  }

  fun drawableToBitmap(drawable: Drawable): Bitmap? {
    if (drawable is BitmapDrawable) {
      return drawable.bitmap
    }

    // Create a Bitmap of the same size as the Drawable
    val width = drawable.intrinsicWidth
    val height = drawable.intrinsicHeight
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)

    // Create a Canvas to draw the Drawable onto the Bitmap
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)

    return bitmap
  }
  private fun printOfflineOrderLabel(call: MethodCall, result: Result) {
    val offlineOrderLabelMap = call.argument<Map<String, Any>>("offline_order_label")
    val gson = Gson()
    val offlineOrderLabel = gson.fromJson(gson.toJson(offlineOrderLabelMap), OfflineOrderLabel::class.java)
    logger(offlineOrderLabelMap!!)
    if (connectedThermalPrinter == null) {
      result.error("NO PRINTER FOUND", "connect to printer before print", "Try to connect to printer before printing.")
      return
    }
    bluetoothPrintBinder!!.WriteSendData(object : TaskCallback {
      override fun OnSucceed() {
      }

      override fun OnFailed() {
      }
    }, ProcessData {
//       width = 4.0 inch, height = 2.0 inch
      val width = 2.54 * 10 * 4.0
      val height = 2.54 * 10 * 2.0
      // padding of 50mm from all sides
      val padding = 50

      val list: MutableList<ByteArray> = ArrayList()
      val packageManager: PackageManager = context.packageManager
      val applicationInfo = packageManager.getApplicationInfo(context.packageName, 0)
      val bitmap1 = BitmapProcess.compressBmpByYourWidth(
        BitmapFactory.decodeResource(
          context.resources,
          R.drawable.splash,
        ), 150
      )

      val logo = BitmapProcess.compressBmpByYourWidth(
        BitmapFactory.decodeResource(
          context.resources,
          R.drawable.app_icon,
        ), 150
      )

      list.add(DataForSendToPrinterTSC.sizeBymm(width, height))
      list.add(DataForSendToPrinterTSC.direction(0))
      list.add(DataForSendToPrinterTSC.cls())
      list.add(DataForSendToPrinterTSC.bitmap(550, 40, 0, bitmap1, BitmapToByteData.BmpType.Threshold))
      list.add(DataForSendToPrinterTSC.qrCode(110, 100, "M", 4, "A", 0, "M1", "S3", offlineOrderLabel.qrCodeText))
      list.add(DataForSendToPrinterTSC.text(150, 25, "3", 0, 1, 1, offlineOrderLabel.businessName))
      list.add(DataForSendToPrinterTSC.text(400, 100, "2", 0, 1, 1, offlineOrderLabel.customerName))
      list.add(DataForSendToPrinterTSC.text(400, 160, "2", 0, 1, 1, "Mob: " + offlineOrderLabel.customerPhone))
      list.add(DataForSendToPrinterTSC.text(400, 220, "2", 0, 1, 1, "Credit Issued: " + offlineOrderLabel.creditIssued))
      list.add(DataForSendToPrinterTSC.text(400, 280, "2", 0, 1, 1, "Issued on: " + offlineOrderLabel.issuedOn))
      list.add(DataForSendToPrinterTSC.text(400, 340, "2", 0, 1, 1, "valid till: " + offlineOrderLabel.validTill))

      list.add(DataForSendToPrinterTSC.print(1, 1))
      list
    })
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    when (call.method) {
      "initialise" -> initialise()
      "getAllBluetoothPairedDevices" -> getAllBluetoothPairedDevices(call, result)
      "connectToBluetoothPrinterByAddress" -> connectToBluetoothPrinterByAddress(call, result)
      "isConnectedToBluetoothThermalPrinter" -> isConnectedToBluetoothThermalPrinter(call, result)
      "disconnectBluetoothThermalPrinter" -> disconnectBluetoothThermalPrinter(call, result)
      "printStringWithBluetoothPrinter" -> printStringWithBluetoothPrinter(call, result)
      "printReceiptWithBluetoothPrinter" -> printReceiptWithBluetoothPrinter(call, result)
      "printOfflineOrderLabel" -> printOfflineOrderLabel(call, result)
      "printReceiptV2" -> printReceiptV2WithBluetoothPrinter(call, result)  // <--- ADD THIS

      else -> result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
  }

  override fun onDetachedFromActivityForConfigChanges() {
    activity = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activity = binding.activity
  }

  override fun onDetachedFromActivity() {
    activity = null
  }
}
