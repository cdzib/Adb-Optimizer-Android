# scrcpy-server

Copia de `server/` del proyecto [scrcpy](https://github.com/Genymobile/scrcpy) en el tag `v5.0.1`
(commit `dc8a55a24c27ae78ae202b5a3288da681d0f141a`), con licencia Apache-2.0 (ver `LICENSE`).

## Por que esta aqui

La app sube `scrcpy-server.jar` al dispositivo para abrir el espejo de video. Versionar el JAR ya
compilado impedía:

- construir el APK desde un clon limpio sin binarios opacos en el árbol, y
- publicar en F-Droid, que rechaza binarios precompilados en el repositorio.

Este módulo compila el servidor desde fuente y `:app` lo copia a `assets/scrcpy/scrcpy-server.jar`.

## Actualizar

1. Copiar `server/src` y `server/proguard-rules.pro` del tag nuevo.
2. Ajustar `versionCode`, `versionName` y `compileSdk` en `build.gradle.kts`.
3. Verificar que `LibAdbShell` siga usando opciones soportadas por esa version del servidor.

No se incluyen `build_without_gradle.sh`, `meson.build` ni los scripts de release porque el modulo
solo necesita las fuentes Java/AIDL.
