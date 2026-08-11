package com.opensystemscanner.hardware

class ZebraDataWedgeScanner : GenericIntentScanner(
    intentAction = ACTION_DATAWEDGE_RESULT,
    dataExtraKey = EXTRA_DATAWEDGE_DATA_STRING
) {

    override val sourceEngine: String = "Zebra_DataWedge"

    companion object {
        private const val ACTION_DATAWEDGE_RESULT = "com.symbol.datawedge.api.RESULT_ACTION"
        private const val EXTRA_DATAWEDGE_DATA_STRING = "com.symbol.datawedge.data_string"
    }
}
