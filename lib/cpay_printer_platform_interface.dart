import 'package:cpay_printer/models/bluetooth_printer.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'cpay_printer_method_channel.dart';

abstract class CpayPrinterPlatform extends PlatformInterface {
  /// Constructs a CpayPrinterPlatform.
  CpayPrinterPlatform() : super(token: _token);

  static final Object _token = Object();

  static CpayPrinterPlatform _instance = MethodChannelCpayPrinter();

  /// The default instance of [CpayPrinterPlatform] to use.
  ///
  /// Defaults to [MethodChannelCpayPrinter].
  static CpayPrinterPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [CpayPrinterPlatform] when
  /// they register themselves.
  static set instance(CpayPrinterPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<void> initialise() {
    throw UnimplementedError('initialise() has not been implemented.');
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Future<List<BluetoothPrinter>> get getAllBluetoothPairedDevices {
    throw UnimplementedError('getAllBluetoothPairedDevices has not been implemented.');
  }

  Future<bool> connectToBluetoothPrinterByAddress(
      String address) {
    throw UnimplementedError(
        'connectToBluetoothPrinterByAddress(address) has not been implemented.');

      }
}
