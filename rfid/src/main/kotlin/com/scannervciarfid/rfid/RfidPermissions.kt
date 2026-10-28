package com.scannervciarfid.rfid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Permissoes de runtime para leitores RFID por Bluetooth (sleds RFD40/RFD90
 * pareados). No Android 12+ (API 31), o app precisa pedir
 * BLUETOOTH_CONNECT antes de RfidReader.initialize(); sem ela, o leitor
 * Bluetooth nao aparece na busca. Leitores embutidos ou por USB nao precisam.
 */
object RfidPermissions {

    /** Permissoes a pedir em runtime nesta versao do Android (pode ser vazio). */
    @JvmStatic
    fun requiredRuntimePermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            emptyArray()
        }

    @JvmStatic
    fun hasRuntimePermissions(context: Context): Boolean =
        requiredRuntimePermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
}
