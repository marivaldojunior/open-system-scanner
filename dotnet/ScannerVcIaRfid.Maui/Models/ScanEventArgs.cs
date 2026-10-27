namespace ScannerVcIaRfid.Maui.Models;

public sealed class ScanResultEventArgs : EventArgs
{
    public ScanResult Result { get; }

    public ScanResultEventArgs(ScanResult result)
    {
        Result = result;
    }
}

public sealed class ScanErrorEventArgs : EventArgs
{
    public string Message { get; }

    public ScanErrorEventArgs(string message)
    {
        Message = message;
    }
}
