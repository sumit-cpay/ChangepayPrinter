import 'package:cpay_printer/models/bluetooth_printer.dart';

import 'cpay_printer_platform_interface.dart';

class CpayPrinter {
  Future<String?> getPlatformVersion() {
    return CpayPrinterPlatform.instance.getPlatformVersion();
  }

  Future<void> initialise() {
    return CpayPrinterPlatform.instance.initialise();
  }

  Future<List<BluetoothPrinter>> get getAllBluetoothPairedDevices async {
    return CpayPrinterPlatform.instance.getAllBluetoothPairedDevices;
  }

  Future<bool> connectToBluetoothPrinterByAddress(String address) async {
    return CpayPrinterPlatform.instance
        .connectToBluetoothPrinterByAddress(address);
  }
}
