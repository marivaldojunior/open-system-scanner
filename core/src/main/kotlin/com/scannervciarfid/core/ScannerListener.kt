package com.scannervciarfid.core

interface ScannerListener {
    fun onScanSuccess(result: ScanResult)
    fun onScanError(error: String)
}
