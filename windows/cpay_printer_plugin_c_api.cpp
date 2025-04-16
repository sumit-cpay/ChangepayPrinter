#include "include/cpay_printer/cpay_printer_plugin_c_api.h"

#include <flutter/plugin_registrar_windows.h>

#include "cpay_printer_plugin.h"

void CpayPrinterPluginCApiRegisterWithRegistrar(
    FlutterDesktopPluginRegistrarRef registrar) {
  cpay_printer::CpayPrinterPlugin::RegisterWithRegistrar(
      flutter::PluginRegistrarManager::GetInstance()
          ->GetRegistrar<flutter::PluginRegistrarWindows>(registrar));
}
