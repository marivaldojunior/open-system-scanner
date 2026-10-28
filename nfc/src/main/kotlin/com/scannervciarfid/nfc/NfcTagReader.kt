package com.scannervciarfid.nfc

import android.app.Activity
import android.app.Application
import android.content.Context
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import java.io.IOException

/**
 * Le tags NFC com o Reader Mode (NfcAdapter.enableReaderMode). O Reader
 * Mode vale so com a Activity em primeiro plano; o proprio Android o
 * desliga em onPause e o liga de novo em onResume, ate stop().
 *
 * Com o Reader Mode ligado, o Android nao abre outros apps para a tag - a
 * leitura vai so para este app.
 *
 * Gravacao: writeTextOnNextTag/writeUriOnNextTag armam uma mensagem NDEF.
 * A proxima tag aproximada recebe a mensagem (em vez de gerar onTagRead) e
 * a gravacao e desarmada. Exige start() ativo.
 */
class NfcTagReader {

    private var listener: NfcTagListener? = null
    private var activity: Activity? = null

    @Volatile
    private var pendingWrite: PendingWrite? = null

    private class PendingWrite(val message: NdefMessage, val listener: NfcWriteListener)

    private val readerCallback = NfcAdapter.ReaderCallback { tag -> onTagDiscovered(tag) }

    // So para soltar a referencia da Activity quando ela e destruida sem
    // stop() (ex.: o usuario fechou o app). Pause/resume o Android ja trata.
    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityDestroyed(destroyed: Activity) {
            if (destroyed === activity) {
                stop()
            }
        }

        override fun onActivityCreated(created: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(started: Activity) = Unit
        override fun onActivityStopped(stopped: Activity) = Unit
        override fun onActivityResumed(resumed: Activity) = Unit
        override fun onActivityPaused(paused: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    }

    fun setNfcTagListener(listener: NfcTagListener?) {
        this.listener = listener
    }

    /**
     * Comeca a ler tags enquanto [activity] estiver em primeiro plano.
     * Chamar de novo troca a Activity acompanhada.
     */
    fun start(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity)
        if (adapter == null) {
            listener?.onNfcError("Este aparelho nao tem NFC.")
            return
        }
        if (!adapter.isEnabled) {
            listener?.onNfcError("O NFC esta desligado nas configuracoes do aparelho.")
        }

        stop()
        this.activity = activity
        activity.application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
        adapter.enableReaderMode(activity, readerCallback, READER_FLAGS, null)
    }

    /**
     * Grava um registro Text (NFC Forum RTD Text, UTF-8) na proxima tag
     * aproximada. [languageCode] e o idioma do texto (ex.: "pt").
     */
    fun writeTextOnNextTag(text: String, languageCode: String, listener: NfcWriteListener) {
        armWrite(NdefMessage(NdefRecord.createTextRecord(languageCode, text)), listener)
    }

    /** Grava um registro URI (ex.: "https://...") na proxima tag aproximada. */
    fun writeUriOnNextTag(uri: String, listener: NfcWriteListener) {
        armWrite(NdefMessage(NdefRecord.createUri(uri)), listener)
    }

    /** Desarma a gravacao pendente. A proxima tag volta a gerar onTagRead. */
    fun cancelPendingWrite() {
        pendingWrite?.listener?.onWriteError("Gravacao cancelada.")
        pendingWrite = null
    }

    private fun armWrite(message: NdefMessage, listener: NfcWriteListener) {
        // Uma gravacao por vez: a anterior e cancelada.
        cancelPendingWrite()
        pendingWrite = PendingWrite(message, listener)
    }

    fun stop() {
        val current = activity ?: return
        current.application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        NfcAdapter.getDefaultAdapter(current)?.disableReaderMode(current)
        activity = null
    }

    private fun onTagDiscovered(tag: Tag) {
        val write = pendingWrite
        if (write != null) {
            pendingWrite = null
            writeTag(tag, write)
            return
        }

        // Sem FLAG_READER_SKIP_NDEF_CHECK, o Android ja leu a mensagem NDEF
        // ao descobrir a tag - cachedNdefMessage nao faz I/O.
        val ndefMessage = Ndef.get(tag)?.cachedNdefMessage

        listener?.onTagRead(tag.toData(ndefMessage))
    }

    // O callback do Reader Mode roda numa thread do binder: I/O com a tag e
    // permitido aqui.
    private fun writeTag(tag: Tag, write: PendingWrite) {
        val message = write.message
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.use {
                    it.connect()
                    if (!it.isWritable) {
                        throw IOException("A tag e somente leitura.")
                    }
                    if (message.byteArrayLength > it.maxSize) {
                        throw IOException("A mensagem (${message.byteArrayLength} bytes) excede a capacidade da tag (${it.maxSize} bytes).")
                    }
                    it.writeNdefMessage(message)
                }
            } else {
                // Tag vazia que aceita NDEF: formata e grava numa operacao so.
                val formatable = NdefFormatable.get(tag)
                    ?: throw IOException("A tag nao suporta NDEF.")
                formatable.use {
                    it.connect()
                    it.format(message)
                }
            }
            write.listener.onWriteSuccess(tag.toData(message))
        } catch (e: Exception) {
            write.listener.onWriteError("Falha ao gravar a tag: ${e.message}")
        }
    }

    private fun Tag.toData(ndefMessage: NdefMessage?) = NfcTagData(
        uid = id.toHex(),
        technologies = techList.joinToString(",") { it.substringAfterLast('.') },
        ndefRecordsJson = NdefJson.toJson(ndefMessage)
    )

    companion object {
        private const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V

        @JvmStatic
        fun isSupported(context: Context): Boolean = NfcAdapter.getDefaultAdapter(context) != null

        @JvmStatic
        fun isEnabled(context: Context): Boolean = NfcAdapter.getDefaultAdapter(context)?.isEnabled == true
    }
}
