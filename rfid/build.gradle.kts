plugins {
    alias(libs.plugins.android.library)
}

// Contrato dos leitores RFID UHF (EPC Gen2) e o leitor via DataWedge da
// Zebra. Leitores com SDK proprietario (Zebra RFID API3, Chainway, Urovo...)
// entram como modulos proprios (rfid-zebra, rfid-chainway...) que
// implementam RfidReader - o SDK do fabricante nao vem neste modulo.
android {
    namespace = "com.scannervciarfid.rfid"
}

dependencies {
    // ContextCompat.registerReceiver (ZebraDataWedgeRfidReader)
    implementation(libs.androidx.core)
}
