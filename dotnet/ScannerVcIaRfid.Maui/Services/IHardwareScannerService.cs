using ScannerVcIaRfid.Maui.Models;

namespace ScannerVcIaRfid.Maui.Services;

/// <summary>
/// Abstracao cross-platform sobre com.scannervciarfid.core.UniversalScanner.
/// Cada plataforma (Platforms/Android, futuramente Platforms/iOS etc.)
/// fornece sua propria implementacao.
/// </summary>
public interface IHardwareScannerService
{
    event EventHandler<ScanResultEventArgs> ScanSucceeded;
    event EventHandler<ScanErrorEventArgs> ScanFailed;

    /// <summary>
    /// Troca o leitor detectado pelo fabricante por um leitor de Intent
    /// generico (Chainway, Sunmi, Seuic...). Use a action e o extra
    /// configurados na saida Intent do aparelho. Pode ser chamado antes ou
    /// depois de <see cref="Initialize"/>.
    /// </summary>
    void UseGenericIntent(string intentAction, string dataExtraKey, string? intentCategory = null);

    void Initialize();

    void Start();

    void Stop();

    void Release();
}
