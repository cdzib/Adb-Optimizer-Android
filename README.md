# Android

App Android de AdbOptimizer. Compose + ViewModels con un contrato MVI ligero y DI manual.

## Requisitos

- JDK 17 o superior (el CI usa 21)
- Android SDK 36
- Gradle Wrapper incluido (`./gradlew`)
- Catalogo de recetas: llega por el submodulo `shared/` (repo `adb-optimizer-contract`).
  Clona con `--recurse-submodules` o inicializa con `git submodule update --init`.
  Para desarrollo se puede apuntar a una carpeta local con `-PsharedDir` o
  `ADB_OPTIMIZER_SHARED` (tiene prioridad y tambien alimenta los tests de conformidad).

## Comandos

```bash
./gradlew ktlintCheck
./gradlew detekt
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
./gradlew :app:assembleDebug -PsharedDir=/ruta/al/shared
```

## Estructura

```text
app/
├── src/main/kotlin/com/optimizer/tools/
│   ├── app/          # Composición, estado global y navegación
│   ├── core/ui/      # Tema, UI compartida, video y UiText
│   ├── data/         # Infraestructura y AppContainer
│   ├── domain/       # Modelos, puertos y use cases
│   ├── feature/      # Una carpeta por capacidad de producto
│   └── shizuku/      # Servicio AIDL local
├── src/main/res/     # Recursos Android
└── src/test/         # Tests JVM
```
scrcpy-server/        # Servidor scrcpy compilado desde fuente (se empaqueta como asset)
shared/               # Submodulo: catalogo de recetas, schema, i18n y fixtures
fdroid/               # Metadata para publicar en F-Droid
fastlane/             # Textos de la ficha en F-Droid
```

Cada feature es dueña de su pantalla, contenido, ViewModel y modelos. Las features no
deben depender entre sí; la composición vive en `app/`.

## Documentación

- [Publicación en F-Droid](fdroid/README.md)

- [Arquitectura Android](docs/arquitectura-android.md)
- [Spike de validación Fase 0](docs/fase-0-spike.md)
- [ADR-014: estructura por feature](docs/adr/ADR-014-estructura-por-feature.md)
- [ADR-008: DI manual](docs/adr/ADR-008-di-manual.md)
- [Diagnóstico del dispositivo: revisión e integración de SysInfo](docs/specs/sysinfo-integration.md)
- [Paridad con ADB Application Manager Pro y funciones portadas](docs/specs/parity-adb-app-manager.md)
- [Plan de soporte de televisiones](docs/specs/tv-support.md)

## Reglas rápidas

- No agregar lógica de negocio en Composables.
- No inyectar `AppContainer` en ViewModels nuevos; declarar un `<Name>Environment` en la
  feature y su adaptador `AppContainer<Name>Environment` en `app/`. Las features no deben
  importar `com.optimizer.tools.data`.
- No agregar strings estáticos hardcodeados; usar `res/values/*_strings.xml` y `UiText`
  cuando el texto se origine en un ViewModel.
- Navegación nueva con destinos `@Serializable` de `AppRoutes.kt`; no agregar rutas por
  string.
- No usar `android.util.Log` directamente; usar el contrato `AppLogger` y su
  implementación de composición.
- No crear un módulo Gradle nuevo sin justificar ciclo de vida u ownership.
- Toda pantalla nueva debe tener previews de sus estados relevantes.
- Antes de cerrar un cambio: `ktlintCheck`, `detekt`, tests JVM y `assembleDebug`/`assembleRelease`.
