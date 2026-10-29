plugins {
    alias(libs.plugins.android.library)
}

// OCR em japones - modelo embarcado, sem Google Play Services.
// Registra o motor "ocr-japanese" no VisionEngineRegistry. O modelo tambem
// reconhece texto latino.
android {
    namespace = "com.scannervciarfid.vision.ocr.japanese"
}

dependencies {
    api(project(":vision-ocr"))

    implementation(libs.mlkit.text.recognition.japanese)
}
