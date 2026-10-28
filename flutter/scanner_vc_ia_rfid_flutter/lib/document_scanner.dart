import 'package:flutter/services.dart';

/// Espelha com.scannervciarfid.documentscanner.DocumentScanResult (Kotlin).
/// Os arquivos ficam no cache do app: copie-os se precisar guarda-los.
class DocumentScanResult {
  const DocumentScanResult({
    required this.pageImageUris,
    required this.pdfUri,
    required this.pageCount,
  });

  factory DocumentScanResult.fromMap(Map<dynamic, dynamic> map) {
    return DocumentScanResult(
      pageImageUris: (map['pageImageUris'] as List<dynamic>? ?? const [])
          .cast<String>()
          .toList(growable: false),
      pdfUri: map['pdfUri'] as String?,
      pageCount: map['pageCount'] as int? ?? 0,
    );
  }

  /// URI de cada pagina em JPEG, ja recortada e corrigida.
  final List<String> pageImageUris;

  /// URI do PDF com todas as paginas, ou null quando o PDF nao foi pedido.
  final String? pdfUri;

  final int pageCount;
}

/// Digitalizacao de documentos pela tela do Google Document Scanner (Play
/// Services): captura, recorte e correcao de perspectiva. Nao exige
/// permissao de camera.
class ScannerVcIaRfidDocumentScanner {
  ScannerVcIaRfidDocumentScanner._();

  static final ScannerVcIaRfidDocumentScanner instance = ScannerVcIaRfidDocumentScanner._();

  static const MethodChannel _methodChannel = MethodChannel('com.scannervciarfid/methods');

  /// Abre o scanner. Retorna null se o usuario cancelar. Lanca
  /// [PlatformException] quando a digitalizacao falha (ex.: aparelho sem
  /// Play Services). [pageLimit] = 0 significa sem limite de paginas.
  Future<DocumentScanResult?> scan({
    int pageLimit = 0,
    bool includePdf = true,
    bool allowGalleryImport = false,
  }) async {
    final result = await _methodChannel.invokeMapMethod<dynamic, dynamic>('scanDocument', {
      'pageLimit': pageLimit,
      'includePdf': includePdf,
      'allowGalleryImport': allowGalleryImport,
    });
    return result == null ? null : DocumentScanResult.fromMap(result);
  }
}
