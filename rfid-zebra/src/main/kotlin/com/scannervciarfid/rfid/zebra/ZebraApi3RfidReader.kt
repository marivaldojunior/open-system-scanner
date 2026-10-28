package com.scannervciarfid.rfid.zebra

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.scannervciarfid.rfid.RfidListener
import com.scannervciarfid.rfid.RfidLocateListener
import com.scannervciarfid.rfid.RfidLockField
import com.scannervciarfid.rfid.RfidLockPrivilege
import com.scannervciarfid.rfid.RfidMemoryBank
import com.scannervciarfid.rfid.RfidOperationListener
import com.scannervciarfid.rfid.RfidReader
import com.scannervciarfid.rfid.RfidTagBuffer
import com.scannervciarfid.rfid.RfidTagOperations
import com.zebra.rfid.api3.ENUM_TRANSPORT
import com.zebra.rfid.api3.ENUM_TRIGGER_MODE
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE
import com.zebra.rfid.api3.InvalidUsageException
import com.zebra.rfid.api3.LOCK_DATA_FIELD
import com.zebra.rfid.api3.LOCK_PRIVILEGE
import com.zebra.rfid.api3.MEMORY_BANK
import com.zebra.rfid.api3.OperationFailureException
import com.zebra.rfid.api3.RFIDReader
import com.zebra.rfid.api3.Readers
import com.zebra.rfid.api3.RfidEventsListener
import com.zebra.rfid.api3.RfidReadEvents
import com.zebra.rfid.api3.RfidStatusEvents
import com.zebra.rfid.api3.SESSION
import com.zebra.rfid.api3.START_TRIGGER_TYPE
import com.zebra.rfid.api3.STATUS_EVENT_TYPE
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE
import com.zebra.rfid.api3.TriggerInfo
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs

/**
 * Leitor RFID UHF pelo SDK Zebra RFID API3. Diferente do
 * ZebraDataWedgeRfidReader, entrega o RSSI e a contagem de leituras de cada
 * tag, nao depende do perfil DataWedge e suporta as operacoes por tag
 * (RfidTagOperations).
 *
 * Conecta ao primeiro leitor encontrado. Os transportes seguem o
 * RFIDHandler do sample oficial (SDK 2.0.5.297): RE_SERIAL (sled RFD40/RFD90
 * acoplado), SERVICE_SERIAL e QC_SERIAL (leitor embutido: MC3390R, TC53E,
 * EM45), SERVICE_USB (RFD40 por USB) e, por ultimo, BLUETOOTH (sled pareado -
 * exige a permissao BLUETOOTH_CONNECT em runtime). O gatilho fisico inicia e
 * para o inventario (ignorado durante a localizacao).
 *
 * As chamadas do SDK bloqueiam: rodam numa thread propria, uma por vez. Os
 * callbacks chegam na main thread.
 */
class ZebraApi3RfidReader : RfidReader, RfidTagOperations {

    private val buffer = RfidTagBuffer(SOURCE_ENGINE)
    private val sdkExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var listener: RfidListener? = null
    private var locateListener: RfidLocateListener? = null
    private var readers: Readers? = null

    @Volatile
    private var reader: RFIDReader? = null

    // EPC da tag em localizacao; null fora do modo localizacao.
    @Volatile
    private var locatingEpc: String? = null

