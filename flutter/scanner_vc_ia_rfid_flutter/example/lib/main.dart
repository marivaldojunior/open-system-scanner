import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:scanner_vc_ia_rfid_flutter/scanner_vc_ia_rfid_hardware.dart';
import 'package:scanner_vc_ia_rfid_flutter/vision_scanner_widget.dart';
import 'package:permission_handler/permission_handler.dart';

void main() {
  runApp(const ScannerExampleApp());
}

class ScannerExampleApp extends StatelessWidget {
  const ScannerExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ScannerVcIaRfid Example',
      theme: ThemeData(useMaterial3: true, colorSchemeSeed: Colors.indigo),
      home: const ScannerExampleScreen(),
    );
  }
}

/// Prova o uso simultaneo das duas vias de captura do plugin: o leitor de
/// hardware (EventChannel, roda em background) e a camera (PlatformView).
/// Ambas convergem para o mesmo _handleScanResult, atualizando a mesma area
/// de UI - assim fica evidente que qualquer uma das duas origens alimenta o
/// mesmo fluxo de dados do app.
class ScannerExampleScreen extends StatefulWidget {
  const ScannerExampleScreen({super.key});

  @override
  State<ScannerExampleScreen> createState() => _ScannerExampleScreenState();
}

class _ScannerExampleScreenState extends State<ScannerExampleScreen> {
  String? _lastValue;
  String? _lastFormat;
  String? _lastSourceEngine;

  StreamSubscription<ScanResult>? _hardwareSubscription;

  // Motor da camera. Trocar o valor reconstroi o VisionScannerWidget, que
  // troca o motor nativo sem recriar a camera.
  String _engine = VisionEngines.barcode;

  // null = ainda verificando/pedindo permissao. O leitor de hardware nao
  // depende disso (chega via Intent do sistema/DataWedge); soh a camera.
  PermissionStatus? _cameraPermissionStatus;

  @override
  void initState() {
    super.initState();

    _hardwareSubscription = ScannerVcIaRfidHardware.instance.onBarcodeScanned.listen(
      _handleScanResult,
      onError: (Object error) {
        debugPrint('[ScannerVcIaRfid] erro no stream do leitor de hardware: $error');
      },
    );

    unawaited(_ensureCameraPermission());
  }

  Future<void> _ensureCameraPermission() async {
    var status = await Permission.camera.status;

    if (!status.isGranted) {
      status = await Permission.camera.request();
    }

    if (!mounted) return;

    setState(() {
      _cameraPermissionStatus = status;
    });
  }

  @override
  void dispose() {
    _hardwareSubscription?.cancel();
    super.dispose();
  }

  void _handleScanResult(ScanResult result) {
    // O JSON estruturado do OcrTextEngine (Kotlin) nao e amigavel para
    // exibir cru na tela - resumimos aqui antes de guardar no estado,
    // mantendo o mesmo padrao usado no MainPage.HandleScanResult do MAUI.
    final displayValue = result.format == 'VISION_OCR'
        ? _summarizeOcrPayload(result.value)
        : result.value;

    setState(() {
      _lastValue = displayValue;
      _lastFormat = result.format;
      _lastSourceEngine = result.sourceEngine;
    });
  }

  String _summarizeOcrPayload(String rawJson) {
    try {
      final decoded = jsonDecode(rawJson) as Map<String, dynamic>;
      final fullText = decoded['fullText'] as String? ?? '';
      final blocks = decoded['blocks'] as List<dynamic>? ?? const [];

      // Truncamento seguro - substring(0, 20) direto quebraria em textos
      // com menos de 20 caracteres (RangeError).
      final preview = fullText.length > 20 ? fullText.substring(0, 20) : fullText;

      return 'OCR: $preview... (${blocks.length} blocos encontrados)';
    } on FormatException catch (error) {
      debugPrint('[ScannerVcIaRfid] falha ao decodificar payload OCR: $error');
      return 'OCR: falha ao interpretar o resultado.';
    }
  }

  void _handleCameraScanError(String message) {
    debugPrint('[ScannerVcIaRfid] erro na leitura via camera: $message');
  }

  @override
  Widget build(BuildContext context) {
    final bool hasResult = _lastValue != null;

    return Scaffold(
      appBar: AppBar(title: const Text('ScannerVcIaRfid')),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        hasResult ? _lastValue! : 'Nenhuma leitura ainda',
                        style: Theme.of(context).textTheme.headlineSmall,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        hasResult
                            ? 'Formato: $_lastFormat  ·  Lido via: $_lastSourceEngine'
                            : 'Aguardando leitura de hardware ou camera',
                        style: Theme.of(context).textTheme.bodyMedium,
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),
              const Text(
                'Pressione o gatilho físico do coletor ou aponte a câmera abaixo:',
              ),
              const SizedBox(height: 8),
              SegmentedButton<String>(
                segments: const [
                  ButtonSegment(value: VisionEngines.barcode, label: Text('Código de barras')),
                  ButtonSegment(value: VisionEngines.ocr, label: Text('OCR')),
                ],
                selected: {_engine},
                onSelectionChanged: (selection) => setState(() => _engine = selection.first),
              ),
              const SizedBox(height: 16),
              // PlatformViews exigem dimensoes explicitas no lado Flutter -
              // sem altura definida, o AndroidView renderiza com tamanho zero.
              SizedBox(
                height: 300,
                child: _buildCameraArea(),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildCameraArea() {
    final status = _cameraPermissionStatus;

    if (status == null) {
      return const Center(child: CircularProgressIndicator());
    }

    if (!status.isGranted) {
      // Nao criamos o VisionScannerWidget (nem o AndroidView por baixo dele)
      // sem permissao concedida - evita depender do try/catch nativo do
      // CameraX contra SecurityException e da uma UX clara sobre o motivo
      // da camera nao estar disponivel.
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('Permissão de câmera negada.'),
            const SizedBox(height: 8),
            FilledButton(
              onPressed: () {
                if (status.isPermanentlyDenied) {
                  unawaited(openAppSettings());
                } else {
                  unawaited(_ensureCameraPermission());
                }
              },
              child: Text(
                status.isPermanentlyDenied ? 'Abrir configurações' : 'Solicitar permissão',
              ),
            ),
          ],
        ),
      );
    }

    return VisionScannerWidget(
      engine: _engine,
      onScan: _handleScanResult,
      onScanError: _handleCameraScanError,
    );
  }
}
