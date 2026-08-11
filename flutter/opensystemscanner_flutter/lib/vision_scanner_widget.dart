import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'opensystemscanner_hardware.dart' show ScanResult;

typedef ScanResultCallback = void Function(ScanResult result);
typedef ScanErrorCallback = void Function(String message);

/// Renderiza a VisionScannerView nativa (CameraX + ML Kit) dentro da arvore
/// de widgets Flutter via PlatformView.
class VisionScannerWidget extends StatefulWidget {
  const VisionScannerWidget({
    super.key,
    required this.onScan,
    this.onScanError,
  });

  static const String viewType = 'com.opensystemscanner/camera_view';

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
      onPlatformViewCreated: _onPlatformViewCreated,
    );
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
