package com.scannervciarfid.flutter

import android.app.Activity
import com.scannervciarfid.codescanner.CodeScannerListener
import com.scannervciarfid.codescanner.GoogleCodeScanner
import com.scannervciarfid.core.ScanResult
import com.scannervciarfid.hardware.HardwareScannerFactory
import com.scannervciarfid.vision.barcode.BarcodeEngine
import com.scannervciarfid.vision.ocr.OcrTextEngine
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/**
 * ActivityAware porque o Google Code Scanner abre uma Activity propria e
 * precisa da Activity atual do Flutter (o applicationContext nao serve).
 */
class ScannerVcIaRfidPlugin : FlutterPlugin, ActivityAware, MethodChannel.MethodCallHandler {

    private var eventChannel: EventChannel? = null
    private var hardwareStreamHandler: HardwareScannerStreamHandler? = null
    private var methodChannel: MethodChannel? = null
    private var activity: Activity? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        val hardwareScanner = HardwareScannerFactory.create()

        val streamHandler = HardwareScannerStreamHandler(
            applicationContext = binding.applicationContext,
            hardwareScanner = hardwareScanner
        )
        hardwareStreamHandler = streamHandler

        eventChannel = EventChannel(binding.binaryMessenger, HARDWARE_EVENT_CHANNEL).apply {
            setStreamHandler(streamHandler)
        }

        methodChannel = MethodChannel(binding.binaryMessenger, METHOD_CHANNEL).apply {
            setMethodCallHandler(this@ScannerVcIaRfidPlugin)
        }

        // O VisionScannerWidget escolhe o motor pelo id ("barcode", "ocr").
        BarcodeEngine.register()
        OcrTextEngine.register()

        binding.platformViewRegistry.registerViewFactory(
            CAMERA_VIEW_TYPE,
            VisionScannerViewFactory(binding.binaryMessenger)
        )
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        eventChannel?.setStreamHandler(null)
        eventChannel = null

        methodChannel?.setMethodCallHandler(null)
        methodChannel = null

        hardwareStreamHandler?.release()
        hardwareStreamHandler = null
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "useGenericIntent" -> useGenericIntent(call, result)
            "scanWithCodeScanner" -> scanWithCodeScanner(result)
            else -> result.notImplemented()
        }
    }

    private fun useGenericIntent(call: MethodCall, result: MethodChannel.Result) {
        val intentAction = call.argument<String>("intentAction")
        val dataExtraKey = call.argument<String>("dataExtraKey")
        if (intentAction.isNullOrEmpty() || dataExtraKey.isNullOrEmpty()) {
            result.error("INVALID_ARGUMENT", "intentAction e dataExtraKey sao obrigatorios.", null)
            return
        }

        val scanner = HardwareScannerFactory.createGeneric(
            intentAction,
            dataExtraKey,
            call.argument<String>("intentCategory")
        )
        hardwareStreamHandler?.replaceScanner(scanner)
        result.success(null)
    }

    private fun scanWithCodeScanner(result: MethodChannel.Result) {
        val currentActivity = activity
        if (currentActivity == null) {
            result.error("NO_ACTIVITY", "Nenhuma Activity ativa para abrir o Google Code Scanner.", null)
            return
        }

        GoogleCodeScanner.scan(currentActivity, object : CodeScannerListener {
            override fun onScanSuccess(scanResult: ScanResult) = result.success(scanResult.toFlutterMap())

            // null no Dart = cancelado pelo usuario (nao e erro).
            override fun onScanCanceled() = result.success(null)

            override fun onScanError(error: String) = result.error("SCAN_ERROR", error, null)
        })
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    companion object {
        private const val HARDWARE_EVENT_CHANNEL = "com.scannervciarfid/hardware"
        private const val METHOD_CHANNEL = "com.scannervciarfid/methods"
        const val CAMERA_VIEW_TYPE = "com.scannervciarfid/camera_view"
    }
}
