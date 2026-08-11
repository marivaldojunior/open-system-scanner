using OpenSystemScanner.Maui.Models;

namespace OpenSystemScanner.Maui.Services;

/// <summary>
/// Abstracao cross-platform sobre com.opensystemscanner.core.UniversalScanner.
/// Cada plataforma (Platforms/Android, futuramente Platforms/iOS etc.)
/// fornece sua propria implementacao.
/// </summary>
public interface IHardwareScannerService
{
    event EventHandler<ScanResultEventArgs> ScanSucceeded;
    event EventHandler<ScanErrorEventArgs> ScanFailed;

    void Initialize();

    void Start();

    void Stop();

    void Release();
}
