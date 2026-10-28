package com.scannervciarfid.rfid

/**
 * Operacoes por tag e ajustes do leitor. Separado de RfidReader porque nem
 * todo leitor suporta: o ZebraDataWedgeRfidReader so faz inventario. As
 * pontes testam `reader is RfidTagOperations`.
 *
 * Pare o inventario antes de uma operacao: o leitor nao executa as duas
 * coisas ao mesmo tempo.
 *
 * Senhas sao 8 digitos hexadecimais ("00000000" = sem senha). Dados de
 * memoria sao hexadecimal, com tamanho em palavras de 16 bits (4 digitos
 * por palavra). Os resultados chegam na main thread.
 */
interface RfidTagOperations {

    /**
     * Le [wordCount] palavras do banco [memoryBank] (ver RfidMemoryBank),
     * a partir de [wordOffset]. Resultado: os dados em hexadecimal.
     */
    fun readMemory(
        epc: String,
        memoryBank: Int,
        wordOffset: Int,
        wordCount: Int,
        accessPassword: String,
        listener: RfidOperationListener
    )

    /** Grava [dataHex] (numero par de bytes) no banco a partir de [wordOffset]. */
    fun writeMemory(
        epc: String,
        memoryBank: Int,
        wordOffset: Int,
        dataHex: String,
        accessPassword: String,
        listener: RfidOperationListener
    )

    /** Aplica [privilege] (RfidLockPrivilege) ao campo [field] (RfidLockField). */
    fun lock(
        epc: String,
        field: Int,
        privilege: Int,
        accessPassword: String,
        listener: RfidOperationListener
    )

    /**
     * Desativa a tag para sempre. Exige uma kill password diferente de zero
     * gravada na tag. Nao tem volta.
     */
    fun kill(epc: String, killPassword: String, listener: RfidOperationListener)

    /**
     * Localiza a tag [epc]: o leitor passa a emitir a distancia relativa
     * (0 a 100) no RfidLocateListener, ate stopLocate().
     */
    fun startLocate(epc: String)

    fun stopLocate()

    fun setLocateListener(listener: RfidLocateListener?)

    /**
     * Ajusta a potencia de transmissao para o nivel suportado mais proximo de
     * [dbm]. Resultado: a potencia aplicada em dBm (ex.: "27.0").
     */
    fun setTransmitPower(dbm: Int, listener: RfidOperationListener)

    /** Sessao Gen2 de 0 a 3 (S0 a S3). */
    fun setSession(session: Int, listener: RfidOperationListener)
}

/** Resultado de uma operacao. [result] e vazio quando a operacao nao retorna dados. */
interface RfidOperationListener {
    fun onSuccess(result: String)
    fun onError(error: String)
}

interface RfidLocateListener {
    /** [relativeDistance]: 0 (longe) a 100 (encostado no leitor). */
    fun onTagLocated(epc: String, relativeDistance: Int)
}

/** Bancos de memoria EPC Gen2 (mesma numeracao da norma). */
object RfidMemoryBank {
    const val RESERVED = 0
    const val EPC = 1
    const val TID = 2
    const val USER = 3
}

object RfidLockField {
    const val KILL_PASSWORD = 0
    const val ACCESS_PASSWORD = 1
    const val EPC = 2
    const val TID = 3
    const val USER = 4
}

object RfidLockPrivilege {
    const val UNLOCK = 0

    /** Bloqueia escrita (e leitura, nos campos de senha) sem a access password. */
    const val LOCK = 1

    /** Bloqueio permanente. Nao tem volta. */
    const val PERMA_LOCK = 2

    /** Desbloqueio permanente. Nao tem volta. */
    const val PERMA_UNLOCK = 3
}
