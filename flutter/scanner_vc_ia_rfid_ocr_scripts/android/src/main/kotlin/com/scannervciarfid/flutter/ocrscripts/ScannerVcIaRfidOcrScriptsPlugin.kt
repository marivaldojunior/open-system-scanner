package com.scannervciarfid.flutter.ocrscripts

import com.scannervciarfid.vision.ocr.chinese.ChineseOcrEngine
import com.scannervciarfid.vision.ocr.devanagari.DevanagariOcrEngine
import com.scannervciarfid.vision.ocr.japanese.JapaneseOcrEngine
import com.scannervciarfid.vision.ocr.korean.KoreanOcrEngine
import io.flutter.embedding.engine.plugins.FlutterPlugin

/**
 * So registra os motores no VisionEngineRegistry nativo. O
 * VisionScannerWidget do plugin principal busca o motor pelo id, entao
 * engine: 'ocr-chinese' / 'ocr-japanese' / 'ocr-korean' / 'ocr-devanagari'
 * passa a funcionar sem mudar o plugin principal.
 */
class ScannerVcIaRfidOcrScriptsPlugin : FlutterPlugin {

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        ChineseOcrEngine.register()
        JapaneseOcrEngine.register()
        KoreanOcrEngine.register()
        DevanagariOcrEngine.register()
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) = Unit
}
