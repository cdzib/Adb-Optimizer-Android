package com.optimizer.tools.feature.tvtuning.domain

/** Bloque visible de la puesta a punto de TV. */
enum class TvTuningCategory {
    PERFORMANCE,
    DISPLAY,
    PRIVACY,
    CLEANUP,
}

/** Un ajuste avanzado puede romper funciones del fabricante; se confirma aparte. */
enum class TvTuningRisk {
    SAFE,
    ADVANCED,
}

/** Presets que agrupan ajustes y paquetes. */
enum class TvTuningPreset {
    RECOMMENDED,
    PERFORMANCE,
    PRIVACY,
    CUSTOM,
}

/**
 * Un comando con su lectura previa y su vuelta atrás. `undo` y `undoIfUnset` aceptan
 * `{{prev}}`, igual que las recetas del catalogo.
 */
data class TvSettingStep(
    val read: String? = null,
    val apply: String,
    val undo: String? = null,
    val undoIfUnset: String? = null,
    val verify: String? = null,
    val verifyEquals: String? = null,
    val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
) {
    val reversible: Boolean get() = undo != null || undoIfUnset != null

    companion object {
        const val DEFAULT_TIMEOUT_MS = 15_000L
    }
}

/** Ajuste de sistema con valor actual y vuelta atrás. */
data class TvSetting(
    val id: String,
    val category: TvTuningCategory,
    val risk: TvTuningRisk,
    val titleKey: String,
    val summaryKey: String,
    val presets: Set<TvTuningPreset>,
    val steps: List<TvSettingStep>,
) {
    val reversible: Boolean get() = steps.all { it.reversible }
}

/** Valor que tenia cada paso antes de aplicarlo; se usa para deshacer. */
data class AppliedSettingStep(
    val step: TvSettingStep,
    val previous: String?,
)

data class AppliedSetting(
    val setting: TvSetting,
    val steps: List<AppliedSettingStep>,
)

/** Bundle de paquetes aportado por el perfil del modelo (telemetria, anuncios, precargados). */
data class TvBundle(
    val id: String,
    val titleKey: String,
    val risk: TvTuningRisk,
    val packages: List<String>,
)

/** Perfil de una TV concreta, importado de listas curadas. */
data class TvModelProfile(
    val brand: String,
    val model: String,
    val device: String,
    val boards: List<String>,
    val sdk: Int?,
    val notes: String,
    val bundles: Map<String, List<String>>,
) {
    fun bundle(
        id: String,
        titleKey: String,
        risk: TvTuningRisk,
    ): TvBundle? {
        val packages = bundles[id]?.filter { it.isNotBlank() }.orEmpty()
        if (packages.isEmpty()) return null
        return TvBundle(id = id, titleKey = titleKey, risk = risk, packages = packages)
    }
}
