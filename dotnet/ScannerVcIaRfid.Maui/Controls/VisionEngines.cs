namespace ScannerVcIaRfid.Maui.Controls;

/// <summary>
/// Ids dos motores de visao para <see cref="VisionScanner.Engine"/>. Espelham
/// BarcodeEngine.ENGINE_ID e OcrTextEngine.ENGINE_ID (Kotlin). Um modulo de
/// motor novo registra o proprio id no VisionEngineRegistry nativo - nesse
/// caso, passe o id direto como string.
/// </summary>
public static class VisionEngines
{
    public const string Barcode = "barcode";

    public const string Ocr = "ocr";
}
