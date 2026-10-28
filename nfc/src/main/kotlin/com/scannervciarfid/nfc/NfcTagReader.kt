package com.scannervciarfid.nfc

import android.app.Activity
import android.app.Application
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle

/**
 * Le tags NFC com o Reader Mode (NfcAdapter.enableReaderMode). O Reader
 * Mode vale so com a Activity em primeiro plano; o proprio Android o
 * desliga em onPause e o liga de novo em onResume, ate stop().
 *
 * Com o Reader Mode ligado, o Android nao abre outros apps para a tag - a
 * leitura vai so para este app.
 */
class NfcTagReader {

    private var listener: NfcTagListener? = null
    private var activity: Activity? = null

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

    fun stop() {
        val current = activity ?: return
        current.application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        NfcAdapter.getDefaultAdapter(current)?.disableReaderMode(current)
        activity = null
    }

    private fun onTagDiscovered(tag: Tag) {
        // Sem FLAG_READER_SKIP_NDEF_CHECK, o Android ja leu a mensagem NDEF
        // ao descobrir a tag - cachedNdefMessage nao faz I/O.
        val ndefMessage = Ndef.get(tag)?.cachedNdefMessage

        val data = NfcTagData(
            uid = tag.id.toHex(),
            technologies = tag.techList.joinToString(",") { it.substringAfterLast('.') },
            ndefRecordsJson = NdefJson.toJson(ndefMessage)
        )
        listener?.onTagRead(data)
    }

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
