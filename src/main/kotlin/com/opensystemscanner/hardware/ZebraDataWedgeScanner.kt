package com.opensystemscanner.hardware

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
) {

    override val sourceEngine: String = "Zebra_DataWedge"

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
            putString("PROFILE_NAME", "OpenSystemScanner_${context.packageName}")
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
        private const val ACTION_SCAN_OUTPUT = "com.opensystemscanner.zebra.SCAN"
        private const val EXTRA_DATAWEDGE_DATA_STRING = "com.symbol.datawedge.data_string"

        private const val DATAWEDGE_PACKAGE = "com.symbol.datawedge"
        private const val ACTION_DATAWEDGE_API = "com.symbol.datawedge.api.ACTION"
        private const val EXTRA_SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG"
        private const val INTENT_DELIVERY_BROADCAST = "2"
    }
}
