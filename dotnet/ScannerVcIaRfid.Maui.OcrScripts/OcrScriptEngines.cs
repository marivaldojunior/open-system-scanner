namespace ScannerVcIaRfid.Maui.OcrScripts;

/// <summary>
/// Ids dos motores de OCR por script para VisionScanner.Engine. Espelham
/// ENGINE_ID de ChineseOcrEngine, JapaneseOcrEngine, KoreanOcrEngine e
/// DevanagariOcrEngine (Kotlin). O resultado tem o mesmo formato do OCR
/// latino (format VISION_OCR). Cada modelo tambem reconhece texto latino.
/// </summary>
public static class OcrScriptEngines
{
    /// <summary>Chines simplificado e tradicional.</summary>
    public const string Chinese = "ocr-chinese";

    public const string Japanese = "ocr-japanese";

    public const string Korean = "ocr-korean";

    /// <summary>Devanagari (hindi, marathi, nepali...).</summary>
    public const string Devanagari = "ocr-devanagari";
}
