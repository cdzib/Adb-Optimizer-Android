package com.optimizer.tools.app

import com.optimizer.tools.data.AppContainer
import com.optimizer.tools.domain.debloat.DebloatAction
import com.optimizer.tools.domain.debloat.DebloatRun
import com.optimizer.tools.feature.tvtuning.data.TvModelsCatalog
import com.optimizer.tools.feature.tvtuning.data.TvTuningRepository
import com.optimizer.tools.feature.tvtuning.domain.AppliedSetting
import com.optimizer.tools.feature.tvtuning.domain.TvBundle
import com.optimizer.tools.feature.tvtuning.domain.TvModelProfile
import com.optimizer.tools.feature.tvtuning.domain.TvSetting
import com.optimizer.tools.feature.tvtuning.domain.TvTuningCatalog
import com.optimizer.tools.feature.tvtuning.presentation.TvTuningEnvironment
import dev.adboptimizer.core.model.DeviceInfo

/** Adaptador de composicion para la puesta a punto de TV. */
internal class AppContainerTvTuningEnvironment(
    private val container: AppContainer,
) : TvTuningEnvironment {
    private val repository = TvTuningRepository(shell = { container.activeShell() })
    private val models = TvModelsCatalog()

    override fun activeDevice(): DeviceInfo? = container.device

    override fun available(): Boolean = container.activeShell() != null

    override suspend fun profile(): TvModelProfile? {
        val device = container.device ?: return null
        return models.match(container.tvModels, device)
    }

    override fun settings(): List<TvSetting> = TvTuningCatalog.settings

    override fun bundles(profile: TvModelProfile?): List<TvBundle> {
        val matched = profile ?: return emptyList()
        return TvTuningCatalog.bundleDefinitions.mapNotNull { definition ->
            matched.bundle(definition.id, definition.titleKey, definition.risk)
        }
    }

    override suspend fun currentValues(setting: TvSetting): List<String?> = repository.readCurrent(setting)

    override suspend fun apply(setting: TvSetting): AppliedSetting = repository.apply(setting)

    override suspend fun undo(setting: AppliedSetting) = repository.undo(setting)

    override suspend fun applyBundle(bundle: TvBundle): DebloatRun = container.debloat.apply(DebloatAction.FREEZE, bundle.packages)

    override suspend fun undoRun(run: DebloatRun): Int = container.debloat.undo(run)
}
