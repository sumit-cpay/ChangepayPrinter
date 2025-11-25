import 'dart:developer';

import 'package:cpay_printer/models/bluetooth_printer.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'cpay_printer_platform_interface.dart';
import 'models/kot_receipt.dart';

/// An implementation of [CpayPrinterPlatform] that uses method channels.
class MethodChannelCpayPrinter extends CpayPrinterPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('cpay_printer');

  @override
  Future<String?> getPlatformVersion() async {
    final version =
        await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<List<BluetoothPrinter>> getAllBluetoothPairedDevices() async {
    final availableDevicesMap =
        await methodChannel.invokeMethod("getAllBluetoothPairedDevices");
    final bluetoothPrinters = <BluetoothPrinter>[];
    for (var printer in availableDevicesMap) {
      bluetoothPrinters
          .add(BluetoothPrinter.fromJson(Map<String, dynamic>.from(printer)));
    }
    return bluetoothPrinters;
  }

  @override
  Future<bool> connectToBluetoothPrinterByAddress(String address) async {
    try {
      final connectedPrinter = await methodChannel.invokeMethod<bool>(
          "connectToBluetoothPrinterByAddress",
          {"bluetooth_printer_address": address});
      return connectedPrinter ?? false;
    } catch (e) {
      log(e.toString());
    }

    return false;
  }

  @override
  Future<void> initialise() async {
    try {
      methodChannel.invokeMapMethod('initialise');
    } catch (err) {
      log(err.toString());
    }
  }
}


/// --- EXTENSION MUST BE OUTSIDE THE CLASS ---
extension CpayPrinterPrinting on MethodChannelCpayPrinter {
  /// Print Main + KOT receipts
  Future<void> printKotReceiptV2(KotPrintableReceiptV2 kotData) async {
    try {
      final jsonData = kotData.toJson(); // Convert model to Map<String, dynamic>
      await methodChannel.invokeMethod('printKotReceiptV2', jsonData);
    } catch (e) {
      log("Printing failed: $e");
      rethrow;
    }
  }
}