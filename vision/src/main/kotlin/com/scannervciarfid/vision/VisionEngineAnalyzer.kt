package com.scannervciarfid.vision

import androidx.camera.core.ImageAnalysis
import com.scannervciarfid.core.ScannerListener

/**
 * Contrato Strategy para os motores de analise de frame plugaveis na
 * VisionScannerView. Cada implementacao decide o que fazer com cada
 * ImageProxy (barcode, OCR, ou futuros motores de visao) e reporta o
 * resultado atraves do mesmo ScannerListener classico usado no restante
 * da biblioteca - nenhuma API nova precisa ser aprendida pelo MAUI/Flutter
 * para suportar um motor adicional.
 */
interface VisionEngineAnalyzer : ImageAnalysis.Analyzer {
    fun setScannerListener(listener: ScannerListener?)
    fun setGraphicOverlay(overlay: GraphicOverlay?)
}
