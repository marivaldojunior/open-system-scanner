package com.scannervciarfid.flutter.rfid

import android.content.Context
import com.scannervciarfid.rfid.RfidListener
import com.scannervciarfid.rfid.RfidReader
import com.scannervciarfid.rfid.RfidReaderFactory
import com.scannervciarfid.rfid.RfidTag
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/**
 * Liga o RfidReader a um EventChannel (um evento por lote de tags) e a um
 * MethodChannel (inicio/fim do inventario). O leitor e inicializado quando o
 * Dart escuta o stream e so e liberado quando o plugin sai do engine - mesmo
 * padrao do leitor de codigo de barras do plugin principal.
 */
class ScannerVcIaRfidRfidPlugin :
    FlutterPlugin,
    EventChannel.StreamHandler,
    MethodChannel.MethodCallHandler {

    private val reader: RfidReader? = RfidReaderFactory.create()
    private var applicationContext: Context? = null
    private var initialized = false
    private var eventSink: EventChannel.EventSink? = null
    private var eventChannel: EventChannel? = null
    private var methodChannel: MethodChannel? = null

    // O RfidTagBuffer ja entrega na main thread - o EventSink exige isso.
    private val rfidListener = object : RfidListener {
        override fun onTagsRead(tags: Array<RfidTag>) {
            eventSink?.success(tags.map { it.toFlutterMap() })
        }

        override fun onRfidError(error: String) {
            eventSink?.error("RFID_ERROR", error, null)
        }
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = binding.applicationContext
        eventChannel = EventChannel(binding.binaryMessenger, EVENT_CHANNEL).apply {
            setStreamHandler(this@ScannerVcIaRfidRfidPlugin)
        }
        methodChannel = MethodChannel(binding.binaryMessenger, METHOD_CHANNEL).apply {
            setMethodCallHandler(this@ScannerVcIaRfidRfidPlugin)
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        if (initialized) {
            reader?.setRfidListener(null)
            reader?.release()
            initialized = false
        }
        eventChannel?.setStreamHandler(null)
        eventChannel = null
        methodChannel?.setMethodCallHandler(null)
        methodChannel = null
        applicationContext = null
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "isAvailable" -> result.success(reader != null)
            "startInventory" -> withReadyReader(result) {
                it.startInventory()
                result.success(null)
            }
            "stopInventory" -> withReadyReader(result) {
                it.stopInventory()
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    private inline fun withReadyReader(result: MethodChannel.Result, action: (RfidReader) -> Unit) {
        val currentReader = reader
        when {
            currentReader == null ->
                result.error("RFID_UNAVAILABLE", "Este aparelho nao tem leitor RFID suportado.", null)
            !initialized ->
                result.error("NOT_LISTENING", "Escute onTagsRead antes de iniciar o inventario.", null)
            else -> action(currentReader)
        }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
        eventSink = events
        val context = applicationContext ?: return
        val currentReader = reader ?: return
        if (!initialized) {
            currentReader.initialize(context)
            initialized = true
        }
        currentReader.setRfidListener(rfidListener)
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
        if (initialized) {
            reader?.stopInventory()
            reader?.setRfidListener(null)
        }
    }

    private fun RfidTag.toFlutterMap(): Map<String, Any?> = mapOf(
        "epc" to epc,
        "rssi" to if (rssi == RfidTag.RSSI_UNKNOWN) null else rssi,
        "readCount" to readCount,
        "sourceEngine" to sourceEngine
    )

    private companion object {
        const val EVENT_CHANNEL = "com.scannervciarfid/rfid/tags"
        const val METHOD_CHANNEL = "com.scannervciarfid/rfid/methods"
    }
}
