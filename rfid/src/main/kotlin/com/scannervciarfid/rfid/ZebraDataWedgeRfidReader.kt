package com.scannervciarfid.rfid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.core.content.ContextCompat

/**
 * Leitor RFID UHF pelo plugin "RFID Input" do DataWedge (MC3300R, RFD40,
 * RFD90, TC53E, EM45, ET6X). So faz inventario: o DataWedge entrega o EPC,
 * sem RSSI e sem acesso aos bancos de memoria. Para ler/gravar tags, use um
 * leitor com o SDK RFID da Zebra.
 *
 * O DataWedge associa um unico perfil ao app. Por isso este leitor usa o
 * mesmo perfil e a mesma action de saida do ZebraDataWedgeScanner
 * (barcode-hardware) e filtra pelo extra source == "rfid". O SET_CONFIG
 * daqui so mexe nos plugins RFID e INTENT - a configuracao de codigo de
 * barras do perfil continua como esta.
 */
class ZebraDataWedgeRfidReader : RfidReader {

    private val buffer = RfidTagBuffer(SOURCE_ENGINE)
    private var appContext: Context? = null
    private var listener: RfidListener? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getStringExtra(EXTRA_SOURCE) != SOURCE_RFID) {
                return
            }
            // Um ciclo de leitura pode trazer varias tags, uma por linha.
            intent.getStringExtra(EXTRA_DATA_STRING)
                ?.lineSequence()
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.forEach { buffer.add(it) }
        }
    }

    override fun initialize(context: Context) {
        if (appContext != null) {
            return
        }
        val app = context.applicationContext
        appContext = app
        ContextCompat.registerReceiver(
            app,
            receiver,
            IntentFilter(ACTION_SCAN_OUTPUT),
            ContextCompat.RECEIVER_EXPORTED
        )
        configureProfile(app)
    }

    override fun startInventory() = sendSoftTrigger(TRIGGER_START)

    override fun stopInventory() = sendSoftTrigger(TRIGGER_STOP)

    override fun release() {
        val app = appContext ?: return
        app.unregisterReceiver(receiver)
        buffer.clear()
        appContext = null
    }

    override fun setRfidListener(listener: RfidListener?) {
        this.listener = listener
        buffer.setRfidListener(listener)
    }

    private fun sendSoftTrigger(command: String) {
        val app = appContext
        if (app == null) {
            listener?.onRfidError("Chame initialize() antes de iniciar o inventario.")
            return
        }
        app.sendBroadcast(
            Intent(ACTION_DATAWEDGE_API)
                .setPackage(DATAWEDGE_PACKAGE)
                .putExtra(EXTRA_SOFT_RFID_TRIGGER, command)
        )
    }

    private fun configureProfile(context: Context) {
        val rfidPlugin = Bundle().apply {
            putString("PLUGIN_NAME", "RFID")
            putString("RESET_CONFIG", "true")
            putBundle("PARAM_LIST", Bundle().apply {
                putString("rfid_input_enabled", "true")
                putString("rfid_hardware_trigger_enabled", "true")
                // O RfidTagBuffer ja agrupa repeticoes; filtrar no DataWedge
                // reduz o numero de broadcasts no inventario.
                putString("rfid_filter_duplicate_tags", "true")
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

        val appConfig = Bundle().apply {
            putString("PACKAGE_NAME", context.packageName)
            putStringArray("ACTIVITY_LIST", arrayOf("*"))
        }

        val profileConfig = Bundle().apply {
            putString("PROFILE_NAME", "ScannerVcIaRfid_${context.packageName}")
            putString("PROFILE_ENABLED", "true")
            putString("CONFIG_MODE", "CREATE_IF_NOT_EXIST")
            putParcelableArray("APP_LIST", arrayOf(appConfig))
            putParcelableArrayList("PLUGIN_CONFIG", arrayListOf(rfidPlugin, intentPlugin))
        }

        context.sendBroadcast(
            Intent(ACTION_DATAWEDGE_API)
                .setPackage(DATAWEDGE_PACKAGE)
                .putExtra(EXTRA_SET_CONFIG, profileConfig)
        )
    }

    companion object {
        // Precisa ser igual ao ACTION_SCAN_OUTPUT do ZebraDataWedgeScanner:
        // o perfil DataWedge tem uma unica saida Intent.
        private const val ACTION_SCAN_OUTPUT = "com.scannervciarfid.zebra.SCAN"
        private const val EXTRA_DATA_STRING = "com.symbol.datawedge.data_string"
        private const val EXTRA_SOURCE = "com.symbol.datawedge.source"
        private const val SOURCE_RFID = "rfid"
        private const val SOURCE_ENGINE = "Zebra_DataWedge_RFID"

        private const val DATAWEDGE_PACKAGE = "com.symbol.datawedge"
        private const val ACTION_DATAWEDGE_API = "com.symbol.datawedge.api.ACTION"
        private const val EXTRA_SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG"
        private const val EXTRA_SOFT_RFID_TRIGGER = "com.symbol.datawedge.api.SOFT_RFID_TRIGGER"
        private const val INTENT_DELIVERY_BROADCAST = "2"

        // STOP_SCANNING exige DataWedge 11.3 ou superior.
        private const val TRIGGER_START = "START_SCANNING"
        private const val TRIGGER_STOP = "STOP_SCANNING"
    }
}
