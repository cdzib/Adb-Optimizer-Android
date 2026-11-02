package com.optimizer.tools.data.debloat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvSafetyGuardTest {
    @Test
    fun `extrae los paquetes que sirven TvInputService`() {
        val output =
            """
            com.google.android.tv/com.android.tv.TvInputService
            com.example.vendor/.input.VendorTvInput
            no-es-un-componente
            """.trimIndent()

        assertEquals(
            setOf("com.google.android.tv", "com.example.vendor"),
            parseServicePackages(output),
        )
    }

    @Test
    fun `protege setupwraith cuando la red guardada es suya`() {
        assertTrue(
            wifiOwnedBySetupWraith(
                "mNetworkInfo cname=com.google.android.tungsten.setupwraith type=WIFI",
            ),
        )
    }

    @Test
    fun `permite setupwraith cuando la red es del sistema`() {
        assertFalse(wifiOwnedBySetupWraith("mNetworkInfo cname=android.uid.system type=WIFI"))
    }

    @Test
    fun `sin redes listadas no se puede verificar y se protege`() {
        assertTrue(wifiOwnedBySetupWraith(""))
        assertTrue(wifiOwnedBySetupWraith("WifiService: no networks"))
    }
}
