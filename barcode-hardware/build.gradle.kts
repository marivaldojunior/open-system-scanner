plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.scannervciarfid.hardware"
}

dependencies {
    // "api": UniversalScanner e ScanResult fazem parte da API publica deste modulo.
    api(project(":core"))

    // ContextCompat.registerReceiver (GenericIntentScanner)
    implementation(libs.androidx.core)

    // Hardware SDKs - compileOnly: provided by device firmware at runtime,
    // must not be embedded in the .aar (would cause bloat/conflicts).
    // Nenhuma classe do codigo-fonte atual referencia EMDK/DatalogicSDK
    // diretamente (os adapters usam apenas BroadcastReceiver/Intent
    // padrao do Android) - essas dependencias existem apenas para o dia
    // em que a lib passar a chamar a API nativa de algum dos dois. Os
    // .jar sao SDKs proprietarios (baixe no portal de developer da Zebra/
    // Datalogic e coloque em libs/ na raiz do repositorio) e nao vem
    // versionados, entao a declaracao e condicional para nao quebrar o
    // build de quem ainda nao os tem.
    val emdkJar = rootProject.file("libs/EMDK.jar")
    if (emdkJar.exists()) {
        compileOnly(files(emdkJar))
    }
    val datalogicJar = rootProject.file("libs/DatalogicSDK.jar")
    if (datalogicJar.exists()) {
        compileOnly(files(datalogicJar))
    }
}
