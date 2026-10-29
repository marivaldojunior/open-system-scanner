plugins {
    alias(libs.plugins.android.library)
}

// OCR em coreano - modelo embarcado, sem Google Play Services.
// Registra o motor "ocr-korean" no VisionEngineRegistry. O modelo tambem
// reconhece texto latino.
android {
    namespace = "com.scannervciarfid.vision.ocr.korean"
}

dependencies {
    api(project(":vision-ocr"))

    implementation(libs.mlkit.text.recognition.korean)
}
