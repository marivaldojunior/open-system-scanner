package com.opensystemscanner.hardware

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.opensystemscanner.core.ScanResult
import com.opensystemscanner.core.ScannerListener
import com.opensystemscanner.core.UniversalScanner

open class GenericIntentScanner(
    private val intentAction: String,
    private val dataExtraKey: String
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
        ContextCompat.registerReceiver(
            appContext!!,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
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
