import 'dart:developer';
import 'package:cpay_printer/models/offline_order_label.dart';
import 'package:cpay_printer/models/printable_receipt.dart';
import 'package:flutter/services.dart';

import 'kot_receipt.dart';

class BluetoothPrinter {
  final String printerAddress;
  final String printerName;
  BluetoothPrinter({
    required this.printerAddress,
    required this.printerName,
  });
  static const MethodChannel _channel =
      MethodChannel('cpay_printer');

  static const paperWidth58 = 58.0;
  static const paperWidth80 = 80.0;

  factory BluetoothPrinter.fromJson(Map<String, dynamic> json) {
    return BluetoothPrinter(
      printerAddress: json['printer_address'],
      printerName: json['printer_name'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      "printer_id": printerAddress,
      "printer_name": printerName,
    };
  }

  Future<void> connect() async {
    try {
      await _channel.invokeMethod("connectToBluetoothPrinterByAddress",
          {"bluetooth_printer_address": printerAddress});
    } catch (e) {
      log(e.toString());
    }
  }

  Future<bool> printString(String printableString) async {
    try {
      await _channel.invokeMethod("printStringWithBluetoothPrinter",
          {"printable_string": printableString});
      return true;
    } catch (e) {
      log(e.toString());
      return false;
    }
  }

  Future<bool> printReceipt(
    PrintableReceipt receipt, {
    String? qrCodeText,
    double paperWidth = BluetoothPrinter.paperWidth58,
  }) async {
    try {
      final result = await _channel.invokeMethod<bool>(
        "printReceiptWithBluetoothPrinter",
        {
          "printable_receipt": receipt.toJson(),
          "qr_code_text": qrCodeText,
          "paper_width": paperWidth,
        },
      );
      return result ?? false;
    } catch (e) {
      rethrow;
    }
  }
  
 
  Future<bool> printReceiptV2(
  KotPrintableReceiptV2 kotReceiptV2, {
  String? qrCodeText,
  double paperWidth = BluetoothPrinter.paperWidth58,
  required bool kotEnabled,
}) async {
  try {
    // --- 1. Convert main receipt ---
    final Map<String, dynamic> mainMap = {
      'orderId': kotReceiptV2.main.orderId,
      'datetime': kotReceiptV2.main.datetime,
      'businessName': kotReceiptV2.main.businessName,
      'customerPhone': kotReceiptV2.main.customerPhone,
      'customerName': kotReceiptV2.main.customerName,
      'deliveryType': kotReceiptV2.main.deliveryType,
      'address': kotReceiptV2.main.address,
      'customerNote': kotReceiptV2.main.customerNote,
      'items': kotReceiptV2.main.items.map((item) {
        return {
          'name': item.name,
          'quantity': item.quantity,
          'price': item.price,
          'total': item.total,
          'category': item.category,
        };
      }).toList(),
      'otherCharges': kotReceiptV2.main.otherCharges,
      'discount': kotReceiptV2.main.otherCharges.fold<double>(
          0.0, (sum, e) => sum + (e['discount'] ?? 0.0)),
      'orderTotal': kotReceiptV2.main.orderTotal,
    };

    // --- 2. Convert KOT sections ---
    final Map<String, dynamic> kotSectionsMap = {};
    kotReceiptV2.kotSections.forEach((key, items) {
      kotSectionsMap[key] = items.map((item) {
        return {
          'name': item.name,
          'quantity': item.quantity,
          'price': item.price,
          'total': item.total,
          'category': item.category,
        };
      }).toList();
    });

    // --- 3. Build payload for Kotlin ---
    final payload = {
      'main': mainMap,
      'kotSections': kotSectionsMap, // <-- Important: nest all KOTs under this key
    };

    // --- 4. Send to Kotlin ---
    final result = await _channel.invokeMethod<bool>(
      "printReceiptV2",
      {
        "printReceiptV2": payload,
        "qr_code_text": qrCodeText,
        "paper_width": paperWidth,
        "kot_enabled": kotEnabled,
      },
    );

    return result ?? false;
  } catch (e, st) {
    print("Error in printReceiptV2: $e\n$st");
    rethrow;
  }
}


  Future<bool> printOfflineOrderLabel({
    required OfflineOrderLabel offlineOrderLabel,
  }) async {
    try {
      final result =
          await _channel.invokeMethod<bool>("printOfflineOrderLabel", {
        'offline_order_label': offlineOrderLabel.toJson(),
      });
      return result ?? false;
    } catch (e) {
      log(e.toString());
      return false;
    }
  }

  // This method checks if the printer is connected to any printer.
  Future<bool> isConnected() async {
    try {
      final isConnected = await _channel.invokeMethod(
          "isConnectedToBluetoothThermalPrinter",
          {"bluetooth_printer_address": printerAddress});
      return isConnected;
    } catch (e) {
      log(e.toString());
      return false;
    }
  }

  Future<void> disconnect() async {
    try {
      await _channel.invokeMethod("disconnectBluetoothThermalPrinter");
    } catch (e) {
      log(e.toString());
    }
  }

  @override
  String toString() =>
      'BluetoothPrinter(printerAddress: $printerAddress, printerName: $printerName)';
}
