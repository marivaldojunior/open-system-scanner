plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.scannervciarfid.vision.barcode"
}

dependencies {
    api(project(":vision"))

    // Modelo embarcado (bundled): funciona sem Google Play Services.
    implementation(libs.mlkit.barcode.scanning)
}
