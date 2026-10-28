import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:scanner_vc_ia_rfid_ai/scanner_vc_ia_rfid_ai.dart';

void main() {
  runApp(const AiExampleApp());
}

class AiExampleApp extends StatelessWidget {
  const AiExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ScannerVcIaRfid IA',
      theme: ThemeData(useMaterial3: true, colorSchemeSeed: Colors.purple),
      home: const AiExampleScreen(),
    );
  }
}

/// Analisa um texto (ex.: copiado de uma etiqueta lida por OCR): idioma,
/// entidades e traducao para ingles.
class AiExampleScreen extends StatefulWidget {
  const AiExampleScreen({super.key});

  @override
  State<AiExampleScreen> createState() => _AiExampleScreenState();
}

class _AiExampleScreenState extends State<AiExampleScreen> {
  final _ai = ScannerVcIaRfidAi.instance;
  final _textController = TextEditingController(
    text: 'Entrega em 12/11/2026 na Av. Paulista, 1000, São Paulo. '
        'Contato: (11) 98765-4321. Valor: R\$ 150,90.',
  );

  String _output = '';
  bool _busy = false;

  Future<void> _analyze() async {
    setState(() => _busy = true);
    try {
      final text = _textController.text;
      final language = await _ai.identifyLanguage(text);
      final entities = await _ai.extractEntities(text, languageTag: language == 'und' ? 'pt' : language);
      final translation = await _ai.translate(text, sourceLanguage: language == 'und' ? 'pt' : language, targetLanguage: 'en');

      setState(() {
        _output = [
          'Idioma: $language',
          for (final entity in entities) '${entity.entities.first.type}: ${entity.text}',
          '',
          'Tradução (en): $translation',
        ].join('\n');
      });
    } on PlatformException catch (error) {
      setState(() => _output = error.message ?? 'Erro de IA.');
    } finally {
      setState(() => _busy = false);
    }
  }

  @override
  void dispose() {
    _textController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('ScannerVcIaRfid IA')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: ListView(
          children: [
            TextField(
              controller: _textController,
              maxLines: 4,
              decoration: const InputDecoration(border: OutlineInputBorder()),
            ),
            const SizedBox(height: 8),
            FilledButton(
              onPressed: _busy ? null : _analyze,
              child: Text(_busy ? 'Analisando...' : 'Analisar'),
            ),
            const SizedBox(height: 16),
            Text(_output),
          ],
        ),
      ),
    );
  }
}
