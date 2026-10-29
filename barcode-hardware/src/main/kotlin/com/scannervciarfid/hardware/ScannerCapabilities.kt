package com.scannervciarfid.hardware

/**
 * Leitor que aceita gatilho por software (um botao na tela, em vez do
 * gatilho fisico). As pontes testam `scanner is SoftTriggerScanner`.
 * Exige initialize().
 */
interface SoftTriggerScanner {
    fun startSoftScan()

    fun stopSoftScan()
}

/**
 * Leitor que liga e desliga simbologias pelo app. Os nomes seguem os
 * decoders do DataWedge sem o prefixo "decoder_" (ex.: "ean13", "qrcode",
 * "code128", "datamatrix", "pdf417", "upca", "code39", "i2of5"). Exige
 * initialize().
 */
interface SymbologyConfigurableScanner {
    fun configureSymbologies(enabled: Array<String>, disabled: Array<String>)
}
