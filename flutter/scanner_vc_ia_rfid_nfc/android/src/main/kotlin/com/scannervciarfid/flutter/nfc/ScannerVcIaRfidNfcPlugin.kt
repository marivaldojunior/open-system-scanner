package com.scannervciarfid.flutter.nfc

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.scannervciarfid.nfc.NfcTagData
import com.scannervciarfid.nfc.NfcTagListener
import com.scannervciarfid.nfc.NfcTagReader
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/**
 * Liga o NfcTagReader a um EventChannel. A leitura comeca quando o Dart
 * escuta o stream e ja existe uma Activity (o Reader Mode exige a Activity),
 * e para quando a subscription e cancelada.
 */
class ScannerVcIaRfidNfcPlugin :
    FlutterPlugin,
    ActivityAware,
    EventChannel.StreamHandler,
    MethodChannel.MethodCallHandler {

    private val reader = NfcTagReader()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var applicationContext: Context? = null
    private var activity: Activity? = null
    private var eventSink: EventChannel.EventSink? = null
    private var eventChannel: EventChannel? = null
    private var methodChannel: MethodChannel? = null

    // O NfcTagReader chama numa thread do binder; o EventSink so aceita a main thread.
    private val tagListener = object : NfcTagListener {
        override fun onTagRead(tag: NfcTagData) {
            mainHandler.post { eventSink?.success(tag.toFlutterMap()) }
        }

        override fun onNfcError(error: String) {
            mainHandler.post { eventSink?.error("NFC_ERROR", error, null) }
        }
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = binding.applicationContext
        eventChannel = EventChannel(binding.binaryMessenger, EVENT_CHANNEL).apply {
            setStreamHandler(this@ScannerVcIaRfidNfcPlugin)
        }
        methodChannel = MethodChannel(binding.binaryMessenger, METHOD_CHANNEL).apply {
            setMethodCallHandler(this@ScannerVcIaRfidNfcPlugin)
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        reader.stop()
        reader.setNfcTagListener(null)
        eventChannel?.setStreamHandler(null)
        eventChannel = null
        methodChannel?.setMethodCallHandler(null)
        methodChannel = null
        applicationContext = null
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        val context = applicationContext
        if (context == null) {
            result.error("NOT_ATTACHED", "Plugin desanexado do engine.", null)
            return
        }
        when (call.method) {
            "isSupported" -> result.success(NfcTagReader.isSupported(context))
            "isEnabled" -> result.success(NfcTagReader.isEnabled(context))
            else -> result.notImplemented()
        }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
        eventSink = events
        reader.setNfcTagListener(tagListener)
        startIfReady()
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
        reader.stop()
        reader.setNfcTagListener(null)
    }

    private fun startIfReady() {
        val currentActivity = activity ?: return
        if (eventSink != null) {
            reader.start(currentActivity)
        }
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        startIfReady()
    }

    override fun onDetachedFromActivityForConfigChanges() = onDetachedFromActivity()

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) =
        onAttachedToActivity(binding)

    override fun onDetachedFromActivity() {
        reader.stop()
        activity = null
    }

    private fun NfcTagData.toFlutterMap(): Map<String, Any> = mapOf(
        "uid" to uid,
        "technologies" to technologies.split(',').filter { it.isNotEmpty() },
        "ndefRecordsJson" to ndefRecordsJson
    )

    private companion object {
        const val EVENT_CHANNEL = "com.scannervciarfid/nfc/tags"
        const val METHOD_CHANNEL = "com.scannervciarfid/nfc/methods"
    }
}
