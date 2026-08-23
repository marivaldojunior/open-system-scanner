package com.opensystemscanner.sample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.opensystemscanner.DataCaptureFactory
import com.opensystemscanner.core.CaptureMode
import com.opensystemscanner.core.ScanResult
import com.opensystemscanner.core.ScannerListener
import com.opensystemscanner.core.UniversalScanner
import com.opensystemscanner.sample.databinding.ActivityMainBinding
import com.opensystemscanner.vision.VisionScannerView

/**
 * Prova de uso da biblioteca opensystemscanner em Android nativo puro,
 * cobrindo as duas vias de captura simultaneamente: leitor de hardware
 * (via DataCaptureFactory.getHardwareScanner()) e camera (via
 * DataCaptureFactory.getCameraScannerView(...)). Ambas convergem para o
 * mesmo ScannerListener, atualizando a mesma area de UI - mesmo padrao
 * usado nos samples de MAUI e Flutter deste repositorio.
 */
class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var hardwareScanner: UniversalScanner
    private var cameraScannerView: VisionScannerView? = null

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

        hardwareScanner = DataCaptureFactory.getHardwareScanner()

        binding.activateCameraButton.setOnClickListener { onActivateCameraClicked() }
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
            view = DataCaptureFactory.getCameraScannerView(this, CaptureMode.CAMERA_BARCODE)
            view.setScannerListener(scannerListener)
            cameraScannerView = view
        }

        binding.cameraContainer.removeAllViews()
        binding.cameraContainer.addView(view)
        view.startCamera()

        binding.activateCameraButton.text = "Ocultar Câmera"
    }

    private fun hideCamera() {
        binding.cameraContainer.removeAllViews()
        binding.activateCameraButton.text = "Ativar Câmera"
    }
}
