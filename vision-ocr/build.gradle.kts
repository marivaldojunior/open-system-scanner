plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.scannervciarfid.vision.ocr"
}

dependencies {
    api(project(":vision"))

    // Modelo latino embarcado (bundled): funciona sem Google Play Services.
    implementation(libs.mlkit.text.recognition)
}
