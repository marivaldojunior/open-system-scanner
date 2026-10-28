package com.scannervciarfid.documentscanner

import android.app.Activity
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * Activity transparente e interna (declarada no manifesto do modulo). Abre o
 * IntentSender do scanner do Google e entrega o resultado ao
 * DocumentScanListener registrado em GoogleDocumentScanner.scan().
 */
internal class DocumentScannerActivity : ComponentActivity() {

    private var requestId = NO_REQUEST

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        val listener = GoogleDocumentScanner.takeListener(requestId)
        if (listener != null) {
            when (activityResult.resultCode) {
                Activity.RESULT_OK -> {
                    val result = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
                    if (result == null) {
                        listener.onScanError("Google Document Scanner nao devolveu resultado.")
                    } else {
                        listener.onScanSuccess(result.toScanResult())
                    }
                }
                Activity.RESULT_CANCELED -> listener.onScanCanceled()
                else -> listener.onScanError("Google Document Scanner terminou com o codigo ${activityResult.resultCode}.")
            }
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestId = intent.getIntExtra(EXTRA_REQUEST_ID, NO_REQUEST)

        // Com savedInstanceState, o scanner ja foi aberto (ex.: rotacao) e o
        // resultado chega pelo mesmo launcher.
        if (savedInstanceState != null) {
            return
        }

        val intentSender = readIntentSender()
        if (intentSender == null) {
            GoogleDocumentScanner.takeListener(requestId)
                ?.onScanError("Google Document Scanner: requisicao invalida.")
            finish()
            return
        }
        scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
    }

    private fun readIntentSender(): IntentSender? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_INTENT_SENDER, IntentSender::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_INTENT_SENDER)
        }

    private fun GmsDocumentScanningResult.toScanResult(): DocumentScanResult {
        val pageUris = pages?.map { it.imageUri.toString() }?.toTypedArray() ?: emptyArray()
        return DocumentScanResult(
            pageImageUris = pageUris,
            pdfUri = pdf?.uri?.toString().orEmpty(),
            pageCount = pdf?.pageCount ?: pageUris.size
        )
    }

    companion object {
        const val EXTRA_INTENT_SENDER = "com.scannervciarfid.documentscanner.INTENT_SENDER"
        const val EXTRA_REQUEST_ID = "com.scannervciarfid.documentscanner.REQUEST_ID"
        private const val NO_REQUEST = -1
    }
}
