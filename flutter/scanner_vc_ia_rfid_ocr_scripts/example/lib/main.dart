import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:scanner_vc_ia_rfid_flutter/scanner_vc_ia_rfid_hardware.dart';
import 'package:scanner_vc_ia_rfid_flutter/vision_scanner_widget.dart';
import 'package:scanner_vc_ia_rfid_ocr_scripts/scanner_vc_ia_rfid_ocr_scripts.dart';

void main() {
  runApp(const OcrScriptsExampleApp());
}

class OcrScriptsExampleApp extends StatelessWidget {
  const OcrScriptsExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ScannerVcIaRfid OCR',
      theme: ThemeData(useMaterial3: true, colorSchemeSeed: Colors.brown),
      home: const OcrScriptsExampleScreen(),
    );
  }
}

/// Usa o VisionScannerWidget do plugin principal com os motores de OCR por
/// script registrados por este plugin.
class OcrScriptsExampleScreen extends StatefulWidget {
  const OcrScriptsExampleScreen({super.key});

  @override
  State<OcrScriptsExampleScreen> createState() => _OcrScriptsExampleScreenState();
}

class _OcrScriptsExampleScreenState extends State<OcrScriptsExampleScreen> {
  String _engine = OcrScriptEngines.chinese;
  String _text = 'Aponte a câmera para um texto';
  bool? _cameraGranted;

  @override
  void initState() {
    super.initState();
    unawaited(_requestCamera());
  }

  Future<void> _requestCamera() async {
    final status = await Permission.camera.request();
    if (!mounted) return;
    setState(() => _cameraGranted = status.isGranted);
  }

  // O OCR devolve um JSON com fullText e blocks (mesmo formato do OCR latino).
  void _handleScan(ScanResult result) {
    final payload = jsonDecode(result.value) as Map<String, dynamic>;
    setState(() => _text = payload['fullText'] as String? ?? '');
  }

  @override
  Widget build(BuildContext context) {
    final granted = _cameraGranted;

    return Scaffold(
      appBar: AppBar(title: const Text('ScannerVcIaRfid OCR')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            SegmentedButton<String>(
              segments: const [
                ButtonSegment(value: OcrScriptEngines.chinese, label: Text('中文')),
                ButtonSegment(value: OcrScriptEngines.japanese, label: Text('日本語')),
                ButtonSegment(value: OcrScriptEngines.korean, label: Text('한국어')),
                ButtonSegment(value: OcrScriptEngines.devanagari, label: Text('देवनागरी')),
              ],
              selected: {_engine},
              onSelectionChanged: (selection) => setState(() => _engine = selection.first),
            ),
            const SizedBox(height: 8),
            // PlatformView exige altura explicita.
            SizedBox(
              height: 320,
              child: granted == null
                  ? const Center(child: CircularProgressIndicator())
                  : granted
                      ? VisionScannerWidget(
                          engine: _engine,
                          onScan: _handleScan,
                          onScanError: (message) => setState(() => _text = message),
                        )
                      : const Center(child: Text('Permissão de câmera negada.')),
            ),
            const SizedBox(height: 8),
            Expanded(child: SingleChildScrollView(child: Text(_text))),
          ],
        ),
      ),
    );
  }
}
