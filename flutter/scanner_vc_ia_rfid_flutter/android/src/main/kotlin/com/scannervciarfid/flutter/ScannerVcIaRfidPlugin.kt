package com.scannervciarfid.flutter

import com.scannervciarfid.hardware.HardwareScannerFactory
import com.scannervciarfid.vision.barcode.BarcodeEngine
import com.scannervciarfid.vision.ocr.OcrTextEngine
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel

class ScannerVcIaRfidPlugin : FlutterPlugin {

    private var eventChannel: EventChannel? = null
    private var hardwareStreamHandler: HardwareScannerStreamHandler? = null

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

        hardwareStreamHandler?.release()
        hardwareStreamHandler = null
    }

    companion object {
        private const val HARDWARE_EVENT_CHANNEL = "com.scannervciarfid/hardware"
        const val CAMERA_VIEW_TYPE = "com.scannervciarfid/camera_view"
    }
}
