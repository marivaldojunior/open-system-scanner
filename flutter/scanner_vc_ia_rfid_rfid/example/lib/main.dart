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
  StreamSubscription<RfidLocateEvent>? _locateSubscription;
  bool? _available;
  bool _supportsOperations = false;
  bool _running = false;
  String? _error;
  String? _locatingEpc;
  int _distance = 0;

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

    // Operacoes por tag so existem com o SDK Zebra RFID API3.
    final supportsOperations = await _rfid.supportsTagOperations();
    if (!mounted) return;
    setState(() => _supportsOperations = supportsOperations);
    if (supportsOperations) {
      _locateSubscription = _rfid.onTagLocated.listen(
        (event) => setState(() => _distance = event.relativeDistance),
      );
    }
  }

  Future<void> _stopInventoryIfRunning() async {
    if (_running) {
      await _rfid.stopInventory();
      setState(() => _running = false);
    }
  }

  // Le as 6 primeiras palavras do TID: o identificador gravado de fabrica no chip.
  Future<void> _readTid(String epc) async {
    try {
      await _stopInventoryIfRunning();
      final tid = await _rfid.readMemory(epc, bank: RfidMemoryBank.tid, wordCount: 6);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('TID: $tid')));
    } on PlatformException catch (error) {
      setState(() => _error = error.message);
    }
  }

  Future<void> _toggleLocate(String epc) async {
    try {
      if (_locatingEpc != null) {
        await _rfid.stopLocate();
        setState(() => _locatingEpc = null);
        return;
      }
      await _stopInventoryIfRunning();
      setState(() {
        _locatingEpc = epc;
        _distance = 0;
      });
      await _rfid.startLocate(epc);
    } on PlatformException catch (error) {
      setState(() {
        _locatingEpc = null;
        _error = error.message;
      });
    }
  }

  void _showTagActions(String epc) {
    showModalBottomSheet<void>(
      context: context,
      builder: (sheetContext) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.memory),
              title: const Text('Ler TID'),
              onTap: () {
                Navigator.pop(sheetContext);
                unawaited(_readTid(epc));
              },
            ),
            ListTile(
              leading: const Icon(Icons.radar),
              title: const Text('Localizar'),
              onTap: () {
                Navigator.pop(sheetContext);
                unawaited(_toggleLocate(epc));
              },
            ),
          ],
        ),
      ),
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
    _locateSubscription?.cancel();
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
                      if (_locatingEpc != null) ...[
                        const SizedBox(height: 8),
                        Text('Localizando $_locatingEpc: $_distance%'),
                        LinearProgressIndicator(value: _distance / 100),
                        TextButton(
                          onPressed: () => _toggleLocate(_locatingEpc!),
                          child: const Text('Parar localização'),
                        ),
                      ],
                      const SizedBox(height: 8),
                      Expanded(
                        child: ListView(
                          children: [
                            for (final tag in _inventory.values)
                              ListTile(
                                dense: true,
                                title: Text(tag.epc),
                                subtitle: Text('RSSI: ${tag.rssi ?? '-'}  ·  leituras: ${tag.readCount}'),
                                // Toque: acoes por tag (so com o SDK Zebra RFID API3).
                                onTap: _supportsOperations ? () => _showTagActions(tag.epc) : null,
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
