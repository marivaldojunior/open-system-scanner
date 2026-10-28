package com.scannervciarfid.nfc

/**
 * Callbacks do NfcTagReader. onTagRead roda numa thread do binder do
 * sistema - quem atualiza UI precisa trocar para a main thread.
 */
interface NfcTagListener {
    fun onTagRead(tag: NfcTagData)
    fun onNfcError(error: String)
}
