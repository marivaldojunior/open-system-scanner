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

// O modulo da biblioteca fica na raiz (projeto ":"). ":sample" e um app
// Android puro que a consome via project(":") - mesmo build Gradle, sempre
// em sincronia com o codigo-fonte atual (sem gerar/copiar .aar manualmente,
// como e necessario nas pontes MAUI/Flutter que sao builds separados).
include(":sample")
