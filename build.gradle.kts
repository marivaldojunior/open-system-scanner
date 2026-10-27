import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Versao explicita obrigatoria aqui: sem settings.gradle.kts declarando
    // pluginManagement.plugins com versao fixa, o Gradle nao tem de onde
    // resolver "com.android.library" (era a causa do erro original "plugin
    // dependency must include a version number").
    //
    // A partir do AGP 9.0, o suporte a Kotlin passou a ser embutido no
    // proprio AGP - o plugin "org.jetbrains.kotlin.android" nao e mais
    // necessario (e o AGP recusa aplica-lo). O Kotlin Gradle Plugin (KGP)
    // e resolvido automaticamente (AGP 9.3.1 traz KGP >= 2.2.10 como
    // dependencia de runtime). https://kotl.in/gradle/agp-built-in-kotlin
    id("com.android.library") version "9.3.1"
}

android {
    namespace = "com.scannervciarfid"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Correcao: compilerOptions NAO fica dentro de android{} - o AGP registra
// sozinho uma extensao kotlin{} de top-level mesmo sem o plugin
// org.jetbrains.kotlin.android (confirmado na doc oficial de migracao:
// https://developer.android.com/build/migrate-to-built-in-kotlin).
// Alias: como jvmTarget ja teria como padrao o valor de
// android.compileOptions.targetCompatibility (17, acima), esta linha e
// redundante na pratica - mantida explicita para nao depender de um
// default implicito.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // CameraX
    implementation("androidx.camera:camera-core:1.6.1")
    implementation("androidx.camera:camera-camera2:1.6.1")
    implementation("androidx.camera:camera-lifecycle:1.6.1")
    implementation("androidx.camera:camera-view:1.6.1")

    // Google ML Kit Barcode Scanning
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // Google ML Kit Text Recognition (OCR) - motor usado por OcrTextEngine
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // Provides the View.findViewTreeLifecycleOwner() extension used by VisionScannerView
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")

    // Hardware SDKs - compileOnly: provided by device firmware at runtime,
    // must not be embedded in the .aar (would cause bloat/conflicts).
    // Nenhuma classe do codigo-fonte atual referencia EMDK/DatalogicSDK
    // diretamente (os adapters usam apenas BroadcastReceiver/Intent
    // padrao do Android) - essas dependencias existem apenas para o dia
    // em que a lib passar a chamar a API nativa de algum dos dois. Os
    // .jar sao SDKs proprietarios (baixe no portal de developer da Zebra/
    // Datalogic e coloque em libs/) e nao vem versionados no repositorio,
    // entao a declaracao e condicional para nao quebrar o build de quem
    // ainda nao os tem.
    if (file("libs/EMDK.jar").exists()) {
        compileOnly(files("libs/EMDK.jar"))
    }
    if (file("libs/DatalogicSDK.jar").exists()) {
        compileOnly(files("libs/DatalogicSDK.jar"))
    }

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
}
