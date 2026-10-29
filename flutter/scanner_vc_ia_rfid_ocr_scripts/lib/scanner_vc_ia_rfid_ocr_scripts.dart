/// Ids dos motores de OCR por script para VisionScannerWidget.engine (plugin
/// scanner_vc_ia_rfid_flutter). Espelham ENGINE_ID de ChineseOcrEngine,
/// JapaneseOcrEngine, KoreanOcrEngine e DevanagariOcrEngine (Kotlin). O
/// resultado tem o mesmo formato do OCR latino (format VISION_OCR). Cada
/// modelo tambem reconhece texto latino. Os motores ficam disponiveis so por
/// este plugin estar no pubspec do app.
abstract final class OcrScriptEngines {
  /// Chines simplificado e tradicional.
  static const String chinese = 'ocr-chinese';
  static const String japanese = 'ocr-japanese';
  static const String korean = 'ocr-korean';

  /// Devanagari (hindi, marathi, nepali...).
  static const String devanagari = 'ocr-devanagari';
}
