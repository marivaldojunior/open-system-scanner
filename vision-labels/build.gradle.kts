plugins {
    alias(libs.plugins.android.library)
}

// Rotulagem de imagem (~400 rotulos genericos) - modelo embarcado,
// funciona sem Google Play Services. Registra o motor "labels" no
// VisionEngineRegistry (modulo vision).
android {
    namespace = "com.scannervciarfid.vision.labels"
}

dependencies {
    api(project(":vision"))

    implementation(libs.mlkit.image.labeling)
}
