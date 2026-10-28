# Jars/

Coloque aqui os `.aar` dos módulos nativos `vision-objects`, `vision-labels` e
`vision-faces`. Na raiz do repositório, rode:

```bash
./gradlew exportMauiAars
```

A task copia `scanner-vc-ia-rfid-vision-objects.aar`,
`scanner-vc-ia-rfid-vision-labels.aar` e `scanner-vc-ia-rfid-vision-faces.aar`
para esta pasta. Os `.aar` não são versionados (são build output) — por isso
esta pasta só existe no clone graças a este README.
