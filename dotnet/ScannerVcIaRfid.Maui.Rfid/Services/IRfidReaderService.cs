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

    /// <summary>
    /// Distancia relativa da tag em localizacao (ver <see cref="StartLocate"/>).
    /// Disparado na main thread.
    /// </summary>
    event EventHandler<RfidLocateEventArgs>? TagLocated;

    /// <summary>O aparelho tem um leitor RFID suportado.</summary>
    bool IsAvailable { get; }

    /// <summary>
    /// O leitor suporta as operacoes por tag (memoria, lock, kill,
    /// localizacao, potencia, sessao). Hoje so o leitor do SDK Zebra RFID
    /// API3; o leitor DataWedge so faz inventario.
    /// </summary>
    bool SupportsTagOperations { get; }

    /// <summary>Idempotente - seguro chamar a cada OnAppearing.</summary>
    void Initialize();

    /// <summary>Comeca o inventario por software (sem o gatilho fisico).</summary>
    void StartInventory();

    void StopInventory();

    void Release();

    // As operacoes abaixo lancam NotSupportedException quando
    // SupportsTagOperations e false, e InvalidOperationException quando o
    // leitor recusa a operacao. Pare o inventario antes de chamar. Senhas sao
    // 8 digitos hexadecimais ("00000000" = sem senha).

    /// <summary>Le palavras de 16 bits do banco. Retorna os dados em hexadecimal.</summary>
    Task<string> ReadMemoryAsync(string epc, RfidMemoryBank bank, int wordOffset, int wordCount, string accessPassword = "00000000");

    /// <summary>Grava <paramref name="dataHex"/> (multiplo de 4 digitos hexadecimais) no banco.</summary>
    Task WriteMemoryAsync(string epc, RfidMemoryBank bank, int wordOffset, string dataHex, string accessPassword = "00000000");

    Task LockAsync(string epc, RfidLockField field, RfidLockPrivilege privilege, string accessPassword);

    /// <summary>
    /// Desativa a tag para sempre. Exige uma kill password diferente de zero
    /// gravada na tag. Nao tem volta.
    /// </summary>
    Task KillAsync(string epc, string killPassword);

    /// <summary>
    /// Ajusta a potencia para o nivel suportado mais proximo. Retorna a
    /// potencia aplicada em dBm.
    /// </summary>
    Task<double> SetTransmitPowerAsync(int dbm);

    /// <summary>Sessao Gen2 de 0 a 3 (S0 a S3).</summary>
    Task SetSessionAsync(int session);

    /// <summary>Comeca a localizar a tag. A distancia chega em <see cref="TagLocated"/>.</summary>
    void StartLocate(string epc);

    void StopLocate();
}
