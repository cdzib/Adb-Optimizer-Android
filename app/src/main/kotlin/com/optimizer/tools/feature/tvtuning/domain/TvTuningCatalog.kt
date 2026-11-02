package com.optimizer.tools.feature.tvtuning.domain

/**
 * Ajustes de sistema de la puesta a punto de TV. No incluye paquetes: esos salen del
 * perfil del modelo (`tv-models.json`) y los aplica el debloater con sus guards.
 */
object TvTuningCatalog {
    const val ANIMATION_SCALE = "0.5"
    const val SCREEN_TIMEOUT_MS = "300000"
    const val PRIVATE_DNS_HOST = "dns.adguard-dns.com"
    const val BACKGROUND_PROCESS_LIMIT = "2"

    val settings: List<TvSetting> =
        listOf(
            TvSetting(
                id = "animation-scales",
                category = TvTuningCategory.PERFORMANCE,
                risk = TvTuningRisk.SAFE,
                titleKey = "tv_tuning_animations_title",
                summaryKey = "tv_tuning_animations_summary",
                presets = setOf(TvTuningPreset.RECOMMENDED, TvTuningPreset.PERFORMANCE),
                steps =
                    listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale").map { key ->
                        settingsStep(
                            namespace = "global",
                            key = key,
                            value = ANIMATION_SCALE,
                        )
                    },
            ),
            TvSetting(
                id = "screensaver",
                category = TvTuningCategory.DISPLAY,
                risk = TvTuningRisk.SAFE,
                titleKey = "tv_tuning_screensaver_title",
                summaryKey = "tv_tuning_screensaver_summary",
                presets = setOf(TvTuningPreset.RECOMMENDED, TvTuningPreset.PERFORMANCE),
                steps =
                    listOf(
                        settingsStep(namespace = "secure", key = "screensaver_enabled", value = "0"),
                        settingsStep(namespace = "secure", key = "screensaver_activate_on_sleep", value = "0"),
                    ),
            ),
            TvSetting(
                id = "screen-timeout",
                category = TvTuningCategory.DISPLAY,
                risk = TvTuningRisk.SAFE,
                titleKey = "tv_tuning_timeout_title",
                summaryKey = "tv_tuning_timeout_summary",
                presets = setOf(TvTuningPreset.RECOMMENDED),
                steps = listOf(settingsStep(namespace = "system", key = "screen_off_timeout", value = SCREEN_TIMEOUT_MS)),
            ),
            TvSetting(
                id = "dexopt",
                category = TvTuningCategory.CLEANUP,
                risk = TvTuningRisk.SAFE,
                titleKey = "tv_tuning_dexopt_title",
                summaryKey = "tv_tuning_dexopt_summary",
                presets = setOf(TvTuningPreset.RECOMMENDED, TvTuningPreset.PERFORMANCE),
                steps =
                    listOf(
                        TvSettingStep(
                            apply = "cmd package bg-dexopt-job",
                            timeoutMs = 120_000L,
                        ),
                    ),
            ),
            TvSetting(
                id = "trim-caches",
                category = TvTuningCategory.CLEANUP,
                risk = TvTuningRisk.SAFE,
                titleKey = "tv_tuning_trim_title",
                summaryKey = "tv_tuning_trim_summary",
                presets = setOf(TvTuningPreset.RECOMMENDED, TvTuningPreset.PERFORMANCE),
                steps =
                    listOf(
                        TvSettingStep(
                            apply = "pm trim-caches 100G",
                            verify = "echo ok",
                            verifyEquals = "ok",
                        ),
                    ),
            ),
            TvSetting(
                id = "fixed-performance",
                category = TvTuningCategory.PERFORMANCE,
                risk = TvTuningRisk.ADVANCED,
                titleKey = "tv_tuning_fixed_performance_title",
                summaryKey = "tv_tuning_fixed_performance_summary",
                presets = setOf(TvTuningPreset.PERFORMANCE),
                steps =
                    listOf(
                        TvSettingStep(
                            apply = "cmd power set-fixed-performance-mode-enabled true",
                            undo = "cmd power set-fixed-performance-mode-enabled false",
                            timeoutMs = 10_000L,
                        ),
                    ),
            ),
            TvSetting(
                id = "private-dns",
                category = TvTuningCategory.PRIVACY,
                risk = TvTuningRisk.SAFE,
                titleKey = "tv_tuning_dns_title",
                summaryKey = "tv_tuning_dns_summary",
                presets = setOf(TvTuningPreset.PRIVACY),
                steps =
                    listOf(
                        settingsStep(namespace = "global", key = "private_dns_mode", value = "hostname"),
                        settingsStep(namespace = "global", key = "private_dns_specifier", value = PRIVATE_DNS_HOST),
                    ),
            ),
            TvSetting(
                id = "limit-ad-tracking",
                category = TvTuningCategory.PRIVACY,
                risk = TvTuningRisk.ADVANCED,
                titleKey = "tv_tuning_ad_tracking_title",
                summaryKey = "tv_tuning_ad_tracking_summary",
                presets = setOf(TvTuningPreset.PRIVACY),
                steps = listOf(settingsStep(namespace = "secure", key = "limit_ad_tracking", value = "1")),
            ),
            TvSetting(
                id = "background-limit",
                category = TvTuningCategory.PERFORMANCE,
                risk = TvTuningRisk.ADVANCED,
                titleKey = "tv_tuning_background_title",
                summaryKey = "tv_tuning_background_summary",
                presets = setOf(TvTuningPreset.PERFORMANCE),
                steps =
                    listOf(
                        settingsStep(
                            namespace = "global",
                            key = "background_process_limit",
                            value = BACKGROUND_PROCESS_LIMIT,
                        ),
                    ),
            ),
        )

    /** Bundles reconocidos en `tv-models.json`, en el orden en que se muestran. */
    val bundleDefinitions: List<BundleDefinition> =
        listOf(
            BundleDefinition("telemetry", "tv_bundle_telemetry", TvTuningRisk.SAFE),
            BundleDefinition("ads", "tv_bundle_ads", TvTuningRisk.SAFE),
            BundleDefinition("preload", "tv_bundle_preload", TvTuningRisk.ADVANCED),
        )

    fun setting(id: String): TvSetting? = settings.firstOrNull { it.id == id }

    /** Un preset nunca incluye ajustes avanzados salvo que se pida explícitamente. */
    fun settingsFor(preset: TvTuningPreset): List<TvSetting> =
        if (preset == TvTuningPreset.CUSTOM) {
            settings
        } else {
            settings.filter { preset in it.presets }
        }

    private fun settingsStep(
        namespace: String,
        key: String,
        value: String,
    ): TvSettingStep =
        TvSettingStep(
            read = "settings get $namespace $key",
            apply = "settings put $namespace $key $value",
            undo = "settings put $namespace $key {{prev}}",
            undoIfUnset = "settings delete $namespace $key",
            verify = "settings get $namespace $key",
            verifyEquals = value,
        )

    data class BundleDefinition(
        val id: String,
        val titleKey: String,
        val risk: TvTuningRisk,
    )
}
