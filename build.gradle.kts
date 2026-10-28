import com.android.build.api.dsl.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // "apply false": a raiz so agrega os modulos. Declarar as versoes aqui
    // coloca o AGP no classpath uma unica vez - os modulos aplicam o plugin
    // sem versao (ver libs.versions.toml).
    //
    // A partir do AGP 9.0, o suporte a Kotlin passou a ser embutido no
    // proprio AGP - o plugin "org.jetbrains.kotlin.android" nao e mais
    // necessario (e o AGP recusa aplica-lo). https://kotl.in/gradle/agp-built-in-kotlin
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
}

// Cada binding .NET MAUI empacota so os .aar do seu dominio - um app MAUI
// que so usa NFC nao leva CameraX nem ML Kit. Todo modulo de biblioteca
// precisa aparecer em exatamente uma binding. Modulos condicionais (ex.:
// rfid-zebra, que exige o SDK proprietario em libs/) sao pulados quando nao
// estao no build.
val mauiBindings = mapOf(
    "ScannerVcIaRfid.Android.Binding" to listOf(
        "core", "barcode-hardware", "vision", "vision-barcode", "vision-ocr", "code-scanner"
    ),
    "ScannerVcIaRfid.Nfc.Android.Binding" to listOf("nfc"),
    "ScannerVcIaRfid.Rfid.Android.Binding" to listOf("rfid", "rfid-zebra"),
    "ScannerVcIaRfid.Ai.Android.Binding" to listOf("ai-language"),
    "ScannerVcIaRfid.VisionExtras.Android.Binding" to listOf("vision-objects", "vision-labels", "vision-faces")
)

allprojects {
    group = "com.scannervciarfid"
    version = providers.gradleProperty("VERSION_NAME").get()
}

subprojects {
    plugins.withId("com.android.library") {
        apply(plugin = "maven-publish")

        // Configuracao comum dos modulos de biblioteca. Cada modulo declara
        // so o namespace e as proprias dependencias.
        extensions.configure<LibraryExtension> {
            compileSdk = 36

            defaultConfig {
                minSdk = 24
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }

            publishing {
                singleVariant("release") {
                    withSourcesJar()
                }
            }
        }

        // jvmTarget ja teria como padrao o targetCompatibility acima - fica
        // explicito para nao depender de um default implicito.
        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
        }

        // O componente "release" so existe depois que o AGP avalia o modulo.
        // O POM gerado lista as dependencias de cada .aar - assim o Flutter
        // (e qualquer app Gradle) resolve CameraX/ML Kit sozinho.
        afterEvaluate {
            extensions.configure<PublishingExtension> {
                publications {
                    create<MavenPublication>("release") {
                        from(components["release"])
                    }
                }
                repositories {
                    maven {
                        name = "repoLocal"
                        url = uri(rootProject.layout.buildDirectory.dir("maven-repo"))
                    }
                }
            }
        }
    }
}

// A binding .NET nao le repositorio Maven: ela consome os .aar soltos em
// <binding>/Jars/. Esta task gera e copia todos eles com nome estavel.
tasks.register<Copy>("exportMauiAars") {
    group = "publishing"
    description = "Gera os .aar release e copia para as bindings .NET MAUI."

    mauiBindings.forEach { (binding, modules) ->
        modules.filter { findProject(":$it") != null }.forEach { module ->
            dependsOn(":$module:assembleRelease")
            from(file("$module/build/outputs/aar/$module-release.aar")) {
                rename { "scanner-vc-ia-rfid-$module.aar" }
                into("$binding/Jars")
            }
        }
    }
    into(layout.projectDirectory.dir("dotnet"))
}
