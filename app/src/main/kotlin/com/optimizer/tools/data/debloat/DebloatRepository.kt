package com.optimizer.tools.data.debloat

import com.optimizer.tools.data.packages.PackageStateReader
import com.optimizer.tools.domain.debloat.DebloatAction
import com.optimizer.tools.domain.debloat.DebloatEntry
import com.optimizer.tools.domain.debloat.DebloatRestoreItem
import com.optimizer.tools.domain.debloat.DebloatRun
import com.optimizer.tools.domain.profile.ProfileAppState
import com.optimizer.tools.domain.profile.ProfileChange
import com.optimizer.tools.domain.profile.ProfilePlanner
import com.optimizer.tools.domain.profile.ProfileStepKind
import dev.adboptimizer.core.shell.AdbShell

/** Ejecuta las tandas del debloater y conserva lo necesario para deshacerlas y restaurarlas. */
class DebloatRepository(
    private val catalog: DebloatCatalog,
    private val journal: DebloatJournal,
    shell: () -> AdbShell?,
) {
    private val packages = PackageStateReader(shell)
    private val homeGuard = HomeLauncherGuard(shell)

    fun available(): Boolean = packages.available()

    fun cachedCatalog(): List<DebloatEntry>? = catalog.cached()

    fun catalogUpdatedAt(): Long? = catalog.updatedAt()

    suspend fun downloadCatalog(onProgress: (Int) -> Unit = {}): List<DebloatEntry> = catalog.download(onProgress)

    suspend fun states(): Map<String, ProfileAppState> = packages.states()

    /**
     * Apps que «Restaurar» puede devolver: las congeladas ahora mismo y las que el
     * debloater desinstalo para el usuario y siguen sin reinstalarse.
     */
    suspend fun restorable(): List<DebloatRestoreItem> {
        val states = packages.states()
        val disabled = states.filterValues { it == ProfileAppState.DISABLED }.keys
        val uninstalled = journal.uninstalled().filter { states[it] == ProfileAppState.UNINSTALLED }
        return (disabled + uninstalled)
            .map { packageName ->
                DebloatRestoreItem(
                    packageName = packageName,
                    label = packageName,
                    state = states.getValue(packageName),
                )
            }.sortedBy { it.packageName }
    }

    /** Congela, descongela o desinstala en orden; un fallo se anota y no detiene la tanda. */
    suspend fun apply(
        action: DebloatAction,
        targets: List<String>,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> },
    ): DebloatRun {
        val before = packages.states()
        val protected = runCatching { homeGuard.blocked(action, targets) }.getOrDefault(emptySet())
        val changes = mutableListOf<ProfileChange>()
        val failures = mutableListOf<String>()
        val skipped = mutableListOf<String>()
        var done = 0
        targets.forEach { packageName ->
            if (packageName in protected) {
                skipped += packageName
            } else {
                runCatching { packages.execute(action.step, packageName) }
                    .onSuccess {
                        if (action == DebloatAction.UNINSTALL) journal.recordUninstall(packageName)
                        changes += ProfileChange(packageName, before[packageName] ?: ProfileAppState.ENABLED, action.targetState)
                    }.onFailure { failures += packageName }
            }
            done++
            onProgress(done, targets.size, packageName)
        }
        return DebloatRun(
            action = action,
            changes = changes.toList(),
            failures = failures.toList(),
            skipped = skipped.toList(),
        )
    }

    /** Devuelve cada paquete al estado que tenia antes de la tanda. */
    suspend fun undo(run: DebloatRun): Int {
        var restored = 0
        ProfilePlanner.undoPlan(run.changes).forEach { step ->
            step.packages.forEach { packageName ->
                runCatching { packages.execute(step.kind, packageName) }
                    .onSuccess {
                        journal.forget(packageName)
                        restored++
                    }
            }
        }
        return restored
    }

    /** Vuelve a habilitar o reinstalar los paquetes elegidos, leyendo su estado actual. */
    suspend fun restore(
        targets: List<String>,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> },
    ): DebloatRun {
        val before = packages.states()
        val changes = mutableListOf<ProfileChange>()
        val failures = mutableListOf<String>()
        var done = 0
        targets.forEach { packageName ->
            val kind =
                when (before[packageName]) {
                    ProfileAppState.DISABLED -> ProfileStepKind.ENABLE
                    ProfileAppState.UNINSTALLED -> ProfileStepKind.REINSTALL
                    else -> null
                }
            if (kind != null) {
                runCatching { packages.execute(kind, packageName) }
                    .onSuccess {
                        journal.forget(packageName)
                        changes += ProfileChange(packageName, before.getValue(packageName), ProfileAppState.ENABLED)
                    }.onFailure { failures += packageName }
            }
            done++
            onProgress(done, targets.size, packageName)
        }
        return DebloatRun(action = DebloatAction.RESTORE, changes = changes.toList(), failures = failures.toList())
    }
}
