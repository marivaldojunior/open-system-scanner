package com.scannervciarfid.rfid

/**
 * Callbacks dos leitores RFID. As tags chegam em lote (ver RfidTagBuffer):
 * um evento por tag sobrecarregaria a ponte JNI do MAUI e o EventChannel
 * do Flutter num inventario com centenas de tags por segundo.
 *
 * Os callbacks rodam na main thread.
 */
interface RfidListener {
    fun onTagsRead(tags: Array<RfidTag>)
    fun onRfidError(error: String)
}
