package com.optimizer.tools.data.debloat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLauncherGuardTest {
    @Test
    fun `keeps the current home when the list would leave no launcher`() {
        val blocked =
            protectedLaunchers(
                launchers = setOf("com.tv.launcher.custom"),
                currentHome = "com.tv.launcher.custom",
                targets = setOf("com.tv.launcher.custom"),
            )

        assertEquals(setOf("com.tv.launcher.custom"), blocked)
    }

    @Test
    fun `does not block when another launcher survives`() {
        val blocked =
            protectedLaunchers(
                launchers = setOf("com.android.tvlauncher", "com.tv.launcher.custom"),
                currentHome = "com.tv.launcher.custom",
                targets = setOf("com.tv.launcher.custom"),
            )

        assertTrue(blocked.isEmpty())
    }

    @Test
    fun `keeps one launcher when every claimant is targeted`() {
        val blocked =
            protectedLaunchers(
                launchers = setOf("com.android.tvlauncher", "com.tv.launcher.custom"),
                currentHome = null,
                targets = setOf("com.android.tvlauncher", "com.tv.launcher.custom"),
            )

        assertEquals(setOf("com.android.tvlauncher"), blocked)
    }

    @Test
    fun `setup wizards and fallbacks do not count as launchers`() {
        assertTrue(isNotAHomeLauncher("com.google.android.tungsten.setupwraith/.RecoveryActivity"))
        assertTrue(isNotAHomeLauncher("com.android.tv.settings/com.android.tv.settings.system.FallbackHome"))
        assertTrue(isNotAHomeLauncher("com.tcl.keycustomfunctionservice/.FallbackHome"))
        assertTrue(isNotAHomeLauncher("com.duokan.factorytest/.FactoryLauncher"))
        assertFalse(isNotAHomeLauncher("com.tv.launcher.custom/.MainActivity"))
        assertFalse(isNotAHomeLauncher("com.google.android.apps.tv.launcherx/.MainActivity"))
    }

    @Test
    fun `a list without launchers is never blocked`() {
        val blocked =
            protectedLaunchers(
                launchers = setOf("com.android.tvlauncher"),
                currentHome = "com.android.tvlauncher",
                targets = setOf("com.mitv.tvhome.atv"),
            )

        assertTrue(blocked.isEmpty())
    }
}
