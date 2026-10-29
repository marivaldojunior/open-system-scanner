package com.scannervciarfid.hardware

import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * Escuta as leituras do DataWedge da Zebra. A action de saida NAO e
 * com.symbol.datawedge.api.RESULT_ACTION (essa action so carrega as
 * respostas dos comandos da API DataWedge, nunca uma leitura). O DataWedge
 * envia a leitura com a action configurada no plugin INTENT do perfil
 * associado ao app - por isso configureDevice() cria/atualiza esse perfil
 * via SET_CONFIG, apontando a saida para ACTION_SCAN_OUTPUT.
 */
class ZebraDataWedgeScanner : GenericIntentScanner(
    intentAction = ACTION_SCAN_OUTPUT,
    dataExtraKey = EXTRA_DATAWEDGE_DATA_STRING
), SoftTriggerScanner, SymbologyConfigurableScanner {

    override val sourceEngine: String = "Zebra_DataWedge"

    // O modulo rfid (ZebraDataWedgeRfidReader) usa o mesmo perfil e a mesma
    // action: as tags RFID chegam aqui com source "rfid" e sao descartadas.
    override fun accepts(intent: Intent): Boolean =
        intent.getStringExtra(EXTRA_DATAWEDGE_SOURCE) != SOURCE_RFID

    // O DataWedge envia a simbologia como "LABEL-TYPE-EAN13", "LABEL-TYPE-QRCODE"...
    override fun readFormat(intent: Intent): String =
        intent.getStringExtra(EXTRA_DATAWEDGE_LABEL_TYPE)?.removePrefix(LABEL_TYPE_PREFIX) ?: "UNKNOWN"

    override fun startSoftScan() = sendApiCommand(EXTRA_SOFT_SCAN_TRIGGER, SOFT_SCAN_START)

    override fun stopSoftScan() = sendApiCommand(EXTRA_SOFT_SCAN_TRIGGER, SOFT_SCAN_STOP)

    /**
     * Atualiza so os decoders informados no perfil do app (CONFIG_MODE
     * UPDATE, sem RESET_CONFIG): os outros parametros do BARCODE continuam.
     */
    override fun configureSymbologies(enabled: Array<String>, disabled: Array<String>) {
        val context = applicationContext ?: return
        val params = Bundle().apply {
            enabled.forEach { putString(DECODER_PREFIX + it.lowercase(), "true") }
            disabled.forEach { putString(DECODER_PREFIX + it.lowercase(), "false") }
        }
        val barcodePlugin = Bundle().apply {
            putString("PLUGIN_NAME", "BARCODE")
            putString("RESET_CONFIG", "false")
            putBundle("PARAM_LIST", params)
        }
        val profileConfig = Bundle().apply {
            putString("PROFILE_NAME", profileName(context))
            putString("PROFILE_ENABLED", "true")
            putString("CONFIG_MODE", "UPDATE")
            putBundle("PLUGIN_CONFIG", barcodePlugin)
        }
        applicationContext?.sendBroadcast(
            Intent(ACTION_DATAWEDGE_API)
                .setPackage(DATAWEDGE_PACKAGE)
                .putExtra(EXTRA_SET_CONFIG, profileConfig)
        )
    }

    private fun sendApiCommand(extra: String, value: String) {
        applicationContext?.sendBroadcast(
            Intent(ACTION_DATAWEDGE_API).setPackage(DATAWEDGE_PACKAGE).putExtra(extra, value)
        )
    }

    private fun profileName(context: Context) = PROFILE_PREFIX + context.packageName

    override fun configureDevice(context: Context) {
        val barcodePlugin = Bundle().apply {
            putString("PLUGIN_NAME", "BARCODE")
            putString("RESET_CONFIG", "true")
            putBundle("PARAM_LIST", Bundle().apply {
                putString("scanner_input_enabled", "true")
            })
        }

        val intentPlugin = Bundle().apply {
            putString("PLUGIN_NAME", "INTENT")
            putString("RESET_CONFIG", "true")
            putBundle("PARAM_LIST", Bundle().apply {
                putString("intent_output_enabled", "true")
                putString("intent_action", ACTION_SCAN_OUTPUT)
                putString("intent_delivery", INTENT_DELIVERY_BROADCAST)
            })
        }

        // Sem desligar o KEYSTROKE, o DataWedge tambem "digitaria" a leitura
        // no campo de texto com foco, duplicando o dado no app.
        val keystrokePlugin = Bundle().apply {
            putString("PLUGIN_NAME", "KEYSTROKE")
            putString("RESET_CONFIG", "true")
            putBundle("PARAM_LIST", Bundle().apply {
                putString("keystroke_output_enabled", "false")
            })
        }

        val appConfig = Bundle().apply {
            putString("PACKAGE_NAME", context.packageName)
            putStringArray("ACTIVITY_LIST", arrayOf("*"))
        }

        val profileConfig = Bundle().apply {
            putString("PROFILE_NAME", profileName(context))
            putString("PROFILE_ENABLED", "true")
            putString("CONFIG_MODE", "CREATE_IF_NOT_EXIST")
            putParcelableArray("APP_LIST", arrayOf(appConfig))
            putParcelableArrayList("PLUGIN_CONFIG", arrayListOf(barcodePlugin, intentPlugin, keystrokePlugin))
        }

        context.sendBroadcast(
            Intent(ACTION_DATAWEDGE_API)
                .setPackage(DATAWEDGE_PACKAGE)
                .putExtra(EXTRA_SET_CONFIG, profileConfig)
        )
    }

    companion object {
        private const val ACTION_SCAN_OUTPUT = "com.scannervciarfid.zebra.SCAN"
        private const val EXTRA_DATAWEDGE_DATA_STRING = "com.symbol.datawedge.data_string"
        private const val EXTRA_DATAWEDGE_LABEL_TYPE = "com.symbol.datawedge.label_type"
        private const val EXTRA_DATAWEDGE_SOURCE = "com.symbol.datawedge.source"
        private const val SOURCE_RFID = "rfid"
        private const val LABEL_TYPE_PREFIX = "LABEL-TYPE-"

        private const val DATAWEDGE_PACKAGE = "com.symbol.datawedge"
        private const val ACTION_DATAWEDGE_API = "com.symbol.datawedge.api.ACTION"
        private const val EXTRA_SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG"
        private const val EXTRA_SOFT_SCAN_TRIGGER = "com.symbol.datawedge.api.SOFT_SCAN_TRIGGER"
        private const val SOFT_SCAN_START = "START_SCANNING"
        private const val SOFT_SCAN_STOP = "STOP_SCANNING"
        private const val DECODER_PREFIX = "decoder_"
        private const val PROFILE_PREFIX = "ScannerVcIaRfid_"
        private const val INTENT_DELIVERY_BROADCAST = "2"
    }
}
