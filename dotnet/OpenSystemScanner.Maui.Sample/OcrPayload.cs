using System.Text.Json.Serialization;

namespace OpenSystemScanner.Maui.Sample;

/// <summary>
/// Espelha o JSON emitido por OcrTextEngine.toPayloadJson() (Kotlin) quando
/// ScanResult.format == "VISION_OCR". Os nomes de propriedade JSON
/// permanecem em camelCase (mesma convencao do lado Kotlin) via
/// JsonPropertyName - nao dependemos de PropertyNameCaseInsensitive.
/// </summary>
public sealed record OcrPayload(
    [property: JsonPropertyName("fullText")] string FullText,
    [property: JsonPropertyName("blocks")] List<OcrBlock> Blocks);

public sealed record OcrBlock(
    [property: JsonPropertyName("text")] string Text,
    [property: JsonPropertyName("boundingBox")] OcrBoundingBox? BoundingBox);

public sealed record OcrBoundingBox(
    [property: JsonPropertyName("left")] int Left,
    [property: JsonPropertyName("top")] int Top,
    [property: JsonPropertyName("right")] int Right,
    [property: JsonPropertyName("bottom")] int Bottom);
