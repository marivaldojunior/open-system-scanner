package com.scannervciarfid.ai.language

/**
 * Resultado de uma chamada do LanguageAiClient. O resultado e sempre
 * String (texto, codigo de idioma ou JSON), pelo mesmo motivo do
 * ScanResult: o binding C#/Dart le sem conversao.
 *
 * Os callbacks rodam na main thread.
 */
interface AiTextListener {
    fun onSuccess(result: String)
    fun onError(error: String)
}
