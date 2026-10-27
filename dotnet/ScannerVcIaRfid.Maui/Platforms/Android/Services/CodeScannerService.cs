using Com.Scannervciarfid.Codescanner;
using ScannerVcIaRfid.Maui.Models;
using ScannerVcIaRfid.Maui.Services;
using AndroidScanResult = Com.Scannervciarfid.Core.ScanResult;
using ScanResult = ScannerVcIaRfid.Maui.Models.ScanResult;

namespace ScannerVcIaRfid.Maui.Platforms.Android.Services;

public sealed class CodeScannerService : ICodeScannerService
{
    public Task<ScanResult?> ScanAsync()
    {
        // O Google Code Scanner abre uma Activity propria - precisa da
        // Activity atual, nao do Application context.
        var activity = Microsoft.Maui.ApplicationModel.Platform.CurrentActivity
            ?? throw new InvalidOperationException("Nenhuma Activity ativa para abrir o Google Code Scanner.");

        var listener = new CodeScannerListener();
        GoogleCodeScanner.Scan(activity, listener);
        return listener.Task;
    }

    /// <summary>
    /// Traduz os callbacks JNI de uma unica leitura em um Task.
    /// </summary>
    private sealed class CodeScannerListener : Java.Lang.Object, ICodeScannerListener
    {
        private readonly TaskCompletionSource<ScanResult?> _completion =
            new(TaskCreationOptions.RunContinuationsAsynchronously);

        public Task<ScanResult?> Task => _completion.Task;

        public void OnScanSuccess(AndroidScanResult result)
        {
            _completion.TrySetResult(new ScanResult
            {
                Value = result.Value ?? string.Empty,
                Format = result.Format ?? string.Empty,
                SourceEngine = result.SourceEngine ?? string.Empty
            });
        }

        public void OnScanCanceled() => _completion.TrySetResult(null);

        public void OnScanError(string error) =>
            _completion.TrySetException(new InvalidOperationException(error));
    }
}
