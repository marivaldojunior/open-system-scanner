package com.scannervciarfid.hardware

/**
 * Escuta o leitor dos coletores Urovo (DT40, DT50, RT40...). Os valores
 * abaixo sao os padroes de fabrica da saida Intent. O modo de saida precisa
 * estar em "Intent" (Configuracoes > Scanner / ScanWedge) - no modo teclado,
 * o broadcast nao e enviado.
 */
class UrovoIntentScanner : GenericIntentScanner(
    intentAction = ACTION_DECODE_DATA,
    dataExtraKey = EXTRA_BARCODE_STRING
) {

    override val sourceEngine: String = "Urovo_Intent"

    companion object {
        private const val ACTION_DECODE_DATA = "android.intent.ACTION_DECODE_DATA"
        private const val EXTRA_BARCODE_STRING = "barcode_string"
    }
}
