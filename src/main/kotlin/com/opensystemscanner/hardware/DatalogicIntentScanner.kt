package com.opensystemscanner.hardware

/**
 * Escuta o Intent Wedge da Datalogic. Os valores abaixo sao os documentados
 * para o Intent Wedge em modo broadcast - o Intent Wedge precisa estar
 * habilitado nas configuracoes de Scanner do aparelho, com a mesma
 * action/categoria (alguns modelos/firmwares vem com valores diferentes).
 */
class DatalogicIntentScanner : GenericIntentScanner(
    intentAction = ACTION_DATALOGIC_DECODE,
    dataExtraKey = EXTRA_DATALOGIC_BARCODE_STRING,
    intentCategory = CATEGORY_DATALOGIC_DECODE
) {

    override val sourceEngine: String = "Datalogic_Intent"

    companion object {
        private const val ACTION_DATALOGIC_DECODE = "com.datalogic.decodewedge.decode_action"
        private const val CATEGORY_DATALOGIC_DECODE = "com.datalogic.decodewedge.decode_category"
        private const val EXTRA_DATALOGIC_BARCODE_STRING = "com.datalogic.decode.intentwedge.barcode_string"
    }
}
