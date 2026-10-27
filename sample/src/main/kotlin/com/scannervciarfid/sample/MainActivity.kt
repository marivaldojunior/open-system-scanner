package com.scannervciarfid.sample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.scannervciarfid.codescanner.CodeScannerListener
import com.scannervciarfid.codescanner.GoogleCodeScanner
import com.scannervciarfid.core.ScanResult
import com.scannervciarfid.core.ScannerListener
import com.scannervciarfid.core.UniversalScanner
import com.scannervciarfid.hardware.HardwareScannerFactory
import com.scannervciarfid.sample.databinding.ActivityMainBinding
import com.scannervciarfid.vision.VisionScannerView
import com.scannervciarfid.vision.barcode.BarcodeEngine
import com.scannervciarfid.vision.ocr.OcrTextEngine

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
    private var ocrEnabled = false

    private val scannerListener = object : ScannerListener {
        override fun onScanSuccess(result: ScanResult) {
            runOnUiThread {
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
    }

    override fun onStop() {
        super.onStop()
        hardwareScanner.setScannerListener(null)
        hardwareScanner.stop()
        hardwareScanner.release()

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
    // VisionEngineRegistry, porque recebem o motor como texto ("barcode", "ocr").
    private fun createEngine() = if (ocrEnabled) OcrTextEngine() else BarcodeEngine()

    private fun onSwitchEngineClicked() {
        ocrEnabled = !ocrEnabled
        // Com a camera ligada, o motor novo passa a analisar os proximos frames.
        cameraScannerView?.setVisionEngine(createEngine())
        binding.switchEngineButton.text = if (ocrEnabled) "Motor: OCR" else "Motor: Código de barras"
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

    private fun hideCamera() {
        binding.cameraContainer.removeAllViews()
        binding.activateCameraButton.text = "Ativar Câmera"
    }
}
