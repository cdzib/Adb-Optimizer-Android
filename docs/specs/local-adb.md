# ADB local (depuración inalámbrica y 127.0.0.1:5555)

Estado: implementado.

## Objetivo

Permitir que la app use la depuración del propio equipo sin PC: detectar
`Settings.Global.ADB_ENABLED` / `adb_wifi_enabled`, guiar al usuario, descubrir el
puerto de la depuración inalámbrica, vincular con el código de 6 dígitos, conectar
por `127.0.0.1` y ejecutar comandos shell.

## Capas

- `feature/localadb/domain`: `LocalAdbRepository`, modelos, errores y casos de uso
  `CheckAdbEnabled`, `ConnectAdb`, `RunShellCommand`.
- `feature/localadb/data`: `LibAdbLocalAdbRepository` sobre `core:adb` (libadb),
  identidad RSA en `AdbKeys` y `LocalAdbShell` como `AdbShell`.
- `feature/localadb/presentation`: `LocalAdbViewModel` (MVVM+MVI, estados
  `DEBUGGING_OFF`, `CONNECTING`, `WAITING_AUTHORIZATION`, `CONNECTED`, `ERROR`) y
  `LocalAdbScreen`.
- Composición en `AppContainerLocalAdbEnvironment` y `AppContainer.localAdb`.

## Integración con el equipo local

- La sesión siempre es del **equipo local**: nunca se registra como dispositivo
  remoto ni cambia `AppTarget`.
- `AppContainer.localShell` usa la sesión ADB local cuando está conectada y cae a
  Shizuku si no lo está. Afecta a Apps, Optimizar, Archivos, Perfiles, Debloater y
  Consola.
- `AppContainer.localPrivilegeReady()` = Shizuku autorizado **o** ADB local
  conectado. Se usa para habilitar acciones y para decidir qué opciones se ocultan:
  - Optimizar muestra una guía en lugar de recetas sin permisos.
  - Energía muestra una guía en lugar de acciones sin permisos.
  - Apps oculta las acciones de escritura hasta tener privilegio.
  - ADB local oculta comandos y salida hasta estar conectado.
- Las acciones privilegiadas (reiniciar, apagar, recovery, bootloader, reiniciar la
  interfaz, congelar apps) usan **ADB local primero** y Shizuku como respaldo; ninguna
  exige Shizuku instalado. Si la sesión ya estaba vinculada pero no conectada, se
  reengancha sola antes de ejecutar. Termux no sirve para esto: `RUN_COMMAND` no
  expone un shell con uid 2000.
- Al conectar, Inicio muestra “ADB local · Conectado” en lugar de la fila de
  Shizuku.

## Límites conocidos

- Una app no puede activar la depuración: solo se detecta y se abre Ajustes.
- **Android 11+ no permite `adb tcpip 5555` desde el propio teléfono.** La ruta
  normal es la depuración inalámbrica: la app descubre por mDNS `adb-tls-pairing`
  (puerto de vinculación) y `adb-tls-connect` (puerto TLS), vincula con el código de
  6 dígitos y conecta por `127.0.0.1`. Se apoya en `core:adb` (libadb), el mismo
  transporte que se usa para dispositivos remotos.
- La vinculación solo hace falta una vez; la identidad RSA se guarda en
  `AdbKeys` y se reutiliza en las siguientes conexiones.
- El botón **Vincular con código** abre la pantalla de *Depuración inalámbrica*
  (alias `Settings$WifiDebuggingActivity`, con respaldo a Opciones de
  desarrollador) y lanza una notificación con `RemoteInput` para escribir el
  código sin volver a la app; `LocalAdbPairingService` vincula y conecta.
- La app mantiene además el formulario de código en pantalla como respaldo si
  las notificaciones están desactivadas.
- Al volver a la app (desde Ajustes o la notificación) se valida la sesión local:
  si el servicio ya vinculó y conectó, pasa a *Conectado*; si no, reintenta la
  conexión y muestra el motivo.
- El ADB clásico en `127.0.0.1:5555` sigue soportado como respaldo para equipos
  donde un PC ya ejecutó `adb tcpip 5555` o para Android antiguos.
- La primera conexión clásica exige aceptar el diálogo RSA; se espera hasta 60 s.
- Logcat, Doze y Ajustes del sistema siguen ligados a una sesión remota; su
  adaptación completa al shell local queda pendiente.
