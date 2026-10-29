plugins {
    alias(libs.plugins.android.library)
}

// OCR em devanagari (hindi, marathi, nepali...) - modelo embarcado, sem Google Play Services.
// Registra o motor "ocr-devanagari" no VisionEngineRegistry. O modelo tambem
// reconhece texto latino.
android {
    namespace = "com.scannervciarfid.vision.ocr.devanagari"
}

dependencies {
    api(project(":vision-ocr"))

    implementation(libs.mlkit.text.recognition.devanagari)
}
