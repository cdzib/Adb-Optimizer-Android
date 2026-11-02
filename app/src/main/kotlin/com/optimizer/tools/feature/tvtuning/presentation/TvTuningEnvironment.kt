package com.optimizer.tools.feature.tvtuning.presentation

import com.optimizer.tools.domain.debloat.DebloatRun
import com.optimizer.tools.feature.tvtuning.domain.AppliedSetting
import com.optimizer.tools.feature.tvtuning.domain.TvBundle
import com.optimizer.tools.feature.tvtuning.domain.TvModelProfile
import com.optimizer.tools.feature.tvtuning.domain.TvSetting
import dev.adboptimizer.core.model.DeviceInfo

/** Puertos de la puesta a punto de TV; la composicion los implementa sobre AppContainer. */
interface TvTuningEnvironment {
    fun activeDevice(): DeviceInfo?

    fun available(): Boolean

    suspend fun profile(): TvModelProfile?

    fun settings(): List<TvSetting>

    fun bundles(profile: TvModelProfile?): List<TvBundle>

    suspend fun currentValues(setting: TvSetting): List<String?>

    suspend fun apply(setting: TvSetting): AppliedSetting

    suspend fun undo(setting: AppliedSetting)

    /** Congela el bundle con los guards del debloater (Home, HDMI, setupwraith). */
    suspend fun applyBundle(bundle: TvBundle): DebloatRun

    suspend fun undoRun(run: DebloatRun): Int
}
