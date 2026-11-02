package dev.adboptimizer.core.model

/** Origen sobre el que se administra una app. */
enum class AppTarget { REMOTE, LOCAL }

/** Filtros de la lista de apps; Usuario/Sistema son excluyentes entre si. */
enum class AppFilter { USER, SYSTEM, DISABLED }

enum class AppSort { NAME, SIZE, UPDATED, VERSION }

/** Acciones de administracion que entiende la UI. */
enum class AppAction {
    OPEN,
    FORCE_STOP,
    ENABLE,
    DISABLE,
    CLEAR_DATA,
    CLEAR_CACHE,
    UNINSTALL,
    EXTRACT_APK,
    APP_INFO,

    /** Compila el codigo dex de la app con el filtro ART `speed` para que arranque sin interpretar. */
    COMPILE_AOT,

    /** Pone la app en el bucket de standby `restricted`. */
    RESTRICT_STANDBY,

    /** Bloquea la ejecucion en segundo plano con AppOps `RUN_IN_BACKGROUND ignore`. */
    RESTRICT_BACKGROUND,
}

/**
 * App instalada, ya sea en el dispositivo remoto (leida por ADB) o en el telefono
 * local (leida con PackageManager). Los campos desconocidos quedan en 0/null.
 */
data class ManagedApp(
    val packageName: String,
    val label: String = packageName,
    val versionName: String = "",
    val versionCode: Long = 0L,
    val minSdk: Int = 0,
    val targetSdk: Int = 0,
    val isSystem: Boolean = false,
    val enabled: Boolean = true,
    val sizeBytes: Long = 0L,
    val installedAt: Long = 0L,
    val updatedAt: Long = 0L,
    val hasSplits: Boolean = false,
    val apkPaths: List<String> = emptyList(),
    val installerPackage: String? = null,
    val requestedPermissions: List<String> = emptyList(),
) {
    val displayName: String get() = label.ifBlank { packageName }
}

data class AppActionResult(
    val success: Boolean,
    val message: String,
)

/** Paquetes que nunca deben desinstalarse/deshabilitarse/limpiarse desde la app. */
object AppSafety {
    val criticalPackages: Set<String> =
        setOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.tv.settings",
            "com.android.shell",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.samsung.android.packageinstaller",
            "com.sec.android.app.launcher",
            // Launcher propio de TV: si se congela, la TV se queda sin pantalla de inicio.
            "com.tv.launcher.custom",
            // Núcleo de Android/MediaTek cuya desactivación deja el equipo sin arrancar.
            "com.android.sdksandbox",
            "com.android.location.fused",
            "com.android.modulemetadata",
            "com.android.ext.services",
            "com.android.ext.shared",
            "android.ext.services",
            "android.ext.shared",
            "com.google.android.ext.services",
            "com.google.android.ext.shared",
            "com.google.android.modulemetadata",
            "com.cltv.mal",
            "com.mediatek.tis",
            "com.mediatek.tv.service",
            "com.mediatek.tv.agent",
        )

    /** Proveedores del sistema que sí se pueden desactivar sin romper el arranque. */
    private val disposableProviders =
        setOf(
            "com.android.providers.calendar",
            "com.android.providers.contacts",
            "com.android.providers.userdictionary",
            "com.android.providers.blockednumber",
        )

    /** Overlays de mainline: sin ellos el framework puede quedar inconsistente. */
    private const val MODULE_OVERLAY_PREFIX = "com.google.android.overlay.modules."
    private const val SYSTEM_PROVIDER_PREFIX = "com.android.providers."

    fun isCritical(packageName: String): Boolean =
        packageName in criticalPackages ||
            packageName.startsWith(MODULE_OVERLAY_PREFIX) ||
            (
                packageName.startsWith(SYSTEM_PROVIDER_PREFIX) &&
                    packageName !in disposableProviders
            ) ||
            packageName == "com.optimizer.tools" ||
            packageName == "com.optimizer.tools.debug" ||
            packageName.startsWith("com.optimizer.tools.")
}
