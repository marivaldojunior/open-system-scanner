package com.scannervciarfid.rfid

import android.os.Build

/**
 * Ponto de entrada do modulo rfid. Escolhe o leitor pelo fabricante do
 * aparelho. Na Zebra, prefere o SDK RFID API3 (modulo rfid-zebra) e cai para
 * o DataWedge quando o modulo ou o SDK nao estao no app.
 *
 * O modulo rfid nao depende dos modulos de SDK de fabricante: eles sao
 * encontrados por reflection. Assim as pontes MAUI/Flutter continuam
 * chamando so RfidReaderFactory.create().
 */
object RfidReaderFactory {

    /**
     * Retorna null quando o aparelho nao tem um leitor RFID suportado. Em
     * aparelhos Zebra sem RFID (ex.: TC52), o leitor e criado, mas o
     * DataWedge nao entrega tags.
     */
    @JvmStatic
    fun create(): RfidReader? {
        val manufacturer = Build.MANUFACTURER.lowercase()

        return when {
            manufacturer.contains("zebra") ->
                createIfPresent(ZEBRA_API3_READER, ZEBRA_API3_SDK) ?: ZebraDataWedgeRfidReader()
            else -> null
        }
    }

    /**
     * Cria o leitor do modulo de SDK so se o modulo ([readerClass]) e o SDK
     * do fabricante ([sdkClass]) estao no app.
     */
    private fun createIfPresent(readerClass: String, sdkClass: String): RfidReader? = try {
        Class.forName(sdkClass)
        Class.forName(readerClass).getDeclaredConstructor().newInstance() as RfidReader
    } catch (e: ReflectiveOperationException) {
        null
    } catch (e: LinkageError) {
        null
    }

    private const val ZEBRA_API3_READER = "com.scannervciarfid.rfid.zebra.ZebraApi3RfidReader"
    private const val ZEBRA_API3_SDK = "com.zebra.rfid.api3.Readers"
}
