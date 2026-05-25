// printable_models.dart

// ==================== Order Item ====================
import 'package:cpay_printer/models/printable_receipt.dart';

/// Formats ISO/raw timestamps for thermal receipts (matches customer receipt style).
String formatKotReceiptDateTime(String raw) {
  final trimmed = raw.trim();
  if (trimmed.isEmpty) return trimmed;
  // Already formatted (e.g. 27/06/2022 03:06 PM)
  if (RegExp(r'\d{1,2}/\d{1,2}/\d{2,4}').hasMatch(trimmed)) {
    return trimmed;
  }
  try {
    final dt = DateTime.parse(trimmed).toLocal();
    final day = dt.day.toString().padLeft(2, '0');
    final month = dt.month.toString().padLeft(2, '0');
    final year = dt.year;
    final hour12 = dt.hour % 12 == 0 ? 12 : dt.hour % 12;
    final minute = dt.minute.toString().padLeft(2, '0');
    final amPm = dt.hour >= 12 ? 'PM' : 'AM';
    return '$day/$month/$year ${hour12.toString().padLeft(2, '0')}:$minute $amPm';
  } catch (_) {
    return trimmed;
  }
}

class PrintableOrderItem {
  final String name;
  final String orderId;

  final int quantity;
  final double price;
  final double total;
  final String? category; // e.g., Juice, Chinese, Main Course
final List<PrintableAddon>? addons;
  PrintableOrderItem({  
    required this.name,
    required this.orderId,
    required this.quantity,
    required this.price,
    required this.total,
    this.category,  
    this.addons,
  });

  factory PrintableOrderItem.fromJson(Map<String, dynamic> json) {
    return PrintableOrderItem(
      name: json['name'] ?? '-',
      orderId: json['order_id'] ?? '',
      quantity: (json['quantity'] ?? 0) as int,
      price: (json['price'] ?? 0).toDouble(),
      total: (json['total'] ?? 0).toDouble(),
      category: json['category'] as String?,
      addons: json['addons'] == null
          ? null
          : (json['addons'] as List)
              .map((e) => PrintableAddon.fromJson(e))
              .toList(),
    );
  }

  Map<String, dynamic> toJson() => {
        'name': name,
        'order_id': orderId,
        'quantity': quantity,
        'price': price,
        'total': total,
        if (category != null) 'category': category,
        if (addons != null) 'addons': addons,
      };
}

// ==================== Main Receipt ====================
class PrintableReceiptMain {
  final String dailyTokenNumber;
  final String datetime;
  final String businessName;
  final List<PrintableOrderItem> items;
  final List<Map<String, dynamic>> otherCharges;
  final double orderTotal;
  final String orderId;
  final String printerId;
  final String customerPhone;
  final String customerName;
  final String deliveryType;
  final String address;
  final String? customerNote;
  final int? tableNumber;

  PrintableReceiptMain({
    this.tableNumber,
    required this.dailyTokenNumber,
    required this.datetime,
    required this.businessName,
    required this.items,
    required this.otherCharges,
    required this.orderTotal,
    required this.orderId,
    required this.printerId,
    required this.customerPhone,
    required this.customerName,
    required this.deliveryType,
    required this.address,
    this.customerNote,
  });

 factory PrintableReceiptMain.fromJson(Map<String, dynamic> json) {
  return PrintableReceiptMain(
    datetime: json['datetime'] ?? '',
    businessName: json['business_name'] ?? '',
    items: (json['items'] as List? ?? [])
        .map((e) => PrintableOrderItem.fromJson(e))
        .toList(),

     dailyTokenNumber: json['daily_token_number'].toString() ?? '',
    otherCharges: parseOtherCharges(json),
    tableNumber: json['table_number'] as int? ?? 0,

    orderTotal: (json['order_total'] ?? 0).toDouble() / 100,
    orderId: json['order_id'] ?? '',
    printerId: json['printer_id'] ?? '',
    customerPhone: json['customer_phone'] ?? '',
    customerName: json['customer_name'] ?? '',
    deliveryType: json['delivery_type'] ?? '',
    address: json['address'] ?? '',
    customerNote: json['customer_note'],
  );
}


  Map<String, dynamic> toJson() {
    return {

      'table_number': tableNumber,
      'daily_token_number': dailyTokenNumber,
      'datetime': datetime,
      'business_name': businessName,
      'items': items.map((x) => x.toJson()).toList(),
      'other_charges': otherCharges,
      'order_total': orderTotal,
      'order_id': orderId,
      'printer_id': printerId,
      'customer_phone': customerPhone,
      'customer_name': customerName,
      'delivery_type': deliveryType,
      'address': address,
      if (customerNote != null) 'customer_note': customerNote,
    };
  }
}

// ==================== Receipt V2 (Main + KOT sections) ====================
class KotPrintableReceiptV2 {
  /// Top-level metadata used for KOT printing when [main] is empty/disabled.
  final String orderId;
  final String dailyTokenNumber;
  final String datetime;
  final String businessName;
  final String? customerNote;

  final PrintableReceiptMain main;
  final Map<String, List<PrintableOrderItem>> kotSections;

  static const Set<String> _reservedKeys = {
    'main',
    'kotSections',
    'order_id',
    'orderId',
    'order_short_number',
    'order_short_id',
    'daily_token_number',
    'dailyTokenNumber',
    'datetime',
    'business_name',
    'businessName',
    'customer_note',
    'customerNote',
  };

  KotPrintableReceiptV2({
    required this.main,
    required this.kotSections,
    this.orderId = '',
    this.dailyTokenNumber = '',
    this.datetime = '',
    this.businessName = '',
    this.customerNote,
  });

