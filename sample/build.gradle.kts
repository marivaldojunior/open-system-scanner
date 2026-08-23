import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Sem versao aqui de proposito: com.android.application vem do mesmo
    // artefato AGP que com.android.library, ja aplicado com versao 9.3.1
    // pelo projeto raiz (:). Redeclarar uma versao aqui conflita com o
    // classpath ja resolvido ("already on the classpath with an unknown
    // version"). Kotlin embutido no AGP 9+ - sem plugin
    // "org.jetbrains.kotlin.android".
    id("com.android.application")
}

android {
    namespace = "com.opensystemscanner.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.opensystemscanner.sample"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // Consome o modulo nativo diretamente como projeto - sempre reflete o
    // codigo-fonte atual de com.opensystemscanner.
    implementation(project(":"))

    implementation("androidx.core:core-ktx:1.13.1")
    // Necessario para ComponentActivity.registerForActivityResult(...)
    // (fluxo de permissao de camera em runtime).
    implementation("androidx.activity:activity-ktx:1.9.3")
}
