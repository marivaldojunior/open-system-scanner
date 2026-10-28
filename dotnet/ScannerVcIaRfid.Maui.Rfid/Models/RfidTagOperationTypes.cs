namespace ScannerVcIaRfid.Maui.Rfid.Models;

/// <summary>
/// Bancos de memoria EPC Gen2. Os valores seguem RfidMemoryBank (Kotlin).
/// </summary>
public enum RfidMemoryBank
{
    Reserved = 0,
    Epc = 1,
    Tid = 2,
    User = 3
}

/// <summary>Campos de lock. Os valores seguem RfidLockField (Kotlin).</summary>
public enum RfidLockField
{
    KillPassword = 0,
    AccessPassword = 1,
    Epc = 2,
    Tid = 3,
    User = 4
}

/// <summary>Privilegios de lock. Os valores seguem RfidLockPrivilege (Kotlin).</summary>
public enum RfidLockPrivilege
{
    Unlock = 0,

    /// <summary>Bloqueia escrita (e leitura, nos campos de senha) sem a access password.</summary>
    Lock = 1,

    /// <summary>Bloqueio permanente. Nao tem volta.</summary>
    PermaLock = 2,

    /// <summary>Desbloqueio permanente. Nao tem volta.</summary>
    PermaUnlock = 3
}

public sealed class RfidLocateEventArgs(string epc, int relativeDistance) : EventArgs
{
    public string Epc { get; } = epc;

    /// <summary>0 (longe) a 100 (encostado no leitor).</summary>
    public int RelativeDistance { get; } = relativeDistance;
}
