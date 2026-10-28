plugins {
    alias(libs.plugins.android.library)
}

// Leitor RFID UHF pelo SDK Zebra RFID API3 (RFD40, RFD90, MC3390R, TC53E,
// EM45...): inventario com RSSI e contagem de leituras, sem depender do
// DataWedge. So entra no build com o SDK em libs/ (ver settings.gradle.kts).
android {
    namespace = "com.scannervciarfid.rfid.zebra"

    defaultConfig {
        // O SDK Zebra RFID API3 2.0.5 declara minSdkVersion 29 (Android 10).
        minSdk = 29
        consumerProguardFiles("consumer-rules.pro")
    }
}

// Usa a versao mais nova de libs/rfidapi3lib-<versao>.aar, com o nome
// original do pacote da Zebra (sem renomear o arquivo).
val zebraRfidSdk = rootProject.file("libs").listFiles()
    ?.filter { it.name.startsWith("rfidapi3lib-") && it.name.endsWith(".aar") }
    ?.maxByOrNull { it.name }
    ?: error("SDK Zebra RFID API3 nao encontrado em libs/ (rfidapi3lib-<versao>.aar).")

dependencies {
    api(project(":rfid"))

    // compileOnly: o AGP nao embute um .aar local dentro de outro .aar, e a
    // licenca da Zebra nao permite publicar o SDK junto com este modulo. O
    // app adiciona o SDK por conta propria (ver README, secao RFID UHF).
    compileOnly(files(zebraRfidSdk))

    // Dependencias do SDK que nao vem embutidas nele (as bibliotecas da
    // Zebra ja vem fundidas no .aar). Ficam aqui para chegarem ao app pelo POM.
    implementation(libs.androidx.core)
    implementation(libs.androidx.localbroadcastmanager)
}
