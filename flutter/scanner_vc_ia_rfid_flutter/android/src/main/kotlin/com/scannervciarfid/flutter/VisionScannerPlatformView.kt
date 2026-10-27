package com.scannervciarfid.flutter

import android.content.Context
import com.scannervciarfid.core.ScanResult
import com.scannervciarfid.core.ScannerListener
import com.scannervciarfid.vision.VisionEngineRegistry
import com.scannervciarfid.vision.VisionScannerView
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

class VisionScannerViewFactory(
    private val messenger: BinaryMessenger
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        val engineId = (args as? Map<*, *>)?.get("engine") as? String ?: DEFAULT_ENGINE_ID
        return VisionScannerPlatformView(context, messenger, viewId, engineId)
    }

    private companion object {
        const val DEFAULT_ENGINE_ID = "barcode"
    }
}

/**
 * Uma instancia por AndroidView criado no lado Dart. O MethodChannel eh
 * atrelado ao viewId (padrao "<view_type>_<id>") para que multiplos
 * VisionScannerWidget na mesma arvore de widgets nao se confundam.
 */
internal class VisionScannerPlatformView(
    context: Context,
    messenger: BinaryMessenger,
    viewId: Int,
    engineId: String
) : PlatformView, MethodChannel.MethodCallHandler {

    private val nativeView = VisionScannerView(context)
    private val methodChannel = MethodChannel(messenger, "${ScannerVcIaRfidPlugin.CAMERA_VIEW_TYPE}_$viewId")

    init {
        methodChannel.setMethodCallHandler(this)

        nativeView.setScannerListener(object : ScannerListener {
            override fun onScanSuccess(result: ScanResult) {
                methodChannel.invokeMethod("onScanSuccess", result.toFlutterMap())
            }

            override fun onScanError(error: String) {
                methodChannel.invokeMethod("onScanError", error)
            }
        })

        setEngine(engineId)
    }

    /** Retorna false quando nenhum modulo registrou [engineId]. */
    private fun setEngine(engineId: String): Boolean {
        val engine = VisionEngineRegistry.create(engineId)
        if (engine == null) {
            methodChannel.invokeMethod("onScanError", "Motor de visao '$engineId' nao registrado.")
            return false
        }
        nativeView.setVisionEngine(engine)
        return true
    }

    override fun getView() = nativeView

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "startCamera" -> {
                nativeView.startCamera()
                result.success(null)
            }
            "setEngine" -> {
                val engineId = call.arguments as? String
                if (engineId != null && setEngine(engineId)) {
                    result.success(null)
                } else {
                    result.error("UNKNOWN_ENGINE", "Motor de visao '$engineId' nao registrado.", null)
                }
            }
            else -> result.notImplemented()
        }
    }

    override fun dispose() {
        // A VisionScannerView ja libera a camera sozinha em
        // onDetachedFromWindow() (Etapa 4); aqui so cortamos as pontas do
        // lado do plugin para nao segurar referencias/callbacks obsoletos.
        methodChannel.setMethodCallHandler(null)
        nativeView.setScannerListener(null)
    }
}
