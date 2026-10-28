# Jars/

Coloque aqui os `.aar` dos módulos nativos `rfid` e `rfid-zebra` (este só com
o SDK Zebra em `libs/`). Na raiz do repositório, rode:

```bash
./gradlew exportMauiAars
```

A task copia `scanner-vc-ia-rfid-rfid.aar` para esta pasta. O `.aar` não é
versionado (é build output) — por isso esta pasta só existe no clone graças a
este README.
