plugins {
    alias(libs.plugins.android.library)
}

// IA de linguagem on-device com ML Kit: identificacao de idioma (modelo
// embarcado), traducao e extracao de entidades (modelos baixados na
// primeira chamada). Nao exige Google Play Services.
android {
    namespace = "com.scannervciarfid.ai.language"

    defaultConfig {
        // O ML Kit Entity Extraction exige API 26 - os outros modulos
        // continuam em 24. Um app que usa este modulo precisa de minSdk 26.
        minSdk = 26
    }
}

dependencies {
    implementation(libs.mlkit.language.id)
    implementation(libs.mlkit.translate)
    implementation(libs.mlkit.entity.extraction)
}
