using Com.Scannervciarfid.Documentscanner;
using ScannerVcIaRfid.Maui.Services;
using DocumentScanResult = ScannerVcIaRfid.Maui.Models.DocumentScanResult;
using NativeDocumentScanResult = Com.Scannervciarfid.Documentscanner.DocumentScanResult;

namespace ScannerVcIaRfid.Maui.Platforms.Android.Services;

public sealed class DocumentScannerService : IDocumentScannerService
{
    public Task<DocumentScanResult?> ScanAsync(int pageLimit = 0, bool includePdf = true, bool allowGalleryImport = false)
    {
        // O scanner abre uma Activity propria a partir da Activity atual.
        var activity = Microsoft.Maui.ApplicationModel.Platform.CurrentActivity
            ?? throw new InvalidOperationException("Nenhuma Activity ativa para abrir o Google Document Scanner.");

        var listener = new DocumentScanListener();
        GoogleDocumentScanner.Scan(activity, pageLimit, includePdf, allowGalleryImport, listener);
        return listener.Task;
    }

    /// <summary>
    /// Traduz os callbacks JNI de uma unica digitalizacao em um Task.
    /// </summary>
    private sealed class DocumentScanListener : Java.Lang.Object, IDocumentScanListener
    {
        private readonly TaskCompletionSource<DocumentScanResult?> _completion =
            new(TaskCreationOptions.RunContinuationsAsynchronously);

        public Task<DocumentScanResult?> Task => _completion.Task;

        public void OnScanSuccess(NativeDocumentScanResult result)
        {
            var pdfUri = result.PdfUri;
            _completion.TrySetResult(new DocumentScanResult
            {
                PageImageUris = result.GetPageImageUris() ?? [],
                PdfUri = string.IsNullOrEmpty(pdfUri) ? null : pdfUri,
                PageCount = result.PageCount
            });
        }

        public void OnScanCanceled() => _completion.TrySetResult(null);

        public void OnScanError(string error) =>
            _completion.TrySetException(new InvalidOperationException(error));
    }
}
