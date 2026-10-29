plugins {
    alias(libs.plugins.android.library)
}

// OCR em chines (simplificado e tradicional) - modelo embarcado, sem Google Play Services.
// Registra o motor "ocr-chinese" no VisionEngineRegistry. O modelo tambem
// reconhece texto latino.
android {
    namespace = "com.scannervciarfid.vision.ocr.chinese"
}

dependencies {
    api(project(":vision-ocr"))

    implementation(libs.mlkit.text.recognition.chinese)
}
