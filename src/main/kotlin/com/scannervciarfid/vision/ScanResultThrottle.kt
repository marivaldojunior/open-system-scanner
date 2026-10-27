package com.scannervciarfid.vision

import android.os.SystemClock

/**
 * Filtro de leituras repetidas para os motores de camera. O CameraX entrega
 * varios frames por segundo e o ML Kit reconhece o mesmo codigo/texto em
 * todos eles - sem este filtro, o ScannerListener receberia dezenas de
 * eventos identicos por segundo.
 *
 * - Um valor igual ao anterior so e emitido de novo depois de ficar
 *   [duplicateWindowMillis] sem aparecer (ex.: o usuario afastou e voltou a
 *   apontar a camera para o mesmo codigo).
 * - [minIntervalMillis] limita a frequencia de emissao mesmo para valores
 *   diferentes (util no OCR, onde o texto varia um pouco a cada frame).
 *
 * Chamado apenas na main thread (listeners das Tasks do ML Kit), entao nao
 * precisa de sincronizacao.
 */
internal class ScanResultThrottle(
    private val duplicateWindowMillis: Long,
    private val minIntervalMillis: Long = 0L
) {

    private var lastValue: String? = null
    private var lastSeenAt: Long = 0L
    private var lastEmittedAt: Long = 0L

    fun shouldEmit(value: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        val isDuplicate = value == lastValue && now - lastSeenAt < duplicateWindowMillis

        if (value == lastValue) {
            lastSeenAt = now
        }

        if (isDuplicate || now - lastEmittedAt < minIntervalMillis) {
            return false
        }

        lastValue = value
        lastSeenAt = now
        lastEmittedAt = now
        return true
    }
}
