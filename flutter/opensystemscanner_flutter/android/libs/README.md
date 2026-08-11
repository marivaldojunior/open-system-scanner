# libs/

Coloque aqui o `opensystemscanner-release.aar` gerado pelo módulo nativo
(build na raiz do repositório: `./gradlew assembleRelease`, saída em
`build/outputs/aar/opensystemscanner-release.aar`).

Referenciado em `android/build.gradle` via `flatDir { dirs 'libs' }`. O
`.aar` não é versionado neste repositório (é build output, regenerável) —
por isso esta pasta só existe no clone graças a este README.
