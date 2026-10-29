package com.optimizer.tools.feature.localadb.domain

/** Fase visible del flujo de ADB local. */
enum class LocalAdbPhase {
    DEBUGGING_OFF,
    CONNECTING,
    WAITING_AUTHORIZATION,
    PAIRING,
    WAITING_PAIRING_CODE,
    CONNECTED,
    ERROR,
}

/** Motivo de error para que la UI elija el mensaje y las instrucciones. */
enum class LocalAdbErrorKind {
    PORT_CLOSED,
    AUTHORIZATION,
    TIMEOUT,
    DISCONNECTED,
    PAIRING_REQUIRED,
    PAIRING_FAILED,
    UNKNOWN,
}

data class ShellCommandResult(
    val command: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val durationMs: Long,
)

sealed class LocalAdbException(
    message: String,
    val kind: LocalAdbErrorKind,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class PortClosed(
        cause: Throwable? = null,
    ) : LocalAdbException("El puerto 5555 no acepta conexiones", LocalAdbErrorKind.PORT_CLOSED, cause)

    class AuthorizationTimeout(
        cause: Throwable? = null,
    ) : LocalAdbException("El dispositivo no respondió a la autorización", LocalAdbErrorKind.AUTHORIZATION, cause)

    /** Android 11+ exige vincular (pairing) antes de aceptar la sesión TLS. */
    class PairingRequired(
        cause: Throwable? = null,
    ) : LocalAdbException("Falta vincular la depuración inalámbrica", LocalAdbErrorKind.PAIRING_REQUIRED, cause)

    class PairingFailed(
        cause: Throwable? = null,
    ) : LocalAdbException(cause?.message ?: "No se pudo vincular el dispositivo", LocalAdbErrorKind.PAIRING_FAILED, cause)

    class NotConnected : LocalAdbException("No hay una sesión ADB local activa", LocalAdbErrorKind.DISCONNECTED)

    class Disconnected(
        cause: Throwable? = null,
    ) : LocalAdbException("La sesión ADB se cerró", LocalAdbErrorKind.DISCONNECTED, cause)

    class Unknown(
        cause: Throwable,
    ) : LocalAdbException(cause.message ?: "Fallo de ADB local", LocalAdbErrorKind.UNKNOWN, cause)
}

/** Puerto de datos: ajustes del dispositivo y sesión ADB de loopback. */
interface LocalAdbRepository {
    fun isAdbEnabled(): Boolean

    fun isDeveloperOptionsEnabled(): Boolean

    fun isAuthorized(): Boolean

    /** Descubre por mDNS el puerto de `adb-tls-pairing`. */
    suspend fun discoverPairingPort(timeoutMs: Long = PAIRING_DISCOVERY_TIMEOUT_MS): Int

    /** Vincula esta app con adbd usando el código de 6 dígitos del sistema. */
    suspend fun pair(
        port: Int,
        code: String,
    )

    suspend fun connect()

    suspend fun runShell(command: String): ShellCommandResult

    fun disconnect()

    companion object {
        const val PAIRING_DISCOVERY_TIMEOUT_MS = 30_000L
    }
}
