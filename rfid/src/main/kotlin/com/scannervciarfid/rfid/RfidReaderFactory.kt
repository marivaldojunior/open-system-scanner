package com.scannervciarfid.rfid

import android.os.Build

/**
 * Ponto de entrada do modulo rfid. Escolhe o leitor pelo fabricante do
 * aparelho. Hoje so a Zebra (DataWedge) tem leitor; os modulos de SDK de
 * fabricante (rfid-zebra, rfid-chainway...) entram aqui quando existirem.
 */
object RfidReaderFactory {

    /**
     * Retorna null quando o aparelho nao tem um leitor RFID suportado. Em
     * aparelhos Zebra sem RFID (ex.: TC52), o leitor e criado, mas o
     * DataWedge nao entrega tags.
     */
    @JvmStatic
    fun create(): RfidReader? {
        val manufacturer = Build.MANUFACTURER.lowercase()

        return when {
            manufacturer.contains("zebra") -> ZebraDataWedgeRfidReader()
            else -> null
        }
    }
}
