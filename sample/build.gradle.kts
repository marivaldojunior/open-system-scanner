import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Versao vem do libs.versions.toml (declarada com "apply false" na raiz).
    // Kotlin embutido no AGP 9+ - sem plugin "org.jetbrains.kotlin.android".
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.scannervciarfid.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.scannervciarfid.sample"
        // 26 por causa do modulo ai-language (ML Kit Entity Extraction).
        minSdk = 26
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
    // Consome os modulos nativos diretamente como projeto - sempre reflete o
    // codigo-fonte atual. vision-barcode e vision-ocr trazem vision e core
    // de forma transitiva.
    implementation(project(":barcode-hardware"))
    implementation(project(":vision-barcode"))
    implementation(project(":vision-ocr"))
    implementation(project(":code-scanner"))
    implementation(project(":nfc"))
    implementation(project(":rfid"))
    implementation(project(":ai-language"))
    implementation(project(":vision-objects"))
    implementation(project(":vision-labels"))
    implementation(project(":vision-faces"))

    implementation(libs.androidx.core.ktx)
    // Necessario para ComponentActivity.registerForActivityResult(...)
    // (fluxo de permissao de camera em runtime).
    implementation(libs.androidx.activity.ktx)
}
