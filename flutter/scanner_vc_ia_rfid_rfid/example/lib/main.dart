import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:scanner_vc_ia_rfid_rfid/scanner_vc_ia_rfid_rfid.dart';

void main() {
  runApp(const RfidExampleApp());
}

class RfidExampleApp extends StatelessWidget {
  const RfidExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ScannerVcIaRfid RFID',
      theme: ThemeData(useMaterial3: true, colorSchemeSeed: Colors.deepOrange),
      home: const RfidExampleScreen(),
    );
  }
}

/// Inventario simples: acumula os EPCs unicos recebidos nos lotes.
class RfidExampleScreen extends StatefulWidget {
  const RfidExampleScreen({super.key});

  @override
  State<RfidExampleScreen> createState() => _RfidExampleScreenState();
}

class _RfidExampleScreenState extends State<RfidExampleScreen> {
  final _rfid = ScannerVcIaRfidRfid.instance;
  final Map<String, RfidTag> _inventory = {};

  StreamSubscription<List<RfidTag>>? _subscription;
  bool? _available;
  bool _running = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    unawaited(_init());
  }

  Future<void> _init() async {
    final available = await _rfid.isAvailable();
    if (!mounted) return;
    setState(() => _available = available);
    if (!available) return;

    _subscription = _rfid.onTagsRead.listen(
      (tags) => setState(() {
        for (final tag in tags) {
          _inventory[tag.epc] = tag;
        }
      }),
      onError: (Object error) => setState(() {
        _error = error is PlatformException ? error.message : error.toString();
      }),
    );
  }

  Future<void> _toggleInventory() async {
    try {
      if (_running) {
        await _rfid.stopInventory();
      } else {
        setState(_inventory.clear);
        await _rfid.startInventory();
      }
      setState(() => _running = !_running);
    } on PlatformException catch (error) {
      setState(() => _error = error.message);
    }
  }

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final available = _available;

    return Scaffold(
      appBar: AppBar(title: const Text('ScannerVcIaRfid RFID')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: available == null
            ? const Center(child: CircularProgressIndicator())
            : !available
                ? const Center(child: Text('Este aparelho não tem leitor RFID suportado.'))
                : Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      FilledButton(
                        onPressed: _toggleInventory,
                        child: Text(_running ? 'Parar inventário' : 'Iniciar inventário'),
                      ),
                      const SizedBox(height: 8),
                      Text('${_inventory.length} tags únicas'),
                      if (_error != null) Text(_error!, style: const TextStyle(color: Colors.red)),
                      const SizedBox(height: 8),
                      Expanded(
                        child: ListView(
                          children: [
                            for (final tag in _inventory.values)
                              ListTile(
                                dense: true,
                                title: Text(tag.epc),
                                subtitle: Text('RSSI: ${tag.rssi ?? '-'}  ·  leituras: ${tag.readCount}'),
                              ),
                          ],
                        ),
                      ),
                    ],
                  ),
      ),
    );
  }
}
