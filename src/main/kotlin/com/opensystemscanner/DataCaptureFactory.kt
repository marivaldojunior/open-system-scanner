package com.opensystemscanner

import android.content.Context
import android.os.Build
import android.util.Log
import com.opensystemscanner.core.CaptureMode
import com.opensystemscanner.core.UniversalScanner
import com.opensystemscanner.hardware.DatalogicIntentScanner
import com.opensystemscanner.hardware.GenericIntentScanner
import com.opensystemscanner.hardware.ZebraDataWedgeScanner
import com.opensystemscanner.vision.BarcodeEngine
import com.opensystemscanner.vision.OcrTextEngine
import com.opensystemscanner.vision.VisionEngineAnalyzer
import com.opensystemscanner.vision.VisionScannerView

object DataCaptureFactory {

    private const val TAG = "DataCaptureFactory"
    private const val FALLBACK_INTENT_ACTION = "com.opensystemscanner.action.UNCONFIGURED_SCAN"
    private const val FALLBACK_DATA_EXTRA_KEY = "com.opensystemscanner.extra.UNCONFIGURED_SCAN_DATA"

    @JvmStatic
    fun getHardwareScanner(): UniversalScanner {
        val manufacturer = Build.MANUFACTURER.lowercase()

        return when {
            manufacturer.contains("zebra") -> ZebraDataWedgeScanner()
            manufacturer.contains("datalogic") -> DatalogicIntentScanner()
            else -> GenericIntentScanner(
                intentAction = FALLBACK_INTENT_ACTION,
                dataExtraKey = FALLBACK_DATA_EXTRA_KEY
            )
        }
    }

    @JvmStatic
    fun getCameraScannerView(context: Context, mode: CaptureMode): VisionScannerView {
        val engine: VisionEngineAnalyzer = when (mode) {
            CaptureMode.CAMERA_BARCODE -> BarcodeEngine()
            CaptureMode.CAMERA_VISION -> OcrTextEngine()
            CaptureMode.HARDWARE_BARCODE -> {
                // CaptureMode.HARDWARE_BARCODE nao faz sentido aqui (esse modo
                // e para getHardwareScanner()) - em vez de lancar excecao,
                // caimos para o motor de barcode como fallback seguro,
                // consistente com o fallback ja usado em getHardwareScanner().
                Log.w(
                    TAG,
                    "getCameraScannerView() chamado com CaptureMode.HARDWARE_BARCODE - " +
                        "esse modo e destinado a getHardwareScanner(). Usando BarcodeEngine como fallback."
                )
                BarcodeEngine()
            }
        }

        val view = VisionScannerView(context)
        view.setVisionEngine(engine)
        engine.setGraphicOverlay(view.getGraphicOverlay())
        return view
    }
}
