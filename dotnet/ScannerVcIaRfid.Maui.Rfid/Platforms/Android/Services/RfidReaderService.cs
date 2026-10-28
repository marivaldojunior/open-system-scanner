using System.Globalization;
using Android.Runtime;
using Com.Scannervciarfid.Rfid;
using ScannerVcIaRfid.Maui.Rfid.Services;
// O binding gera classes e EventArgs com os mesmos nomes a partir das
// interfaces e objects Kotlin; estes sao os POCOs/enums do pacote.
using RfidErrorEventArgs = ScannerVcIaRfid.Maui.Rfid.Models.RfidErrorEventArgs;
using RfidLocateEventArgs = ScannerVcIaRfid.Maui.Rfid.Models.RfidLocateEventArgs;
using RfidLockField = ScannerVcIaRfid.Maui.Rfid.Models.RfidLockField;
using RfidLockPrivilege = ScannerVcIaRfid.Maui.Rfid.Models.RfidLockPrivilege;
using RfidMemoryBank = ScannerVcIaRfid.Maui.Rfid.Models.RfidMemoryBank;
using RfidTag = ScannerVcIaRfid.Maui.Rfid.Models.RfidTag;
using RfidTagsEventArgs = ScannerVcIaRfid.Maui.Rfid.Models.RfidTagsEventArgs;
using NativeRfidTag = Com.Scannervciarfid.Rfid.RfidTag;

namespace ScannerVcIaRfid.Maui.Rfid.Platforms.Android.Services;

public sealed class RfidReaderService : IRfidReaderService, IDisposable
{
    private readonly IRfidReader? _reader = RfidReaderFactory.Create();
    private readonly IRfidTagOperations? _operations;
    private readonly RfidListener _listener;
    private readonly LocateListener _locateListener;
    private bool _initialized;
    private bool _disposed;

    public event EventHandler<RfidTagsEventArgs>? TagsRead;
    public event EventHandler<RfidErrorEventArgs>? RfidError;
    public event EventHandler<RfidLocateEventArgs>? TagLocated;

    public RfidReaderService()
    {
        _listener = new RfidListener(this);
        _locateListener = new LocateListener(this);
        _operations = AsTagOperations(_reader);
    }

    public bool IsAvailable => _reader is not null;

    public bool SupportsTagOperations => _operations is not null;

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
        _operations?.SetLocateListener(_locateListener);
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
        _operations?.SetLocateListener(null);
        _reader.Release();
        _initialized = false;
    }

    public Task<string> ReadMemoryAsync(
        string epc, RfidMemoryBank bank, int wordOffset, int wordCount, string accessPassword = "00000000") =>
        Run((operations, listener) =>
            operations.ReadMemory(epc, (int)bank, wordOffset, wordCount, accessPassword, listener));

    public Task WriteMemoryAsync(
        string epc, RfidMemoryBank bank, int wordOffset, string dataHex, string accessPassword = "00000000") =>
        Run((operations, listener) =>
            operations.WriteMemory(epc, (int)bank, wordOffset, dataHex, accessPassword, listener));

    public Task LockAsync(string epc, RfidLockField field, RfidLockPrivilege privilege, string accessPassword) =>
        Run((operations, listener) =>
            operations.Lock(epc, (int)field, (int)privilege, accessPassword, listener));

    public Task KillAsync(string epc, string killPassword) =>
        Run((operations, listener) => operations.Kill(epc, killPassword, listener));

    public async Task<double> SetTransmitPowerAsync(int dbm)
    {
        var applied = await Run((operations, listener) => operations.SetTransmitPower(dbm, listener));
        return double.Parse(applied, CultureInfo.InvariantCulture);
    }

    public Task SetSessionAsync(int session) =>
        Run((operations, listener) => operations.SetSession(session, listener));

    public void StartLocate(string epc) => RequireOperations().StartLocate(epc);

    public void StopLocate() => RequireOperations().StopLocate();

    public void Dispose()
    {
        if (_disposed)
        {
            return;
        }

        _disposed = true;
        Release();
        _listener.Dispose();
        _locateListener.Dispose();
    }

    // O factory devolve o leitor como IRfidReader. Um cast C# comum falha
    // num objeto Java: JavaCast pergunta ao Java se ele tambem implementa
    // RfidTagOperations.
    private static IRfidTagOperations? AsTagOperations(IRfidReader? reader)
    {
        if (reader is null)
        {
            return null;
        }

        try
        {
            return reader.JavaCast<IRfidTagOperations>();
        }
        catch (InvalidCastException)
        {
            return null;
        }
    }

    private IRfidTagOperations RequireOperations()
    {
        if (_operations is null)
        {
            throw new NotSupportedException(
                "O leitor RFID deste aparelho nao suporta operacoes por tag (exige o SDK Zebra RFID API3).");
        }

        if (!_initialized)
        {
            throw new InvalidOperationException("Chame Initialize() antes das operacoes por tag.");
        }

        return _operations;
    }

    private Task<string> Run(Action<IRfidTagOperations, IRfidOperationListener> start)
    {
        var operations = RequireOperations();
        var listener = new OperationListener();
        start(operations, listener);
        return listener.Task;
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

    private sealed class LocateListener(RfidReaderService owner) : Java.Lang.Object, IRfidLocateListener
    {
        public void OnTagLocated(string epc, int relativeDistance) =>
            owner.TagLocated?.Invoke(owner, new RfidLocateEventArgs(epc, relativeDistance));
    }

    /// <summary>
    /// Traduz o resultado JNI de uma unica operacao em um Task.
    /// </summary>
    private sealed class OperationListener : Java.Lang.Object, IRfidOperationListener
    {
        private readonly TaskCompletionSource<string> _completion =
            new(TaskCreationOptions.RunContinuationsAsynchronously);

        public Task<string> Task => _completion.Task;

        public void OnSuccess(string result) => _completion.TrySetResult(result);

        public void OnError(string error) => _completion.TrySetException(new InvalidOperationException(error));
    }
}
