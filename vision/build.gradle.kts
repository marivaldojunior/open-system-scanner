plugins {
    alias(libs.plugins.android.library)
}

// VisionScannerView (CameraX) e o contrato dos motores de analise. Nao
// depende de ML Kit: cada motor (vision-barcode, vision-ocr, ...) traz o
// proprio modelo, entao o app so embarca os motores que usar.
android {
    namespace = "com.scannervciarfid.vision"
}

dependencies {
    api(project(":core"))

    // "api": VisionEngineAnalyzer estende ImageAnalysis.Analyzer, entao os
    // modulos de motor compilam contra o camera-core.
    api(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // Provides the View.findViewTreeLifecycleOwner() extension used by VisionScannerView
    implementation(libs.lifecycle.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
}
