import 'dart:convert';
import 'dart:developer';

import 'package:cpay_printer/models/printable_receipt.dart';
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
              final r = PrintableReceipt.fromJson(jsonDecode(json));
              f.first.printReceipt(r);
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

var json = """
    {
        "printer_id": "86:67:7A:00:15:A8",
        "order_id": "b0869d",
        "datetime": "27/06/2022 15:06PM UTC",
        "delivery_type": "SMART_BOX_DELIVERY",
        "items": [
            {
                "name": "Kalakand",
                "quantity": 2,
                "price": 28000,
                "total": 56000
            },
            {
                "name": "Kalakand sn jsjndn jsd sndjns dsdjmsjd",
                "quantity": 5585,
                "price": 2800000,
                "total": 56000000
            }
        ],
        "other_charges": [
            {
                "name": "PACKING",
                "value": 400,
                "merchant_added": false,
                "breakup": {}
            },
            {
                "name": "EXTRA",
                "value": 1000,
                "merchant_added": false,
                "breakup": {}
            },
            {
                "name": "TAX",
                "value": 2870,
                "merchant_added": false,
                "breakup": {}
            }
        ],
        "discount": 0,
        "order_total": 60270,
        "address": "Pretty Address, eSamudaay TESTBOX",
        "customer_phone": "+91-7750860057",
        "customer_name": "Sumit"
    }
""";
