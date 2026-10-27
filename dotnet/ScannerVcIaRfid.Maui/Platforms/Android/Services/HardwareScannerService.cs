using Android.App;
using Com.Scannervciarfid.Core;
using Com.Scannervciarfid.Hardware;
using ScannerVcIaRfid.Maui.Models;
using ScannerVcIaRfid.Maui.Platforms.Android.Native;
using ScannerVcIaRfid.Maui.Services;
using ScanErrorEventArgs = ScannerVcIaRfid.Maui.Models.ScanErrorEventArgs;

namespace ScannerVcIaRfid.Maui.Platforms.Android.Services;

public sealed class HardwareScannerService : IHardwareScannerService, IDisposable
{
    private IUniversalScanner _nativeScanner;
    private readonly ScannerListenerWrapper _listenerWrapper;
    private bool _disposed;
    private bool _initialized;

    public event EventHandler<ScanResultEventArgs>? ScanSucceeded;
    public event EventHandler<ScanErrorEventArgs>? ScanFailed;

    public HardwareScannerService()
    {
        _nativeScanner = HardwareScannerFactory.Create()
            ?? throw new InvalidOperationException(
                "HardwareScannerFactory.Create() retornou null - verifique o binding.");

        _listenerWrapper = new ScannerListenerWrapper();
        _listenerWrapper.ScanSucceeded += OnNativeScanSucceeded;
        _listenerWrapper.ScanFailed += OnNativeScanFailed;
    }

    public void UseGenericIntent(string intentAction, string dataExtraKey, string? intentCategory = null)
    {
        var wasInitialized = _initialized;
        if (wasInitialized)
        {
            Release();
        }

        _nativeScanner = HardwareScannerFactory.CreateGeneric(intentAction, dataExtraKey, intentCategory)
            ?? throw new InvalidOperationException(
                "HardwareScannerFactory.CreateGeneric() retornou null - verifique o binding.");

        if (wasInitialized)
        {
            Initialize();
        }
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
