using ScannerVcIaRfid.Maui.Rfid.Models;

namespace ScannerVcIaRfid.Maui.Rfid.Services;

/// <summary>
/// Leitor RFID UHF (EPC Gen2). Registre como Singleton: o leitor e um
/// recurso global do aparelho.
/// </summary>
public interface IRfidReaderService
{
    /// <summary>
    /// Lote de tags lidas (cada EPC uma vez por lote). Disparado na main thread.
    /// </summary>
    event EventHandler<RfidTagsEventArgs>? TagsRead;

    event EventHandler<RfidErrorEventArgs>? RfidError;

    /// <summary>O aparelho tem um leitor RFID suportado.</summary>
    bool IsAvailable { get; }

    /// <summary>Idempotente - seguro chamar a cada OnAppearing.</summary>
    void Initialize();

    /// <summary>Comeca o inventario por software (sem o gatilho fisico).</summary>
    void StartInventory();

    void StopInventory();

    void Release();
}
