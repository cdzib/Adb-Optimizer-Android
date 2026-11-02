package com.optimizer.tools.feature.tvtuning

import com.optimizer.tools.feature.tvtuning.domain.TvTuningCatalog
import com.optimizer.tools.feature.tvtuning.domain.TvTuningPreset
import com.optimizer.tools.feature.tvtuning.domain.TvTuningRisk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TvTuningCatalogTest {
    @Test
    fun `el preset recomendado solo lleva ajustes seguros`() {
        val settings = TvTuningCatalog.settingsFor(TvTuningPreset.RECOMMENDED)

        assertTrue(settings.isNotEmpty())
        assertTrue(settings.all { it.risk == TvTuningRisk.SAFE })
        assertEquals(
            setOf("animation-scales", "screensaver", "screen-timeout", "dexopt", "trim-caches"),
            settings.map { it.id }.toSet(),
        )
    }

    @Test
    fun `rendimiento incluye los ajustes avanzados`() {
        val ids = TvTuningCatalog.settingsFor(TvTuningPreset.PERFORMANCE).map { it.id }

        assertTrue("fixed-performance" in ids)
        assertTrue("background-limit" in ids)
    }

    @Test
    fun `privacidad solo toca dns y seguimiento`() {
        val ids = TvTuningCatalog.settingsFor(TvTuningPreset.PRIVACY).map { it.id }

        assertEquals(setOf("private-dns", "limit-ad-tracking"), ids.toSet())
    }

    @Test
    fun `personalizado muestra todos los ajustes`() {
        assertEquals(
            TvTuningCatalog.settings.size,
            TvTuningCatalog.settingsFor(TvTuningPreset.CUSTOM).size,
        )
    }

    @Test
    fun `todo ajuste con valor previo se puede deshacer`() {
        TvTuningCatalog.settings.forEach { setting ->
            if (setting.steps.any { it.read != null }) {
                assertTrue("${setting.id} debería ser reversible", setting.reversible)
            }
        }
    }

    @Test
    fun `los ids de ajuste son unicos`() {
        val ids = TvTuningCatalog.settings.map { it.id }.toSet()

        assertEquals(TvTuningCatalog.settings.size, ids.size)
    }
}
