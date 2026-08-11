package com.opensystemscanner.hardware

class DatalogicIntentScanner : GenericIntentScanner(
    intentAction = ACTION_DATALOGIC_USER_ACTION,
    dataExtraKey = EXTRA_DATALOGIC_BARCODE_DATA
) {

    override val sourceEngine: String = "Datalogic_Intent"

    companion object {
        private const val ACTION_DATALOGIC_USER_ACTION = "com.datalogic.decode.action.USER_ACTION"
        private const val EXTRA_DATALOGIC_BARCODE_DATA = "com.datalogic.decode.intent.EXTRA_BARCODE_DATA"
    }
}
