import 'dart:developer';

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:cpay_printer/cpay_printer.dart';
import 'package:permission_handler/permission_handler.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  final _cpayPrinterPlugin = CpayPrinter();

  @override
  void initState() {
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        floatingActionButton: FloatingActionButton(
          onPressed: () async {
            await Permission.bluetoothConnect.request();
            await Permission.bluetoothScan.request();
            final printer = CpayPrinter();
            await printer.initialise();
            try {
              final f = await printer.getAllBluetoothPairedDevices();
              log('${f.first}');
              await f.first.connect();
              f.first.printString('printableString\n\n\n\n');
            } catch (e) {
              debugPrint('$e');
            }
          },
        ),
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Text('Running on: $_platformVersion\n'),
        ),
      ),
    );
  }
}
