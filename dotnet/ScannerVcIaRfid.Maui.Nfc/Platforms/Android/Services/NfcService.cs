using System.Text.Json;
using Com.Scannervciarfid.Nfc;
using ScannerVcIaRfid.Maui.Nfc.Models;
using ScannerVcIaRfid.Maui.Nfc.Services;
// O binding gera EventArgs com os mesmos nomes a partir de NfcTagListener (Java).
using NfcErrorEventArgs = ScannerVcIaRfid.Maui.Nfc.Models.NfcErrorEventArgs;
using NfcTagEventArgs = ScannerVcIaRfid.Maui.Nfc.Models.NfcTagEventArgs;

namespace ScannerVcIaRfid.Maui.Nfc.Platforms.Android.Services;

public sealed class NfcService : INfcService, IDisposable
{
    private readonly NfcTagReader _reader = new();
    private readonly NfcTagListener _listener;
    private NfcWriteListener? _pendingWrite;
    private bool _disposed;

    public event EventHandler<NfcTagEventArgs>? TagRead;
    public event EventHandler<NfcErrorEventArgs>? NfcError;

    public NfcService()
    {
        _listener = new NfcTagListener(this);
        _reader.SetNfcTagListener(_listener);
    }

    public bool IsSupported => NfcTagReader.IsSupported(global::Android.App.Application.Context);

    public bool IsEnabled => NfcTagReader.IsEnabled(global::Android.App.Application.Context);

    public void Start()
    {
        // Reader Mode exige a Activity em primeiro plano, nao o Application context.
        var activity = Microsoft.Maui.ApplicationModel.Platform.CurrentActivity;
        if (activity is null)
        {
            NfcError?.Invoke(this, new NfcErrorEventArgs("Nenhuma Activity ativa para ler NFC."));
            return;
        }

        _reader.Start(activity);
    }

    public void Stop() => _reader.Stop();

    public Task<NfcTag> WriteTextAsync(string text, string languageCode = "pt") =>
        ArmWrite(listener => _reader.WriteTextOnNextTag(text, languageCode, listener));

    public Task<NfcTag> WriteUriAsync(string uri) =>
        ArmWrite(listener => _reader.WriteUriOnNextTag(uri, listener));

    public void CancelWrite()
    {
        // Cancela o Task antes do nativo: o onWriteError("cancelada") que o
        // nativo envia depois nao muda mais o resultado.
        _pendingWrite?.Cancel();
        _pendingWrite = null;
        _reader.CancelPendingWrite();
    }

    private Task<NfcTag> ArmWrite(Action<INfcWriteListener> arm)
    {
        // Uma gravacao por vez: o nativo tambem cancela a anterior.
        _pendingWrite?.Cancel();
        var listener = new NfcWriteListener();
        _pendingWrite = listener;
        arm(listener);
        return listener.Task;
    }

    public void Dispose()
    {
        if (_disposed)
        {
            return;
        }

        _disposed = true;
        _reader.Stop();
        _reader.SetNfcTagListener(null);
        _listener.Dispose();
    }

    private static NfcTag Map(NfcTagData data)
    {
        var json = data.NdefRecordsJson;
        var records = string.IsNullOrEmpty(json)
            ? []
            : JsonSerializer.Deserialize(json, NfcJsonContext.Default.ListNdefRecordInfo) ?? [];

        return new NfcTag
        {
            Uid = data.Uid ?? string.Empty,
            Technologies = (data.Technologies ?? string.Empty)
                .Split(',', StringSplitOptions.RemoveEmptyEntries),
            NdefRecords = records
        };
    }

    /// <summary>
    /// Traduz o resultado JNI de uma unica gravacao em um Task.
    /// </summary>
    private sealed class NfcWriteListener : Java.Lang.Object, INfcWriteListener
    {
        private readonly TaskCompletionSource<NfcTag> _completion =
            new(TaskCreationOptions.RunContinuationsAsynchronously);

        public Task<NfcTag> Task => _completion.Task;

        public void Cancel() => _completion.TrySetCanceled();

        public void OnWriteSuccess(NfcTagData tag)
        {
            try
            {
                _completion.TrySetResult(Map(tag));
            }
            catch (JsonException ex)
            {
                _completion.TrySetException(new InvalidOperationException($"NDEF invalido: {ex.Message}"));
            }
        }

        public void OnWriteError(string error) =>
            _completion.TrySetException(new InvalidOperationException(error));
    }

    /// <summary>
    /// Traduz os callbacks JNI de INfcTagListener em eventos .NET.
    /// </summary>
    private sealed class NfcTagListener(NfcService owner) : Java.Lang.Object, INfcTagListener
    {
        public void OnTagRead(NfcTagData tag)
        {
            try
            {
                owner.TagRead?.Invoke(owner, new NfcTagEventArgs(Map(tag)));
            }
            catch (JsonException ex)
            {
                owner.NfcError?.Invoke(owner, new NfcErrorEventArgs($"NDEF invalido: {ex.Message}"));
            }
        }

        public void OnNfcError(string error) => owner.NfcError?.Invoke(owner, new NfcErrorEventArgs(error));
    }
}
