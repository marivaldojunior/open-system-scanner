import 'package:flutter/services.dart';

/// Espelha com.scannervciarfid.rfid.RfidTag (Kotlin).
class RfidTag {
  const RfidTag({
    required this.epc,
    required this.rssi,
    required this.readCount,
    required this.sourceEngine,
  });

  factory RfidTag.fromMap(Map<dynamic, dynamic> map) {
    return RfidTag(
      epc: map['epc'] as String? ?? '',
      rssi: map['rssi'] as int?,
      readCount: map['readCount'] as int? ?? 0,
      sourceEngine: map['sourceEngine'] as String? ?? '',
    );
  }

  /// EPC em hexadecimal maiusculo.
  final String epc;

  /// Maior RSSI (dBm) no lote, ou null se o leitor nao informa (ex.: DataWedge).
  final int? rssi;

  /// Quantas vezes a tag foi lida no lote.
  final int readCount;

  final String sourceEngine;

  @override
  String toString() => 'RfidTag(epc: $epc, rssi: $rssi, readCount: $readCount)';
}

/// Leitor RFID UHF (EPC Gen2).
///
/// Escutar [onTagsRead] inicializa o leitor; o gatilho fisico ja dispara o
/// inventario. [startInventory] e [stopInventory] fazem o mesmo por
/// software. Cada evento e um lote de tags (cada EPC uma vez por lote).
class ScannerVcIaRfidRfid {
  ScannerVcIaRfidRfid._();

  static final ScannerVcIaRfidRfid instance = ScannerVcIaRfidRfid._();

  static const EventChannel _eventChannel = EventChannel('com.scannervciarfid/rfid/tags');
  static const MethodChannel _methodChannel = MethodChannel('com.scannervciarfid/rfid/methods');

  Stream<List<RfidTag>>? _onTagsRead;

  /// Erros do leitor chegam como [PlatformException] no stream.
  Stream<List<RfidTag>> get onTagsRead {
    return _onTagsRead ??= _eventChannel.receiveBroadcastStream().map(
          (dynamic event) => (event as List<dynamic>)
              .map((dynamic item) => RfidTag.fromMap(item as Map<dynamic, dynamic>))
              .toList(growable: false),
        );
  }

  /// O aparelho tem um leitor RFID suportado.
  Future<bool> isAvailable() async => await _methodChannel.invokeMethod<bool>('isAvailable') ?? false;

  /// Exige uma subscription ativa de [onTagsRead].
  Future<void> startInventory() => _methodChannel.invokeMethod<void>('startInventory');

  Future<void> stopInventory() => _methodChannel.invokeMethod<void>('stopInventory');
}
