package com.optimizer.tools.feature.localadb.presentation

import com.optimizer.tools.feature.localadb.domain.ShellCommandResult
import kotlinx.coroutines.flow.StateFlow

/** Puertos que consume el ViewModel; la composición los implementa con los UseCases. */
interface LocalAdbEnvironment {
    /** Pasos de la conexion, en orden, para mostrarlos en pantalla y en Logcat. */
    val logs: StateFlow<List<String>>

    /** `host:puerto` de la sesión activa, o `null` si no hay ninguna. */
    val endpoint: StateFlow<String?>

    fun log(message: String)

    fun isAdbEnabled(): Boolean

    fun isDeveloperOptionsEnabled(): Boolean

    fun isAuthorized(): Boolean

    /** Descubre por mDNS el puerto de vinculación (`adb-tls-pairing`). */
    suspend fun discoverPairingPort(): Int

    /** Vincula con el código de 6 dígitos que muestra el sistema. */
    suspend fun pair(
        port: Int,
        code: String,
    )

    suspend fun connect()

    suspend fun run(command: String): ShellCommandResult

    fun disconnect()
}
