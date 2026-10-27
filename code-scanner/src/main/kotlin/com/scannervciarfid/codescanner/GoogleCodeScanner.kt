package com.scannervciarfid.codescanner

import android.app.Activity
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.scannervciarfid.core.ScanResult

/**
 * Leitura unica de codigo de barras pela tela do Google Play Services. O
 * Google abre a propria Activity de camera e devolve um resultado - sem
 * VisionScannerView, sem permissao de camera no app.
 *
 * Na primeira chamada, o Play Services pode baixar o modulo de leitura; a
 * chamada so retorna quando o download termina.
 */
object GoogleCodeScanner {

    private const val SOURCE_ENGINE = "Google_CodeScanner"

    @JvmStatic
    fun scan(activity: Activity, listener: CodeScannerListener) {
        val options = GmsBarcodeScannerOptions.Builder()
            .enableAutoZoom()
            .build()

        GmsBarcodeScanning.getClient(activity, options)
            .startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue
                if (rawValue == null) {
                    listener.onScanError("Google Code Scanner retornou um codigo sem valor.")
                    return@addOnSuccessListener
                }
                listener.onScanSuccess(
                    ScanResult(
                        value = rawValue,
                        format = barcodeFormatName(barcode.format),
                        sourceEngine = SOURCE_ENGINE
                    )
                )
            }
            .addOnCanceledListener { listener.onScanCanceled() }
            .addOnFailureListener { exception ->
                listener.onScanError("Google Code Scanner falhou: ${exception.message}")
            }
    }

    // Mesmos nomes do BarcodeEngine (vision-barcode). Duplicado de proposito:
    // depender de vision-barcode so por esta tabela embarcaria o modelo do
    // ML Kit, que o Code Scanner nao usa.
    private fun barcodeFormatName(format: Int): String = when (format) {
        Barcode.FORMAT_CODE_128 -> "CODE_128"
        Barcode.FORMAT_CODE_39 -> "CODE_39"
        Barcode.FORMAT_CODE_93 -> "CODE_93"
        Barcode.FORMAT_CODABAR -> "CODABAR"
        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
        Barcode.FORMAT_EAN_13 -> "EAN_13"
        Barcode.FORMAT_EAN_8 -> "EAN_8"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        Barcode.FORMAT_UPC_A -> "UPC_A"
        Barcode.FORMAT_UPC_E -> "UPC_E"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_AZTEC -> "AZTEC"
        else -> "UNKNOWN"
    }
}
