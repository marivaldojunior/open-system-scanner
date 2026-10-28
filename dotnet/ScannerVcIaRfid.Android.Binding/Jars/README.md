# Jars/

Coloque aqui os `.aar` dos módulos nativos. Na raiz do repositório, rode:

```bash
./gradlew exportMauiAars
```

A task gera o build release de cada módulo e copia o resultado para esta
pasta:

- `scanner-vc-ia-rfid-core.aar`
- `scanner-vc-ia-rfid-barcode-hardware.aar`
- `scanner-vc-ia-rfid-vision.aar`
- `scanner-vc-ia-rfid-vision-barcode.aar`
- `scanner-vc-ia-rfid-vision-ocr.aar`
- `scanner-vc-ia-rfid-code-scanner.aar`

O `.csproj` inclui todo `Jars\*.aar`. Módulos de outros domínios (ex.: `nfc`)
vão para a pasta `Jars/` da binding do domínio. Os `.aar` não são versionados neste
repositório (são build output, regeneráveis) — por isso esta pasta só existe
no clone graças a este README.
