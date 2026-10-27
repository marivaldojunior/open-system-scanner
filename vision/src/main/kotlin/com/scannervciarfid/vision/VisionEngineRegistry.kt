package com.scannervciarfid.vision

import java.util.concurrent.ConcurrentHashMap

/**
 * Cria uma instancia nova de um motor a cada chamada - cada
 * VisionScannerView precisa do proprio motor (estado de throttle e
 * overlay sao por view).
 */
interface VisionEngineProvider {
    fun create(): VisionEngineAnalyzer
}

/**
 * Registro de motores de visao por id (ex.: "barcode", "ocr"). O modulo
 * vision nao conhece os modulos de motor: cada um se registra (ex.:
 * BarcodeEngine.register()), e as pontes MAUI/Flutter escolhem o motor
 * pelo id recebido do C#/Dart. Um modulo de motor novo nao exige mudar o
 * modulo vision nem o controle de camera das pontes.
 */
object VisionEngineRegistry {

    private val providers = ConcurrentHashMap<String, VisionEngineProvider>()

    @JvmStatic
    fun register(engineId: String, provider: VisionEngineProvider) {
        providers[engineId] = provider
    }

    /** Retorna null quando nenhum modulo registrou [engineId]. */
    @JvmStatic
    fun create(engineId: String): VisionEngineAnalyzer? = providers[engineId]?.create()
}
