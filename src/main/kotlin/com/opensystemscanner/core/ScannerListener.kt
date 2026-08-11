package com.opensystemscanner.core

interface ScannerListener {
    fun onScanSuccess(result: ScanResult)
    fun onScanError(error: String)
}
