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

  // Operacoes por tag. Exigem um leitor com suporte ([supportsTagOperations])
  // e uma subscription ativa de [onTagsRead]. Pare o inventario antes de
  // chamar. Falhas chegam como PlatformException. Senhas sao 8 digitos
  // hexadecimais ('00000000' = sem senha).

  static const EventChannel _locateChannel = EventChannel('com.scannervciarfid/rfid/locate');

  Stream<RfidLocateEvent>? _onTagLocated;

  /// O leitor suporta as operacoes por tag (memoria, lock, kill,
  /// localizacao, potencia, sessao). Hoje so o leitor do SDK Zebra RFID
  /// API3; o leitor DataWedge so faz inventario.
  Future<bool> supportsTagOperations() async =>
      await _methodChannel.invokeMethod<bool>('supportsTagOperations') ?? false;

  /// Le [wordCount] palavras de 16 bits do [bank]. Retorna os dados em
  /// hexadecimal.
  Future<String> readMemory(
    String epc, {
    required RfidMemoryBank bank,
    int wordOffset = 0,
    required int wordCount,
    String accessPassword = '00000000',
  }) async {
    return await _methodChannel.invokeMethod<String>('readMemory', {
          'epc': epc,
          'memoryBank': bank.value,
          'wordOffset': wordOffset,
          'wordCount': wordCount,
          'accessPassword': accessPassword,
        }) ??
        '';
  }

  /// Grava [dataHex] (multiplo de 4 digitos hexadecimais) no [bank].
  Future<void> writeMemory(
    String epc, {
    required RfidMemoryBank bank,
    int wordOffset = 0,
    required String dataHex,
    String accessPassword = '00000000',
  }) {
    return _methodChannel.invokeMethod<void>('writeMemory', {
      'epc': epc,
      'memoryBank': bank.value,
      'wordOffset': wordOffset,
      'dataHex': dataHex,
      'accessPassword': accessPassword,
    });
  }

  Future<void> lock(
    String epc, {
    required RfidLockField field,
    required RfidLockPrivilege privilege,
    required String accessPassword,
  }) {
    return _methodChannel.invokeMethod<void>('lock', {
      'epc': epc,
      'field': field.value,
      'privilege': privilege.value,
      'accessPassword': accessPassword,
    });
  }

  /// Desativa a tag para sempre. Exige uma kill password diferente de zero
  /// gravada na tag. Nao tem volta.
  Future<void> kill(String epc, {required String killPassword}) {
    return _methodChannel.invokeMethod<void>('kill', {'epc': epc, 'killPassword': killPassword});
  }

  /// Ajusta a potencia para o nivel suportado mais proximo de [dbm].
  /// Retorna a potencia aplicada em dBm.
  Future<double> setTransmitPower(int dbm) async {
    final applied = await _methodChannel.invokeMethod<String>('setTransmitPower', {'dbm': dbm});
    return double.parse(applied ?? '0');
  }

  /// Sessao Gen2 de 0 a 3 (S0 a S3).
  Future<void> setSession(int session) {
    return _methodChannel.invokeMethod<void>('setSession', {'session': session});
  }

  /// Distancia relativa da tag em localizacao (ver [startLocate]).
  Stream<RfidLocateEvent> get onTagLocated {
    return _onTagLocated ??= _locateChannel.receiveBroadcastStream().map(
          (dynamic event) => RfidLocateEvent.fromMap(event as Map<dynamic, dynamic>),
        );
  }

  /// Comeca a localizar a tag. A distancia chega em [onTagLocated].
  Future<void> startLocate(String epc) => _methodChannel.invokeMethod<void>('startLocate', {'epc': epc});

  Future<void> stopLocate() => _methodChannel.invokeMethod<void>('stopLocate');
}

/// Bancos de memoria EPC Gen2. Os valores seguem RfidMemoryBank (Kotlin).
enum RfidMemoryBank {
  reserved(0),
  epc(1),
  tid(2),
  user(3);

  const RfidMemoryBank(this.value);

  final int value;
}

/// Campos de lock. Os valores seguem RfidLockField (Kotlin).
enum RfidLockField {
  killPassword(0),
  accessPassword(1),
  epc(2),
  tid(3),
  user(4);

  const RfidLockField(this.value);

  final int value;
}

/// Privilegios de lock. Os valores seguem RfidLockPrivilege (Kotlin).
enum RfidLockPrivilege {
  unlock(0),

  /// Bloqueia escrita (e leitura, nos campos de senha) sem a access password.
  lock(1),

  /// Bloqueio permanente. Nao tem volta.
  permaLock(2),

  /// Desbloqueio permanente. Nao tem volta.
  permaUnlock(3);

  const RfidLockPrivilege(this.value);

  final int value;
}

class RfidLocateEvent {
  const RfidLocateEvent({required this.epc, required this.relativeDistance});

  factory RfidLocateEvent.fromMap(Map<dynamic, dynamic> map) {
    return RfidLocateEvent(
      epc: map['epc'] as String? ?? '',
      relativeDistance: map['relativeDistance'] as int? ?? 0,
    );
  }

  final String epc;

  /// 0 (longe) a 100 (encostado no leitor).
  final int relativeDistance;
}
