package com.scannervciarfid.hardware

import android.content.Context
import android.content.Intent

/**
 * Escuta o leitor dos coletores Newland (MT90, NLS-N7...). Por padrao o
 * Newland "digita" a leitura no campo com foco; configureDevice() envia
 * ACTION_BAR_SCANCFG com EXTRA_SCAN_MODE = 3 (saida por API/broadcast),
 * para o resultado chegar em nlscan.action.SCANNER_RESULT.
 */
class NewlandIntentScanner : GenericIntentScanner(
    intentAction = ACTION_SCANNER_RESULT,
    dataExtraKey = EXTRA_BARCODE
) {

    override val sourceEngine: String = "Newland_Intent"

    override fun configureDevice(context: Context) {
        context.sendBroadcast(
            Intent(ACTION_SCAN_CONFIG).putExtra(EXTRA_SCAN_MODE, SCAN_MODE_BROADCAST)
        )
    }

    companion object {
        private const val ACTION_SCANNER_RESULT = "nlscan.action.SCANNER_RESULT"
        private const val EXTRA_BARCODE = "SCAN_BARCODE1"

        private const val ACTION_SCAN_CONFIG = "ACTION_BAR_SCANCFG"
        private const val EXTRA_SCAN_MODE = "EXTRA_SCAN_MODE"
        private const val SCAN_MODE_BROADCAST = 3
    }
}
