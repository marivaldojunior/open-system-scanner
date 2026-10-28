using System.Text.Json;
using System.Text.Json.Serialization;

namespace ScannerVcIaRfid.Maui.VisionExtras.Models;

/// <summary>
/// POCOs do ScanResult.Value dos motores extras. Os nomes JSON seguem
/// ObjectDetectionEngine.kt, ImageLabelingEngine.kt e FaceDetectionEngine.kt.
/// </summary>
public sealed class BoundingBox
{
    [JsonPropertyName("left")] public int Left { get; init; }
    [JsonPropertyName("top")] public int Top { get; init; }
    [JsonPropertyName("right")] public int Right { get; init; }
    [JsonPropertyName("bottom")] public int Bottom { get; init; }
}

public sealed class VisionLabel
{
    [JsonPropertyName("text")] public string Text { get; init; } = string.Empty;
    [JsonPropertyName("confidence")] public double Confidence { get; init; }

    /// <summary>So nos rotulos de cena (VISION_LABELS).</summary>
    [JsonPropertyName("index")] public int? Index { get; init; }
}

public sealed class DetectedObjectInfo
{
    [JsonPropertyName("trackingId")] public int? TrackingId { get; init; }
    [JsonPropertyName("boundingBox")] public BoundingBox BoundingBox { get; init; } = new();
    [JsonPropertyName("labels")] public IReadOnlyList<VisionLabel> Labels { get; init; } = [];
}

public sealed class DetectedFaceInfo
{
    [JsonPropertyName("trackingId")] public int? TrackingId { get; init; }
    [JsonPropertyName("boundingBox")] public BoundingBox BoundingBox { get; init; } = new();
    [JsonPropertyName("smilingProbability")] public double? SmilingProbability { get; init; }
    [JsonPropertyName("leftEyeOpenProbability")] public double? LeftEyeOpenProbability { get; init; }
    [JsonPropertyName("rightEyeOpenProbability")] public double? RightEyeOpenProbability { get; init; }
    [JsonPropertyName("headEulerAngleY")] public double HeadEulerAngleY { get; init; }
    [JsonPropertyName("headEulerAngleZ")] public double HeadEulerAngleZ { get; init; }
}

public sealed class ObjectsPayload
{
    [JsonPropertyName("objects")] public IReadOnlyList<DetectedObjectInfo> Objects { get; init; } = [];
}

public sealed class LabelsPayload
{
    [JsonPropertyName("labels")] public IReadOnlyList<VisionLabel> Labels { get; init; } = [];
}

public sealed class FacesPayload
{
    [JsonPropertyName("faces")] public IReadOnlyList<DetectedFaceInfo> Faces { get; init; } = [];
}

/// <summary>
/// Converte ScanResult.Value conforme o ScanResult.Format. Lanca
/// <see cref="JsonException"/> se o JSON estiver invalido.
/// </summary>
public static class VisionExtrasPayload
{
    public static ObjectsPayload ParseObjects(string json) =>
        JsonSerializer.Deserialize(json, VisionExtrasJsonContext.Default.ObjectsPayload) ?? new();

    public static LabelsPayload ParseLabels(string json) =>
        JsonSerializer.Deserialize(json, VisionExtrasJsonContext.Default.LabelsPayload) ?? new();

    public static FacesPayload ParseFaces(string json) =>
        JsonSerializer.Deserialize(json, VisionExtrasJsonContext.Default.FacesPayload) ?? new();
}

[JsonSerializable(typeof(ObjectsPayload))]
[JsonSerializable(typeof(LabelsPayload))]
[JsonSerializable(typeof(FacesPayload))]
internal sealed partial class VisionExtrasJsonContext : JsonSerializerContext
{
}
