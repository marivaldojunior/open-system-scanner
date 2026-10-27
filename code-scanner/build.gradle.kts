plugins {
    alias(libs.plugins.android.library)
}

// Google Code Scanner: a UI de leitura e do Google Play Services, entao o
// app nao precisa da permissao de camera nem do CameraX. Exige Play
// Services - coletores AOSP sem GMS devem usar vision-barcode.
android {
    namespace = "com.scannervciarfid.codescanner"
}

dependencies {
    api(project(":core"))

    implementation(libs.gms.code.scanner)
}
