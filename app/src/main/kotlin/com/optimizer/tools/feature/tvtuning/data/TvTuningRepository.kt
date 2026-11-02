package com.optimizer.tools.feature.tvtuning.data

import com.optimizer.tools.feature.tvtuning.domain.AppliedSetting
import com.optimizer.tools.feature.tvtuning.domain.AppliedSettingStep
import com.optimizer.tools.feature.tvtuning.domain.TvSetting
import com.optimizer.tools.feature.tvtuning.domain.TvSettingStep
import dev.adboptimizer.core.shell.AdbShell
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Lee y aplica los ajustes de la puesta a punto de TV. Cada paso guarda su valor previo y
 * se verifica releyendo el ajuste; si algo falla a mitad, se deshace lo ya aplicado.
 */
class TvTuningRepository(
    private val shell: () -> AdbShell?,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    fun available(): Boolean = shell() != null

    /** Valores actuales de cada paso, en el mismo orden que [TvSetting.steps]. */
    suspend fun readCurrent(setting: TvSetting): List<String?> =
        withContext(io) {
            val adb = requireShell()
            setting.steps.map { step -> step.read?.let { readValue(adb, it) } }
        }

    suspend fun apply(setting: TvSetting): AppliedSetting =
        withContext(io) {
            val adb = requireShell()
            val applied = mutableListOf<AppliedSettingStep>()
            try {
                setting.steps.forEach { step ->
                    val previous = step.read?.let { readValue(adb, it) }
                    exec(adb, step.apply, step.timeoutMs)
                    verify(adb, step)
                    applied += AppliedSettingStep(step, previous)
                }
            } catch (error: Exception) {
                undoSteps(adb, applied.asReversed())
                throw error
            }
            AppliedSetting(setting = setting, steps = applied)
        }

    suspend fun undo(applied: AppliedSetting) =
        withContext(io) {
            undoSteps(requireShell(), applied.steps.asReversed())
        }

    private suspend fun undoSteps(
        adb: AdbShell,
        steps: List<AppliedSettingStep>,
    ) {
        steps.forEach { (step, previous) ->
            val command = undoCommand(step, previous) ?: return@forEach
            runCatching { exec(adb, command, step.timeoutMs) }
        }
    }

    private suspend fun verify(
        adb: AdbShell,
        step: TvSettingStep,
    ) {
        val command = step.verify ?: return
        val expected = step.verifyEquals ?: return
        val actual = readValue(adb, command)
        if (actual != expected) {
            throw IOException("El ajuste no se confirmo: se esperaba $expected y quedo ${actual ?: "sin valor"}.")
        }
    }

    private suspend fun readValue(
        adb: AdbShell,
        command: String,
    ): String? = normalizeSettingValue(exec(adb, command, READ_TIMEOUT_MS))

    private suspend fun exec(
        adb: AdbShell,
        command: String,
        timeoutMs: Long,
    ): String {
        val result = adb.exec(command, timeoutMs)
        if (result.exitCode != 0) {
            throw IOException(result.output.trim().ifBlank { "El dispositivo rechazo «$command»." })
        }
        return result.stdout
    }

    private fun requireShell(): AdbShell = shell() ?: throw IOException("Conecta la TV por ADB para aplicar la puesta a punto.")

    private companion object {
        const val READ_TIMEOUT_MS = 8_000L
    }
}

/** `null`, vacio y la cadena "null" de `settings get` significan que el ajuste no existia. */
internal fun normalizeSettingValue(raw: String?): String? = raw?.trim()?.takeIf { it.isNotEmpty() && it != "null" }

/** Comando para volver al valor previo; `null` si el paso no se puede deshacer. */
internal fun undoCommand(
    step: TvSettingStep,
    previous: String?,
): String? =
    if (previous == null) {
        step.undoIfUnset
    } else {
        step.undo?.replace("{{prev}}", previous)
    }
