package com.scannervciarfid.documentscanner

import android.app.Activity
import android.content.Intent
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Digitaliza documentos pela tela do Google Play Services: captura, deteccao
 * de bordas, recorte, correcao de perspectiva e filtros. Na primeira
 * chamada, o Play Services pode baixar o modulo do scanner.
 */
object GoogleDocumentScanner {

    private val nextRequestId = AtomicInteger()
    private val pendingListeners = ConcurrentHashMap<Int, DocumentScanListener>()

    /**
     * Abre o scanner. [pageLimit] limita o numero de paginas (0 = sem
     * limite). Com [includePdf], o resultado tambem traz um PDF. Com
     * [allowGalleryImport], o usuario pode importar fotos da galeria.
     */
    @JvmStatic
    fun scan(
        activity: Activity,
        pageLimit: Int,
        includePdf: Boolean,
        allowGalleryImport: Boolean,
        listener: DocumentScanListener
    ) {
        val builder = GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setGalleryImportAllowed(allowGalleryImport)
        if (pageLimit > 0) {
            builder.setPageLimit(pageLimit)
        }
        if (includePdf) {
            builder.setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
        } else {
            builder.setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
        }

        GmsDocumentScanning.getClient(builder.build())
            .getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                val requestId = nextRequestId.incrementAndGet()
                pendingListeners[requestId] = listener
                activity.startActivity(
                    Intent(activity, DocumentScannerActivity::class.java)
                        .putExtra(DocumentScannerActivity.EXTRA_INTENT_SENDER, intentSender)
                        .putExtra(DocumentScannerActivity.EXTRA_REQUEST_ID, requestId)
                )
            }
            .addOnFailureListener { exception ->
                listener.onScanError("Google Document Scanner falhou: ${exception.message}")
            }
    }

    /** Usado pela DocumentScannerActivity para entregar o resultado. */
    internal fun takeListener(requestId: Int): DocumentScanListener? = pendingListeners.remove(requestId)
}
