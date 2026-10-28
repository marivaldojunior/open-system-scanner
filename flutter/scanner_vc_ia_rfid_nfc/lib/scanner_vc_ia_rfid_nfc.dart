import 'dart:convert';

import 'package:flutter/services.dart';

/// Um registro NDEF. Os nomes seguem NdefJson.kt (modulo nativo nfc).
class NdefRecord {
  const NdefRecord({
    required this.tnf,
    required this.type,
    required this.payloadHex,
    this.text,
    this.languageCode,
    this.uri,
    this.mimeType,
  });

  factory NdefRecord.fromJson(Map<String, dynamic> json) {
    return NdefRecord(
      tnf: json['tnf'] as int? ?? 0,
      type: json['type'] as String? ?? '',
      payloadHex: json['payloadHex'] as String? ?? '',
      text: json['text'] as String?,
      languageCode: json['languageCode'] as String?,
      uri: json['uri'] as String?,
      mimeType: json['mimeType'] as String?,
    );
  }

  final int tnf;
  final String type;
  final String payloadHex;

  /// Texto decodificado (registro Text).
  final String? text;
  final String? languageCode;

  /// URI decodificada (registro URI ou URI absoluta).
  final String? uri;

  /// Tipo MIME (registro MIME).
  final String? mimeType;
}

/// Espelha com.scannervciarfid.nfc.NfcTagData (Kotlin), com o JSON NDEF ja
/// convertido.
class NfcTag {
  const NfcTag({
    required this.uid,
    required this.technologies,
    required this.ndefRecords,
  });

  factory NfcTag.fromMap(Map<dynamic, dynamic> map) {
    final rawJson = map['ndefRecordsJson'] as String? ?? '[]';
    final records = (jsonDecode(rawJson) as List<dynamic>)
        .map((dynamic item) => NdefRecord.fromJson(item as Map<String, dynamic>))
        .toList(growable: false);

    return NfcTag(
      uid: map['uid'] as String? ?? '',
      technologies: (map['technologies'] as List<dynamic>? ?? const [])
          .cast<String>()
          .toList(growable: false),
      ndefRecords: records,
    );
  }

  /// Identificador da tag em hexadecimal maiusculo.
  final String uid;

  /// Tecnologias da tag (ex.: NfcA, MifareUltralight, Ndef).
  final List<String> technologies;

  /// Registros NDEF. Lista vazia quando a tag nao tem NDEF.
  final List<NdefRecord> ndefRecords;

  @override
  String toString() => 'NfcTag(uid: $uid, technologies: $technologies, ndefRecords: ${ndefRecords.length})';
}

/// Leitura de tags NFC/RFID HF (13,56 MHz).
///
/// Escutar [onTagRead] liga o Reader Mode do Android; cancelar a
/// subscription o desliga. O Android so entrega tags com o app em primeiro
/// plano.
class ScannerVcIaRfidNfc {
  ScannerVcIaRfidNfc._();

  static final ScannerVcIaRfidNfc instance = ScannerVcIaRfidNfc._();

  static const EventChannel _eventChannel = EventChannel('com.scannervciarfid/nfc/tags');
  static const MethodChannel _methodChannel = MethodChannel('com.scannervciarfid/nfc/methods');

  Stream<NfcTag>? _onTagRead;

  /// Erros do leitor (ex.: NFC desligado) chegam como [PlatformException]
  /// no stream.
  Stream<NfcTag> get onTagRead {
    return _onTagRead ??= _eventChannel
        .receiveBroadcastStream()
        .map((dynamic event) => NfcTag.fromMap(event as Map<dynamic, dynamic>));
  }

  /// O aparelho tem NFC.
  Future<bool> isSupported() async => await _methodChannel.invokeMethod<bool>('isSupported') ?? false;

  /// O NFC esta ligado nas configuracoes do aparelho.
  Future<bool> isEnabled() async => await _methodChannel.invokeMethod<bool>('isEnabled') ?? false;
}
