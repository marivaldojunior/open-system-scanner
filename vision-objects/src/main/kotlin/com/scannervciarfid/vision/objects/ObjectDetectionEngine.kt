package com.scannervciarfid.vision.objects

import android.graphics.Rect
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.scannervciarfid.core.ScanResult
import com.scannervciarfid.core.ScannerListener
import com.scannervciarfid.vision.BoundingBoxGraphic
import com.scannervciarfid.vision.GraphicOverlay
import com.scannervciarfid.vision.ScanResultThrottle
import com.scannervciarfid.vision.VisionEngineAnalyzer
import com.scannervciarfid.vision.VisionEngineProvider
import com.scannervciarfid.vision.VisionEngineRegistry
import org.json.JSONArray
import org.json.JSONObject

/**
 * Detecta e rastreia objetos no preview (ate 5 por frame). ScanResult.value
 * e um JSON {"objects": [{trackingId, boundingBox, labels: [{text,
 * confidence}]}]}. As categorias do modelo padrao sao grosseiras
 * (Fashion good, Food, Home good, Place, Plant) - para produtos ou caixas
 * especificos, use um modelo customizado.
 *
 * Um resultado so e emitido quando o conjunto de objetos rastreados muda,
 * no maximo uma vez por segundo.
 */
class ObjectDetectionEngine : VisionEngineAnalyzer {

    private val detector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )
    private var scannerListener: ScannerListener? = null
    private var graphicOverlay: GraphicOverlay? = null
    private val throttle = ScanResultThrottle(
        duplicateWindowMillis = DUPLICATE_WINDOW_MS,
        minIntervalMillis = MIN_EMIT_INTERVAL_MS
    )

    override fun setScannerListener(listener: ScannerListener?) {
        this.scannerListener = listener
    }

    override fun setGraphicOverlay(overlay: GraphicOverlay?) {
        this.graphicOverlay = overlay
    }

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        graphicOverlay?.clear()

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(inputImage)
            .addOnSuccessListener { objects -> handleObjects(objects, imageProxy) }
            .addOnFailureListener { exception ->
                scannerListener?.onScanError("MLKit object detection failed: ${exception.message}")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun handleObjects(objects: List<DetectedObject>, imageProxy: ImageProxy) {
        drawBoundingBoxes(objects, imageProxy)

        if (objects.isEmpty()) {
            return
        }

        // A posicao muda a cada frame; o que importa e quais objetos estao no quadro.
        val signature = objects.joinToString(",") { "${it.trackingId}:${it.labels.firstOrNull()?.text}" }
        if (!throttle.shouldEmit(signature)) {
            return
        }

        val result = ScanResult(
            value = objects.toPayloadJson(),
            format = "VISION_OBJECTS",
            sourceEngine = SOURCE_ENGINE
        )
        scannerListener?.onScanSuccess(result)
    }

    private fun drawBoundingBoxes(objects: List<DetectedObject>, imageProxy: ImageProxy) {
        val overlay = graphicOverlay ?: return

        overlay.setImageSourceInfo(imageProxy.width, imageProxy.height, imageProxy.imageInfo.rotationDegrees)

        for (detectedObject in objects) {
            overlay.add(BoundingBoxGraphic(overlay, detectedObject.boundingBox))
        }

        overlay.postInvalidate()
    }

    private fun List<DetectedObject>.toPayloadJson(): String {
        val objectsArray = JSONArray()
        for (detectedObject in this) {
            val labels = JSONArray()
            for (label in detectedObject.labels) {
                labels.put(JSONObject().put("text", label.text).put("confidence", label.confidence.toDouble()))
            }
            objectsArray.put(
                JSONObject()
                    .put("trackingId", detectedObject.trackingId ?: JSONObject.NULL)
                    .put("boundingBox", detectedObject.boundingBox.toJson())
                    .put("labels", labels)
            )
        }
        return JSONObject().put("objects", objectsArray).toString()
    }

    private fun Rect.toJson(): JSONObject = JSONObject()
        .put("left", left)
        .put("top", top)
        .put("right", right)
        .put("bottom", bottom)

    companion object {
        /** Id usado no VisionEngineRegistry e pelas pontes MAUI/Flutter. */
        const val ENGINE_ID = "objects"

        /** Registra este motor no VisionEngineRegistry. Pode ser chamado mais de uma vez. */
        @JvmStatic
        fun register() {
            VisionEngineRegistry.register(ENGINE_ID, object : VisionEngineProvider {
                override fun create(): VisionEngineAnalyzer = ObjectDetectionEngine()
            })
        }

        private const val SOURCE_ENGINE = "MLKit_Objects"
        private const val DUPLICATE_WINDOW_MS = 1500L
        private const val MIN_EMIT_INTERVAL_MS = 1000L
    }
}
