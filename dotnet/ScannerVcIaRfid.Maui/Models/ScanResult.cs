namespace ScannerVcIaRfid.Maui.Models;

/// <summary>
/// POCO cross-platform, independente de JNI/Java.Lang.Object.
/// Espelha o Kotlin data class com.scannervciarfid.core.ScanResult.
/// </summary>
public sealed class ScanResult
{
    public required string Value { get; init; }
    public required string Format { get; init; }
    public required string SourceEngine { get; init; }
}
