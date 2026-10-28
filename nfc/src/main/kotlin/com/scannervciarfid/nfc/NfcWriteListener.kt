package com.scannervciarfid.nfc

/**
 * Resultado de uma gravacao armada com writeTextOnNextTag/writeUriOnNextTag.
 * Os callbacks rodam numa thread do binder do sistema - quem atualiza UI
 * precisa trocar para a main thread.
 */
interface NfcWriteListener {
    /** [tag] traz o conteudo ja gravado. */
    fun onWriteSuccess(tag: NfcTagData)

    fun onWriteError(error: String)
}
