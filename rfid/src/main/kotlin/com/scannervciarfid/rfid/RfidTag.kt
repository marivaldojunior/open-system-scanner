package com.scannervciarfid.rfid

/**
 * Tag UHF lida num inventario. Tipos simples, pelo mesmo motivo do
 * ScanResult: o binding C#/Dart le sem conversao.
 *
 * - [epc]: EPC em hexadecimal maiusculo.
 * - [rssi]: maior RSSI (dBm) da tag na janela de entrega, ou [RSSI_UNKNOWN]
 *   quando o leitor nao informa (ex.: DataWedge).
 * - [readCount]: quantas vezes a tag foi lida na janela de entrega.
 * - [sourceEngine]: leitor de origem (ex.: "Zebra_DataWedge_RFID").
 */
data class RfidTag(
    val epc: String,
    val rssi: Int,
    val readCount: Int,
    val sourceEngine: String
) {
    companion object {
        const val RSSI_UNKNOWN = Int.MIN_VALUE
    }
}
