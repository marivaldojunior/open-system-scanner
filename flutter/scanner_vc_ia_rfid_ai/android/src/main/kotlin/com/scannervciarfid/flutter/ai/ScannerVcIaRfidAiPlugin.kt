package com.scannervciarfid.flutter.ai

import com.scannervciarfid.ai.language.AiTextListener
import com.scannervciarfid.ai.language.LanguageAiClient
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/**
 * Expoe o LanguageAiClient por MethodChannel. O Flutter chama os handlers
 * na main thread, onde o cache de modelos do cliente nativo precisa ficar.
 */
class ScannerVcIaRfidAiPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {

    private var client: LanguageAiClient? = null
    private var methodChannel: MethodChannel? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        client = LanguageAiClient()
        methodChannel = MethodChannel(binding.binaryMessenger, METHOD_CHANNEL).apply {
            setMethodCallHandler(this@ScannerVcIaRfidAiPlugin)
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel?.setMethodCallHandler(null)
        methodChannel = null
        client?.release()
        client = null
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        val currentClient = client
        val text = call.argument<String>("text")
        if (currentClient == null || text == null) {
            result.error("INVALID_ARGUMENT", "O argumento text e obrigatorio.", null)
            return
        }

        when (call.method) {
            "identifyLanguage" -> currentClient.identifyLanguage(text, result.asListener())
            "translate" -> currentClient.translate(
                text,
                call.argument<String>("sourceLanguage").orEmpty(),
                call.argument<String>("targetLanguage").orEmpty(),
                call.argument<Boolean>("requireWifi") ?: false,
                result.asListener()
            )
            "extractEntities" -> currentClient.extractEntities(
                text,
                call.argument<String>("languageTag") ?: "pt",
                result.asListener()
            )
            else -> result.notImplemented()
        }
    }

    // Cada chamada responde uma unica vez ao Result do MethodChannel.
    private fun MethodChannel.Result.asListener(): AiTextListener = object : AiTextListener {
        override fun onSuccess(result: String) = this@asListener.success(result)
        override fun onError(error: String) = this@asListener.error("AI_ERROR", error, null)
    }

    private companion object {
        const val METHOD_CHANNEL = "com.scannervciarfid/ai/methods"
    }
}
