package com.scannervciarfid.flutter.visionextras

import com.scannervciarfid.vision.faces.FaceDetectionEngine
import com.scannervciarfid.vision.labels.ImageLabelingEngine
import com.scannervciarfid.vision.objects.ObjectDetectionEngine
import io.flutter.embedding.engine.plugins.FlutterPlugin

/**
 * So registra os motores no VisionEngineRegistry nativo. O
 * VisionScannerWidget do plugin principal busca o motor pelo id, entao
 * engine: 'objects' / 'labels' / 'faces' passa a funcionar sem mudar o
 * plugin principal.
 */
class ScannerVcIaRfidVisionExtrasPlugin : FlutterPlugin {

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        ObjectDetectionEngine.register()
        ImageLabelingEngine.register()
        FaceDetectionEngine.register()
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) = Unit
}
