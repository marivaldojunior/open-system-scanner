namespace ScannerVcIaRfid.Maui.Nfc.Models;

public sealed class NfcTagEventArgs(NfcTag tag) : EventArgs
{
    public NfcTag Tag { get; } = tag;
}

public sealed class NfcErrorEventArgs(string message) : EventArgs
{
    public string Message { get; } = message;
}
