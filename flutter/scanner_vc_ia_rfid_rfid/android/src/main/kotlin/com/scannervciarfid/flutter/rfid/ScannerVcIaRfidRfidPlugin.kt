package com.scannervciarfid.flutter.rfid

import android.content.Context
import com.scannervciarfid.rfid.RfidListener
import com.scannervciarfid.rfid.RfidLocateListener
import com.scannervciarfid.rfid.RfidOperationListener
import com.scannervciarfid.rfid.RfidPermissions
import com.scannervciarfid.rfid.RfidReader
import com.scannervciarfid.rfid.RfidReaderFactory
import com.scannervciarfid.rfid.RfidReaderStatusListener
import com.scannervciarfid.rfid.RfidTag
import com.scannervciarfid.rfid.RfidTagOperations
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/**
 * Liga o RfidReader a dois EventChannels (lotes de tags e localizacao) e a
 * um MethodChannel (inventario e operacoes por tag). O leitor e inicializado
 * quando o Dart escuta o stream de tags e so e liberado quando o plugin sai
 * do engine - mesmo padrao do leitor de codigo de barras do plugin principal.
 */
class ScannerVcIaRfidRfidPlugin :
    FlutterPlugin,
    EventChannel.StreamHandler,
    MethodChannel.MethodCallHandler {

    private val reader: RfidReader? = RfidReaderFactory.create()

    // So o leitor do SDK Zebra RFID API3 suporta operacoes por tag.
    private val operations: RfidTagOperations? = reader as? RfidTagOperations

    private var applicationContext: Context? = null
    private var initialized = false
    private var eventSink: EventChannel.EventSink? = null
    private var locateSink: EventChannel.EventSink? = null
    private var batterySink: EventChannel.EventSink? = null
    private var batteryChannel: EventChannel? = null
    private var eventChannel: EventChannel? = null
    private var locateChannel: EventChannel? = null
    private var methodChannel: MethodChannel? = null

    // O RfidTagBuffer e o leitor API3 ja entregam na main thread - o
    // EventSink exige isso.
    private val rfidListener = object : RfidListener {
        override fun onTagsRead(tags: Array<RfidTag>) {
            eventSink?.success(tags.map { it.toFlutterMap() })
        }

        override fun onRfidError(error: String) {
            eventSink?.error("RFID_ERROR", error, null)
        }
    }

    private val locateListener = object : RfidLocateListener {
        override fun onTagLocated(epc: String, relativeDistance: Int) {
            locateSink?.success(mapOf("epc" to epc, "relativeDistance" to relativeDistance))
        }
    }

    private val statusListener = object : RfidReaderStatusListener {
        override fun onBatteryStatus(levelPercent: Int, charging: Boolean) {
            batterySink?.success(mapOf("levelPercent" to levelPercent, "charging" to charging))
        }
    }

    private val batteryStreamHandler = object : EventChannel.StreamHandler {
        override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
            batterySink = events
        }

        override fun onCancel(arguments: Any?) {
            batterySink = null
        }
    }

    // Stream separado para a localizacao: a tela de busca escuta so ele.
    private val locateStreamHandler = object : EventChannel.StreamHandler {
        override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
            locateSink = events
        }

        override fun onCancel(arguments: Any?) {
            locateSink = null
        }
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = binding.applicationContext
        eventChannel = EventChannel(binding.binaryMessenger, EVENT_CHANNEL).apply {
            setStreamHandler(this@ScannerVcIaRfidRfidPlugin)
        }
        locateChannel = EventChannel(binding.binaryMessenger, LOCATE_CHANNEL).apply {
            setStreamHandler(locateStreamHandler)
        }
        batteryChannel = EventChannel(binding.binaryMessenger, BATTERY_CHANNEL).apply {
            setStreamHandler(batteryStreamHandler)
        }
        methodChannel = MethodChannel(binding.binaryMessenger, METHOD_CHANNEL).apply {
            setMethodCallHandler(this@ScannerVcIaRfidRfidPlugin)
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        if (initialized) {
            reader?.setRfidListener(null)
            operations?.setLocateListener(null)
            operations?.setReaderStatusListener(null)
            reader?.release()
            initialized = false
        }
        eventChannel?.setStreamHandler(null)
        eventChannel = null
        locateChannel?.setStreamHandler(null)
        locateChannel = null
        batteryChannel?.setStreamHandler(null)
        batteryChannel = null
        methodChannel?.setMethodCallHandler(null)
        methodChannel = null
        applicationContext = null
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "isAvailable" -> result.success(reader != null)
            "supportsTagOperations" -> result.success(operations != null)
            // So o leitor do SDK API3 usa Bluetooth (sleds pareados).
            "requiredBluetoothPermissions" -> result.success(
                if (operations == null) emptyList() else RfidPermissions.requiredRuntimePermissions().toList()
            )
            "startInventory" -> withReadyReader(result) {
                it.startInventory()
                result.success(null)
            }
            "stopInventory" -> withReadyReader(result) {
                it.stopInventory()
                result.success(null)
            }
            "readMemory" -> withOperations(result) {
                it.readMemory(
                    call.requireString("epc"),
                    call.argument<Int>("memoryBank") ?: 0,
                    call.argument<Int>("wordOffset") ?: 0,
                    call.argument<Int>("wordCount") ?: 0,
                    call.password("accessPassword"),
                    result.asOperationListener()
                )
            }
            "writeMemory" -> withOperations(result) {
                it.writeMemory(
                    call.requireString("epc"),
                    call.argument<Int>("memoryBank") ?: 0,
                    call.argument<Int>("wordOffset") ?: 0,
                    call.requireString("dataHex"),
                    call.password("accessPassword"),
                    result.asOperationListener()
                )
            }
            "lock" -> withOperations(result) {
                it.lock(
                    call.requireString("epc"),
                    call.argument<Int>("field") ?: 0,
                    call.argument<Int>("privilege") ?: 0,
                    call.password("accessPassword"),
                    result.asOperationListener()
                )
            }
            "kill" -> withOperations(result) {
                it.kill(call.requireString("epc"), call.password("killPassword"), result.asOperationListener())
            }
            "setTransmitPower" -> withOperations(result) {
                it.setTransmitPower(call.argument<Int>("dbm") ?: 0, result.asOperationListener())
            }
            "setSession" -> withOperations(result) {
                it.setSession(call.argument<Int>("session") ?: 0, result.asOperationListener())
            }
            "setInventoryFilter" -> withOperations(result) {
                it.setInventoryFilter(
                    call.argument<Int>("memoryBank") ?: 0,
                    call.argument<Int>("bitOffset") ?: 0,
                    call.requireString("patternHex"),
                    result.asOperationListener()
                )
            }
            "clearInventoryFilter" -> withOperations(result) {
                it.clearInventoryFilter(result.asOperationListener())
            }
            "requestBatteryStatus" -> withOperations(result) {
                it.requestBatteryStatus()
                result.success(null)
            }
            "startLocate" -> withOperations(result) {
                it.startLocate(call.requireString("epc"))
                result.success(null)
            }
            "stopLocate" -> withOperations(result) {
                it.stopLocate()
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
                result.error("NOT_LISTENING", "Escute onTagsRead antes de usar o leitor.", null)
            else -> action(currentReader)
        }
    }

    private inline fun withOperations(result: MethodChannel.Result, action: (RfidTagOperations) -> Unit) {
        val currentOperations = operations
        when {
            currentOperations == null -> result.error(
                "NOT_SUPPORTED",
                "O leitor RFID deste aparelho nao suporta operacoes por tag (exige o SDK Zebra RFID API3).",
                null
            )
            !initialized ->
                result.error("NOT_LISTENING", "Escute onTagsRead antes de usar o leitor.", null)
            else -> try {
                action(currentOperations)
            } catch (e: IllegalArgumentException) {
                result.error("INVALID_ARGUMENT", e.message, null)
            }
        }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
        eventSink = events
        val context = applicationContext ?: return
        val currentReader = reader ?: return
        if (!initialized) {
            currentReader.initialize(context)
            operations?.setLocateListener(locateListener)
            operations?.setReaderStatusListener(statusListener)
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

    // Cada operacao responde uma unica vez ao Result do MethodChannel.
    private fun MethodChannel.Result.asOperationListener(): RfidOperationListener =
        object : RfidOperationListener {
            override fun onSuccess(result: String) = this@asOperationListener.success(result)
            override fun onError(error: String) = this@asOperationListener.error("RFID_OPERATION_ERROR", error, null)
        }

    private fun MethodCall.requireString(name: String): String =
        argument<String>(name) ?: throw IllegalArgumentException("O argumento $name e obrigatorio.")

    private fun MethodCall.password(name: String): String = argument<String>(name) ?: "00000000"

    private fun RfidTag.toFlutterMap(): Map<String, Any?> = mapOf(
        "epc" to epc,
        "rssi" to if (rssi == RfidTag.RSSI_UNKNOWN) null else rssi,
        "readCount" to readCount,
        "sourceEngine" to sourceEngine
    )

    private companion object {
        const val EVENT_CHANNEL = "com.scannervciarfid/rfid/tags"
        const val LOCATE_CHANNEL = "com.scannervciarfid/rfid/locate"
        const val BATTERY_CHANNEL = "com.scannervciarfid/rfid/battery"
        const val METHOD_CHANNEL = "com.scannervciarfid/rfid/methods"
    }
}
