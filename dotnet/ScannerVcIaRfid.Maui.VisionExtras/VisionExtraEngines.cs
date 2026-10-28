namespace ScannerVcIaRfid.Maui.VisionExtras;

/// <summary>
/// Ids dos motores extras para VisionScanner.Engine. Espelham ENGINE_ID de
/// ObjectDetectionEngine, ImageLabelingEngine e FaceDetectionEngine (Kotlin).
/// </summary>
public static class VisionExtraEngines
{
    /// <summary>Deteccao e rastreio de objetos. Format: VISION_OBJECTS.</summary>
    public const string Objects = "objects";

    /// <summary>Rotulos genericos da cena. Format: VISION_LABELS.</summary>
    public const string Labels = "labels";

    /// <summary>Deteccao de rostos (sem reconhecimento). Format: VISION_FACES.</summary>
    public const string Faces = "faces";
}
