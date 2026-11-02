package com.optimizer.tools.feature.tvtuning

import com.optimizer.tools.domain.debloat.DebloatAction
import com.optimizer.tools.domain.debloat.DebloatRun
import com.optimizer.tools.domain.profile.ProfileAppState
import com.optimizer.tools.domain.profile.ProfileChange
import com.optimizer.tools.feature.tvtuning.domain.AppliedSetting
import com.optimizer.tools.feature.tvtuning.domain.AppliedSettingStep
import com.optimizer.tools.feature.tvtuning.domain.TvBundle
import com.optimizer.tools.feature.tvtuning.domain.TvModelProfile
import com.optimizer.tools.feature.tvtuning.domain.TvSetting
import com.optimizer.tools.feature.tvtuning.domain.TvTuningCatalog
import com.optimizer.tools.feature.tvtuning.domain.TvTuningPreset
import com.optimizer.tools.feature.tvtuning.domain.TvTuningRisk
import com.optimizer.tools.feature.tvtuning.presentation.TvTuningEnvironment
import com.optimizer.tools.feature.tvtuning.presentation.TvTuningViewModel
import com.optimizer.tools.testutil.runViewModelTest
import dev.adboptimizer.core.model.DeviceInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TvTuningViewModelTest {
    @Test
    fun `el preset recomendado selecciona ajustes seguros y bundles seguros`() =
        runViewModelTest {
            val environment = FakeEnvironment()
            val viewModel = TvTuningViewModel(environment)

            viewModel.onEnter()
            advanceUntilIdle()

            val state = viewModel.state.value
            val selectedSettings = state.settings.filter { it.selected }
            val selectedBundles = state.bundles.filter { it.selected }

            assertEquals(TvTuningPreset.RECOMMENDED, state.preset)
            assertTrue(selectedSettings.all { it.setting.risk == TvTuningRisk.SAFE })
            assertEquals(listOf("telemetry"), selectedBundles.map { it.bundle.id })
            assertFalse(state.settings.isEmpty())
        }

    @Test
    fun `cambiar un ajuste pasa el preset a personalizado`() =
        runViewModelTest {
            val viewModel = TvTuningViewModel(FakeEnvironment())
            viewModel.onEnter()
            advanceUntilIdle()

            viewModel.toggleSetting("screen-timeout")

            assertEquals(TvTuningPreset.CUSTOM, viewModel.state.value.preset)
        }

    @Test
    fun `aplicar ejecuta ajustes y bundles seleccionados`() =
        runViewModelTest {
            val environment = FakeEnvironment()
            val viewModel = TvTuningViewModel(environment)
            viewModel.onEnter()
            advanceUntilIdle()

            viewModel.apply()
            advanceUntilIdle()

            assertTrue(environment.appliedSettings.any { it.id == "animation-scales" })
            assertEquals(listOf("telemetry"), environment.appliedBundles.map { it.id })
            val state = viewModel.state.value
            assertFalse(state.running)
            assertEquals(1, state.lastRun?.count)
        }

    @Test
    fun `deshacer revierte ajustes y paquetes`() =
        runViewModelTest {
            val environment = FakeEnvironment()
            val viewModel = TvTuningViewModel(environment)
            viewModel.onEnter()
            advanceUntilIdle()
            viewModel.apply()
            advanceUntilIdle()

            viewModel.undo()
            advanceUntilIdle()

            val applied = viewModel.state.value.lastApplied

            assertEquals(environment.appliedSettings.size, environment.undoCalls)
            assertEquals(1, environment.undoRunCalls)
            assertTrue(applied.isEmpty())
        }

    @Test
    fun `sin dispositivo no carga ajustes`() =
        runViewModelTest {
            val environment = FakeEnvironment(available = false)
            val viewModel = TvTuningViewModel(environment)

            viewModel.onEnter()
            advanceUntilIdle()

            assertFalse(viewModel.state.value.deviceAvailable)
            assertTrue(environment.appliedSettings.isEmpty())
        }

    private class FakeEnvironment(
        private val available: Boolean = true,
    ) : TvTuningEnvironment {
        val appliedSettings = mutableListOf<TvSetting>()
        val appliedBundles = mutableListOf<TvBundle>()
        var undoCalls = 0
        var undoRunCalls = 0

        override fun activeDevice(): DeviceInfo? = if (available) tv else null

        override fun available(): Boolean = available

        override suspend fun profile(): TvModelProfile? = previewProfile

        override fun settings(): List<TvSetting> = TvTuningCatalog.settings

        override fun bundles(profile: TvModelProfile?): List<TvBundle> =
            listOf(
                TvBundle("telemetry", "tv_bundle_telemetry", TvTuningRisk.SAFE, listOf("com.miui.tv.analytics")),
                TvBundle("preload", "tv_bundle_preload", TvTuningRisk.ADVANCED, listOf("com.google.android.feedback")),
            )

        override suspend fun currentValues(setting: TvSetting): List<String?> = setting.steps.map { "1.0" }

        override suspend fun apply(setting: TvSetting): AppliedSetting {
            appliedSettings += setting
            return AppliedSetting(setting, setting.steps.map { AppliedSettingStep(it, "1.0") })
        }

        override suspend fun undo(setting: AppliedSetting) {
            undoCalls++
        }

        override suspend fun applyBundle(bundle: TvBundle): DebloatRun {
            appliedBundles += bundle
            return DebloatRun(
                action = DebloatAction.FREEZE,
                changes =
                    listOf(
                        ProfileChange(
                            bundle.packages.first(),
                            ProfileAppState.ENABLED,
                            ProfileAppState.DISABLED,
                        ),
                    ),
            )
        }

        override suspend fun undoRun(run: DebloatRun): Int {
            undoRunCalls++
            return 0
        }
    }

    private companion object {
        val previewProfile = TvModelProfile("Xiaomi", "MiTV-MOOQ1", "tarzan", emptyList(), 30, "", emptyMap())

        val tv =
            DeviceInfo(
                id = "192.0.2.20:5555",
                model = "TV",
                manufacturer = "Google",
                sdk = 34,
                androidVersion = "14",
                isTv = true,
                hasRoot = false,
                abi = "arm64-v8a",
            )
    }
}
