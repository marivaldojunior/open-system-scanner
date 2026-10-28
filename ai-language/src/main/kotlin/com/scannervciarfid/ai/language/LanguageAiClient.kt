package com.scannervciarfid.ai.language

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.entityextraction.DateTimeEntity
import com.google.mlkit.nl.entityextraction.Entity
import com.google.mlkit.nl.entityextraction.EntityAnnotation
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractor
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import com.google.mlkit.nl.entityextraction.MoneyEntity
import com.google.mlkit.nl.entityextraction.TrackingNumberEntity
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import org.json.JSONArray
import org.json.JSONObject

/**
 * Ponto de entrada do modulo ai-language. Tudo roda no aparelho: o texto
 * nao sai do coletor. Traducao e extracao de entidades baixam o modelo do
 * idioma na primeira chamada (precisa de rede uma vez); depois funcionam
 * offline.
 *
 * Os clientes do ML Kit ficam em cache por idioma. Chame release() quando
 * nao precisar mais deles.
 */
class LanguageAiClient {

    private val languageIdentifier by lazy { LanguageIdentification.getClient() }
    private val translators = HashMap<String, Translator>()
    private val entityExtractors = HashMap<String, EntityExtractor>()

    /**
     * Resultado: codigo BCP-47 do idioma (ex.: "pt", "en") ou "und" quando
     * o modelo nao reconhece o idioma.
     */
    fun identifyLanguage(text: String, listener: AiTextListener) {
        languageIdentifier.identifyLanguage(text).deliverTo(listener, "Identificacao de idioma") { it }
    }

    /**
     * Resultado: [text] traduzido de [sourceLanguage] para [targetLanguage]
     * (codigos BCP-47, ex.: "en", "pt"). Com [requireWifi], o download do
     * modelo (~30 MB por idioma) so acontece em Wi-Fi.
     */
    fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
        requireWifi: Boolean,
        listener: AiTextListener
    ) {
        val source = TranslateLanguage.fromLanguageTag(sourceLanguage)
        val target = TranslateLanguage.fromLanguageTag(targetLanguage)
        if (source == null || target == null) {
            listener.onError("Idioma de traducao nao suportado: '$sourceLanguage' -> '$targetLanguage'.")
            return
        }

        val translator = translators.getOrPut("$source>$target") {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(source)
                    .setTargetLanguage(target)
                    .build()
            )
        }
        translator.downloadModelIfNeeded(downloadConditions(requireWifi))
            .onSuccessTask { translator.translate(text) }
            .deliverTo(listener, "Traducao") { it }
    }

    /**
     * Resultado: array JSON com os trechos reconhecidos em [text]. Cada item
     * tem start, end, text e entities (type e, quando aplicavel,
     * timestampMillis, currency, integerPart, fractionalPart, carrier,
     * trackingNumber). Tipos:
     * ADDRESS, DATE_TIME, EMAIL, FLIGHT_NUMBER, IBAN, ISBN, MONEY,
     * PAYMENT_CARD, PHONE, TRACKING_NUMBER, URL.
     */
    fun extractEntities(text: String, languageTag: String, listener: AiTextListener) {
        val modelIdentifier = EntityExtractorOptions.fromLanguageTag(languageTag)
        if (modelIdentifier == null) {
            listener.onError("Idioma de extracao de entidades nao suportado: '$languageTag'.")
            return
        }

        val extractor = entityExtractors.getOrPut(modelIdentifier) {
            EntityExtraction.getClient(EntityExtractorOptions.Builder(modelIdentifier).build())
        }
        extractor.downloadModelIfNeeded()
            .onSuccessTask { extractor.annotate(text) }
            .deliverTo(listener, "Extracao de entidades") { annotations -> annotations.toJson() }
    }

    fun release() {
        languageIdentifier.close()
        translators.values.forEach { it.close() }
        translators.clear()
        entityExtractors.values.forEach { it.close() }
        entityExtractors.clear()
    }

    private fun downloadConditions(requireWifi: Boolean): DownloadConditions {
        val builder = DownloadConditions.Builder()
        if (requireWifi) {
            builder.requireWifi()
        }
        return builder.build()
    }

    private fun <T> Task<T>.deliverTo(
        listener: AiTextListener,
        operation: String,
        toResult: (T) -> String
    ) {
        addOnSuccessListener { listener.onSuccess(toResult(it)) }
        addOnFailureListener { listener.onError("$operation falhou: ${it.message}") }
    }

    private fun List<EntityAnnotation>.toJson(): String {
        val annotations = JSONArray()
        for (annotation in this) {
            val entities = JSONArray()
            annotation.entities.forEach { entities.put(it.toJson()) }
            annotations.put(
                JSONObject()
                    .put("start", annotation.start)
                    .put("end", annotation.end)
                    .put("text", annotation.annotatedText)
                    .put("entities", entities)
            )
        }
        return annotations.toString()
    }

    private fun Entity.toJson(): JSONObject {
        val json = JSONObject().put("type", entityTypeName(type))
        when (this) {
            is DateTimeEntity -> json.put("timestampMillis", timestampMillis)
            is MoneyEntity -> json
                .put("currency", unnormalizedCurrency)
                .put("integerPart", integerPart)
                .put("fractionalPart", fractionalPart)
            is TrackingNumberEntity -> json
                .put("carrier", parcelCarrier)
                .put("trackingNumber", parcelTrackingNumber)
        }
        return json
    }

    private fun entityTypeName(type: Int): String = when (type) {
        Entity.TYPE_ADDRESS -> "ADDRESS"
        Entity.TYPE_DATE_TIME -> "DATE_TIME"
        Entity.TYPE_EMAIL -> "EMAIL"
        Entity.TYPE_FLIGHT_NUMBER -> "FLIGHT_NUMBER"
        Entity.TYPE_IBAN -> "IBAN"
        Entity.TYPE_ISBN -> "ISBN"
        Entity.TYPE_MONEY -> "MONEY"
        Entity.TYPE_PAYMENT_CARD -> "PAYMENT_CARD"
        Entity.TYPE_PHONE -> "PHONE"
        Entity.TYPE_TRACKING_NUMBER -> "TRACKING_NUMBER"
        Entity.TYPE_URL -> "URL"
        else -> "UNKNOWN"
    }
}
