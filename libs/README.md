# libs/

Coloque aqui os SDKs proprietários dos fabricantes, se e quando o código
passar a chamar as APIs nativas deles diretamente (hoje não chama — os
adapters usam apenas `BroadcastReceiver`/`Intent` padrão do Android):

- `EMDK.jar` — Zebra EMDK for Android (baixe no [Zebra Developer Portal](https://developer.zebra.com)).
- `DatalogicSDK.jar` — Datalogic SDK (baixe no portal de developer da Datalogic).

Esses arquivos são proprietários e **não devem ser versionados** no
repositório. `barcode-hardware/build.gradle.kts` só os declara como `compileOnly` se
existirem localmente — sem eles, o projeto compila normalmente.
