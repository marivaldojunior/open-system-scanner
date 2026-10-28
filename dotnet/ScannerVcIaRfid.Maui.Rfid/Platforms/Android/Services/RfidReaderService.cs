using Com.Scannervciarfid.Rfid;
using ScannerVcIaRfid.Maui.Rfid.Services;
// O binding gera EventArgs a partir de RfidListener (Java); estes sao os POCOs do pacote.
using RfidErrorEventArgs = ScannerVcIaRfid.Maui.Rfid.Models.RfidErrorEventArgs;
using RfidTag = ScannerVcIaRfid.Maui.Rfid.Models.RfidTag;
using RfidTagsEventArgs = ScannerVcIaRfid.Maui.Rfid.Models.RfidTagsEventArgs;
using NativeRfidTag = Com.Scannervciarfid.Rfid.RfidTag;

namespace ScannerVcIaRfid.Maui.Rfid.Platforms.Android.Services;

public sealed class RfidReaderService : IRfidReaderService, IDisposable
{
    private readonly IRfidReader? _reader = RfidReaderFactory.Create();
    private readonly RfidListener _listener;
    private bool _initialized;
    private bool _disposed;

    public event EventHandler<RfidTagsEventArgs>? TagsRead;
    public event EventHandler<RfidErrorEventArgs>? RfidError;

    public RfidReaderService()
    {
        _listener = new RfidListener(this);
    }

    public bool IsAvailable => _reader is not null;

    public void Initialize()
    {
        // Mesmo motivo do HardwareScannerService: OnAppearing pode rodar
        // varias vezes, e registrar o BroadcastReceiver duas vezes falha.
        if (_reader is null || _initialized)
        {
            return;
        }

        _reader.Initialize(global::Android.App.Application.Context);
        _reader.SetRfidListener(_listener);
        _initialized = true;
    }

    public void StartInventory()
    {
        if (!EnsureReady())
        {
            return;
        }

        _reader!.StartInventory();
    }

    public void StopInventory()
    {
        if (_initialized)
        {
            _reader!.StopInventory();
        }
    }

    public void Release()
    {
        if (!_initialized)
        {
            return;
        }

        _reader!.SetRfidListener(null);
        _reader.Release();
        _initialized = false;
    }

    public void Dispose()
    {
        if (_disposed)
        {
            return;
        }

        _disposed = true;
        Release();
        _listener.Dispose();
    }

    private bool EnsureReady()
    {
        if (_reader is null)
        {
            RfidError?.Invoke(this, new RfidErrorEventArgs("Este aparelho nao tem leitor RFID suportado."));
            return false;
        }

        if (!_initialized)
        {
            RfidError?.Invoke(this, new RfidErrorEventArgs("Chame Initialize() antes de iniciar o inventario."));
            return false;
        }

        return true;
    }

    private static RfidTag Map(NativeRfidTag tag) => new()
    {
        Epc = tag.Epc ?? string.Empty,
        Rssi = tag.Rssi == NativeRfidTag.RssiUnknown ? null : tag.Rssi,
        ReadCount = tag.ReadCount,
        SourceEngine = tag.SourceEngine ?? string.Empty
    };

    /// <summary>
    /// Traduz os callbacks JNI de IRfidListener em eventos .NET.
    /// </summary>
    private sealed class RfidListener(RfidReaderService owner) : Java.Lang.Object, IRfidListener
    {
        public void OnTagsRead(NativeRfidTag[] tags)
        {
            var mapped = tags.Select(Map).ToArray();
            owner.TagsRead?.Invoke(owner, new RfidTagsEventArgs(mapped));
        }

        public void OnRfidError(string error) =>
            owner.RfidError?.Invoke(owner, new RfidErrorEventArgs(error));
    }
}
