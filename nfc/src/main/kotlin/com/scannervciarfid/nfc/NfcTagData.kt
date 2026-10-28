package com.scannervciarfid.nfc

/**
 * Tag NFC lida. So Strings, pelo mesmo motivo do ScanResult: o binding
 * C#/Dart le tipos simples sem conversao.
 *
 * - [uid]: identificador da tag em hexadecimal maiusculo (ex.: "04A2B3C4D5E6F7").
 * - [technologies]: tecnologias separadas por virgula (ex.: "NfcA,MifareUltralight,Ndef").
 * - [ndefRecordsJson]: array JSON com os registros NDEF ("[]" se a tag nao
 *   tem NDEF). Cada item tem tnf, type, payloadHex e, quando aplicavel,
 *   text, languageCode, uri ou mimeType.
 */
data class NfcTagData(
    val uid: String,
    val technologies: String,
    val ndefRecordsJson: String
)
