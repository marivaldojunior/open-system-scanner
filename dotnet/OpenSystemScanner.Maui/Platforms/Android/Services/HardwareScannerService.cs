using Android.App;
using Com.Opensystemscanner;
using Com.Opensystemscanner.Core;
using OpenSystemScanner.Maui.Models;
using OpenSystemScanner.Maui.Platforms.Android.Native;
using OpenSystemScanner.Maui.Services;
using ScanErrorEventArgs = OpenSystemScanner.Maui.Models.ScanErrorEventArgs;

namespace OpenSystemScanner.Maui.Platforms.Android.Services;

public sealed class HardwareScannerService : IHardwareScannerService, IDisposable
{
    private readonly IUniversalScanner _nativeScanner;
    private readonly ScannerListenerWrapper _listenerWrapper;
    private bool _disposed;
    private bool _initialized;

    public event EventHandler<ScanResultEventArgs>? ScanSucceeded;
    public event EventHandler<ScanErrorEventArgs>? ScanFailed;

    public HardwareScannerService()
    {
        // Nota: dependendo de como o binding generator expoe o metodo estatico
        // Kotlin `object DataCaptureFactory { @JvmStatic fun getHardwareScanner() }`,
        // isto pode aparecer como propriedade (`.HardwareScanner`) ou como
        // metodo (`.GetHardwareScanner()`) - veja Transforms/Metadata.xml.
        _nativeScanner = DataCaptureFactory.HardwareScanner
            ?? throw new InvalidOperationException(
                "DataCaptureFactory.HardwareScanner retornou null - verifique o binding.");

        _listenerWrapper = new ScannerListenerWrapper();
        _listenerWrapper.ScanSucceeded += OnNativeScanSucceeded;
        _listenerWrapper.ScanFailed += OnNativeScanFailed;
    }

    public void Initialize()
    {
        // Este servico e registrado como Singleton (uma unica instancia para
        // o app inteiro), mas paginas MAUI podem chamar Initialize() toda vez
        // que reaparecem (OnAppearing). GenericIntentScanner.initialize()
        // registra um BroadcastReceiver do lado Kotlin - chama-lo duas vezes
        // sem um release() no meio lanca IllegalArgumentException
        // ("Receiver not registered" / already registered). Por isso a
        // idempotencia mora aqui, no servico, e nao em cada consumidor.
        if (_initialized)
        {
            return;
        }

        _nativeScanner.Initialize(global::Android.App.Application.Context);
        _nativeScanner.SetScannerListener(_listenerWrapper);
        _initialized = true;
    }

    public void Start() => _nativeScanner.Start();

    public void Stop() => _nativeScanner.Stop();

    public void Release()
    {
        _nativeScanner.SetScannerListener(null);
        _nativeScanner.Release();
        _initialized = false;
    }

    private void OnNativeScanSucceeded(object? sender, ScanResultEventArgs e) => ScanSucceeded?.Invoke(this, e);

    private void OnNativeScanFailed(object? sender, ScanErrorEventArgs e) => ScanFailed?.Invoke(this, e);

    public void Dispose()
    {
        if (_disposed)
        {
            return;
        }

        _disposed = true;

        _listenerWrapper.ScanSucceeded -= OnNativeScanSucceeded;
        _listenerWrapper.ScanFailed -= OnNativeScanFailed;

        Release();
        _listenerWrapper.Dispose();
    }
}
