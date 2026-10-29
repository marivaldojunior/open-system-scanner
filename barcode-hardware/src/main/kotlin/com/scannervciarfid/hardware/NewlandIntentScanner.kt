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
), SoftTriggerScanner {

    override val sourceEngine: String = "Newland_Intent"

    override fun configureDevice(context: Context) {
        context.sendBroadcast(
            Intent(ACTION_SCAN_CONFIG).putExtra(EXTRA_SCAN_MODE, SCAN_MODE_BROADCAST)
        )
    }

    // O Newland para sozinho na primeira leitura ou no timeout.
    override fun startSoftScan() {
        applicationContext?.sendBroadcast(
            Intent(ACTION_SCANNER_TRIGGER)
                .putExtra(EXTRA_SCAN_TIMEOUT, SOFT_SCAN_TIMEOUT_SECONDS)
                .putExtra(EXTRA_SCAN_TYPE, SCAN_TYPE_SINGLE)
        )
    }

    override fun stopSoftScan() {
        applicationContext?.sendBroadcast(Intent(ACTION_SCANNER_STOP))
    }

    companion object {
        private const val ACTION_SCANNER_RESULT = "nlscan.action.SCANNER_RESULT"
        private const val EXTRA_BARCODE = "SCAN_BARCODE1"

        private const val ACTION_SCAN_CONFIG = "ACTION_BAR_SCANCFG"
        private const val EXTRA_SCAN_MODE = "EXTRA_SCAN_MODE"
        private const val SCAN_MODE_BROADCAST = 3

        private const val ACTION_SCANNER_TRIGGER = "nlscan.action.SCANNER_TRIG"
        private const val ACTION_SCANNER_STOP = "nlscan.action.STOP_SCAN"
        private const val EXTRA_SCAN_TIMEOUT = "SCAN_TIMEOUT"
        private const val EXTRA_SCAN_TYPE = "SCAN_TYPE"
        private const val SOFT_SCAN_TIMEOUT_SECONDS = 5
        private const val SCAN_TYPE_SINGLE = 1
    }
}
