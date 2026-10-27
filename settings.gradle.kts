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
include(":core")
include(":barcode-hardware")
include(":vision")
include(":vision-barcode")
include(":vision-ocr")

// ":sample" e um app Android puro que consome os modulos via project(...) -
// mesmo build Gradle, sempre em sincronia com o codigo-fonte atual.
include(":sample")
