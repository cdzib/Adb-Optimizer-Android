package com.optimizer.tools.feature.tvtuning.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.optimizer.tools.core.ui.AdbOptimizerTheme
import com.optimizer.tools.domain.model.ThemeMode
import com.optimizer.tools.feature.tvtuning.domain.TvBundle
import com.optimizer.tools.feature.tvtuning.domain.TvModelProfile
import com.optimizer.tools.feature.tvtuning.domain.TvTuningCatalog
import com.optimizer.tools.feature.tvtuning.domain.TvTuningPreset
import com.optimizer.tools.feature.tvtuning.domain.TvTuningRisk

private val previewProfile =
    TvModelProfile(
        brand = "Xiaomi",
        model = "MiTV-MOOQ1",
        device = "tarzan",
        boards = emptyList(),
        sdk = 30,
        notes = "",
        bundles = emptyMap(),
    )

private val previewSettings =
    TvTuningCatalog.settingsFor(TvTuningPreset.RECOMMENDED).mapIndexed { index, setting ->
        TvSettingState(
            setting = setting,
            current = setting.steps.map { "1.0" },
            selected = index % 2 == 0,
        )
    }

private val previewBundles =
    listOf(
        TvBundleState(
            TvBundle(
                id = "telemetry",
                titleKey = "tv_bundle_telemetry",
                risk = TvTuningRisk.SAFE,
                packages = listOf("com.miui.tv.analytics", "com.xiaomi.statistic"),
            ),
            selected = true,
        ),
        TvBundleState(
            TvBundle(
                id = "preload",
                titleKey = "tv_bundle_preload",
                risk = TvTuningRisk.ADVANCED,
                packages = listOf("com.google.android.feedback", "fusion.android.tv.demo"),
            ),
            selected = false,
        ),
    )

@Composable
private fun Shell(content: @Composable () -> Unit) {
    AdbOptimizerTheme(themeMode = ThemeMode.DARK) { content() }
}

@Preview(showBackground = true, name = "Puesta a punto TV", heightDp = 900)
@Composable
private fun TvTuningReadyPreview() {
    Shell {
        TvTuningScreen(
            state =
                TvTuningState(
                    profile = previewProfile,
                    settings = previewSettings,
                    bundles = previewBundles,
                    preset = TvTuningPreset.RECOMMENDED,
                ),
            onBack = {},
            onRefresh = {},
            onPreset = {},
            onToggleSetting = {},
            onToggleBundle = {},
            onApply = {},
            onUndo = {},
            onDismissMessage = {},
            onDismissError = {},
        )
    }
}

@Preview(showBackground = true, name = "Puesta a punto TV sin conexión")
@Composable
private fun TvTuningNoDevicePreview() {
    Shell {
        TvTuningScreen(
            state = TvTuningState(deviceAvailable = false),
            onBack = {},
            onRefresh = {},
            onPreset = {},
            onToggleSetting = {},
            onToggleBundle = {},
            onApply = {},
            onUndo = {},
            onDismissMessage = {},
            onDismissError = {},
        )
    }
}
