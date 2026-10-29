namespace ScannerVcIaRfid.Maui.OcrScripts;

public static class OcrScriptsMauiAppBuilderExtensions
{
    /// <summary>
    /// Registra os motores de OCR em chines, japones, coreano e devanagari no
    /// VisionEngineRegistry nativo. Depois disso, VisionScanner.Engine aceita
    /// os ids de <see cref="OcrScriptEngines"/>.
    /// </summary>
    public static MauiAppBuilder UseOcrScripts(this MauiAppBuilder builder)
    {
#if ANDROID
        Com.Scannervciarfid.Vision.Ocr.Chinese.ChineseOcrEngine.Register();
        Com.Scannervciarfid.Vision.Ocr.Japanese.JapaneseOcrEngine.Register();
        Com.Scannervciarfid.Vision.Ocr.Korean.KoreanOcrEngine.Register();
        Com.Scannervciarfid.Vision.Ocr.Devanagari.DevanagariOcrEngine.Register();
#endif
        return builder;
    }
}
