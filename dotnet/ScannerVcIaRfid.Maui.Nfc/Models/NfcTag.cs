using System.Text.Json.Serialization;

namespace ScannerVcIaRfid.Maui.Nfc.Models;

/// <summary>
/// POCO cross-platform de uma tag NFC lida. Espelha
/// com.scannervciarfid.nfc.NfcTagData (Kotlin), com o JSON NDEF ja convertido.
/// </summary>
public sealed class NfcTag
{
    /// <summary>Identificador da tag em hexadecimal maiusculo.</summary>
    public required string Uid { get; init; }

    /// <summary>Tecnologias da tag (ex.: NfcA, MifareUltralight, Ndef).</summary>
    public required IReadOnlyList<string> Technologies { get; init; }

    /// <summary>Registros NDEF. Lista vazia quando a tag nao tem NDEF.</summary>
    public required IReadOnlyList<NdefRecordInfo> NdefRecords { get; init; }
}

/// <summary>
/// Um registro NDEF. Os nomes JSON seguem NdefJson.kt.
/// </summary>
public sealed class NdefRecordInfo
{
    [JsonPropertyName("tnf")]
    public int Tnf { get; init; }

    [JsonPropertyName("type")]
    public string Type { get; init; } = string.Empty;

    [JsonPropertyName("payloadHex")]
    public string PayloadHex { get; init; } = string.Empty;

    /// <summary>Texto decodificado (registro Text).</summary>
    [JsonPropertyName("text")]
    public string? Text { get; init; }

    [JsonPropertyName("languageCode")]
    public string? LanguageCode { get; init; }

    /// <summary>URI decodificada (registro URI ou URI absoluta).</summary>
    [JsonPropertyName("uri")]
    public string? Uri { get; init; }

    /// <summary>Tipo MIME (registro MIME).</summary>
    [JsonPropertyName("mimeType")]
    public string? MimeType { get; init; }
}

/// <summary>
/// Contexto de serializacao gerado em compilacao: funciona com trimming/AOT
/// no build release, sem reflection.
/// </summary>
[JsonSerializable(typeof(List<NdefRecordInfo>))]
internal sealed partial class NfcJsonContext : JsonSerializerContext
{
}
