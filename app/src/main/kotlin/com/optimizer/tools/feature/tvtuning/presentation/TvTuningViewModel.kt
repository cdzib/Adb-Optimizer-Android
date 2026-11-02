package com.optimizer.tools.feature.tvtuning.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.optimizer.tools.R
import com.optimizer.tools.core.ui.UiText
import com.optimizer.tools.core.ui.asUiText
import com.optimizer.tools.core.ui.uiText
import com.optimizer.tools.domain.debloat.DebloatRun
import com.optimizer.tools.feature.tvtuning.domain.AppliedSetting
import com.optimizer.tools.feature.tvtuning.domain.TvBundle
import com.optimizer.tools.feature.tvtuning.domain.TvModelProfile
import com.optimizer.tools.feature.tvtuning.domain.TvSetting
import com.optimizer.tools.feature.tvtuning.domain.TvTuningCategory
import com.optimizer.tools.feature.tvtuning.domain.TvTuningPreset
import com.optimizer.tools.feature.tvtuning.domain.TvTuningRisk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TvSettingState(
    val setting: TvSetting,
    val current: List<String?> = emptyList(),
    val selected: Boolean = false,
)

data class TvBundleState(
    val bundle: TvBundle,
    val selected: Boolean = false,
)

data class TvTuningState(
    val deviceAvailable: Boolean = true,
    val isTv: Boolean = true,
    val profile: TvModelProfile? = null,
    val settings: List<TvSettingState> = emptyList(),
    val bundles: List<TvBundleState> = emptyList(),
    val preset: TvTuningPreset = TvTuningPreset.RECOMMENDED,
    val loading: Boolean = false,
    val running: Boolean = false,
    val done: Int = 0,
    val total: Int = 0,
    val message: UiText? = null,
    val error: UiText? = null,
    val lastApplied: List<AppliedSetting> = emptyList(),
    val lastRun: DebloatRun? = null,
) {
    val selectionCount: Int
        get() = settings.count { it.selected } + bundles.count { it.selected }

    fun byCategory(category: TvTuningCategory): List<TvSettingState> = settings.filter { it.setting.category == category }
}

