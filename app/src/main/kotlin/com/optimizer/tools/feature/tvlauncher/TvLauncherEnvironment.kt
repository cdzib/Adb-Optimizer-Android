package com.optimizer.tools.feature.tvlauncher

import android.net.Uri
import dev.adboptimizer.core.model.DeviceInfo
import dev.adboptimizer.core.model.RemoteLauncher

/** Puertos de la feature Launchers de TV. */
interface TvLauncherEnvironment {
    fun activeDevice(): DeviceInfo?

    suspend fun homeLaunchers(): List<RemoteLauncher>

    suspend fun currentHomeLauncher(): RemoteLauncher?

    suspend fun setHomeLauncher(packageName: String): RemoteLauncher

    /** Instala un APK de launcher en la TV y devuelve el paquete instalado. */
    suspend fun installLauncherApk(
        uri: Uri,
        onProgress: (String) -> Unit,
    ): String
}
