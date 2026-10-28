plugins {
    alias(libs.plugins.android.library)
}

// Deteccao de rostos (sem reconhecimento de pessoa) - modelo embarcado,
// funciona sem Google Play Services. Registra o motor "faces" no
// VisionEngineRegistry (modulo vision).
android {
    namespace = "com.scannervciarfid.vision.faces"
}

dependencies {
    api(project(":vision"))

    implementation(libs.mlkit.face.detection)
}
