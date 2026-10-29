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
  static const MethodChannel _methodChannel = MethodChannel('com.scannervciarfid/methods');

  Stream<ScanResult>? _onBarcodeScanned;

  Stream<ScanResult> get onBarcodeScanned {
    return _onBarcodeScanned ??= _eventChannel
        .receiveBroadcastStream()
        .map((dynamic event) => ScanResult.fromMap(event as Map<dynamic, dynamic>));
  }

  /// O leitor aceita gatilho por software (Zebra, Honeywell, Newland). O
  /// valor muda com [useGenericIntent].
  Future<bool> supportsSoftTrigger() async =>
      await _methodChannel.invokeMethod<bool>('supportsSoftTrigger') ?? false;

  /// Dispara a leitura sem o gatilho fisico. Exige uma subscription ativa de
  /// [onBarcodeScanned]. Lanca [PlatformException] (code NOT_SUPPORTED)
  /// quando o leitor nao suporta.
  Future<void> startSoftScan() => _methodChannel.invokeMethod<void>('startSoftScan');

  Future<void> stopSoftScan() => _methodChannel.invokeMethod<void>('stopSoftScan');

  /// O leitor liga e desliga simbologias pelo app (hoje so Zebra/DataWedge).
  Future<bool> supportsSymbologyConfiguration() async =>
      await _methodChannel.invokeMethod<bool>('supportsSymbologyConfiguration') ?? false;

  /// Liga e desliga simbologias. Os nomes seguem os decoders do DataWedge
  /// sem o prefixo 'decoder_' (ex.: 'ean13', 'qrcode', 'code128',
  /// 'datamatrix', 'pdf417').
  Future<void> configureSymbologies({List<String> enabled = const [], List<String> disabled = const []}) {
    return _methodChannel.invokeMethod<void>('configureSymbologies', {
      'enabled': enabled,
      'disabled': disabled,
    });
  }

  /// Troca o leitor detectado pelo fabricante por um leitor de Intent
  /// generico (Chainway, Sunmi, Seuic...). Use a action e o extra
  /// configurados na saida Intent do aparelho. Uma subscription ativa de
  /// [onBarcodeScanned] continua valendo com o leitor novo.
  Future<void> useGenericIntent({
    required String intentAction,
    required String dataExtraKey,
    String? intentCategory,
  }) {
    return _methodChannel.invokeMethod<void>('useGenericIntent', <String, String?>{
      'intentAction': intentAction,
      'dataExtraKey': dataExtraKey,
      'intentCategory': intentCategory,
    });
  }
}
