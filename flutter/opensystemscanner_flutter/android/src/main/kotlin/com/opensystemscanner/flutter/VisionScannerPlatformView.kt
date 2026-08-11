package com.opensystemscanner.flutter

import android.content.Context
import com.opensystemscanner.DataCaptureFactory
import com.opensystemscanner.core.CaptureMode
import com.opensystemscanner.core.ScanResult
import com.opensystemscanner.core.ScannerListener
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
        return VisionScannerPlatformView(context, messenger, viewId)
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
    viewId: Int
) : PlatformView, MethodChannel.MethodCallHandler {

    private val nativeView = DataCaptureFactory.getCameraScannerView(context, CaptureMode.CAMERA_BARCODE)
    private val methodChannel = MethodChannel(messenger, "${OpenSystemScannerPlugin.CAMERA_VIEW_TYPE}_$viewId")

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
    }

    override fun getView() = nativeView

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "startCamera" -> {
                nativeView.startCamera()
                result.success(null)
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
