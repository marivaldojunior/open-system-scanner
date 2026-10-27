package com.scannervciarfid.codescanner

import com.scannervciarfid.core.ScanResult

/**
 * Resultado de uma leitura unica do Google Code Scanner. O cancelamento
 * (usuario fechou a tela) e separado do erro, para o app nao tratar como
 * falha uma acao normal do usuario.
 */
interface CodeScannerListener {
    fun onScanSuccess(result: ScanResult)
    fun onScanCanceled()
    fun onScanError(error: String)
}
