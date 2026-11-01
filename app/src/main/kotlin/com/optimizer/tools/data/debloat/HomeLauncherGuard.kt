package com.optimizer.tools.data.debloat

import com.optimizer.tools.domain.debloat.DebloatAction
import dev.adboptimizer.core.shell.AdbShell

/**
 * Impide que una tanda del debloater deje la TV sin pantalla de inicio.
 *
 * Antes de aplicar, consulta qué actividades responden HOME y, si la lista apagara
 * todos los launchers reales, conserva uno: el Home actual si está en la lista, o
 * el primero por orden alfabético. Es la misma protección que usan las listas de
 * CleanYourTV: sin ella, `pm disable-user` del launcher deja la TV en una pantalla
 * negra o en un `FallbackHome` de fábrica.
 */
class HomeLauncherGuard(
    private val shell: () -> AdbShell?,
) {
    suspend fun blocked(
        action: DebloatAction,
        targets: List<String>,
    ): Set<String> {
        if (action != DebloatAction.FREEZE && action != DebloatAction.UNINSTALL) return emptySet()
        if (targets.isEmpty()) return emptySet()
        val adb = shell() ?: return emptySet()
        val claimants = homeClaimants(adb)
        if (claimants.isEmpty()) return emptySet()
        val launchers = claimants.filterNot(::isNotAHomeLauncher).map { it.substringBefore('/') }.toSet()
        if (launchers.isEmpty()) return emptySet()
        return protectedLaunchers(launchers, currentHome(adb), targets.toSet())
    }

    private suspend fun homeClaimants(adb: AdbShell): Set<String> {
        val result =
            runCatching {
                adb.exec(
                    "cmd package query-activities --brief --components --user 0 " +
                        "-a android.intent.action.MAIN -c android.intent.category.HOME",
                    20_000,
                )
            }.getOrNull() ?: return emptySet()
        if (result.exitCode != 0) return emptySet()
        return parseComponents(result.output)
    }

    private suspend fun currentHome(adb: AdbShell): String? {
        val result =
            runCatching {
                adb.exec(
                    "cmd package resolve-activity --brief --user 0 " +
                        "-a android.intent.action.MAIN -c android.intent.category.HOME",
                    20_000,
                )
            }.getOrNull() ?: return null
        if (result.exitCode != 0) return null
        return parseComponents(result.output).firstOrNull()?.substringBefore('/')
    }

    private fun parseComponents(output: String): Set<String> =
        output
            .lineSequence()
            .map { line ->
                val trimmed = line.trim()
                trimmed
                    .substringAfterLast("ComponentInfo{", trimmed)
                    .substringBefore('}')
                    .substringBefore(' ')
            }.filter { COMPONENT.matches(it) }
            .toSet()

    private companion object {
        val COMPONENT = Regex("^[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)*/[A-Za-z0-9_.$]+$")
    }
}

/**
 * Decide qué launcher debe quedarse. Solo bloquea algo cuando la tanda dejaría
 * **cero** launchers utilizables; si otro launcher sobrevive, la lista se aplica tal cual.
 */
internal fun protectedLaunchers(
    launchers: Set<String>,
    currentHome: String?,
    targets: Set<String>,
): Set<String> {
    if (launchers.isEmpty()) return emptySet()
    if ((launchers - targets).isNotEmpty()) return emptySet()
    val keep = currentHome?.takeIf { it in launchers } ?: launchers.minOrNull() ?: return emptySet()
    return setOf(keep)
}

/**
 * Descarta las actividades que responden HOME sin ser un launcher utilizable:
 * asistentes de configuración, ajustes, `FallbackHome` de fábrica y apps de test.
 */
internal fun isNotAHomeLauncher(component: String): Boolean {
    val packageName = component.substringBefore('/').lowercase()
    val activity = component.substringAfter('/', "").lowercase()
    return packageName == "com.google.android.tungsten.setupwraith" ||
        packageName == "com.android.settings" ||
        packageName == "com.android.tv.settings" ||
        packageName == "com.android.provision" ||
        activity.endsWith("fallbackhome") ||
        packageName.contains("fallbackhome") ||
        packageName.contains("factorytest") ||
        packageName.contains("factory_test") ||
        packageName.contains("factorymode")
}
