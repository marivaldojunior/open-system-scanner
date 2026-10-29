package com.scannervciarfid.sample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.scannervciarfid.ai.language.AiTextListener
import com.scannervciarfid.ai.language.LanguageAiClient
import com.scannervciarfid.codescanner.CodeScannerListener
import com.scannervciarfid.codescanner.GoogleCodeScanner
import com.scannervciarfid.core.ScanResult
import com.scannervciarfid.documentscanner.DocumentScanListener
import com.scannervciarfid.documentscanner.DocumentScanResult
import com.scannervciarfid.documentscanner.GoogleDocumentScanner
import com.scannervciarfid.core.ScannerListener
import com.scannervciarfid.core.UniversalScanner
import com.scannervciarfid.hardware.HardwareScannerFactory
import com.scannervciarfid.nfc.NfcTagData
import com.scannervciarfid.nfc.NfcTagListener
import com.scannervciarfid.nfc.NfcTagReader
import com.scannervciarfid.rfid.RfidListener
import com.scannervciarfid.rfid.RfidReader
import com.scannervciarfid.rfid.RfidReaderFactory
import com.scannervciarfid.rfid.RfidTag
import com.scannervciarfid.sample.databinding.ActivityMainBinding
import com.scannervciarfid.vision.VisionScannerView
import com.scannervciarfid.vision.barcode.BarcodeEngine
import com.scannervciarfid.vision.VisionEngineAnalyzer
import com.scannervciarfid.vision.faces.FaceDetectionEngine
import com.scannervciarfid.vision.labels.ImageLabelingEngine
import com.scannervciarfid.vision.objects.ObjectDetectionEngine
import com.scannervciarfid.vision.ocr.OcrTextEngine
import com.scannervciarfid.vision.ocr.chinese.ChineseOcrEngine
import com.scannervciarfid.vision.ocr.devanagari.DevanagariOcrEngine
import com.scannervciarfid.vision.ocr.japanese.JapaneseOcrEngine
import com.scannervciarfid.vision.ocr.korean.KoreanOcrEngine
import org.json.JSONArray
import org.json.JSONObject

/**
 * Prova de uso da biblioteca scannervciarfid em Android nativo puro,
 * cobrindo as duas vias de captura simultaneamente: leitor de hardware
 * (modulo barcode-hardware, via HardwareScannerFactory.create()) e camera
 * (modulo vision + um motor: BarcodeEngine ou OcrTextEngine). Ambas convergem para o
 * mesmo ScannerListener, atualizando a mesma area de UI - mesmo padrao
 * usado nos samples de MAUI e Flutter deste repositorio.
 */
