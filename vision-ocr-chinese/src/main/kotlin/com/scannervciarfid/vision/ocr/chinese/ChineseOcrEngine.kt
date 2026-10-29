package com.scannervciarfid.vision.ocr.chinese

import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.scannervciarfid.vision.VisionEngineAnalyzer
import com.scannervciarfid.vision.VisionEngineProvider
import com.scannervciarfid.vision.VisionEngineRegistry
import com.scannervciarfid.vision.ocr.OcrTextEngine

/**
 * OCR em chines (simplificado e tradicional). Usa o mesmo OcrTextEngine (mesmo JSON, format
 * VISION_OCR) com o modelo do script.
 */
object ChineseOcrEngine {

    /** Id usado no VisionEngineRegistry e pelas pontes MAUI/Flutter. */
    const val ENGINE_ID = "ocr-chinese"

    @JvmStatic
    fun create(): OcrTextEngine = OcrTextEngine(ChineseTextRecognizerOptions.Builder().build())

    /** Registra este motor no VisionEngineRegistry. Pode ser chamado mais de uma vez. */
    @JvmStatic
    fun register() {
        VisionEngineRegistry.register(ENGINE_ID, object : VisionEngineProvider {
            override fun create(): VisionEngineAnalyzer = ChineseOcrEngine.create()
        })
    }
}
