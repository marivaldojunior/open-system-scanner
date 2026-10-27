package com.scannervciarfid.vision

import android.content.Context
import android.content.ContextWrapper
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.scannervciarfid.core.ScannerListener
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VisionScannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val previewView: PreviewView = PreviewView(context)
    private val graphicOverlay: GraphicOverlay = GraphicOverlay(context)
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var cameraProvider: ProcessCameraProvider? = null
    private var scannerListener: ScannerListener? = null
    private var visionEngine: VisionEngineAnalyzer? = null
    private var startPendingAttach = false

    init {
        addView(
            previewView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        // Adicionado logo apos o preview para ficar por cima dele (ordem de
        // desenho no FrameLayout segue a ordem de addView).
        addView(
            graphicOverlay,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    fun getGraphicOverlay(): GraphicOverlay = graphicOverlay

    /**
     * Define qual Strategy de analise de frame (BarcodeEngine, OcrTextEngine,
     * ou um motor futuro) alimenta o ImageAnalysis desta view. Deve ser
     * chamado antes de startCamera() - ver DataCaptureFactory.getCameraScannerView().
     */
    fun setVisionEngine(engine: VisionEngineAnalyzer) {
        this.visionEngine = engine
        engine.setScannerListener(scannerListener)
    }

    fun setScannerListener(listener: ScannerListener?) {
        this.scannerListener = listener
        visionEngine?.setScannerListener(listener)
    }

    fun startCamera() {
        val engine = visionEngine
        if (engine == null) {
            val message = "VisionScannerView.startCamera() foi chamado sem um VisionEngineAnalyzer " +
                "configurado. Chame setVisionEngine(...) antes de iniciar a camera."
            Log.e(TAG, message)
            scannerListener?.onScanError(message)
            return
        }

        // Pontes como o PlatformView do Flutter pedem startCamera() logo na
        // criacao da view, antes do attach. Em vez de falhar, adiamos o
        // inicio para onAttachedToWindow().
        if (!isAttachedToWindow) {
            startPendingAttach = true
            return
        }

        val lifecycleOwner = resolveLifecycleOwner()
        if (lifecycleOwner == null) {
            val message = "VisionScannerView.startCamera() nao encontrou um LifecycleOwner: a view " +
                "nao esta em uma Activity/Fragment com Lifecycle do AndroidX."
            Log.e(TAG, message)
            scannerListener?.onScanError(message)
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider
                bindUseCases(provider, lifecycleOwner, engine)
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun bindUseCases(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        engine: VisionEngineAnalyzer
    ) {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(analysisExecutor, engine)
            }

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind CameraX use cases", e)
            scannerListener?.onScanError("Failed to bind camera use cases: ${e.message}")
        }
    }

    /**
     * findViewTreeLifecycleOwner() so funciona quando a Activity registra o
     * owner na arvore de views (ComponentActivity/AppCompatActivity fazem
     * isso). FlutterActivity herda de Activity "pura" e nao registra - mas
     * implementa LifecycleOwner. Por isso, como fallback, procuramos um
     * LifecycleOwner na cadeia de Context (o Flutter entrega a Activity
     * envolvida em um MutableContextWrapper).
     */
    private fun resolveLifecycleOwner(): LifecycleOwner? {
        findViewTreeLifecycleOwner()?.let { return it }

        var current: Context? = context
        while (current != null) {
            if (current is LifecycleOwner) {
                return current
            }
            current = (current as? ContextWrapper)?.baseContext
        }
        return null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (startPendingAttach) {
            startPendingAttach = false
            startCamera()
        }
    }

    override fun onDetachedFromWindow() {
        startPendingAttach = false
        cameraProvider?.unbindAll()
        cameraProvider = null
        super.onDetachedFromWindow()
    }

    companion object {
        private const val TAG = "VisionScannerView"
    }
}
