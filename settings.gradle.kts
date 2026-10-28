pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "scanner-vc-ia-rfid"

// Cada recurso e um modulo publicado como .aar proprio. O app so leva o que
// usar: quem nao usa OCR nao embarca o ML Kit Text Recognition.
//   core             -> contratos (ScanResult, ScannerListener, UniversalScanner)
//   barcode-hardware -> leitores fisicos (Zebra, Datalogic, Intent generico)
//   vision           -> VisionScannerView (CameraX) + registro de motores
//   vision-barcode   -> motor de codigo de barras (ML Kit Barcode)
//   vision-ocr       -> motor de OCR (ML Kit Text Recognition)
//   code-scanner     -> Google Code Scanner (sem permissao de camera, exige Play Services)
//   nfc              -> leitura de tags NFC/HF (Reader Mode do Android)
//   rfid             -> contrato de leitores RFID UHF + leitor via DataWedge (Zebra)
//   ai-language      -> IA de linguagem on-device (idioma, traducao, entidades - ML Kit)
//   vision-objects   -> motor de deteccao e rastreio de objetos (ML Kit Object Detection)
//   vision-labels    -> motor de rotulagem de imagem (ML Kit Image Labeling)
//   vision-faces     -> motor de deteccao de rostos (ML Kit Face Detection)
include(":core")
include(":barcode-hardware")
include(":vision")
include(":vision-barcode")
include(":vision-ocr")
include(":code-scanner")
include(":nfc")
include(":rfid")
include(":ai-language")
include(":vision-objects")
include(":vision-labels")
include(":vision-faces")

// rfid-zebra usa o SDK Zebra RFID API3, que e proprietario e nao vem no
// repositorio (ver libs/README.md). O modulo so entra no build quando o SDK
// (libs/rfidapi3lib-<versao>.aar) existe - sem ele, o resto do projeto
// compila normalmente.
val zebraRfidSdkPresent = file("libs").listFiles()
    ?.any { it.name.startsWith("rfidapi3lib-") && it.name.endsWith(".aar") } == true
if (zebraRfidSdkPresent) {
    include(":rfid-zebra")
}

// ":sample" e um app Android puro que consome os modulos via project(...) -
// mesmo build Gradle, sempre em sincronia com o codigo-fonte atual.
include(":sample")
