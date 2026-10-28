package com.scannervciarfid.documentscanner

/**
 * Resultado do Google Document Scanner. Tipos simples, pelo mesmo motivo do
 * ScanResult: o binding C#/Dart le sem conversao.
 *
 * - [pageImageUris]: URI de cada pagina em JPEG, ja recortada e corrigida.
 * - [pdfUri]: URI do PDF com todas as paginas, ou "" quando o PDF nao foi pedido.
 *
 * Os arquivos ficam no cache do app: copie-os se precisar guarda-los.
 */
data class DocumentScanResult(
    val pageImageUris: Array<String>,
    val pdfUri: String,
    val pageCount: Int
) {
    override fun equals(other: Any?): Boolean =
        other is DocumentScanResult &&
            pageImageUris.contentEquals(other.pageImageUris) &&
            pdfUri == other.pdfUri &&
            pageCount == other.pageCount

    override fun hashCode(): Int = 31 * (31 * pageImageUris.contentHashCode() + pdfUri.hashCode()) + pageCount
}

/** O cancelamento (usuario fechou a tela) e separado do erro. */
interface DocumentScanListener {
    fun onScanSuccess(result: DocumentScanResult)
    fun onScanCanceled()
    fun onScanError(error: String)
}
