package com.scannervciarfid.flutter

import android.content.Context
import com.scannervciarfid.core.ScanResult
import com.scannervciarfid.core.ScannerListener
import com.scannervciarfid.core.UniversalScanner
import io.flutter.plugin.common.EventChannel

/**
 * Faz a ponte entre o UniversalScanner nativo (hardware) e um EventChannel do
 * Flutter. onListen/onCancel seguem o ciclo de vida da subscription do lado
 * Dart: quando o app cancela o stream, paramos o leitor mas nao chamamos
 * release() - isso só acontece quando o plugin inteiro é desanexado
 * (ver ScannerVcIaRfidPlugin.onDetachedFromEngine).
 */
internal class HardwareScannerStreamHandler(
    private val applicationContext: Context,
    private var hardwareScanner: UniversalScanner
) : EventChannel.StreamHandler {

    private var initialized = false
    private var eventSink: EventChannel.EventSink? = null

    /**
     * Troca o leitor (ex.: Intent generico configurado pelo app). Se o Dart
     * ja escuta o stream, o leitor novo assume a mesma subscription.
     */
    fun replaceScanner(scanner: UniversalScanner) {
        val sink = eventSink
        release()
        hardwareScanner = scanner
        if (sink != null) {
            onListen(null, sink)
        }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
        eventSink = events
        if (!initialized) {
            hardwareScanner.initialize(applicationContext)
            initialized = true
        }

        hardwareScanner.setScannerListener(object : ScannerListener {
            override fun onScanSuccess(result: ScanResult) {
                events.success(result.toFlutterMap())
            }

            override fun onScanError(error: String) {
                events.error("SCAN_ERROR", error, null)
            }
        })
        hardwareScanner.start()
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
        hardwareScanner.setScannerListener(null)
        hardwareScanner.stop()
    }

    fun release() {
        hardwareScanner.setScannerListener(null)
        hardwareScanner.stop()
        hardwareScanner.release()
        initialized = false
    }
}

internal fun ScanResult.toFlutterMap(): Map<String, String> = mapOf(
    "value" to value,
    "format" to format,
    "sourceEngine" to sourceEngine
)
