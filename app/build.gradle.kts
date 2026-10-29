import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/** Carpeta `shared/` del repo de escritorio, resuelta una sola vez en el build raiz. */
val sharedCatalogDir: File? = rootProject.extra["sharedRootDir"] as File?

val sharedCatalogPresent = sharedCatalogDir != null

/**
 * Copia versionada del catalogo para que un clon limpio (CI o F-Droid) pueda empaquetar el APK.
 * `-PsharedDir` / `ADB_OPTIMIZER_SHARED` siguen teniendo prioridad durante el desarrollo.
 */
val vendoredCatalogDir = File(projectDir, "catalog")
val vendoredCatalogPresent = File(vendoredCatalogDir, "recipes/recipes.json").isFile

val catalogPresent = sharedCatalogPresent || vendoredCatalogPresent

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    FileInputStream(keystorePropertiesFile).use { keystoreProperties.load(it) }
}

android {
    namespace = "com.optimizer.tools"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.optimizer.tools"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                val storeFilePath = keystoreProperties["storeFile"] as String
                storeFile = rootProject.file(storeFilePath)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            signingConfig =
                if (keystorePropertiesFile.exists()) {
                    signingConfigs.getByName("release")
                } else {
                    null
                }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }

    // El catalogo sale de `-PsharedDir` si existe; si no, de la copia versionada `app/catalog`.
    sourceSets["main"].assets.srcDir(sharedCatalogDir ?: vendoredCatalogDir)

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:adb"))
    implementation(project(":core:optimization"))
    implementation(libs.dadb) {
        // dadb publica junit-platform-native en runtime; no debe viajar en el APK.
        exclude(group = "org.graalvm.buildtools", module = "junit-platform-native")
    }

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.zxing.core)
    implementation(libs.zxing.embedded)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// `scrcpy-server` se compila desde fuente y se empaqueta como `assets/scrcpy/scrcpy-server.jar`.
// Asi el APK se reproduce desde un clon limpio y no viaja ningun binario precompilado en el repo.
val scrcpyAssetsDir = layout.buildDirectory.dir("generated/scrcpy-assets")

val scrcpyServerApk =
    project(":scrcpy-server")
        .layout.buildDirectory
        .file("outputs/apk/release/scrcpy-server-release-unsigned.apk")

val prepareScrcpyServer =
    tasks.register<Sync>("prepareScrcpyServer") {
        group = "build"
        description = "Compila scrcpy-server y lo deja en los assets generados."
        dependsOn(":scrcpy-server:assembleRelease")
        into(scrcpyAssetsDir)
        from(scrcpyServerApk) {
            into("scrcpy")
            rename { "scrcpy-server.jar" }
        }
    }

// Se registra como provider para que cualquier consumidor de assets (merge, lint) dependa del task.
android.sourceSets["main"].assets.srcDir(prepareScrcpyServer)
tasks
    .matching { it.name.startsWith("lint") || it.name.contains("Lint") }
    .configureEach { dependsOn(prepareScrcpyServer) }

// Un APK sin catalogo no sirve: el empaquetado falla con un mensaje que dice como resolverlo.
tasks
    .matching {
        it.name.startsWith("merge") &&
            it.name.endsWith("Assets") &&
            !it.name.contains("UnitTest") &&
            !it.name.contains("AndroidTest")
    }.configureEach {
        dependsOn(prepareScrcpyServer)
        if (!catalogPresent) {
            doFirst {
                throw GradleException(
                    "No se encontro el catalogo (recipes/recipes.json). " +
                        "Restaura app/catalog/recipes o ejecuta con " +
                        "-PsharedDir=/ruta/al/shared.",
                )
            }
        }
    }

// Los tests del catalogo reciben la misma carpeta que el empaquetado, para no adivinar rutas.
// Se declara como entrada para que un cambio en el catalogo invalide la tarea.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    sharedCatalogDir?.let {
        systemProperty("adb.shared.dir", it.absolutePath)
        inputs.dir(it).withPropertyName("sharedCatalog")
    }
}

detekt {
    buildUponDefaultConfig = false
    parallel = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
}

ktlint {
    version.set(libs.versions.ktlintCli.get())
    android.set(true)
    filter {
        exclude { it.file.path.contains("/build/") }
    }
}