class TvTuningViewModel(
    private val environment: TvTuningEnvironment,
) : ViewModel() {
    private val _state = MutableStateFlow(TvTuningState())
    val state: StateFlow<TvTuningState> = _state.asStateFlow()

    private val appliedThisSession = mutableListOf<AppliedSetting>()
    private val appliedRuns = mutableListOf<DebloatRun>()

    fun onEnter() {
        val device = environment.activeDevice()
        val available = environment.available() && device != null
        _state.update {
            it.copy(
                deviceAvailable = available,
                isTv = device?.isTv != false,
                error =
                    when {
                        !available -> uiText(R.string.tv_tuning_no_device)
                        device?.isTv == false -> uiText(R.string.tv_tuning_not_tv)
                        else -> null
                    },
            )
        }
        if (!available || device?.isTv == false) return
        refresh()
    }

    fun refresh() {
        if (!environment.available()) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val profile = environment.profile()
                val settings =
                    environment.settings().map { setting ->
                        TvSettingState(setting = setting, current = environment.currentValues(setting))
                    }
                val bundles = environment.bundles(profile).map { TvBundleState(it) }
                val (selectedSettings, selectedBundles) = selectPreset(settings, bundles, _state.value.preset)
                _state.update {
                    it.copy(
                        loading = false,
                        profile = profile,
                        settings = selectedSettings,
                        bundles = selectedBundles,
                    )
                }
            } catch (error: Exception) {
                _state.update {
                    it.copy(loading = false, error = error.message?.asUiText() ?: uiText(R.string.tv_tuning_error))
                }
            }
        }
    }

    fun selectPreset(preset: TvTuningPreset) {
        val (settings, bundles) = selectPreset(_state.value.settings, _state.value.bundles, preset)
        _state.update { it.copy(preset = preset, settings = settings, bundles = bundles) }
    }

    fun toggleSetting(id: String) {
        _state.update {
            it.copy(
                preset = TvTuningPreset.CUSTOM,
                settings = it.settings.map { item -> if (item.setting.id == id) item.copy(selected = !item.selected) else item },
            )
        }
    }

    fun toggleBundle(id: String) {
        _state.update {
            it.copy(
                preset = TvTuningPreset.CUSTOM,
                bundles = it.bundles.map { item -> if (item.bundle.id == id) item.copy(selected = !item.selected) else item },
            )
        }
    }

    fun apply() {
        if (_state.value.running) return
        val selectedSettings =
            _state.value.settings
                .filter { it.selected }
                .map { it.setting }
        val selectedBundles =
            _state.value.bundles
                .filter { it.selected }
                .map { it.bundle }
        if (selectedSettings.isEmpty() && selectedBundles.isEmpty()) {
            _state.update { it.copy(message = uiText(R.string.debloat_message_nothing_applicable)) }
            return
        }
        val total = selectedSettings.size + selectedBundles.size
        viewModelScope.launch {
            _state.update {
                it.copy(running = true, done = 0, total = total, message = null, error = null)
            }
            val applied = mutableListOf<AppliedSetting>()
            var failures = 0
            var packages = 0
            var skipped = 0
            var lastRun: DebloatRun? = null
            try {
                selectedSettings.forEach { setting ->
                    runCatching { environment.apply(setting) }
                        .onSuccess { applied += it }
                        .onFailure { failures++ }
                    _state.update { it.copy(done = it.done + 1) }
                }
                selectedBundles.forEach { bundle ->
                    runCatching { environment.applyBundle(bundle) }
                        .onSuccess { run ->
                            lastRun = run
                            appliedRuns += run
                            packages += run.count
                            skipped += run.skipped.size
                            failures += run.failures.size
                        }.onFailure { failures++ }
                    _state.update { it.copy(done = it.done + 1) }
                }
                appliedThisSession += applied
                _state.update {
                    it.copy(
                        running = false,
                        done = total,
                        lastApplied = appliedThisSession.toList(),
                        lastRun = lastRun,
                        message =
                            if (failures == 0) {
                                uiText(R.string.tv_tuning_applied, applied.size + packages)
                            } else {
                                uiText(R.string.tv_tuning_partial, applied.size + packages, failures, skipped)
                            },
                    )
                }
                refresh()
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        running = false,
                        error = error.message?.asUiText() ?: uiText(R.string.tv_tuning_error),
                    )
                }
            }
        }
    }

    fun undo() {
        if (_state.value.running || appliedThisSession.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(running = true, message = null, error = null) }
            appliedThisSession.asReversed().forEach { applied ->
                runCatching { environment.undo(applied) }
            }
            appliedRuns.asReversed().forEach { run -> runCatching { environment.undoRun(run) } }
            appliedRuns.clear()
            appliedThisSession.clear()
            _state.update {
                it.copy(
                    running = false,
                    lastApplied = emptyList(),
                    lastRun = null,
                    message = uiText(R.string.tv_tuning_undone),
                )
            }
            refresh()
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null) }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun selectPreset(
        settings: List<TvSettingState>,
        bundles: List<TvBundleState>,
        preset: TvTuningPreset,
    ): Pair<List<TvSettingState>, List<TvBundleState>> {
        if (preset == TvTuningPreset.CUSTOM) return settings to bundles
        val selectedIds =
            environment
                .settings()
                .filter { preset in it.presets }
                .map { it.id }
                .toSet()
        return settings.map { it.copy(selected = it.setting.id in selectedIds) } to
            bundles.map { it.copy(selected = it.bundle.risk == TvTuningRisk.SAFE) }
    }

    companion object {
        fun factory(environment: TvTuningEnvironment): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = TvTuningViewModel(environment) as T
            }
    }
}
