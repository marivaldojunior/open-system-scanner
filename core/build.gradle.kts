plugins {
    alias(libs.plugins.android.library)
}

// Contratos publicos de todos os modulos. Sem dependencias externas: todo
// modulo de captura (hardware, visao, e no futuro NFC/RFID/IA) depende dele.
android {
    namespace = "com.scannervciarfid.core"
}
