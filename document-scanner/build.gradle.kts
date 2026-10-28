plugins {
    alias(libs.plugins.android.library)
}

// Google Document Scanner: a UI de captura, o recorte e a correcao de
// perspectiva sao do Google Play Services. O app nao precisa da permissao
// de camera. Exige Play Services - nao funciona em coletores AOSP sem GMS.
android {
    namespace = "com.scannervciarfid.documentscanner"
}

dependencies {
    implementation(libs.gms.document.scanner)

    // registerForActivityResult na DocumentScannerActivity.
    implementation(libs.androidx.activity)
}
