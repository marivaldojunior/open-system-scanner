package com.opensystemscanner.vision

import android.graphics.Rect
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.opensystemscanner.core.ScanResult
import com.opensystemscanner.core.ScannerListener
import org.json.JSONArray
import org.json.JSONObject

/**
 * Motor de OCR generico (nao ligado a codigo de barras). Como ScanResult.value
 * eh sempre String (contrato classico, sem generics/objetos complexos para
 * facilitar o binding com C#/Dart), o resultado estruturado do ML Kit
 * (blocos + bounding boxes) eh serializado para uma string JSON usando
 * org.json - ja disponivel na plataforma Android, sem dependencia extra
 * como Gson/Moshi.
 */
class OcrTextEngine : VisionEngineAnalyzer {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
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

        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText -> handleTextResult(visionText, imageProxy) }
            .addOnFailureListener { exception ->
                scannerListener?.onScanError("MLKit OCR processing failed: ${exception.message}")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun handleTextResult(visionText: Text, imageProxy: ImageProxy) {
        drawBoundingBoxes(visionText, imageProxy)

        if (visionText.text.isEmpty() || !throttle.shouldEmit(visionText.text)) {
            return
        }

        val result = ScanResult(
            value = visionText.toPayloadJson(),
            format = "VISION_OCR",
            sourceEngine = SOURCE_ENGINE
        )
        scannerListener?.onScanSuccess(result)
    }

    private fun drawBoundingBoxes(visionText: Text, imageProxy: ImageProxy) {
        val overlay = graphicOverlay ?: return

        overlay.setImageSourceInfo(imageProxy.width, imageProxy.height, imageProxy.imageInfo.rotationDegrees)

        for (block in visionText.textBlocks) {
            val boundingBox = block.boundingBox ?: continue
            overlay.add(BoundingBoxGraphic(overlay, boundingBox))
        }

        overlay.postInvalidate()
    }

    private fun Text.toPayloadJson(): String {
        val blocksArray = JSONArray()

        for (block in textBlocks) {
            val blockJson = JSONObject().apply {
                put("text", block.text)
                block.boundingBox?.let { put("boundingBox", it.toJson()) }
            }
            blocksArray.put(blockJson)
        }

        return JSONObject().apply {
            put("fullText", text)
            put("blocks", blocksArray)
        }.toString()
    }

    private fun Rect.toJson(): JSONObject = JSONObject().apply {
        put("left", left)
        put("top", top)
        put("right", right)
        put("bottom", bottom)
    }

    private companion object {
        const val SOURCE_ENGINE = "MLKit_OCR"
        const val DUPLICATE_WINDOW_MS = 1500L

        // O texto reconhecido varia um pouco a cada frame (ruido do OCR), entao
        // so o filtro de duplicados nao basta - limitamos tambem a frequencia.
        const val MIN_EMIT_INTERVAL_MS = 1000L
    }
}
