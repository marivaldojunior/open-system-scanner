package com.scannervciarfid.hardware

import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * Escuta o leitor dos coletores Honeywell (CT40, CT60, EDA51...) pela
 * Intent API do servico de data collection. Diferente do DataWedge, a
 * Honeywell exige "reivindicar" o leitor: start() envia CLAIM_SCANNER com
 * as propriedades do perfil (saida por Intent com a nossa action) e stop()
 * devolve o leitor com RELEASE_SCANNER. Sem o claim, o leitor continua na
 * saida padrao do aparelho (teclado) e o broadcast nunca chega.
 *
 * Os nomes abaixo seguem o sample de Intent API da Honeywell. Alguns
 * firmwares usam outros valores - nesse caso, configure a saida "Data
 * intent" no perfil do aparelho e use HardwareScannerFactory.createGeneric().
 */
class HoneywellIntentScanner : GenericIntentScanner(
    intentAction = ACTION_SCAN_OUTPUT,
    dataExtraKey = EXTRA_DATA
) {

    override val sourceEngine: String = "Honeywell_Intent"

    private var appContext: Context? = null

    override fun configureDevice(context: Context) {
        appContext = context
    }

    override fun start() {
        val context = appContext ?: return
        val properties = Bundle().apply {
            putBoolean("DPR_DATA_INTENT", true)
            putString("DPR_DATA_INTENT_ACTION", ACTION_SCAN_OUTPUT)
        }
        context.sendBroadcast(
            Intent(ACTION_CLAIM_SCANNER)
                .setPackage(DATA_COLLECTION_SERVICE_PACKAGE)
                .putExtra(EXTRA_SCANNER, SCANNER_INTERNAL_IMAGER)
                .putExtra(EXTRA_PROFILE, PROFILE_NAME)
                .putExtra(EXTRA_PROPERTIES, properties)
        )
    }

    override fun stop() {
        val context = appContext ?: return
        context.sendBroadcast(
            Intent(ACTION_RELEASE_SCANNER).setPackage(DATA_COLLECTION_SERVICE_PACKAGE)
        )
    }

    override fun release() {
        stop()
        appContext = null
        super.release()
    }

    companion object {
        private const val ACTION_SCAN_OUTPUT = "com.scannervciarfid.honeywell.SCAN"
        private const val EXTRA_DATA = "data"

        private const val DATA_COLLECTION_SERVICE_PACKAGE = "com.intermec.datacollectionservice"
        private const val ACTION_CLAIM_SCANNER = "com.honeywell.aidc.action.ACTION_CLAIM_SCANNER"
        private const val ACTION_RELEASE_SCANNER = "com.honeywell.aidc.action.ACTION_RELEASE_SCANNER"
        private const val EXTRA_SCANNER = "com.honeywell.aidc.extra.EXTRA_SCANNER"
        private const val EXTRA_PROFILE = "com.honeywell.aidc.extra.EXTRA_PROFILE"
        private const val EXTRA_PROPERTIES = "com.honeywell.aidc.extra.EXTRA_PROPERTIES"
        private const val SCANNER_INTERNAL_IMAGER = "dcs.scanner.imager"
        private const val PROFILE_NAME = "ScannerVcIaRfid"
    }
}
