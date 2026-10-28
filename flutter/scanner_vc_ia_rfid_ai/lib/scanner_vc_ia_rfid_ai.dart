import 'dart:convert';

import 'package:flutter/services.dart';

/// Uma interpretacao de um trecho do texto. Os nomes seguem
/// LanguageAiClient.kt (modulo nativo ai-language).
class EntityInfo {
  const EntityInfo({
    required this.type,
    this.timestampMillis,
    this.currency,
    this.integerPart,
    this.fractionalPart,
    this.carrier,
    this.trackingNumber,
  });

  factory EntityInfo.fromJson(Map<String, dynamic> json) {
    return EntityInfo(
      type: json['type'] as String? ?? 'UNKNOWN',
      timestampMillis: json['timestampMillis'] as int?,
      currency: json['currency'] as String?,
      integerPart: json['integerPart'] as int?,
      fractionalPart: json['fractionalPart'] as int?,
      carrier: json['carrier'] as int?,
      trackingNumber: json['trackingNumber'] as String?,
    );
  }

  /// ADDRESS, DATE_TIME, EMAIL, FLIGHT_NUMBER, IBAN, ISBN, MONEY,
  /// PAYMENT_CARD, PHONE, TRACKING_NUMBER ou URL.
  final String type;

  /// DATE_TIME: instante em milissegundos desde 1970 (UTC).
  final int? timestampMillis;

  /// MONEY: moeda como aparece no texto (ex.: "R$").
  final String? currency;
  final int? integerPart;
  final int? fractionalPart;

  /// TRACKING_NUMBER: codigo da transportadora no ML Kit.
  final int? carrier;
  final String? trackingNumber;
}

/// Trecho do texto reconhecido pela extracao de entidades.
class TextEntity {
  const TextEntity({
    required this.start,
    required this.end,
    required this.text,
    required this.entities,
  });

  factory TextEntity.fromJson(Map<String, dynamic> json) {
    return TextEntity(
      start: json['start'] as int? ?? 0,
      end: json['end'] as int? ?? 0,
      text: json['text'] as String? ?? '',
      entities: (json['entities'] as List<dynamic>? ?? const [])
          .map((dynamic item) => EntityInfo.fromJson(item as Map<String, dynamic>))
          .toList(growable: false),
    );
  }

  final int start;
  final int end;
  final String text;

  /// Um trecho pode ter mais de uma interpretacao (ex.: telefone ou rastreio).
  final List<EntityInfo> entities;
}

/// IA de linguagem on-device (ML Kit). O texto nao sai do aparelho.
///
/// Traducao e extracao de entidades baixam o modelo do idioma na primeira
/// chamada (precisa de rede uma vez). Falhas lancam [PlatformException].
class ScannerVcIaRfidAi {
  ScannerVcIaRfidAi._();

  static final ScannerVcIaRfidAi instance = ScannerVcIaRfidAi._();

  static const MethodChannel _methodChannel = MethodChannel('com.scannervciarfid/ai/methods');

  /// Codigo BCP-47 do idioma (ex.: "pt") ou "und" se nao reconhecido.
  Future<String> identifyLanguage(String text) async {
    return await _methodChannel.invokeMethod<String>('identifyLanguage', {'text': text}) ?? 'und';
  }

  /// Com [requireWifi], o modelo (~30 MB por idioma) so e baixado em Wi-Fi.
  Future<String> translate(
    String text, {
    required String sourceLanguage,
    required String targetLanguage,
    bool requireWifi = false,
  }) async {
    return await _methodChannel.invokeMethod<String>('translate', {
          'text': text,
          'sourceLanguage': sourceLanguage,
          'targetLanguage': targetLanguage,
          'requireWifi': requireWifi,
        }) ??
        '';
  }

  /// Encontra datas, enderecos, telefones, e-mails, valores, numeros de
  /// rastreio e outros dados no texto (ex.: resultado do OCR).
  Future<List<TextEntity>> extractEntities(String text, {String languageTag = 'pt'}) async {
    final json = await _methodChannel.invokeMethod<String>('extractEntities', {
      'text': text,
      'languageTag': languageTag,
    });
    if (json == null) return const [];

    return (jsonDecode(json) as List<dynamic>)
        .map((dynamic item) => TextEntity.fromJson(item as Map<String, dynamic>))
        .toList(growable: false);
  }
}
