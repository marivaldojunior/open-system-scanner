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

    /// <summary>
    /// Grava um registro Text na proxima tag aproximada (exige <see cref="Start"/>
    /// ativo). O Task conclui com a tag gravada. Lanca
    /// <see cref="InvalidOperationException"/> se a gravacao falhar e
    /// <see cref="TaskCanceledException"/> apos <see cref="CancelWrite"/>.
    /// </summary>
    Task<NfcTag> WriteTextAsync(string text, string languageCode = "pt");

    /// <summary>Grava um registro URI (ex.: "https://...") na proxima tag aproximada.</summary>
    Task<NfcTag> WriteUriAsync(string uri);

    /// <summary>Desarma a gravacao pendente. A proxima tag volta a gerar TagRead.</summary>
    void CancelWrite();
}
