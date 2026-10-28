plugins {
    alias(libs.plugins.android.library)
}

// Leitura de tags NFC/RFID HF (13,56 MHz) com o Reader Mode do Android.
// Usa so a API da plataforma (android.nfc): nao depende de core, CameraX
// nem ML Kit - um app que so le NFC leva apenas este modulo.
android {
    namespace = "com.scannervciarfid.nfc"
}
