namespace ScannerVcIaRfid.Maui.Models;

/// <summary>
/// POCO cross-platform. Espelha com.scannervciarfid.documentscanner.DocumentScanResult.
/// Os arquivos ficam no cache do app: copie-os se precisar guarda-los.
/// </summary>
public sealed class DocumentScanResult
{
    /// <summary>URI de cada pagina em JPEG, ja recortada e corrigida.</summary>
    public required IReadOnlyList<string> PageImageUris { get; init; }

    /// <summary>URI do PDF com todas as paginas, ou null quando o PDF nao foi pedido.</summary>
    public string? PdfUri { get; init; }

    public int PageCount { get; init; }
}
