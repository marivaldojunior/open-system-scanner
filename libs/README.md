# libs/

Coloque aqui os SDKs proprietários dos fabricantes. Esses arquivos **não
devem ser versionados** no repositório (o `.gitignore` já ignora `libs/*.jar` e
`libs/*.aar`). Sem eles, o projeto compila normalmente.

- `rfidapi3lib-<versão>.aar` — Zebra RFID SDK for Android (baixe no
  [portal da Zebra](https://www.zebra.com/us/en/support-downloads/software/developer-tools/rfid-sdk-for-android.html)).
  Copie com o nome original. Com ele aqui, o módulo `rfid-zebra` entra no
  build (ver `settings.gradle.kts` e o README, seção RFID UHF).
- `EMDK.jar` — Zebra EMDK for Android (baixe no [Zebra Developer Portal](https://developer.zebra.com)).
- `DatalogicSDK.jar` — Datalogic SDK (baixe no portal de developer da Datalogic).

O EMDK e o SDK Datalogic ainda não são usados: os adapters de código de barras
usam apenas `BroadcastReceiver`/`Intent` padrão do Android.
`barcode-hardware/build.gradle.kts` só os declara como `compileOnly` se
existirem localmente.
