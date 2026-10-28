package com.scannervciarfid.rfid

import android.os.Handler
import android.os.Looper

/**
 * Agrupa as leituras de tags e entrega um lote por janela de tempo, na main
 * thread. Na mesma janela, cada EPC aparece uma vez, com a soma das
 * leituras e o maior RSSI. Em janelas diferentes, a mesma tag aparece de
 * novo - quem precisa de "tags unicas no inventario" mantem o proprio
 * conjunto de EPCs.
 *
 * add() pode ser chamado de qualquer thread (SDKs de fabricante costumam
 * entregar as tags em threads proprias).
 */
class RfidTagBuffer @JvmOverloads constructor(
    private val sourceEngine: String,
    private val flushIntervalMillis: Long = DEFAULT_FLUSH_INTERVAL_MS
) {

    private val handler = Handler(Looper.getMainLooper())
    private val pending = LinkedHashMap<String, RfidTag>()
    private var flushScheduled = false
    private var listener: RfidListener? = null

    private val flushRunnable = Runnable { flush() }

    fun setRfidListener(listener: RfidListener?) {
        this.listener = listener
    }

    /**
     * [readCount] serve para SDKs que ja agrupam leituras (ex.: tagSeenCount
     * da Zebra API3).
     */
    @JvmOverloads
    fun add(epc: String, rssi: Int = RfidTag.RSSI_UNKNOWN, readCount: Int = 1) {
        val key = epc.uppercase()
        synchronized(pending) {
            val previous = pending[key]
            pending[key] = if (previous == null) {
                RfidTag(key, rssi, readCount, sourceEngine)
            } else {
                previous.copy(
                    rssi = maxOf(previous.rssi, rssi),
                    readCount = previous.readCount + readCount
                )
            }
            if (!flushScheduled) {
                flushScheduled = true
                handler.postDelayed(flushRunnable, flushIntervalMillis)
            }
        }
    }

    /** Descarta as tags ainda nao entregues (ex.: no release do leitor). */
    fun clear() {
        synchronized(pending) {
            pending.clear()
            flushScheduled = false
            handler.removeCallbacks(flushRunnable)
        }
    }

    private fun flush() {
        val batch = synchronized(pending) {
            flushScheduled = false
            val tags = pending.values.toTypedArray()
            pending.clear()
            tags
        }
        if (batch.isNotEmpty()) {
            listener?.onTagsRead(batch)
        }
    }

    companion object {
        const val DEFAULT_FLUSH_INTERVAL_MS = 100L
    }
}
