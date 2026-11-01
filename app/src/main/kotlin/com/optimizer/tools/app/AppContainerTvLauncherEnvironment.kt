package com.optimizer.tools.app

import android.net.Uri
import com.optimizer.tools.data.AppContainer
import com.optimizer.tools.domain.apk.InstallOptions
import com.optimizer.tools.feature.tvlauncher.TvLauncherEnvironment
import dev.adboptimizer.core.model.DeviceInfo
import dev.adboptimizer.core.model.RemoteLauncher
import java.io.IOException

/** Adaptador de composición para Launchers de TV. */
internal class AppContainerTvLauncherEnvironment(
    private val container: AppContainer,
) : TvLauncherEnvironment {
    override fun activeDevice(): DeviceInfo? = container.device

    override suspend fun homeLaunchers(): List<RemoteLauncher> = container.remoteHomeLaunchers()

    override suspend fun currentHomeLauncher(): RemoteLauncher? = container.currentRemoteHomeLauncher()

    override suspend fun setHomeLauncher(packageName: String): RemoteLauncher = container.setRemoteHomeLauncher(packageName)

    override suspend fun installLauncherApk(
        uri: Uri,
        onProgress: (String) -> Unit,
    ): String {
        val outcomes = container.installApks(listOf(uri), InstallOptions(), onProgress)
        val installed = outcomes.firstOrNull { it.success }
        if (installed == null) {
            throw IOException(outcomes.firstOrNull()?.output?.ifBlank { null } ?: "La TV rechazó el APK del launcher.")
        }
        return installed.packageName
    }
}