    private val eventsListener = object : RfidEventsListener {
        override fun eventReadNotify(event: RfidReadEvents?) {
            val tags = reader?.Actions?.getReadTags(MAX_TAGS_PER_READ) ?: return
            val locating = locatingEpc
            for (tag in tags) {
                if (locating != null) {
                    if (tag.isContainsLocationInfo) {
                        val distance = tag.LocationInfo.relativeDistance.toInt()
                        mainHandler.post { locateListener?.onTagLocated(locating, distance) }
                    }
                    continue
                }
                val epc = tag.tagID ?: continue
                buffer.add(epc, tag.peakRSSI.toInt(), maxOf(tag.tagSeenCount, 1))
            }
        }

        override fun eventStatusNotify(event: RfidStatusEvents?) {
            val data = event?.StatusEventData ?: return
            when (data.statusEventType) {
                STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT -> {
                    // Durante a localizacao, o gatilho nao inicia inventario.
                    if (locatingEpc != null) {
                        return
                    }
                    when (data.HandheldTriggerEventData.handheldEvent) {
                        HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED -> startInventory()
                        HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED -> stopInventory()
                    }
                }
                STATUS_EVENT_TYPE.DISCONNECTION_EVENT -> {
                    reader = null
                    locatingEpc = null
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
        locatingEpc = null
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

    override fun setLocateListener(listener: RfidLocateListener?) {
        this.locateListener = listener
    }

    override fun readMemory(
        epc: String,
        memoryBank: Int,
        wordOffset: Int,
        wordCount: Int,
        accessPassword: String,
        listener: RfidOperationListener
    ) = runOperation("Ler a memoria", listener) { current ->
        val tagAccess = current.Actions.TagAccess
        val params = tagAccess.ReadAccessParams().apply {
            this.memoryBank = toMemoryBank(memoryBank)
            offset = wordOffset
            count = wordCount
            this.accessPassword = parsePassword(accessPassword)
        }
        tagAccess.readWait(epc, params, null)?.memoryBankData.orEmpty()
    }

    override fun writeMemory(
        epc: String,
        memoryBank: Int,
        wordOffset: Int,
        dataHex: String,
        accessPassword: String,
        listener: RfidOperationListener
    ) = runOperation("Gravar a memoria", listener) { current ->
        val data = parseHex(dataHex)
        val tagAccess = current.Actions.TagAccess
        val params = tagAccess.WriteAccessParams().apply {
            this.memoryBank = toMemoryBank(memoryBank)
            offset = wordOffset
            setWriteData(data)
            // Tamanho em palavras de 16 bits.
            writeDataLength = data.size / 2
            this.accessPassword = parsePassword(accessPassword)
        }
        tagAccess.writeWait(epc, params, null, null)
        ""
    }

    override fun lock(
        epc: String,
        field: Int,
        privilege: Int,
        accessPassword: String,
        listener: RfidOperationListener
    ) = runOperation("Bloquear a tag", listener) { current ->
        val tagAccess = current.Actions.TagAccess
        val params = tagAccess.LockAccessParams().apply {
            setLockPrivilege(toLockField(field), toLockPrivilege(privilege))
            this.accessPassword = parsePassword(accessPassword)
        }
        tagAccess.lockWait(epc, params, null)
        ""
    }

    override fun kill(epc: String, killPassword: String, listener: RfidOperationListener) =
        runOperation("Desativar a tag (kill)", listener) { current ->
            val password = parsePassword(killPassword)
            require(password != 0L) { "A kill password nao pode ser zero." }
            val tagAccess = current.Actions.TagAccess
            tagAccess.killWait(epc, tagAccess.KillAccessParams(password), null)
            ""
        }

    override fun startLocate(epc: String) {
        val target = epc.uppercase(Locale.ROOT)
        runOnReader("Localizar a tag") {
            locatingEpc = target
            try {
                it.Actions.TagLocationing.Perform(target, null, null)
            } catch (e: Exception) {
                locatingEpc = null
                throw e
            }
        }
    }

    override fun stopLocate() = runOnReader("Parar a localizacao") {
        try {
            it.Actions.TagLocationing.Stop()
        } finally {
            locatingEpc = null
        }
    }

    override fun setTransmitPower(dbm: Int, listener: RfidOperationListener) =
        runOperation("Ajustar a potencia", listener) { current ->
            // Os niveis vem em decimos de dBm (ex.: 270 = 27,0 dBm).
            val levels = current.ReaderCapabilities.transmitPowerLevelValues
            require(levels != null && levels.isNotEmpty()) { "O leitor nao informou os niveis de potencia." }
            val target = dbm * 10
            val index = levels.indices.minBy { abs(levels[it] - target) }
            forEachAntenna(current) { antenna ->
                val config = current.Config.Antennas.getAntennaRfConfig(antenna)
                config.transmitPowerIndex = index
                current.Config.Antennas.setAntennaRfConfig(antenna, config)
            }
            String.format(Locale.ROOT, "%.1f", levels[index] / 10.0)
        }

    override fun setSession(session: Int, listener: RfidOperationListener) =
        runOperation("Ajustar a sessao", listener) { current ->
            val value = when (session) {
                0 -> SESSION.SESSION_S0
                1 -> SESSION.SESSION_S1
                2 -> SESSION.SESSION_S2
                3 -> SESSION.SESSION_S3
                else -> throw IllegalArgumentException("Sessao invalida: $session (use 0 a 3).")
            }
            forEachAntenna(current) { antenna ->
                val singulation = current.Config.Antennas.getSingulationControl(antenna)
                singulation.session = value
                current.Config.Antennas.setSingulationControl(antenna, singulation)
            }
            ""
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

    private fun forEachAntenna(current: RFIDReader, action: (Int) -> Unit) {
        // Antenas comecam em 1. Leitores de mao tem uma so.
        val count = maxOf(current.ReaderCapabilities.numAntennaSupported, 1)
        for (antenna in 1..count) {
            action(antenna)
        }
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
                postError("$operation falhou: ${describe(e)}")
            }
        }
    }

    private fun runOperation(
        operation: String,
        operationListener: RfidOperationListener,
        action: (RFIDReader) -> String
    ) {
        sdkExecutor.execute {
            val current = reader
            val outcome: Result<String> = if (current == null) {
                Result.failure(IllegalStateException("nenhum leitor RFID Zebra conectado"))
            } else {
                runCatching { action(current) }
            }
            mainHandler.post {
                outcome.fold(
                    onSuccess = { operationListener.onSuccess(it) },
                    onFailure = { operationListener.onError("$operation falhou: ${describe(it)}") }
                )
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

        // As excecoes do SDK trazem o motivo em campos proprios, nao em message.
        fun describe(error: Throwable): String = when (error) {
            is OperationFailureException ->
                listOfNotNull(error.statusDescription, error.vendorMessage).joinToString(" - ")
            is InvalidUsageException -> error.info ?: error.toString()
            else -> error.message ?: error.toString()
        }

        fun toMemoryBank(bank: Int): MEMORY_BANK = when (bank) {
            RfidMemoryBank.RESERVED -> MEMORY_BANK.MEMORY_BANK_RESERVED
            RfidMemoryBank.EPC -> MEMORY_BANK.MEMORY_BANK_EPC
            RfidMemoryBank.TID -> MEMORY_BANK.MEMORY_BANK_TID
            RfidMemoryBank.USER -> MEMORY_BANK.MEMORY_BANK_USER
            else -> throw IllegalArgumentException("Banco de memoria invalido: $bank.")
        }

        fun toLockField(field: Int): LOCK_DATA_FIELD = when (field) {
            RfidLockField.KILL_PASSWORD -> LOCK_DATA_FIELD.LOCK_KILL_PASSWORD
            RfidLockField.ACCESS_PASSWORD -> LOCK_DATA_FIELD.LOCK_ACCESS_PASSWORD
            RfidLockField.EPC -> LOCK_DATA_FIELD.LOCK_EPC_MEMORY
            RfidLockField.TID -> LOCK_DATA_FIELD.LOCK_TID_MEMORY
            RfidLockField.USER -> LOCK_DATA_FIELD.LOCK_USER_MEMORY
            else -> throw IllegalArgumentException("Campo de lock invalido: $field.")
        }

        fun toLockPrivilege(privilege: Int): LOCK_PRIVILEGE = when (privilege) {
            RfidLockPrivilege.UNLOCK -> LOCK_PRIVILEGE.LOCK_PRIVILEGE_UNLOCK
            RfidLockPrivilege.LOCK -> LOCK_PRIVILEGE.LOCK_PRIVILEGE_READ_WRITE
            RfidLockPrivilege.PERMA_LOCK -> LOCK_PRIVILEGE.LOCK_PRIVILEGE_PERMA_LOCK
            RfidLockPrivilege.PERMA_UNLOCK -> LOCK_PRIVILEGE.LOCK_PRIVILEGE_PERMA_UNLOCK
            else -> throw IllegalArgumentException("Privilegio de lock invalido: $privilege.")
        }

        fun parsePassword(hex: String): Long {
            val clean = hex.trim().ifEmpty { "0" }
            require(clean.length <= 8 && clean.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                "Senha invalida: use ate 8 digitos hexadecimais."
            }
            return clean.toLong(16)
        }

        fun parseHex(hex: String): ByteArray {
            val clean = hex.replace(" ", "")
            require(clean.isNotEmpty() && clean.length % 4 == 0) {
                "Dados invalidos: use palavras de 16 bits (multiplo de 4 digitos hexadecimais)."
            }
            return ByteArray(clean.length / 2) { index ->
                clean.substring(index * 2, index * 2 + 2).toInt(16).toByte()
            }
        }
    }
}
