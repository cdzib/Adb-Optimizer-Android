package com.optimizer.tools.feature.localadb.data

import android.content.Context
import android.provider.Settings
import com.optimizer.tools.domain.logging.AppLogger
import com.optimizer.tools.feature.localadb.domain.LocalAdbErrorKind
import com.optimizer.tools.feature.localadb.domain.LocalAdbException
import com.optimizer.tools.feature.localadb.domain.LocalAdbRepository
import com.optimizer.tools.feature.localadb.domain.ShellCommandResult
import dev.adboptimizer.core.adb.AdbConnectionFactory
import dev.adboptimizer.core.adb.AdbDiscovery
import dev.adboptimizer.core.adb.AdbKeys
import dev.adboptimizer.core.shell.AdbErrorKind
import dev.adboptimizer.core.shell.AdbException
import dev.adboptimizer.core.shell.AdbShell
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.Closeable
import java.io.IOException
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Sesión ADB contra el propio dispositivo.
 *
 * Android 11+ ya no deja activar `adb tcpip 5555` desde el teléfono: adbd escucha en un
 * puerto aleatorio con TLS y exige vincular (pairing) antes de aceptar la sesión. Este
 * repositorio reutiliza el transporte de `core:adb` (libadb) para:
 *
 * 1. descubrir por mDNS el puerto `adb-tls-connect` y conectar por `127.0.0.1`;
 * 2. si no hay sesión, pedir el código de 6 dígitos y vincular con `adb-tls-pairing`;
 * 3. caer al ADB clásico en `127.0.0.1:5555` para equipos que sí lo tengan activo.
 *
 * Todo el I/O se ejecuta en [io]; la UI nunca bloquea el hilo principal.
 */
