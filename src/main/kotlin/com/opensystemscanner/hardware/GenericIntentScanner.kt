package com.opensystemscanner.hardware

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.opensystemscanner.core.ScanResult
import com.opensystemscanner.core.ScannerListener
import com.opensystemscanner.core.UniversalScanner

open class GenericIntentScanner @JvmOverloads constructor(
    private val intentAction: String,
    private val dataExtraKey: String,
    // Alguns fabricantes (ex.: Datalogic Intent Wedge) enviam o broadcast com
    // uma categoria. Um IntentFilter so casa se contiver TODAS as categorias
    // do Intent - sem declarar a categoria aqui, o broadcast nunca chega.
    private val intentCategory: String? = null
) : UniversalScanner {

    protected open val sourceEngine: String = "Generic_Intent"

    private var appContext: Context? = null
    private var scannerListener: ScannerListener? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val data = intent.getStringExtra(dataExtraKey)
            if (data.isNullOrEmpty()) {
                scannerListener?.onScanError(
                    "Broadcast received for action '$intentAction' but extra '$dataExtraKey' was empty or missing."
                )
                return
            }

            val result = ScanResult(
                value = data,
                format = "UNKNOWN",
                sourceEngine = sourceEngine
            )
            scannerListener?.onScanSuccess(result)
        }
    }

    override fun initialize(context: Context) {
        appContext = context.applicationContext
        val filter = IntentFilter(intentAction)
        intentCategory?.let { filter.addCategory(it) }
        ContextCompat.registerReceiver(
            appContext!!,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        configureDevice(appContext!!)
    }

    /**
     * Ponto de extensao para adapters que precisam configurar o servico do
     * fabricante (ex.: criar o perfil DataWedge da Zebra) para que ele envie
     * as leituras com a mesma action/extra registradas em initialize().
     */
    protected open fun configureDevice(context: Context) {
        // No-op: o leitor generico depende de configuracao manual no aparelho.
    }

    override fun start() {
        // No-op: capture is triggered by the device's physical scan button/laser.
    }

    override fun stop() {
        // No-op: capture is triggered by the device's physical scan button/laser.
    }

    override fun release() {
        appContext?.unregisterReceiver(receiver)
        appContext = null
    }

    override fun setScannerListener(listener: ScannerListener?) {
        this.scannerListener = listener
    }
}
