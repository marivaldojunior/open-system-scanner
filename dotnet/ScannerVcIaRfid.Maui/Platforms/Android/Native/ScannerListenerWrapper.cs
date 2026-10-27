using Com.Scannervciarfid.Core;
using ScannerVcIaRfid.Maui.Models;
using AndroidScanResult = Com.Scannervciarfid.Core.ScanResult;
using ScanErrorEventArgs = ScannerVcIaRfid.Maui.Models.ScanErrorEventArgs;
using ScanResult = ScannerVcIaRfid.Maui.Models.ScanResult;

namespace ScannerVcIaRfid.Maui.Platforms.Android.Native;

/// <summary>
/// Implementa IScannerListener (gerado a partir da interface Kotlin ScannerListener)
/// e traduz os callbacks JNI em eventos .NET tradicionais, usando apenas os POCOs
/// de ScannerVcIaRfid.Maui.Models - assim, nada acima desta classe precisa
/// conhecer tipos Java.Lang.Object/JNI.
/// </summary>
public sealed class ScannerListenerWrapper : Java.Lang.Object, IScannerListener
{
    public event EventHandler<ScanResultEventArgs>? ScanSucceeded;
    public event EventHandler<ScanErrorEventArgs>? ScanFailed;

    public void OnScanSuccess(AndroidScanResult result)
    {
        var mapped = new ScanResult
        {
            Value = result.Value ?? string.Empty,
            Format = result.Format ?? string.Empty,
            SourceEngine = result.SourceEngine ?? string.Empty
        };

        ScanSucceeded?.Invoke(this, new ScanResultEventArgs(mapped));
    }

    public void OnScanError(string error)
    {
        ScanFailed?.Invoke(this, new ScanErrorEventArgs(error));
    }
}