class LibAdbLocalAdbRepository(
    context: Context,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val logger: AppLogger? = null,
) : LocalAdbRepository {
    private val appContext = context.applicationContext
    private val keys = AdbKeys.load(appContext)
    private val discovery = AdbDiscovery(appContext)
    private val preferences = LocalAdbPreferences(appContext)
    private val mutex = Mutex()
    private var shell: AdbShell? = null

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _endpoint = MutableStateFlow<String?>(null)

    /** `host:puerto` de la sesión activa, para mostrarlo en la pantalla. */
    val endpoint: StateFlow<String?> = _endpoint.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())

    /** Pasos de la última conexión, para mostrarlos en la pantalla de ADB local. */
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    fun isConnected(): Boolean = _connected.value

    /** Añade una línea al registro visible y, en debug, a Logcat. */
    fun appendLog(
        message: String,
        error: Throwable? = null,
    ) {
        val stamp = LocalTime.now().format(LOG_TIME_FORMAT)
        _logs.value = (_logs.value + "$stamp  $message").takeLast(MAX_LOG_LINES)
        if (error == null) {
            logger?.debug(TAG, message)
        } else {
            logger?.error(TAG, message, error)
        }
    }

    override fun isAdbEnabled(): Boolean =
        runCatching {
            Settings.Global.getInt(appContext.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1 ||
                isWirelessDebuggingEnabled()
        }.getOrDefault(false)

    override fun isDeveloperOptionsEnabled(): Boolean =
        runCatching {
            Settings.Global.getInt(appContext.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
        }.getOrDefault(false)

    fun isWirelessDebuggingEnabled(): Boolean =
        runCatching {
            Settings.Global.getInt(appContext.contentResolver, ADB_WIFI_ENABLED, 0) == 1
        }.getOrDefault(false)

    override suspend fun discoverPairingPort(timeoutMs: Long): Int =
        withContext(io) {
            appendLog("buscando el servicio ${AdbDiscovery.PAIRING} (${timeoutMs}ms)")
            try {
                val service =
                    withTimeout(timeoutMs) {
                        discovery.observe(AdbDiscovery.PAIRING).first()
                    }
                appendLog("puerto de vinculación encontrado: ${service.port}")
                service.port
            } catch (timeout: TimeoutCancellationException) {
                appendLog("no apareció el servicio de vinculación", timeout)
                throw LocalAdbException.PairingFailed(
                    IOException(
                        "No apareció el servicio de vinculación. Abre «Depuración inalámbrica» " +
                            "y toca «Vincular dispositivo con código».",
                    ),
                )
            } catch (error: Exception) {
                appendLog("fallo descubriendo el puerto de vinculación: ${error.message}", error)
                throw LocalAdbException.PairingFailed(error)
            }
        }

    override suspend fun pair(
        port: Int,
        code: String,
    ) = withContext(io) {
        appendLog("vinculando con 127.0.0.1:$port")
        val session = AdbConnectionFactory.createPairing(appContext, keys)
        try {
            session.pair(HOST, port, code).getOrElse { error ->
                appendLog("vinculación rechazada: ${error.message}", error)
                throw LocalAdbException.PairingFailed(error)
            }
            preferences.markAuthorized()
            appendLog("vinculación aceptada")
        } finally {
            session.close()
        }
    }

    override suspend fun connect() {
        mutex.withLock {
            if (shell != null) return
            withContext(io) {
                appendLog(
                    "estado adb_enabled=${isAdbEnabled()} inalambrico=${isWirelessDebuggingEnabled()} " +
                        "autorizado_antes=${isAuthorized()}",
                )
                val tlsPort = if (isWirelessDebuggingEnabled()) discoverConnectPort() else null
                var pairingRequired = false
                if (tlsPort != null) {
                    try {
                        shell = openSession(tlsPort, secure = true)
                        _endpoint.value = "$HOST:$tlsPort"
                        _connected.value = true
                        preferences.markAuthorized()
                        appendLog("sesión ADB inalámbrica establecida en $HOST:$tlsPort")
                        return@withContext
                    } catch (error: LocalAdbException) {
                        if (error.kind == LocalAdbErrorKind.PAIRING_REQUIRED) {
                            pairingRequired = true
                        }
                        appendLog("fallo la sesión TLS: ${error.message}", error)
                    }
                }

                // Equipos con `adb tcpip 5555` ya activo (o Android antiguos).
                try {
                    shell = openSession(CLASSIC_PORT, secure = false)
                    _endpoint.value = "$HOST:$CLASSIC_PORT"
                    _connected.value = true
                    preferences.markAuthorized()
                    appendLog("sesión ADB clásica establecida en $HOST:$CLASSIC_PORT")
                    return@withContext
                } catch (error: LocalAdbException) {
                    appendLog("fallo la sesión clásica: ${error.message}", error)
                    if (pairingRequired || tlsPort != null || isWirelessDebuggingEnabled()) {
                        throw LocalAdbException.PairingRequired(error)
                    }
                    throw LocalAdbException.PortClosed(error)
                }
            }
        }
    }

    override suspend fun runShell(command: String): ShellCommandResult =
        withContext(io) {
            val active = mutex.withLock { shell } ?: throw LocalAdbException.NotConnected()
            try {
                val result = active.exec(command, SHELL_TIMEOUT_MS)
                ShellCommandResult(
                    command = command,
                    stdout = result.stdout,
                    stderr = result.stderr,
                    exitCode = result.exitCode,
                    durationMs = result.durationMs,
                )
            } catch (error: AdbException) {
                appendLog("shell interrumpido: ${error.message}", error)
                resetSession()
                throw when (error.kind) {
                    AdbErrorKind.TIMEOUT -> LocalAdbException.AuthorizationTimeout(error)
                    AdbErrorKind.PAIRING -> LocalAdbException.PairingRequired(error)
                    else -> LocalAdbException.Disconnected(error)
                }
            } catch (error: IOException) {
                appendLog("shell interrumpido: ${error.message}", error)
                resetSession()
                throw LocalAdbException.Disconnected(error)
            }
        }

    override fun disconnect() {
        resetSession()
    }

    override fun isAuthorized(): Boolean = preferences.isAuthorized()

    private fun resetSession() {
        val active = shell
        shell = null
        _connected.value = false
        _endpoint.value = null
        runCatching { (active as? Closeable)?.close() }
    }

    /** Puerto TLS de `adb-tls-connect`; ausente si la depuración inalámbrica está apagada. */
    @Suppress("SwallowedException")
    private suspend fun discoverConnectPort(): Int? =
        try {
            withTimeout(CONNECT_DISCOVERY_TIMEOUT_MS) {
                discovery.observe(AdbDiscovery.CONNECT).first().port
            }.also { appendLog("servicio ${AdbDiscovery.CONNECT} en el puerto $it") }
        } catch (timeout: TimeoutCancellationException) {
            appendLog("sin servicio ${AdbDiscovery.CONNECT}: la depuración inalámbrica está apagada")
            null
        } catch (error: Exception) {
            appendLog("fallo descubriendo ${AdbDiscovery.CONNECT}: ${error.message}", error)
            null
        }

    private suspend fun openSession(
        port: Int,
        secure: Boolean,
    ): AdbShell {
        appendLog("abriendo sesión ${if (secure) "TLS" else "clásica"} en $HOST:$port")
        val candidate =
            AdbConnectionFactory.createShell(keys, HOST, port, secure) { message, error ->
                appendLog(message, error)
            }
        try {
            // El handshake (y el diálogo RSA o la vinculación) ocurre en el primer comando.
            candidate.exec("echo ready", CONNECT_TIMEOUT_MS)
            return candidate
        } catch (error: Exception) {
            runCatching { (candidate as? Closeable)?.close() }
            throw when (error) {
                is AdbException -> {
                    when (error.kind) {
                        AdbErrorKind.PAIRING -> LocalAdbException.PairingRequired(error)
                        AdbErrorKind.UNAUTHORIZED, AdbErrorKind.TIMEOUT -> LocalAdbException.AuthorizationTimeout(error)
                        AdbErrorKind.OFFLINE, AdbErrorKind.NOT_FOUND -> LocalAdbException.PortClosed(error)
                        else -> LocalAdbException.Unknown(error)
                    }
                }

                is IOException -> {
                    LocalAdbException.PortClosed(error)
                }

                else -> {
                    LocalAdbException.Unknown(error as? Exception ?: IOException(error))
                }
            }
        }
    }

    companion object {
        const val HOST = "127.0.0.1"
        const val CLASSIC_PORT = 5555

        private const val ADB_WIFI_ENABLED = "adb_wifi_enabled"
        private const val TAG = "LocalAdb"
        private const val MAX_LOG_LINES = 80
        private val LOG_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")

        private const val CONNECT_DISCOVERY_TIMEOUT_MS = 6_000L
        private const val CONNECT_TIMEOUT_MS = 15_000L
        private const val SHELL_TIMEOUT_MS = 45_000L
    }
}
