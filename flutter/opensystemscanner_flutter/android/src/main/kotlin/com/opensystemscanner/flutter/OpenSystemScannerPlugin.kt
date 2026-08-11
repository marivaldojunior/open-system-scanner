package com.opensystemscanner.flutter

import com.opensystemscanner.DataCaptureFactory
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel

class OpenSystemScannerPlugin : FlutterPlugin {

    private var eventChannel: EventChannel? = null
    private var hardwareStreamHandler: HardwareScannerStreamHandler? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        val hardwareScanner = DataCaptureFactory.getHardwareScanner()

        val streamHandler = HardwareScannerStreamHandler(
            applicationContext = binding.applicationContext,
            hardwareScanner = hardwareScanner
        )
        hardwareStreamHandler = streamHandler

        eventChannel = EventChannel(binding.binaryMessenger, HARDWARE_EVENT_CHANNEL).apply {
            setStreamHandler(streamHandler)
        }

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
        private const val HARDWARE_EVENT_CHANNEL = "com.opensystemscanner/hardware"
        const val CAMERA_VIEW_TYPE = "com.opensystemscanner/camera_view"
    }
}
