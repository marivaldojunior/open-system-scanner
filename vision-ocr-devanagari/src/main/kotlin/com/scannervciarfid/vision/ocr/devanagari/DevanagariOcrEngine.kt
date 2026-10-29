package com.scannervciarfid.vision.ocr.devanagari

import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.scannervciarfid.vision.VisionEngineAnalyzer
import com.scannervciarfid.vision.VisionEngineProvider
import com.scannervciarfid.vision.VisionEngineRegistry
import com.scannervciarfid.vision.ocr.OcrTextEngine

/**
 * OCR em devanagari (hindi, marathi, nepali...). Usa o mesmo OcrTextEngine (mesmo JSON, format
 * VISION_OCR) com o modelo do script.
 */
object DevanagariOcrEngine {

    /** Id usado no VisionEngineRegistry e pelas pontes MAUI/Flutter. */
    const val ENGINE_ID = "ocr-devanagari"

    @JvmStatic
    fun create(): OcrTextEngine = OcrTextEngine(DevanagariTextRecognizerOptions.Builder().build())

    /** Registra este motor no VisionEngineRegistry. Pode ser chamado mais de uma vez. */
    @JvmStatic
    fun register() {
        VisionEngineRegistry.register(ENGINE_ID, object : VisionEngineProvider {
            override fun create(): VisionEngineAnalyzer = DevanagariOcrEngine.create()
        })
    }
}
