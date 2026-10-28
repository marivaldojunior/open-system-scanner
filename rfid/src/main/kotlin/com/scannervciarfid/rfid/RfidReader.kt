package com.scannervciarfid.rfid

import android.content.Context

/**
 * Contrato comum dos leitores RFID UHF. Mesmo estilo do UniversalScanner:
 * metodos classicos e listener, sem coroutines na API publica.
 *
 * Operacoes por tag (ler/gravar bancos de memoria, lock, kill, localizar
 * por RSSI) dependem do SDK do fabricante e entram junto com os modulos
 * desses SDKs.
 */
interface RfidReader {
    fun initialize(context: Context)

    /** Comeca o inventario por software (sem o gatilho fisico). */
    fun startInventory()

    fun stopInventory()

    fun release()

    fun setRfidListener(listener: RfidListener?)
}
