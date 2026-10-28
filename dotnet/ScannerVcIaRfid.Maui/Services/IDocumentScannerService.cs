using ScannerVcIaRfid.Maui.Models;

namespace ScannerVcIaRfid.Maui.Services;

/// <summary>
/// Digitalizacao de documentos pela tela do Google Document Scanner (Play
/// Services): captura, recorte e correcao de perspectiva. Nao exige
/// permissao de camera.
/// </summary>
public interface IDocumentScannerService
{
    /// <summary>
    /// Abre o scanner e retorna as paginas digitalizadas, ou null se o
    /// usuario cancelar. Lanca <see cref="InvalidOperationException"/> quando
    /// a digitalizacao falha (ex.: aparelho sem Play Services).
    /// </summary>
    /// <param name="pageLimit">Numero maximo de paginas (0 = sem limite).</param>
    /// <param name="includePdf">Tambem gera um PDF com todas as paginas.</param>
    /// <param name="allowGalleryImport">Permite importar fotos da galeria.</param>
    Task<DocumentScanResult?> ScanAsync(int pageLimit = 0, bool includePdf = true, bool allowGalleryImport = false);
}
