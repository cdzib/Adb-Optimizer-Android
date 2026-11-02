package com.optimizer.tools.data.debloat

import dev.adboptimizer.core.shell.AdbShell

/**
 * Reglas de seguridad específicas de Android TV, tomadas de los casos documentados por
 * CleanYourTV:
 *
 * - **Entradas HDMI/AV**: los paquetes que sirven `TvInputService` mueven el routing, el
 *   HDCP y el escalado de los puertos. Deshabilitarlos deja la entrada en negro y en
 *   algunas Sony se pierde también el acceso a Ajustes.
 * - **`setupwraith`**: si la red Wi-Fi guardada le pertenece, deshabilitarlo borra esa red
 *   y la TV se queda sin ADB. Si no se puede comprobar, se niega (fail-closed).
 */
class TvSafetyGuard(
    private val shell: () -> AdbShell?,
) {
    suspend fun blocked(targets: List<String>): Set<String> {
        if (targets.isEmpty()) return emptySet()
        val adb = shell() ?: return emptySet()
        val wanted = targets.toSet()
        val blocked = mutableSetOf<String>()

        if (SETUPWRAITH in wanted && ownsSavedWifi(adb)) {
            blocked += SETUPWRAITH
        }
        blocked += tvInputPackages(adb).intersect(wanted)
        return blocked
    }

    /**
     * `dumpsys wifi` lista `cname=<paquete>` por cada red guardada. Si el comando falla o
     * no hay ninguna red, no se puede asegurar que deshabilitar setupwraith no borre la
     * conexión, así que se protege.
     */
    private suspend fun ownsSavedWifi(adb: AdbShell): Boolean {
        val result =
            runCatching { adb.exec("dumpsys wifi", 20_000) }.getOrNull()
                ?: return true
        if (result.exitCode != 0) return true
        return wifiOwnedBySetupWraith(result.stdout)
    }

    private suspend fun tvInputPackages(adb: AdbShell): Set<String> {
        val result =
            runCatching {
                adb.exec(
                    "cmd package query-services --brief --components --user 0 " +
                        "-a android.media.tv.TvInputService",
                    20_000,
                )
            }.getOrNull() ?: return emptySet()
        if (result.exitCode != 0) return emptySet()
        return parseServicePackages(result.stdout)
    }
}

internal const val SETUPWRAITH = "com.google.android.tungsten.setupwraith"

private val WIFI_OWNER = Regex("""cname=([A-Za-z0-9_.]+)""")

/**
 * Sin redes listadas no se puede verificar el dueño: se asume que sí, para no perder la
 * conexión. Si hay redes y ninguna es de setupwraith, se puede deshabilitar.
 */
internal fun wifiOwnedBySetupWraith(dump: String): Boolean {
    val owners = WIFI_OWNER.findAll(dump).map { it.groupValues[1] }.toList()
    if (owners.isEmpty()) return true
    return owners.any { it == SETUPWRAITH }
}

/** Extrae el paquete de cada `pkg/actividad` devuelto por `query-services`. */
internal fun parseServicePackages(output: String): Set<String> =
    output
        .lineSequence()
        .map { it.trim() }
        .filter { COMPONENT.matches(it) }
        .map { it.substringBefore('/') }
        .toSet()

private val COMPONENT = Regex("^[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)*/[A-Za-z0-9_.$]+$")
