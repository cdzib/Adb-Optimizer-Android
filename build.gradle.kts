plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
}

/**
 * El catalogo y los fixtures de conformidad llegan por el submodulo `shared/`
 * (repo adb-optimizer-contract). No hay copia vendorizada: `-PsharedDir` sigue
 * teniendo prioridad para desarrollo. Se usa la primera carpeta con
 * `recipes/recipes.json`: `-PsharedDir=...`, `ADB_OPTIMIZER_SHARED` y `shared`.
 */
val sharedRootDir: File? =
    listOfNotNull(
        providers.gradleProperty("sharedDir").orNull,
        providers.environmentVariable("ADB_OPTIMIZER_SHARED").orNull,
        file("shared").path,
    ).map(::File)
        .firstOrNull { File(it, "recipes/recipes.json").isFile }

extra["sharedRootDir"] = sharedRootDir
