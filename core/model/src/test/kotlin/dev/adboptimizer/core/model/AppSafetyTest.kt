package dev.adboptimizer.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSafetyTest {
    @Test
    fun `protege el nucleo de Android TV y MediaTek`() {
        assertTrue(AppSafety.isCritical("com.android.tv.settings"))
        assertTrue(AppSafety.isCritical("com.android.location.fused"))
        assertTrue(AppSafety.isCritical("com.android.sdksandbox"))
        assertTrue(AppSafety.isCritical("com.cltv.mal"))
        assertTrue(AppSafety.isCritical("com.mediatek.tis"))
        assertTrue(AppSafety.isCritical("com.mediatek.tv.service"))
        assertTrue(AppSafety.isCritical("com.mediatek.tv.agent"))
    }

    @Test
    fun `protege overlays de mainline y proveedores del sistema`() {
        assertTrue(AppSafety.isCritical("com.google.android.overlay.modules.permissioncontroller"))
        assertTrue(AppSafety.isCritical("com.android.providers.settings"))
        assertTrue(AppSafety.isCritical("com.android.providers.media"))
        assertTrue(AppSafety.isCritical("com.android.providers.telephony"))
    }

    @Test
    fun `permite los proveedores que las listas de TV apagan`() {
        assertFalse(AppSafety.isCritical("com.android.providers.calendar"))
        assertFalse(AppSafety.isCritical("com.android.providers.contacts"))
        assertFalse(AppSafety.isCritical("com.android.providers.userdictionary"))
        assertFalse(AppSafety.isCritical("com.android.providers.blockednumber"))
    }

    @Test
    fun `protege el launcher propio y los paquetes de la app`() {
        assertTrue(AppSafety.isCritical("com.tv.launcher.custom"))
        assertTrue(AppSafety.isCritical("com.optimizer.tools"))
        assertTrue(AppSafety.isCritical("com.optimizer.tools.debug"))
        assertFalse(AppSafety.isCritical("com.google.android.apps.tv.launcherx"))
    }
}
