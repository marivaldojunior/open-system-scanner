using ScannerVcIaRfid.Maui.Nfc.Models;

namespace ScannerVcIaRfid.Maui.Nfc.Services;

/// <summary>
/// Leitura de tags NFC/RFID HF (13,56 MHz). Registre como Singleton: o
/// leitor NFC e um recurso global do aparelho.
/// </summary>
public interface INfcService
{
    /// <summary>
    /// Disparado numa thread de background - use
    /// MainThread.BeginInvokeOnMainThread para tocar na UI.
    /// </summary>
    event EventHandler<NfcTagEventArgs>? TagRead;

    event EventHandler<NfcErrorEventArgs>? NfcError;

    /// <summary>O aparelho tem NFC.</summary>
    bool IsSupported { get; }

    /// <summary>O NFC esta ligado nas configuracoes do aparelho.</summary>
    bool IsEnabled { get; }

    /// <summary>
    /// Comeca a ler tags enquanto a Activity atual estiver em primeiro
    /// plano. Chame em OnAppearing.
    /// </summary>
    void Start();

    /// <summary>Para a leitura. Chame em OnDisappearing.</summary>
    void Stop();
}
