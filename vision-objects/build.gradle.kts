plugins {
    alias(libs.plugins.android.library)
}

// Deteccao e rastreio de objetos (ate 5 por frame, com categoria grosseira) - modelo embarcado,
// funciona sem Google Play Services. Registra o motor "objects" no
// VisionEngineRegistry (modulo vision).
android {
    namespace = "com.scannervciarfid.vision.objects"
}

dependencies {
    api(project(":vision"))

    implementation(libs.mlkit.objectDetection)
}
