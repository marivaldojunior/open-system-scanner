import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:scanner_vc_ia_rfid_nfc/scanner_vc_ia_rfid_nfc.dart';

void main() {
  runApp(const NfcExampleApp());
}

class NfcExampleApp extends StatelessWidget {
  const NfcExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ScannerVcIaRfid NFC',
      theme: ThemeData(useMaterial3: true, colorSchemeSeed: Colors.teal),
      home: const NfcExampleScreen(),
    );
  }
}

/// Mostra a ultima tag lida: UID, tecnologias e o conteudo dos registros
/// NDEF (texto ou URI).
class NfcExampleScreen extends StatefulWidget {
  const NfcExampleScreen({super.key});

  @override
  State<NfcExampleScreen> createState() => _NfcExampleScreenState();
}

class _NfcExampleScreenState extends State<NfcExampleScreen> {
  StreamSubscription<NfcTag>? _subscription;
  NfcTag? _lastTag;
  String _status = 'Verificando NFC...';

  @override
  void initState() {
    super.initState();
    unawaited(_startReading());
  }

  Future<void> _startReading() async {
    final nfc = ScannerVcIaRfidNfc.instance;

    if (!await nfc.isSupported()) {
      setState(() => _status = 'Este aparelho não tem NFC.');
      return;
    }
    if (!await nfc.isEnabled()) {
      setState(() => _status = 'Ligue o NFC nas configurações do aparelho.');
    } else {
      setState(() => _status = 'Aproxime uma tag NFC.');
    }

    _subscription = nfc.onTagRead.listen(
      (tag) => setState(() => _lastTag = tag),
      onError: (Object error) {
        final message = error is PlatformException ? error.message : error.toString();
        setState(() => _status = message ?? 'Erro de NFC.');
      },
    );
  }

  // Arma a gravacao: a proxima tag aproximada recebe o texto.
  Future<void> _writeText() async {
    setState(() => _status = 'Aproxime a tag para gravar...');
    try {
      final tag = await ScannerVcIaRfidNfc.instance.writeText('ScannerVcIaRfid ${DateTime.now()}');
      setState(() {
        _lastTag = tag;
        _status = 'Tag gravada.';
      });
    } on PlatformException catch (error) {
      setState(() => _status = error.code == 'CANCELED' ? 'Gravação cancelada.' : error.message ?? 'Erro.');
    }
  }

  @override
  void dispose() {
    // Cancelar a subscription desliga o Reader Mode.
    _subscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final tag = _lastTag;

    return Scaffold(
      appBar: AppBar(title: const Text('ScannerVcIaRfid NFC')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(_status),
            const SizedBox(height: 8),
            OutlinedButton(onPressed: _writeText, child: const Text('Gravar texto na próxima tag')),
            const SizedBox(height: 16),
            if (tag != null) ...[
              Text('UID: ${tag.uid}', style: Theme.of(context).textTheme.titleMedium),
              Text('Tecnologias: ${tag.technologies.join(', ')}'),
              const SizedBox(height: 8),
              for (final record in tag.ndefRecords)
                Text('NDEF: ${record.text ?? record.uri ?? record.mimeType ?? record.payloadHex}'),
            ],
          ],
        ),
      ),
    );
  }
}
