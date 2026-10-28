package com.scannervciarfid.rfid.zebra

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.scannervciarfid.rfid.RfidListener
import com.scannervciarfid.rfid.RfidReader
import com.scannervciarfid.rfid.RfidTagBuffer
import com.zebra.rfid.api3.ENUM_TRANSPORT
import com.zebra.rfid.api3.ENUM_TRIGGER_MODE
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE
import com.zebra.rfid.api3.RFIDReader
import com.zebra.rfid.api3.Readers
import com.zebra.rfid.api3.RfidEventsListener
import com.zebra.rfid.api3.RfidReadEvents
import com.zebra.rfid.api3.RfidStatusEvents
import com.zebra.rfid.api3.START_TRIGGER_TYPE
import com.zebra.rfid.api3.STATUS_EVENT_TYPE
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE
import com.zebra.rfid.api3.TriggerInfo
import java.util.concurrent.Executors

/**
 * Leitor RFID UHF pelo SDK Zebra RFID API3. Diferente do
 * ZebraDataWedgeRfidReader, entrega o RSSI e a contagem de leituras de cada
 * tag, e nao depende do perfil DataWedge.
 *
 * Conecta ao primeiro leitor encontrado. Os transportes seguem o
 * RFIDHandler do sample oficial (SDK 2.0.5.297): RE_SERIAL (sled RFD40/RFD90
 * acoplado), SERVICE_SERIAL e QC_SERIAL (leitor embutido: MC3390R, TC53E,
 * EM45), SERVICE_USB (RFD40 por USB) e, por ultimo, BLUETOOTH (sled pareado -
 * exige a permissao BLUETOOTH_CONNECT em runtime). O gatilho fisico inicia e
 * para o inventario.
 *
 * As chamadas do SDK bloqueiam (conexao, inventario): rodam numa thread
 * propria. Os callbacks do RfidListener chegam na main thread.
 */
class ZebraApi3RfidReader : RfidReader {

    private val buffer = RfidTagBuffer(SOURCE_ENGINE)
    private val sdkExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var listener: RfidListener? = null
    private var readers: Readers? = null

    @Volatile
    private var reader: RFIDReader? = null

    private val eventsListener = object : RfidEventsListener {
        override fun eventReadNotify(event: RfidReadEvents?) {
            val tags = reader?.Actions?.getReadTags(MAX_TAGS_PER_READ) ?: return
            for (tag in tags) {
                val epc = tag.tagID ?: continue
                buffer.add(epc, tag.peakRSSI.toInt(), maxOf(tag.tagSeenCount, 1))
            }
        }

        override fun eventStatusNotify(event: RfidStatusEvents?) {
            val data = event?.StatusEventData ?: return
            when (data.statusEventType) {
                STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT -> when (data.HandheldTriggerEventData.handheldEvent) {
                    HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED -> startInventory()
                    HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED -> stopInventory()
                }
                STATUS_EVENT_TYPE.DISCONNECTION_EVENT -> {
                    reader = null
                    postError("O leitor RFID Zebra foi desconectado.")
                }
            }
        }
    }

    override fun initialize(context: Context) {
        val appContext = context.applicationContext
        sdkExecutor.execute { connect(appContext) }
    }

    override fun startInventory() = runOnReader("Iniciar o inventario") { it.Actions.Inventory.perform() }

    override fun stopInventory() = runOnReader("Parar o inventario") { it.Actions.Inventory.stop() }

    override fun release() {
        buffer.clear()
        sdkExecutor.execute {
            try {
                reader?.let {
                    it.Events.removeEventsListener(eventsListener)
                    it.disconnect()
                }
            } catch (e: Exception) {
                // O leitor pode ja estar desconectado; nao ha o que tratar.
            } finally {
                reader = null
                readers?.Dispose()
                readers = null
            }
        }
    }

    override fun setRfidListener(listener: RfidListener?) {
        this.listener = listener
        buffer.setRfidListener(listener)
    }

    private fun connect(context: Context) {
        if (reader != null) {
            return
        }

        // Como no sample oficial: uma unica instancia de Readers, trocando o
        // transporte ate encontrar um leitor.
        val candidate = Readers(context, TRANSPORTS.first())
        for (transport in TRANSPORTS) {
            try {
                candidate.setTransport(transport)
                val device = candidate.GetAvailableRFIDReaderList()?.firstOrNull() ?: continue

                val rfidReader = device.rfidReader
                if (!rfidReader.isConnected) {
                    rfidReader.connect()
                }
                configure(rfidReader)
                readers = candidate
                reader = rfidReader
                return
            } catch (e: Exception) {
                // Transporte indisponivel neste aparelho (ou Bluetooth sem
                // permissao): tenta o proximo.
            }
        }
        candidate.Dispose()
        postError("Nenhum leitor RFID Zebra encontrado (serial, USB ou Bluetooth).")
    }

    private fun configure(rfidReader: RFIDReader) {
        val triggerInfo = TriggerInfo().apply {
            StartTrigger.triggerType = START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE
            StopTrigger.triggerType = STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE
        }

        rfidReader.Events.addEventsListener(eventsListener)
        rfidReader.Events.setHandheldEvent(true)
        rfidReader.Events.setTagReadEvent(true)
        rfidReader.Events.setReaderDisconnectEvent(true)
        // Sem os dados da tag no evento: getReadTags() busca em lote.
        rfidReader.Events.setAttachTagDataWithReadEvent(false)
        rfidReader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true)
        rfidReader.Config.setStartTrigger(triggerInfo.StartTrigger)
        rfidReader.Config.setStopTrigger(triggerInfo.StopTrigger)
    }

    private fun runOnReader(operation: String, action: (RFIDReader) -> Unit) {
        sdkExecutor.execute {
            val current = reader
            if (current == null) {
                postError("$operation: nenhum leitor RFID Zebra conectado.")
                return@execute
            }
            try {
                action(current)
            } catch (e: Exception) {
                postError("$operation falhou: ${e.message}")
            }
        }
    }

    private fun postError(message: String) {
        mainHandler.post { listener?.onRfidError(message) }
    }

    private companion object {
        const val SOURCE_ENGINE = "Zebra_RFID_API3"
        const val MAX_TAGS_PER_READ = 100

        // Bluetooth por ultimo: prefere o leitor acoplado ao aparelho.
        val TRANSPORTS = listOf(
            ENUM_TRANSPORT.RE_SERIAL,
            ENUM_TRANSPORT.SERVICE_SERIAL,
            ENUM_TRANSPORT.QC_SERIAL,
            ENUM_TRANSPORT.SERVICE_USB,
            ENUM_TRANSPORT.BLUETOOTH
        )
    }
}
