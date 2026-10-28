package com.scannervciarfid.vision.faces

import android.graphics.Rect
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
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
 * Detecta rostos no preview. Detecta, nao reconhece: o modelo nao sabe de
 * quem e o rosto. ScanResult.value e um JSON {"faces": [{trackingId,
 * boundingBox, smilingProbability, leftEyeOpenProbability,
 * rightEyeOpenProbability, headEulerAngleY, headEulerAngleZ}]}. As
 * probabilidades sao null quando o modelo nao consegue estimar.
 *
 * Um resultado so e emitido quando o conjunto de rostos rastreados muda, no
 * maximo uma vez por segundo.
 */
class FaceDetectionEngine : VisionEngineAnalyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
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
            .addOnSuccessListener { faces -> handleFaces(faces, imageProxy) }
            .addOnFailureListener { exception ->
                scannerListener?.onScanError("MLKit face detection failed: ${exception.message}")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun handleFaces(faces: List<Face>, imageProxy: ImageProxy) {
        drawBoundingBoxes(faces, imageProxy)

        if (faces.isEmpty()) {
            return
        }

        val signature = faces.map { it.trackingId }.sortedBy { it ?: -1 }.joinToString(",")
        if (!throttle.shouldEmit(signature)) {
            return
        }

        val result = ScanResult(
            value = faces.toPayloadJson(),
            format = "VISION_FACES",
            sourceEngine = SOURCE_ENGINE
        )
        scannerListener?.onScanSuccess(result)
    }

    private fun drawBoundingBoxes(faces: List<Face>, imageProxy: ImageProxy) {
        val overlay = graphicOverlay ?: return

        overlay.setImageSourceInfo(imageProxy.width, imageProxy.height, imageProxy.imageInfo.rotationDegrees)

        for (face in faces) {
            overlay.add(BoundingBoxGraphic(overlay, face.boundingBox))
        }

        overlay.postInvalidate()
    }

    private fun List<Face>.toPayloadJson(): String {
        val facesArray = JSONArray()
        for (face in this) {
            facesArray.put(
                JSONObject()
                    .put("trackingId", face.trackingId ?: JSONObject.NULL)
                    .put("boundingBox", face.boundingBox.toJson())
                    .put("smilingProbability", face.smilingProbability.orNull())
                    .put("leftEyeOpenProbability", face.leftEyeOpenProbability.orNull())
                    .put("rightEyeOpenProbability", face.rightEyeOpenProbability.orNull())
                    .put("headEulerAngleY", face.headEulerAngleY.toDouble())
                    .put("headEulerAngleZ", face.headEulerAngleZ.toDouble())
            )
        }
        return JSONObject().put("faces", facesArray).toString()
    }

    private fun Float?.orNull(): Any = this?.toDouble() ?: JSONObject.NULL

    private fun Rect.toJson(): JSONObject = JSONObject()
        .put("left", left)
        .put("top", top)
        .put("right", right)
        .put("bottom", bottom)

    companion object {
        /** Id usado no VisionEngineRegistry e pelas pontes MAUI/Flutter. */
        const val ENGINE_ID = "faces"

        /** Registra este motor no VisionEngineRegistry. Pode ser chamado mais de uma vez. */
        @JvmStatic
        fun register() {
            VisionEngineRegistry.register(ENGINE_ID, object : VisionEngineProvider {
                override fun create(): VisionEngineAnalyzer = FaceDetectionEngine()
            })
        }

        private const val SOURCE_ENGINE = "MLKit_Faces"
        private const val DUPLICATE_WINDOW_MS = 1500L
        private const val MIN_EMIT_INTERVAL_MS = 1000L
    }
}
