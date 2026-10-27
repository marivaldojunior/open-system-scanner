import 'package:flutter/services.dart';

import 'scanner_vc_ia_rfid_hardware.dart' show ScanResult;

/// Leitura unica de codigo de barras pela tela do Google Code Scanner
/// (Play Services). Nao exige permissao de camera nem o VisionScannerWidget.
class ScannerVcIaRfidCodeScanner {
  ScannerVcIaRfidCodeScanner._();

  static final ScannerVcIaRfidCodeScanner instance = ScannerVcIaRfidCodeScanner._();

  static const MethodChannel _methodChannel = MethodChannel('com.scannervciarfid/methods');

  /// Abre a tela de leitura. Retorna null se o usuario cancelar. Lanca
  /// [PlatformException] quando a leitura falha (ex.: aparelho sem Play
  /// Services).
  Future<ScanResult?> scan() async {
    final result = await _methodChannel.invokeMapMethod<dynamic, dynamic>('scanWithCodeScanner');
    return result == null ? null : ScanResult.fromMap(result);
  }
}
