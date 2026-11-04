# Simulador de dispositivo · AdbOptimizer

Prototipo interactivo construido con el sistema visual de los diseños originales
(`app-redesign.html` para el teléfono y `tv-redesign.html` para el televisor).
No es una galería: se abre un dispositivo y todo ocurre dentro de su marco.

## Cómo abrirlo

```bash
open design-proposals/redesign-tools/simulator/index.html
```

Sin build, sin dependencias y sin servidor. También vale doble clic.

## Qué se puede simular

1. **Primer arranque** — todos los equipos abren en la pantalla de conexión: pasos reales
   de Android (Opciones de desarrollador, Depuración inalámbrica, código de 6 dígitos) y una
   tarjeta con el equipo encontrado. En TV ocupa la pantalla completa; en el teléfono es una
   lista de pasos con las mismas acciones.
2. **Vinculación** — código de 6 dígitos → 4 pasos de progreso → Inicio.
3. **Inicio** — estado del equipo, salud (76/100), accesos rápidos, del dispositivo y
   "Explora tu equipo". En TV se presenta con hero, tres métricas y cuadrícula de accesos.
4. **Apps** — búsqueda, filtros Todas / Usuario / Sistema, lista en teléfono y cuadrícula
   en TV. El panel de acciones respeta los paquetes protegidos.
5. **Destino** (solo teléfono) — este teléfono o remoto, dispositivos conocidos y alta de
   uno nuevo con el asistente de dos pasos (código, QR, red o IP).
6. **Optimizar** — perfil, 6 recetas con riesgo explícito, selección, aplicación con
   confirmación, progreso y deshacer.
7. **Herramientas** — cada utilidad tiene su pantalla real: Registro en vivo con filtros,
   Consola con historial, Doze y energía, Ajustes del sistema con edición y deshacer,
   Archivos con navegación, Debloater con selección y confirmación, y Perfiles.
8. **Ajustes** — Tema, **Mostrar apps del sistema**, **Mantener pantalla encendida**,
   **Actualizar métricas automáticamente** y Administración (DPC). Los tres interruptores
   tienen efecto real en la simulación.
9. **Control remoto y espejo** — desde Destino: D-pad con teclas, y espejo con estado de transmisión.
10. **Cambio de equipo en vivo** — la mesa de control cambia marco y capacidades sin recargar.

## Mesa de control

Barra inferior, fuera del dispositivo:

- `TV · Proyector · Caja · Teléfono` — cambia el equipo simulado.
- `Reiniciar` — vuelve al estado desconectado del equipo actual.

En TV, las flechas del teclado mueven el foco, `OK`/`Enter` abre el elemento enfocado y
`Esc` vuelve.

## Qué hace cada preferencia

| Ajuste | Efecto en el simulador |
|---|---|
| Mostrar apps del sistema | La lista de Apps incluye los paquetes `sys` y los contadores cambian |
| Mantener pantalla encendida | Aviso en Inicio y distintivo en la barra de ayuda de la TV |
| Actualizar métricas automáticamente | Refresca RAM/almacenamiento/CPU cada 30 s y actualiza "Actualizado hace X s"; si se apaga, solo el botón de refrescar actualiza |

## Matriz por equipo

| | TV | Proyector | Caja | Teléfono |
|---|---|---|---|---|
| Navegación | rail lateral | rail lateral | rail lateral | barra inferior |
| Destino | — | — | — | sí |
| Métricas de batería | — | — | — | sí |
| Ajustes de TV | sí | sí | sí | no |
| Apps | cuadrícula | cuadrícula | cuadrícula | lista |

## Parámetros de URL (QA)

| Parámetro | Valores |
|---|---|
| `kind` | `tv`, `projector`, `box`, `phone` |
| `connected` | `1` |
| `screen` | `connect`, `pair`, `connecting`, `home`, `apps`, `destine`, `optimize`, `tools`, `settings`, `wizard` |
| `tool` | `log`, `consola`, `doze`, `sysset`, `debloat`, `files`, `profiles`, `power`, `dpc` |
| `modal` | `kind`, `app`, `about`, `confirm`, `pair`, `manual`, `qr` |
| `pkg` | paquete para `modal=app` |

Ejemplo: `index.html?kind=phone&connected=1&screen=optimize`.

## Estructura

```text
simulator/
  index.html        entrada, sprite de iconos y mesa de control
  sim/styles.css    sistema visual de app-redesign y tv-redesign
  sim/core.js       estado, datos, pantallas y acciones
  sim/phone.js      marco y overlays del teléfono
  sim/tv.js         marco y overlays del televisor
  sim/tools.js      herramientas: registro, consola, debloater, archivos, perfiles…
  screenshots/      capturas por estado
```

## Capturas

`01-tv-setup`, `02-tv-pairing`, `03-tv-home`, `04-tv-apps`, `05-tv-optimize`,
`06-tv-tools`, `07-tv-settings`, `08-tv-confirm`, `09-phone-destine`, `10-phone-home`,
`11-phone-apps`, `12-phone-optimize`, `13-phone-tools`, `14-phone-settings`,
`15-phone-wizard`, `16-phone-app-sheet`, `17-projector-home`, `18-box-home`,
`19-phone-log`, `20-phone-console`, `21-phone-debloat`, `22-phone-files`, `23-phone-sysset`,
`24-phone-profiles`, `25-phone-remote`, `26-phone-mirror`, `27-tv-tool-log`, `28-tv-tool-debloat`,
`29-phone-connect`, `30-phone-pair-modal`.

Para regenerarlas:

```bash
EDGE="/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge"
"$EDGE" --headless=new --disable-gpu --hide-scrollbars \
  --force-prefers-reduced-motion --force-device-scale-factor=1 \
  --virtual-time-budget=1800 --window-size=1500,1100 \
  --user-data-dir="/tmp/edge-sim" \
  --screenshot=screenshots/03-tv-home.png \
  "file://$PWD/index.html?connected=1&screen=home"
```

Usa siempre un `--user-data-dir` propio: si Edge ya está abierto, la captura puede
reutilizar la pestaña anterior.

## Límites

- No ejecuta ADB, `pm` ni `settings`: todo el resultado está simulado.
- Métricas, latencias y códigos son valores fijos con pequeñas variaciones.
- Los datos de apps y paquetes son de ejemplo.
