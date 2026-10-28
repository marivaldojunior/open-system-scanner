using System.Text.Json.Serialization;

namespace ScannerVcIaRfid.Maui.Ai.Models;

/// <summary>
/// Trecho do texto reconhecido pela extracao de entidades. Os nomes JSON
/// seguem LanguageAiClient.kt (modulo nativo ai-language).
/// </summary>
public sealed class TextEntity
{
    [JsonPropertyName("start")]
    public int Start { get; init; }

    [JsonPropertyName("end")]
    public int End { get; init; }

    [JsonPropertyName("text")]
    public string Text { get; init; } = string.Empty;

    /// <summary>Um trecho pode ter mais de uma interpretacao (ex.: telefone ou numero de rastreio).</summary>
    [JsonPropertyName("entities")]
    public IReadOnlyList<EntityInfo> Entities { get; init; } = [];
}

public sealed class EntityInfo
{
    /// <summary>
    /// ADDRESS, DATE_TIME, EMAIL, FLIGHT_NUMBER, IBAN, ISBN, MONEY,
    /// PAYMENT_CARD, PHONE, TRACKING_NUMBER ou URL.
    /// </summary>
    [JsonPropertyName("type")]
    public string Type { get; init; } = string.Empty;

    /// <summary>DATE_TIME: instante em milissegundos desde 1970 (UTC).</summary>
    [JsonPropertyName("timestampMillis")]
    public long? TimestampMillis { get; init; }

    /// <summary>MONEY: moeda como aparece no texto (ex.: "R$").</summary>
    [JsonPropertyName("currency")]
    public string? Currency { get; init; }

    [JsonPropertyName("integerPart")]
    public int? IntegerPart { get; init; }

    [JsonPropertyName("fractionalPart")]
    public int? FractionalPart { get; init; }

    /// <summary>TRACKING_NUMBER: codigo da transportadora no ML Kit.</summary>
    [JsonPropertyName("carrier")]
    public int? Carrier { get; init; }

    [JsonPropertyName("trackingNumber")]
    public string? TrackingNumber { get; init; }
}

[JsonSerializable(typeof(List<TextEntity>))]
internal sealed partial class AiJsonContext : JsonSerializerContext
{
}
