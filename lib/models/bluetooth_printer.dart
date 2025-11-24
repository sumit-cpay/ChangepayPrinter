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
  KotPrintableReceiptV2 kotreceiptV2, {
  String? qrCodeText,
  double paperWidth = BluetoothPrinter.paperWidth58,
  required bool kotEnabled,
}) async {
  try {
    final result = await _channel.invokeMethod<bool>(
      "printReceiptV2",
      {
        "printable_receipt_v2":   kotreceiptV2.toJson(),
        "qr_code_text": qrCodeText,
        "paper_width": paperWidth,
        "kot_enabled": kotEnabled,
      },
    );

    return result ?? false;
  } catch (e) {
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
