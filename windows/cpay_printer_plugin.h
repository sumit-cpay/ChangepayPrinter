#ifndef FLUTTER_PLUGIN_CPAY_PRINTER_PLUGIN_H_
#define FLUTTER_PLUGIN_CPAY_PRINTER_PLUGIN_H_

#include <flutter/method_channel.h>
#include <flutter/plugin_registrar_windows.h>

#include <memory>

namespace cpay_printer {

class CpayPrinterPlugin : public flutter::Plugin {
 public:
  static void RegisterWithRegistrar(flutter::PluginRegistrarWindows *registrar);

  CpayPrinterPlugin();

  virtual ~CpayPrinterPlugin();

  // Disallow copy and assign.
  CpayPrinterPlugin(const CpayPrinterPlugin&) = delete;
  CpayPrinterPlugin& operator=(const CpayPrinterPlugin&) = delete;

  // Called when a method is called on this plugin's channel from Dart.
  void HandleMethodCall(
      const flutter::MethodCall<flutter::EncodableValue> &method_call,
      std::unique_ptr<flutter::MethodResult<flutter::EncodableValue>> result);
};

}  // namespace cpay_printer

#endif  // FLUTTER_PLUGIN_CPAY_PRINTER_PLUGIN_H_
