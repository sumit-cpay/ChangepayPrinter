import 'package:flutter_test/flutter_test.dart';
import 'package:cpay_printer/cpay_printer.dart';
import 'package:cpay_printer/cpay_printer_platform_interface.dart';
import 'package:cpay_printer/cpay_printer_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockCpayPrinterPlatform
    with MockPlatformInterfaceMixin
    implements CpayPrinterPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final CpayPrinterPlatform initialPlatform = CpayPrinterPlatform.instance;

  test('$MethodChannelCpayPrinter is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelCpayPrinter>());
  });

  test('getPlatformVersion', () async {
    CpayPrinter cpayPrinterPlugin = CpayPrinter();
    MockCpayPrinterPlatform fakePlatform = MockCpayPrinterPlatform();
    CpayPrinterPlatform.instance = fakePlatform;

    expect(await cpayPrinterPlugin.getPlatformVersion(), '42');
  });
}
