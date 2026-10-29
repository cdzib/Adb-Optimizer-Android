package com.optimizer.tools.feature.debloat

import com.optimizer.tools.domain.debloat.DebloatAction
import com.optimizer.tools.domain.debloat.DebloatEntry
import com.optimizer.tools.domain.debloat.DebloatRestoreItem
import com.optimizer.tools.domain.debloat.DebloatRun
import com.optimizer.tools.domain.profile.ProfileAppState
import dev.adboptimizer.core.model.ManagedApp

/** Puertos del debloater: catalogo descargable y cambios sobre el destino activo. */
interface DebloatEnvironment {
    fun hasDevice(): Boolean

    fun cachedCatalog(): List<DebloatEntry>?

    fun catalogUpdatedAt(): Long?

    suspend fun downloadCatalog(onProgress: (Int) -> Unit): List<DebloatEntry>

    suspend fun apps(): List<ManagedApp>

    suspend fun states(): Map<String, ProfileAppState>

    /** Apps congeladas o desinstaladas por el debloater que se pueden volver a habilitar. */
    suspend fun restorable(): List<DebloatRestoreItem>

    suspend fun apply(
        action: DebloatAction,
        targets: List<String>,
        onProgress: (Int, Int, String) -> Unit,
    ): DebloatRun

    suspend fun undo(run: DebloatRun): Int

    suspend fun restore(
        targets: List<String>,
        onProgress: (Int, Int, String) -> Unit,
    ): DebloatRun
}
