plugins {
    alias(libs.plugins.android.application)
}

/**
 * Servidor de scrcpy empaquetado como APK y renombrado a `scrcpy-server.jar` en los assets de `:app`.
 *
 * Se compila desde fuente (no se versiona el binario) para que el APK de la app pueda reproducirse
 * en un clon limpio y para cumplir la politica de F-Droid sobre binarios precompilados.
 *
 * Origen: https://github.com/Genymobile/scrcpy (tag v5.0.1, Apache-2.0). Ver `README.md`.
 */
android {
    namespace = "com.genymobile.scrcpy"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.genymobile.scrcpy"
        minSdk = 21
        targetSdk = 36
        versionCode = 50001
        versionName = "5.0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        buildConfig = true
        aidl = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        disable += "UseRequiresApi"
    }
}

dependencies {
    testImplementation(libs.junit)
}
