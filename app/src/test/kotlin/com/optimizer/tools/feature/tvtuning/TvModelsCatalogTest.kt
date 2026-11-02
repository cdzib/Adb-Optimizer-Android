package com.optimizer.tools.feature.tvtuning

import com.optimizer.tools.feature.tvtuning.data.TvModelsCatalog
import com.optimizer.tools.feature.tvtuning.data.normalizeSettingValue
import com.optimizer.tools.feature.tvtuning.data.undoCommand
import com.optimizer.tools.feature.tvtuning.domain.TvSettingStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TvModelsCatalogTest {
    private val sample =
        """
        {
          "schemaVersion": 1,
          "models": [
            {
              "brand": "Xiaomi",
              "model": "MiTV-MOOQ1",
              "device": "tarzan",
              "boards": ["mocha"],
              "sdk": 30,
              "bundles": {
                "telemetry": ["com.miui.tv.analytics", "com.miui.tv.analytics"],
                "ads": []
              }
            }
          ]
        }
        """.trimIndent()

    @Test
    fun `lee los perfiles y deduplica paquetes`() {
        val models = TvModelsCatalog().parse(sample)

        assertEquals(1, models.size)
        assertEquals("Xiaomi", models.single().brand)
        assertEquals(listOf("com.miui.tv.analytics"), models.single().bundles["telemetry"])
    }

    @Test
    fun `empareja por modelo, producto o board`() {
        val models = TvModelsCatalog().parse(sample)

        assertEquals("MiTV-MOOQ1", TvModelsCatalog.matchModel(models, "MiTV-MOOQ1", null, null)?.model)
        assertEquals("MiTV-MOOQ1", TvModelsCatalog.matchModel(models, "otro", "tarzan", null)?.model)
        assertEquals("MiTV-MOOQ1", TvModelsCatalog.matchModel(models, "otro", "otro", "mocha")?.model)
        assertNull(TvModelsCatalog.matchModel(models, "otro", "otro", "otro"))
    }

    @Test
    fun `un JSON invalido no rompe la carga`() {
        assertTrue(TvModelsCatalog().parse("{no es json").isEmpty())
    }

    @Test
    fun `el catalogo incluido en assets se puede leer`() {
        val file =
            listOf(
                File("src/main/assets/tv/tv-models.json"),
                File("app/src/main/assets/tv/tv-models.json"),
            ).firstOrNull { it.isFile }
                ?: return

        val models = TvModelsCatalog().parse(file.readText())

        assertTrue(models.isNotEmpty())
        assertTrue(models.any { it.model == "MiTV-MOOQ1" })
    }
}

class TvTuningRepositoryTest {
    @Test
    fun `vuelve al valor previo cuando existia`() {
        val step = TvSettingStep(read = "settings get global x", apply = "settings put global x 0")

        assertEquals(
            "settings put global x 1.0",
            undoCommand(step.copy(undo = "settings put global x {{prev}}"), "1.0"),
        )
    }

    @Test
    fun `borra el ajuste cuando no existia`() {
        val step =
            TvSettingStep(
                read = "settings get global x",
                apply = "settings put global x 0",
                undo = "settings put global x {{prev}}",
                undoIfUnset = "settings delete global x",
            )

        assertEquals("settings delete global x", undoCommand(step, null))
    }

    @Test
    fun `un paso sin vuelta atras no produce comando`() {
        val step = TvSettingStep(apply = "pm trim-caches 100G")

        assertNull(undoCommand(step, null))
    }

    @Test
    fun `normaliza null y vacio de settings get`() {
        assertNull(normalizeSettingValue(null))
        assertNull(normalizeSettingValue(""))
        assertNull(normalizeSettingValue("null"))
        assertEquals("0.5", normalizeSettingValue(" 0.5\n"))
    }
}