  /// Same format as customer [PrintableReceipt]: shortOrderId-dailyTokenNumber.
  String get orderIdLabel {
    final id = effectiveOrderId;
    final token = effectiveDailyTokenNumber;
    if (id.isEmpty) return token;
    if (token.isEmpty) return id;
    return '$id-$token';
  }

  String get effectiveOrderId =>
      orderId.isNotEmpty ? orderId : main.orderId;

  String get effectiveDailyTokenNumber =>
      dailyTokenNumber.isNotEmpty ? dailyTokenNumber : main.dailyTokenNumber;

  String get effectiveDatetime =>
      datetime.isNotEmpty ? datetime : main.datetime;

  /// Formatted time string for KOT header (customer receipt uses [PrintableReceipt.dateTime] as-is).
  String get effectiveFormattedDatetime =>
      formatKotReceiptDateTime(effectiveDatetime);

  String get effectiveBusinessName =>
      businessName.isNotEmpty ? businessName : main.businessName;

  String? get effectiveCustomerNote =>
      customerNote ?? main.customerNote;

  factory KotPrintableReceiptV2.fromJson(Map<String, dynamic> json) {
    final mainJson = json['main'];
    final main = mainJson != null && mainJson is Map
        ? PrintableReceiptMain.fromJson(
            Map<String, dynamic>.from(mainJson as Map),
          )
        : PrintableReceiptMain.fromJson({});

    final kotMap = <String, List<PrintableOrderItem>>{};

    void parseKotSectionEntries(Map<String, dynamic> sections) {
      sections.forEach((key, value) {
        if (_reservedKeys.contains(key)) return;
        if (value is! List) return;
        kotMap[key] = value
            .map((e) => PrintableOrderItem.fromJson(
                  Map<String, dynamic>.from(e as Map),
                ))
            .toList();
      });
    }

    final nestedKot = json['kotSections'];
    if (nestedKot is Map) {
      parseKotSectionEntries(Map<String, dynamic>.from(nestedKot));
    }

    json.forEach((key, value) {
      if (_reservedKeys.contains(key) || key == 'kotSections') return;
      if (value is! List) return;
      kotMap[key] = value
          .map((e) => PrintableOrderItem.fromJson(
                Map<String, dynamic>.from(e as Map),
              ))
          .toList();
    });

    return KotPrintableReceiptV2(
      main: main,
      kotSections: kotMap,
      orderId: json['order_short_number']?.toString() ??
          json['order_short_id']?.toString() ??
          json['order_id']?.toString() ??
          json['orderId']?.toString() ??
          '',
      dailyTokenNumber: json['daily_token_number']?.toString() ??
          json['dailyTokenNumber']?.toString() ??
          '',
      datetime: json['datetime']?.toString() ?? '',
      businessName: json['business_name']?.toString() ??
          json['businessName']?.toString() ??
          '',
      customerNote: json['customer_note']?.toString() ??
          json['customerNote']?.toString(),
    );
  }

  /// Fills top-level KOT fields from order details when API main receipt is null.
  KotPrintableReceiptV2 withOrderContext({
    required String orderShortNumber,
    required String dailyTokenNumber,
    String? businessName,
    String? customerNote,
    String? datetime,
    String? printableReceiptDateTime,
  }) {
    final resolvedDatetime = this.datetime.isNotEmpty
        ? this.datetime
        : (printableReceiptDateTime?.trim().isNotEmpty == true
            ? printableReceiptDateTime!.trim()
            : formatKotReceiptDateTime(datetime ?? ''));

    return KotPrintableReceiptV2(
      main: main,
      kotSections: kotSections,
      orderId: orderId.isNotEmpty ? orderId : orderShortNumber,
      dailyTokenNumber: this.dailyTokenNumber.isNotEmpty
          ? this.dailyTokenNumber
          : dailyTokenNumber,
      datetime: resolvedDatetime,
      businessName: this.businessName.isNotEmpty
          ? this.businessName
          : (businessName ?? ''),
      customerNote: this.customerNote ?? customerNote,
    );
  }

  Map<String, dynamic> toJson() {
    final map = <String, dynamic>{
      'orderId': orderIdLabel,
      'order_id': effectiveOrderId,
      'daily_token_number': effectiveDailyTokenNumber,
      'datetime': effectiveFormattedDatetime,
      'businessName': effectiveBusinessName,
      'customerNote': effectiveCustomerNote,
      'main': main.toJson(),
      'kotSections': <String, dynamic>{},
    };

    kotSections.forEach((key, value) {
      (map['kotSections'] as Map<String, dynamic>)[key] = value
          .map((item) => {
                'name': item.name,
                'quantity': item.quantity,
                'price': item.price,
                'total': item.total,
                'category': item.category,
                if (item.addons != null)
                  'addons': item.addons!
                      .map((a) => {'name': a.name, 'price': a.price})
                      .toList(),
              })
          .toList();
    });

    return map;
  }
}

// ==================== Extension for Paisa to Rupee Conversion ====================
extension ConvertPaisaToRupee on num? {
  double get paisaToRupee => this == null ? 0 : this! / 100;
}



List<Map<String, dynamic>> parseOtherCharges(Map<String, dynamic> json) {
  final List<Map<String, dynamic>> charges = [];

  void addCharge(String name, dynamic value) {
    if (value == null) return;
    if (value is num && value != 0) {
      charges.add({
        'name': name,
        'value': value.toDouble() / 100, // paisa → rupee
      });
    }
  }

  addCharge('Delivery', json['delivery_charges']);
  addCharge('Platform', json['platform_charge']);
  addCharge('Other', json['other_charges']);
  addCharge('Offer Discount', json['offer_discount']);
  addCharge('Product Discount', json['product_discount']);

  return charges;
}
