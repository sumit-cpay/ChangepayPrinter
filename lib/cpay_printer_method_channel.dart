import 'dart:developer';

import 'package:cpay_printer/models/bluetooth_printer.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

import 'cpay_printer_platform_interface.dart';

/// An implementation of [CpayPrinterPlatform] that uses method channels.
class MethodChannelCpayPrinter extends CpayPrinterPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('cpay_printer');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<List<BluetoothPrinter>> get getAllBluetoothPairedDevices async {
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
    const bluetoothConnectPermission = Permission.bluetoothConnect;
    final status = await bluetoothConnectPermission.request();
    if (status.isGranted || status.isLimited) {
      try {
        final connectedPrinter = await methodChannel.invokeMethod<bool>(
            "connectToBluetoothPrinterByAddress",
            {"bluetooth_printer_address": address});
        return connectedPrinter ?? false;
      } catch (e) {
        log(e.toString());
      }
    }
    return false;
  }
}
