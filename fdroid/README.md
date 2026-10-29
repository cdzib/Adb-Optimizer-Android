# Publicacion en F-Droid

Esta carpeta guarda el metadata que hay que enviar a
[fdroiddata](https://gitlab.com/fdroid/fdroiddata) para publicar la app. No forma
parte del build de Android.

## Requisitos ya cubiertos en el repo

- Licencia MIT (`LICENSE`).
- Sin SDKs propietarios, anuncios, analitica ni rastreadores.
- Sin binarios precompilados: `scrcpy-server` se compila desde fuente en el
  modulo `:scrcpy-server`.
- Catalogo de recetas y traducciones versionados en `app/catalog`, de modo que
  un clon limpio empaqueta el APK sin carpetas externas.
- `fastlane/metadata/android/` con titulo, descripcion corta y descripcion
  larga en `en-US` y `es-MX`.

## Pasos para el Merge Request

1. Confirmar que `versionCode`/`versionName` en `app/build.gradle.kts` coinciden
   con el tag de la release.
2. Crear y subir el tag:

   ```bash
   git tag -a v0.1.0 -m "0.1.0"
   git push origin v0.1.0
   ```

3. Copiar `fdroid/metadata/com.optimizer.tools.yml` a `metadata/` en un fork de
   `fdroiddata` y reemplazar `commit:` por el hash completo del tag:

   ```bash
   git rev-list -n 1 v0.1.0
   ```

4. Validar el metadata con `fdroid rewritemeta com.optimizer.tools` y
   `fdroid lint com.optimizer.tools`.
5. Probar el build localmente:

   ```bash
   fdroid build -v -l com.optimizer.tools
   ```

6. Abrir el Merge Request contra `fdroiddata` explicando:
   - que la app no requiere root (usa Shizuku o ADB inalambrico),
   - que `scrcpy-server` se compila desde fuente en el build,
   - que el catalogo de recetas es propio y esta versionado.

## Notas

- F-Droid firma el APK con su clave. Si mas adelante distribuyes un APK firmado
  con `keystore/adboptimizer-release.jks`, los usuarios no podran actualizar
  entre ambas firmas. Conserva `AllowedAPKSigningKeys` o usa builds
  reproducibles si eso importa.
- El buildserver usa el Gradle del wrapper (`gradlew-fdroid`), por eso el
  `gradle: yes` funciona con Gradle 8.14.3 y AGP 8.11.1.
- `prebuild` instala el SDK 36 porque el buildserver no lo trae preinstalado.
