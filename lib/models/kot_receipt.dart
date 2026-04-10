// printable_models.dart

// ==================== Order Item ====================
import 'package:cpay_printer/models/printable_receipt.dart';

class PrintableOrderItem {
  final String name;
  final int quantity;
  final double price;
  final double total;
  final String? category; // e.g., Juice, Chinese, Main Course
final List<PrintableAddon>? addons;
  PrintableOrderItem({
    required this.name,
    required this.quantity,
    required this.price,
    required this.total,
    this.category,  
    this.addons,
  });

  factory PrintableOrderItem.fromJson(Map<String, dynamic> json) {
    return PrintableOrderItem(
      name: json['name'] ?? '-',
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
  final PrintableReceiptMain main;
  final Map<String, List<PrintableOrderItem>> kotSections;

  KotPrintableReceiptV2({
    required this.main,
    required this.kotSections,
  });

  factory KotPrintableReceiptV2.fromJson(Map<String, dynamic> json) {
    final main = PrintableReceiptMain.fromJson(json['main'] ?? {});
    final kotMap = <String, List<PrintableOrderItem>>{};
    json.forEach((key, value) {
      if (key == 'main') return;
      kotMap[key] = (value as List? ?? [])
          .map((e) => PrintableOrderItem.fromJson(e))
          .toList();
    });
    return KotPrintableReceiptV2(main: main, kotSections: kotMap);
  }

Map<String, dynamic> toJson() {
  final map = <String, dynamic>{};
  map['main'] = main.toJson();

  // Convert each KOT section to match Kotlin CartItemReceipt
  kotSections.forEach((key, value) {
    map[key] = value
        .map((item) => {
              "name": item.name,
              "quantity": item.quantity,
              "price": item.price,
              "total": item.total,
              "category": item.category,
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
