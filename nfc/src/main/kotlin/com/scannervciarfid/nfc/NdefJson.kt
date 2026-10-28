package com.scannervciarfid.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.Charset

/**
 * Serializa uma NdefMessage para o JSON de NfcTagData.ndefRecordsJson,
 * decodificando os tipos mais comuns (Text, URI e MIME). Usa org.json da
 * plataforma, como o OcrTextEngine.
 */
internal object NdefJson {

    fun toJson(message: NdefMessage?): String {
        val records = JSONArray()
        message?.records?.forEach { records.put(toJson(it)) }
        return records.toString()
    }

    private fun toJson(record: NdefRecord): JSONObject = JSONObject().apply {
        put("tnf", record.tnf.toInt())
        put("type", String(record.type, Charsets.US_ASCII))
        put("payloadHex", record.payload.toHex())

        if (record.isWellKnown(NdefRecord.RTD_TEXT)) {
            decodeText(record.payload)?.let { (languageCode, text) ->
                put("languageCode", languageCode)
                put("text", text)
            }
        }
        record.toUri()?.let { put("uri", it.toString()) }
        record.toMimeType()?.let { put("mimeType", it) }
    }

    private fun NdefRecord.isWellKnown(rtd: ByteArray): Boolean =
        tnf == NdefRecord.TNF_WELL_KNOWN && type.contentEquals(rtd)

    /**
     * Registro Text (NFC Forum RTD Text): o 1o byte guarda a codificacao
     * (bit 7: 0 = UTF-8, 1 = UTF-16) e o tamanho do codigo de idioma (bits 0-5).
     */
    private fun decodeText(payload: ByteArray): Pair<String, String>? {
        if (payload.isEmpty()) {
            return null
        }
        val status = payload[0].toInt()
        val languageCodeLength = status and 0x3F
        if (1 + languageCodeLength > payload.size) {
            return null
        }
        val charset: Charset = if (status and 0x80 == 0) Charsets.UTF_8 else Charsets.UTF_16
        val languageCode = String(payload, 1, languageCodeLength, Charsets.US_ASCII)
        val textStart = 1 + languageCodeLength
        val text = String(payload, textStart, payload.size - textStart, charset)
        return languageCode to text
    }
}

internal fun ByteArray.toHex(): String = joinToString(separator = "") { "%02X".format(it) }
