using ScannerVcIaRfid.Maui.Models;

namespace ScannerVcIaRfid.Maui.Services;

/// <summary>
/// Leitura unica de codigo de barras pela tela do Google Code Scanner (Play
/// Services). Nao exige permissao de camera nem o controle VisionScanner.
/// </summary>
public interface ICodeScannerService
{
    /// <summary>
    /// Abre a tela de leitura e retorna o codigo lido, ou null se o usuario
    /// cancelar. Lanca <see cref="InvalidOperationException"/> quando a
    /// leitura falha (ex.: aparelho sem Play Services).
    /// </summary>
    Task<ScanResult?> ScanAsync();
}
