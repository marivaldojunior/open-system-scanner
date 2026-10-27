import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'scanner_vc_ia_rfid_hardware.dart' show ScanResult;

typedef ScanResultCallback = void Function(ScanResult result);
typedef ScanErrorCallback = void Function(String message);

/// Ids dos motores de visao para [VisionScannerWidget.engine]. Espelham
/// BarcodeEngine.ENGINE_ID e OcrTextEngine.ENGINE_ID (Kotlin). Um modulo de
/// motor novo registra o proprio id no VisionEngineRegistry nativo - nesse
/// caso, passe o id direto como String.
abstract final class VisionEngines {
  static const String barcode = 'barcode';
  static const String ocr = 'ocr';
}

/// Renderiza a VisionScannerView nativa (CameraX + ML Kit) dentro da arvore
/// de widgets Flutter via PlatformView.
class VisionScannerWidget extends StatefulWidget {
  const VisionScannerWidget({
    super.key,
    required this.onScan,
    this.onScanError,
    this.engine = VisionEngines.barcode,
  });

  static const String viewType = 'com.scannervciarfid/camera_view';

  /// Motor de analise de frame (ver [VisionEngines]). Pode mudar com a
  /// camera ligada: o motor novo passa a analisar os proximos frames.
  final String engine;
  final ScanResultCallback onScan;
  final ScanErrorCallback? onScanError;

  @override
  State<VisionScannerWidget> createState() => _VisionScannerWidgetState();
}

class _VisionScannerWidgetState extends State<VisionScannerWidget> {
  MethodChannel? _channel;

  @override
  Widget build(BuildContext context) {
    return AndroidView(
      viewType: VisionScannerWidget.viewType,
      // O motor inicial vai na criacao da view nativa; mudancas depois
      // disso usam o metodo setEngine (ver didUpdateWidget).
      creationParams: <String, dynamic>{'engine': widget.engine},
      creationParamsCodec: const StandardMessageCodec(),
      onPlatformViewCreated: _onPlatformViewCreated,
    );
  }

  @override
  void didUpdateWidget(VisionScannerWidget oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.engine != widget.engine) {
      _channel?.invokeMethod<void>('setEngine', widget.engine).catchError(
        (Object error) => widget.onScanError?.call(error.toString()),
      );
    }
  }

  Future<void> _onPlatformViewCreated(int viewId) async {
    final channel = MethodChannel('${VisionScannerWidget.viewType}_$viewId');
    channel.setMethodCallHandler(_handleNativeCall);
    _channel = channel;

    await channel.invokeMethod<void>('startCamera');
  }

  Future<dynamic> _handleNativeCall(MethodCall call) async {
    if (!mounted) return null;

    switch (call.method) {
      case 'onScanSuccess':
        final map = Map<dynamic, dynamic>.from(call.arguments as Map);
        widget.onScan(ScanResult.fromMap(map));
        break;
      case 'onScanError':
        widget.onScanError?.call(call.arguments as String);
        break;
    }
    return null;
  }

  @override
  void dispose() {
    // O descarte da camera/preview nativa (unbindAll) ja acontece em
    // VisionScannerView.onDetachedFromWindow() e em
    // VisionScannerPlatformView.dispose() do lado Kotlin, disparados pelo
    // proprio framework de PlatformView quando este widget sai da arvore.
    // Aqui so soltamos o handler para evitar callbacks apos o unmount.
    _channel?.setMethodCallHandler(null);
    _channel = null;
    super.dispose();
  }
}
