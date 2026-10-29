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

    /// <summary>
    /// O leitor aceita gatilho por software (Zebra, Honeywell, Newland). O
    /// valor muda com <see cref="UseGenericIntent"/>.
    /// </summary>
    bool SupportsSoftTrigger { get; }

    /// <summary>Dispara a leitura sem o gatilho fisico. Exige <see cref="Initialize"/>.</summary>
    void StartSoftScan();

    void StopSoftScan();

    /// <summary>O leitor liga e desliga simbologias pelo app (hoje so Zebra/DataWedge).</summary>
    bool SupportsSymbologyConfiguration { get; }

    /// <summary>
    /// Liga e desliga simbologias. Os nomes seguem os decoders do DataWedge
    /// sem o prefixo "decoder_" (ex.: "ean13", "qrcode", "code128",
    /// "datamatrix", "pdf417"). Exige <see cref="Initialize"/>.
    /// </summary>
    void ConfigureSymbologies(IEnumerable<string> enabled, IEnumerable<string> disabled);
}
