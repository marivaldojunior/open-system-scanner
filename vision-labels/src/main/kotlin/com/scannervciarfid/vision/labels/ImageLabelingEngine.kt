package com.scannervciarfid.vision.labels

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.scannervciarfid.core.ScanResult
import com.scannervciarfid.core.ScannerListener
import com.scannervciarfid.vision.GraphicOverlay
import com.scannervciarfid.vision.ScanResultThrottle
import com.scannervciarfid.vision.VisionEngineAnalyzer
import com.scannervciarfid.vision.VisionEngineProvider
import com.scannervciarfid.vision.VisionEngineRegistry
import org.json.JSONArray
import org.json.JSONObject

/**
 * Descreve a cena inteira com rotulos genericos em ingles (~400, ex.:
 * "Box", "Shelf", "Vehicle"). ScanResult.value e um JSON {"labels":
 * [{text, confidence, index}]}, so com confianca >= 70%. Nao desenha no
 * overlay: os rotulos valem para o frame inteiro, nao para uma regiao.
 *
 * Um resultado so e emitido quando o conjunto de rotulos muda, no maximo
 * uma vez por segundo.
 */
class ImageLabelingEngine : VisionEngineAnalyzer {

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(MIN_CONFIDENCE)
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

        // Limpa caixas deixadas por um motor anterior (troca de motor com a camera ligada).
        graphicOverlay?.let {
            it.clear()
            it.postInvalidate()
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        labeler.process(inputImage)
            .addOnSuccessListener { labels -> handleLabels(labels) }
            .addOnFailureListener { exception ->
                scannerListener?.onScanError("MLKit image labeling failed: ${exception.message}")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun handleLabels(labels: List<ImageLabel>) {
        if (labels.isEmpty()) {
            return
        }

        val signature = labels.map { it.text }.sorted().joinToString(",")
        if (!throttle.shouldEmit(signature)) {
            return
        }

        val labelsArray = JSONArray()
        for (label in labels) {
            labelsArray.put(
                JSONObject()
                    .put("text", label.text)
                    .put("confidence", label.confidence.toDouble())
                    .put("index", label.index)
            )
        }

        val result = ScanResult(
            value = JSONObject().put("labels", labelsArray).toString(),
            format = "VISION_LABELS",
            sourceEngine = SOURCE_ENGINE
        )
        scannerListener?.onScanSuccess(result)
    }

    companion object {
        /** Id usado no VisionEngineRegistry e pelas pontes MAUI/Flutter. */
        const val ENGINE_ID = "labels"

        /** Registra este motor no VisionEngineRegistry. Pode ser chamado mais de uma vez. */
        @JvmStatic
        fun register() {
            VisionEngineRegistry.register(ENGINE_ID, object : VisionEngineProvider {
                override fun create(): VisionEngineAnalyzer = ImageLabelingEngine()
            })
        }

        private const val SOURCE_ENGINE = "MLKit_Labels"
        private const val MIN_CONFIDENCE = 0.7f
        private const val DUPLICATE_WINDOW_MS = 1500L
        private const val MIN_EMIT_INTERVAL_MS = 1000L
    }
}
