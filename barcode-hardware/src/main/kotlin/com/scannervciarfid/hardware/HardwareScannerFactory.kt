package com.scannervciarfid.hardware

import android.os.Build
import com.scannervciarfid.core.UniversalScanner

/**
 * Ponto de entrada do modulo barcode-hardware. Escolhe o adapter pelo
 * fabricante do aparelho - o app nunca precisa saber qual coletor esta
 * por tras do UniversalScanner.
 */
object HardwareScannerFactory {

    private const val FALLBACK_INTENT_ACTION = "com.scannervciarfid.action.UNCONFIGURED_SCAN"
    private const val FALLBACK_DATA_EXTRA_KEY = "com.scannervciarfid.extra.UNCONFIGURED_SCAN_DATA"

    @JvmStatic
    fun create(): UniversalScanner {
        val manufacturer = Build.MANUFACTURER.lowercase()

        return when {
            manufacturer.contains("zebra") -> ZebraDataWedgeScanner()
            manufacturer.contains("datalogic") -> DatalogicIntentScanner()
            manufacturer.contains("honeywell") -> HoneywellIntentScanner()
            manufacturer.contains("newland") -> NewlandIntentScanner()
            manufacturer.contains("urovo") -> UrovoIntentScanner()
            else -> GenericIntentScanner(
                intentAction = FALLBACK_INTENT_ACTION,
                dataExtraKey = FALLBACK_DATA_EXTRA_KEY
            )
        }
    }

    /**
     * Leitor para qualquer coletor que envie a leitura por broadcast
     * (Chainway, Sunmi, Seuic...). Use os mesmos valores configurados na
     * saida Intent do aparelho (ex.: app Keyboard Emulator / InfoWedge).
     */
    @JvmStatic
    @JvmOverloads
    fun createGeneric(
        intentAction: String,
        dataExtraKey: String,
        intentCategory: String? = null
    ): UniversalScanner = GenericIntentScanner(intentAction, dataExtraKey, intentCategory)
}
