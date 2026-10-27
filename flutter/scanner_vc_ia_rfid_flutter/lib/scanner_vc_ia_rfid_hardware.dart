import 'package:flutter/services.dart';

/// Espelha com.scannervciarfid.core.ScanResult (Kotlin) do lado Dart.
class ScanResult {
  const ScanResult({
    required this.value,
    required this.format,
    required this.sourceEngine,
  });

  factory ScanResult.fromMap(Map<dynamic, dynamic> map) {
    return ScanResult(
      value: map['value'] as String? ?? '',
      format: map['format'] as String? ?? '',
      sourceEngine: map['sourceEngine'] as String? ?? '',
    );
  }

  final String value;
  final String format;
  final String sourceEngine;

  @override
  String toString() =>
      'ScanResult(value: $value, format: $format, sourceEngine: $sourceEngine)';
}

/// Ponte para o leitor de hardware (Zebra/Datalogic/generico) via EventChannel.
///
/// O ciclo de vida do leitor nativo acompanha a subscription do stream:
/// escutar [onBarcodeScanned] inicializa e inicia o leitor do lado Kotlin;
/// cancelar a subscription o para (sem liberar recursos - isso só acontece
/// quando o plugin é desanexado do engine). Não há dispose explícito a
/// chamar do lado Dart além de cancelar a subscription do stream.
class ScannerVcIaRfidHardware {
  ScannerVcIaRfidHardware._();

  static final ScannerVcIaRfidHardware instance = ScannerVcIaRfidHardware._();

  static const EventChannel _eventChannel = EventChannel('com.scannervciarfid/hardware');

  Stream<ScanResult>? _onBarcodeScanned;

  Stream<ScanResult> get onBarcodeScanned {
    return _onBarcodeScanned ??= _eventChannel
        .receiveBroadcastStream()
        .map((dynamic event) => ScanResult.fromMap(event as Map<dynamic, dynamic>));
  }
}
