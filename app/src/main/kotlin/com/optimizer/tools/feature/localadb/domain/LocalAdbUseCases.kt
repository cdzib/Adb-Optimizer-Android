package com.optimizer.tools.feature.localadb.domain

/** Comprueba `Settings.Global.ADB_ENABLED` sin intentar activarlo. */
class CheckAdbEnabled(
    private val repository: LocalAdbRepository,
) {
    operator fun invoke(): Boolean = repository.isAdbEnabled()
}

/** Abre la sesión contra 127.0.0.1:5555 reutilizando el par de llaves guardado. */
class ConnectAdb(
    private val repository: LocalAdbRepository,
) {
    suspend operator fun invoke() = repository.connect()
}

/** Descubre el puerto de `adb-tls-pairing` de la depuración inalámbrica. */
class DiscoverPairingPort(
    private val repository: LocalAdbRepository,
) {
    suspend operator fun invoke(): Int = repository.discoverPairingPort()
}

/** Vincula esta app con adbd usando el código de 6 dígitos del sistema. */
class PairAdb(
    private val repository: LocalAdbRepository,
) {
    suspend operator fun invoke(
        port: Int,
        code: String,
    ) {
        require(port in 1..65535) { "El puerto de vinculación no es válido" }
        val trimmed = code.trim()
        require(trimmed.isNotEmpty()) { "El código de vinculación no puede estar vacío" }
        repository.pair(port, trimmed)
    }
}

/** Ejecuta un comando shell y devuelve salida y código de salida. */
class RunShellCommand(
    private val repository: LocalAdbRepository,
) {
    suspend operator fun invoke(command: String): ShellCommandResult {
        require(command.isNotBlank()) { "El comando no puede estar vacío" }
        return repository.runShell(command.trim())
    }
}
