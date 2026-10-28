import 'dart:async';

import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:scanner_vc_ia_rfid_flutter/scanner_vc_ia_rfid_hardware.dart';
import 'package:scanner_vc_ia_rfid_flutter/vision_scanner_widget.dart';
import 'package:scanner_vc_ia_rfid_vision_extras/scanner_vc_ia_rfid_vision_extras.dart';

void main() {
  runApp(const VisionExtrasExampleApp());
}

class VisionExtrasExampleApp extends StatelessWidget {
  const VisionExtrasExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ScannerVcIaRfid Visão',
      theme: ThemeData(useMaterial3: true, colorSchemeSeed: Colors.blueGrey),
      home: const VisionExtrasExampleScreen(),
    );
  }
}

/// Usa o VisionScannerWidget do plugin principal com os motores registrados
/// por este plugin (objetos, rotulos, rostos).
class VisionExtrasExampleScreen extends StatefulWidget {
  const VisionExtrasExampleScreen({super.key});

  @override
  State<VisionExtrasExampleScreen> createState() => _VisionExtrasExampleScreenState();
}

class _VisionExtrasExampleScreenState extends State<VisionExtrasExampleScreen> {
  String _engine = VisionExtraEngines.objects;
  String _summary = 'Aponte a câmera';
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

  void _handleScan(ScanResult result) {
    final summary = switch (result.format) {
      'VISION_OBJECTS' => VisionExtrasPayload.parseObjects(result.value)
          .map((o) => '#${o.trackingId} ${o.labels.firstOrNull?.text ?? '?'}')
          .join(', '),
      'VISION_LABELS' => VisionExtrasPayload.parseLabels(result.value)
          .map((l) => '${l.text} ${(l.confidence * 100).round()}%')
          .join(', '),
      'VISION_FACES' => '${VisionExtrasPayload.parseFaces(result.value).length} rosto(s)',
      _ => result.value,
    };
    setState(() => _summary = summary);
  }

  @override
  Widget build(BuildContext context) {
    final granted = _cameraGranted;

    return Scaffold(
      appBar: AppBar(title: const Text('ScannerVcIaRfid Visão')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            SegmentedButton<String>(
              segments: const [
                ButtonSegment(value: VisionExtraEngines.objects, label: Text('Objetos')),
                ButtonSegment(value: VisionExtraEngines.labels, label: Text('Rótulos')),
                ButtonSegment(value: VisionExtraEngines.faces, label: Text('Rostos')),
              ],
              selected: {_engine},
              onSelectionChanged: (selection) => setState(() => _engine = selection.first),
            ),
            const SizedBox(height: 8),
            Text(_summary),
            const SizedBox(height: 8),
            // PlatformView exige altura explicita.
            SizedBox(
              height: 360,
              child: granted == null
                  ? const Center(child: CircularProgressIndicator())
                  : granted
                      ? VisionScannerWidget(
                          engine: _engine,
                          onScan: _handleScan,
                          onScanError: (message) => setState(() => _summary = message),
                        )
                      : const Center(child: Text('Permissão de câmera negada.')),
            ),
          ],
        ),
      ),
    );
  }
}
