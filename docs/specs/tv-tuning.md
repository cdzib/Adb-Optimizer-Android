# Puesta a punto de TV

Estado: fases 1 y 2 (catalogo, motor y pantalla).

## Objetivo

Cuando el destino es Android TV / Google TV, ofrecer un conjunto de **ajustes reversibles**
agrupados en presets, mas bundles de paquetes por modelo. No es un boton unico: cada
cambio muestra su valor previo, se puede deshacer y pasa por los guards del debloater.

## Capas

- `feature/tvtuning/domain/TvTuning.kt`: presets, ajustes, pasos con lectura previa y
  `{{prev}}`, y el perfil de modelo.
- `feature/tvtuning/domain/TvTuningCatalog.kt`: catalogo de ajustes y presets.
- `feature/tvtuning/data/TvModelsCatalog.kt`: lee `assets/tv/tv-models.json` y empareja
  por `ro.product.model`, `ro.product.name/device` o `ro.product.board`.
- `feature/tvtuning/data/TvTuningRepository.kt`: lee valores, aplica con verificacion y
  deshace; si un paso falla, revierte los anteriores.
- `assets/tv/tv-models.json`: listas por modelo (punto de partida importado de
  CleanYourTV, MIT). Cada paquete pasa despues por `AppSafety`, `HomeLauncherGuard` y
  `TvSafetyGuard` antes de tocarse.

## Presets

| Preset | Ajustes |
|---|---|
| Recomendado | animaciones 0.5, salvapantallas off, timeout 5 min, dexopt, trim-caches |
| Rendimiento | lo anterior + fixed-performance + limite de procesos (avanzado) |
| Privacidad | Private DNS (AdGuard) + limit_ad_tracking (avanzado) |
| Personalizado | todos, a eleccion |

Los ajustes avanzados nunca entran en el preset recomendado.

## Reglas

- Solo aparece con `DeviceInfo.isTv` (deteccion por `pm list features`, no solo
  `ro.build.characteristics`).
- Todo paso con lectura previa es reversible; los one-shot (dexopt, trim) se marcan como
  tales y no se deshacen.
- Los bundles de paquetes no se aplican sin modelo detectado y sin pasar los guards.
- `private_dns` no quita anuncios de YouTube y se avisa en el resumen del ajuste.
- `background_process_limit` es avanzado: puede romper grabacion o PiP.

## Fase 2 (implementada)

- Pantalla `TvTuningScreen`: chips de preset, ajustes por categoria con valor actual,
  badges de riesgo, bundles del modelo y progreso.
- Entrada desde **Herramientas → Puesta a punto de TV**, visible solo cuando el destino
  remoto es TV (`dashboardState.device.isTv`).
- Aplicar ejecuta primero los ajustes seleccionados y despues los bundles con
  `DebloatRepository` (con `HomeLauncherGuard`, `TvSafetyGuard` y `AppSafety`).
- Deshacer revierte los ajustes aplicados y los paquetes congelados.
- El resumen reporta aplicados, fallidos y protegidos.

Limite: la pantalla usa la sesion remota (`AppContainer.device`); en un equipo local no
hay destino TV todavia.
