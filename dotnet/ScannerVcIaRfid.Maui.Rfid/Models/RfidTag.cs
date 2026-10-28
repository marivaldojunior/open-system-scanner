namespace ScannerVcIaRfid.Maui.Rfid.Models;

/// <summary>
/// POCO cross-platform de uma tag UHF. Espelha com.scannervciarfid.rfid.RfidTag.
/// </summary>
public sealed class RfidTag
{
    /// <summary>EPC em hexadecimal maiusculo.</summary>
    public required string Epc { get; init; }

    /// <summary>Maior RSSI (dBm) na janela de entrega, ou null se o leitor nao informa.</summary>
    public int? Rssi { get; init; }

    /// <summary>Quantas vezes a tag foi lida na janela de entrega.</summary>
    public int ReadCount { get; init; }

    public required string SourceEngine { get; init; }
}

public sealed class RfidTagsEventArgs(IReadOnlyList<RfidTag> tags) : EventArgs
{
    public IReadOnlyList<RfidTag> Tags { get; } = tags;
}

public sealed class RfidErrorEventArgs(string message) : EventArgs
{
    public string Message { get; } = message;
}
