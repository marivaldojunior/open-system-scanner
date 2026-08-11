package com.opensystemscanner.core

import android.content.Context

interface UniversalScanner {
    fun initialize(context: Context)
    fun start()
    fun stop()
    fun release()
    fun setScannerListener(listener: ScannerListener?)
}
