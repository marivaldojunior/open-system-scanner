package com.opensystemscanner.vision

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.opensystemscanner.core.ScanResult
import com.opensystemscanner.core.ScannerListener

class BarcodeEngine : VisionEngineAnalyzer {

    private val barcodeScanner = BarcodeScanning.getClient()
    private var scannerListener: ScannerListener? = null
    private var graphicOverlay: GraphicOverlay? = null
    private val throttle = ScanResultThrottle(duplicateWindowMillis = DUPLICATE_WINDOW_MS)

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

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes -> handleBarcodes(barcodes, imageProxy) }
            .addOnFailureListener { exception ->
                scannerListener?.onScanError("MLKit barcode processing failed: ${exception.message}")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun handleBarcodes(barcodes: List<Barcode>, imageProxy: ImageProxy) {
        drawBoundingBoxes(barcodes, imageProxy)

        val barcode = barcodes.firstOrNull() ?: return
        val rawValue = barcode.rawValue ?: return
        if (!throttle.shouldEmit(rawValue)) {
            return
        }

        val result = ScanResult(
            value = rawValue,
            format = barcodeFormatName(barcode.format),
            sourceEngine = SOURCE_ENGINE
        )
        scannerListener?.onScanSuccess(result)
    }

    private fun drawBoundingBoxes(barcodes: List<Barcode>, imageProxy: ImageProxy) {
        val overlay = graphicOverlay ?: return

        overlay.setImageSourceInfo(imageProxy.width, imageProxy.height, imageProxy.imageInfo.rotationDegrees)

        for (barcode in barcodes) {
            val boundingBox = barcode.boundingBox ?: continue
            overlay.add(BoundingBoxGraphic(overlay, boundingBox))
        }

        overlay.postInvalidate()
    }

    private fun barcodeFormatName(format: Int): String = when (format) {
        Barcode.FORMAT_CODE_128 -> "CODE_128"
        Barcode.FORMAT_CODE_39 -> "CODE_39"
        Barcode.FORMAT_CODE_93 -> "CODE_93"
        Barcode.FORMAT_CODABAR -> "CODABAR"
        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
        Barcode.FORMAT_EAN_13 -> "EAN_13"
        Barcode.FORMAT_EAN_8 -> "EAN_8"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        Barcode.FORMAT_UPC_A -> "UPC_A"
        Barcode.FORMAT_UPC_E -> "UPC_E"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_AZTEC -> "AZTEC"
        else -> "UNKNOWN"
    }

    private companion object {
        const val SOURCE_ENGINE = "MLKit_Barcode"
        const val DUPLICATE_WINDOW_MS = 1500L
    }
}