class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var hardwareScanner: UniversalScanner
    private var cameraScannerView: VisionScannerView? = null
    private var engineIndex = 0
    private val nfcTagReader = NfcTagReader()

    // null em aparelhos sem leitor RFID suportado.
    private val rfidReader: RfidReader? = RfidReaderFactory.create()
    private val inventoryEpcs = mutableSetOf<String>()
    private var inventoryRunning = false

    // Callbacks na main thread (RfidTagBuffer).
    private val rfidListener = object : RfidListener {
        override fun onTagsRead(tags: Array<RfidTag>) {
            tags.forEach { inventoryEpcs.add(it.epc) }
            binding.rfidText.text = "RFID: ${inventoryEpcs.size} tags (última: ${tags.last().epc})"
        }

        override fun onRfidError(error: String) {
            binding.rfidText.text = error
        }
    }

    private val nfcTagListener = object : NfcTagListener {
        override fun onTagRead(tag: NfcTagData) {
            runOnUiThread {
                binding.lastNfcText.text = "NFC ${tag.uid}  (${tag.technologies})"
            }
        }

        override fun onNfcError(error: String) {
            runOnUiThread { binding.lastNfcText.text = error }
        }
    }

    private val languageAi = LanguageAiClient()
    private var lastReadText: String? = null

    private val scannerListener = object : ScannerListener {
        override fun onScanSuccess(result: ScanResult) {
            runOnUiThread {
                lastReadText = if (result.format == "VISION_OCR") {
                    JSONObject(result.value).optString("fullText")
                } else {
                    result.value
                }
                binding.lastScanText.text =
                    "${result.value}  (${result.format} via ${result.sourceEngine})"
            }
        }

        override fun onScanError(error: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showCamera()
        } else {
            Toast.makeText(this, "Permissão de câmera negada.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hardwareScanner = HardwareScannerFactory.create()

        binding.activateCameraButton.setOnClickListener { onActivateCameraClicked() }
        binding.switchEngineButton.setOnClickListener { onSwitchEngineClicked() }
        binding.codeScannerButton.setOnClickListener { onCodeScannerClicked() }
        binding.documentScannerButton.setOnClickListener { onDocumentScannerClicked() }
        binding.rfidInventoryButton.setOnClickListener { onRfidInventoryClicked() }
        binding.extractEntitiesButton.setOnClickListener { onExtractEntitiesClicked() }
        binding.rfidInventoryButton.isEnabled = rfidReader != null
        if (rfidReader == null) {
            binding.rfidText.text = "Este aparelho não tem leitor RFID suportado."
        }
    }

    override fun onStart() {
        super.onStart()
        // onStart/onStop de uma Activity sempre vem em par, entao
        // initialize()/release() aqui nao correm risco de dupla-chamada
        // (diferente do MainPage.OnAppearing do MAUI, que exigiu um guard
        // de idempotencia por causa de re-navegacao).
        hardwareScanner.initialize(applicationContext)
        hardwareScanner.setScannerListener(scannerListener)
        hardwareScanner.start()

        rfidReader?.let {
            it.initialize(applicationContext)
            it.setRfidListener(rfidListener)
        }

        if (NfcTagReader.isSupported(this)) {
            nfcTagReader.setNfcTagListener(nfcTagListener)
            nfcTagReader.start(this)
        } else {
            binding.lastNfcText.text = "Este aparelho não tem NFC."
        }
    }

    override fun onStop() {
        super.onStop()
        hardwareScanner.setScannerListener(null)
        hardwareScanner.stop()
        hardwareScanner.release()

        nfcTagReader.stop()
        nfcTagReader.setNfcTagListener(null)

        rfidReader?.let {
            if (inventoryRunning) {
                it.stopInventory()
                setInventoryRunning(false)
            }
            it.setRfidListener(null)
            it.release()
        }

        // VisionScannerView nao expoe um stopCamera() publico - remover a
        // view da arvore aciona onDetachedFromWindow(), que ja faz
        // unbindAll() internamente (ver VisionScannerView.kt). Evita
        // segurar a camera presa enquanto o app esta em background.
        hideCamera()
    }

    private fun onActivateCameraClicked() {
        if (binding.cameraContainer.childCount > 0) {
            hideCamera()
            return
        }

        val hasCameraPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            showCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun showCamera() {
        var view = cameraScannerView
        if (view == null) {
            view = VisionScannerView(this)
            view.setVisionEngine(createEngine())
            view.setScannerListener(scannerListener)
            cameraScannerView = view
        }

        binding.cameraContainer.removeAllViews()
        binding.cameraContainer.addView(view)
        view.startCamera()

        binding.activateCameraButton.text = "Ocultar Câmera"
    }

    // App nativo instancia o motor direto. As pontes MAUI/Flutter usam o
    // VisionEngineRegistry, porque recebem o motor como texto ("barcode", "ocr"...).
    private fun createEngine(): VisionEngineAnalyzer = ENGINES[engineIndex].second()

    private fun onSwitchEngineClicked() {
        engineIndex = (engineIndex + 1) % ENGINES.size
        // Com a camera ligada, o motor novo passa a analisar os proximos frames.
        cameraScannerView?.setVisionEngine(createEngine())
        binding.switchEngineButton.text = "Motor: ${ENGINES[engineIndex].first}"
    }

    // Leitura unica pela tela do Google Play Services - nao usa a
    // VisionScannerView nem a permissao de camera deste app.
    private fun onCodeScannerClicked() {
        GoogleCodeScanner.scan(this, object : CodeScannerListener {
            override fun onScanSuccess(result: ScanResult) = scannerListener.onScanSuccess(result)

            override fun onScanCanceled() {
                Toast.makeText(this@MainActivity, "Leitura cancelada.", Toast.LENGTH_SHORT).show()
            }

            override fun onScanError(error: String) = scannerListener.onScanError(error)
        })
    }

    override fun onDestroy() {
        languageAi.release()
        super.onDestroy()
    }

    // IA sobre a captura: o texto do OCR vira dados estruturados (datas,
    // telefones, valores, rastreios...) sem sair do aparelho.
    private fun onExtractEntitiesClicked() {
        val text = lastReadText
        if (text.isNullOrBlank()) {
            binding.aiText.text = "Nenhum texto lido ainda."
            return
        }

        binding.aiText.text = "Analisando..."
        languageAi.extractEntities(text, "pt", object : AiTextListener {
            override fun onSuccess(result: String) {
                val annotations = JSONArray(result)
                binding.aiText.text = if (annotations.length() == 0) {
                    "Nenhuma entidade encontrada."
                } else {
                    (0 until annotations.length()).joinToString("; ") { index ->
                        val annotation = annotations.getJSONObject(index)
                        val type = annotation.getJSONArray("entities").getJSONObject(0).getString("type")
                        "$type: ${annotation.getString("text")}"
                    }
                }
            }

            override fun onError(error: String) {
                binding.aiText.text = error
            }
        })
    }

    private fun onRfidInventoryClicked() {
        val reader = rfidReader ?: return
        if (inventoryRunning) {
            reader.stopInventory()
        } else {
            inventoryEpcs.clear()
            reader.startInventory()
        }
        setInventoryRunning(!inventoryRunning)
    }

    private fun setInventoryRunning(running: Boolean) {
        inventoryRunning = running
        binding.rfidInventoryButton.text =
            if (running) "Parar inventário RFID" else "Iniciar inventário RFID"
    }

    // Digitalizacao pela tela do Google Play Services: ate 5 paginas, com PDF.
    private fun onDocumentScannerClicked() {
        GoogleDocumentScanner.scan(this, 5, true, false, object : DocumentScanListener {
            override fun onScanSuccess(result: DocumentScanResult) {
                binding.lastScanText.text = "Documento: ${result.pageCount} página(s) - ${result.pdfUri}"
            }

            override fun onScanCanceled() {
                Toast.makeText(this@MainActivity, "Digitalização cancelada.", Toast.LENGTH_SHORT).show()
            }

            override fun onScanError(error: String) = scannerListener.onScanError(error)
        })
    }

    private fun hideCamera() {
        binding.cameraContainer.removeAllViews()
        binding.activateCameraButton.text = "Ativar Câmera"
    }

    private companion object {
        val ENGINES: List<Pair<String, () -> VisionEngineAnalyzer>> = listOf(
            "Código de barras" to { BarcodeEngine() },
            "OCR" to { OcrTextEngine() },
            "Objetos" to { ObjectDetectionEngine() },
            "Rótulos" to { ImageLabelingEngine() },
            "Rostos" to { FaceDetectionEngine() },
            "OCR chinês" to { ChineseOcrEngine.create() },
            "OCR japonês" to { JapaneseOcrEngine.create() },
            "OCR coreano" to { KoreanOcrEngine.create() },
            "OCR devanágari" to { DevanagariOcrEngine.create() }
        )
    }
}
