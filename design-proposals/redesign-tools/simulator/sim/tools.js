/* =========================================================================
   Herramientas · pantallas de detalle
   Datos y composición tomados de app-redesign.html (Registro, Consola, Doze,
   Ajustes del sistema, Debloater, Archivos, Perfiles, Energía, DPC).
   ========================================================================= */

const LOG_TINT = { D:'var(--secondary)', I:'var(--primary)', W:'var(--tertiary)', E:'var(--danger)' };
const LOG_SEED = [
  ['I','ActivityManager','Start proc com.example.player'],
  ['W','PackageManager','Package no debuggable'],
  ['E','AndroidRuntime','FATAL EXCEPTION: main'],
  ['D','SurfaceFlinger','Display 0: mode 1920x1080@60'],
  ['I','Doze','Stepped to deep: IDLE_PENDING']
];
const LOG_GEN = [
  ['D','InputDispatcher','Focus changed to com.netflix.ninja'],
  ['I','WifiService','RSSI -52 dBm'],
  ['W','ConnectivityService','Network validation delayed'],
  ['D','Choreographer','Skipped 3 frames'],
  ['I','NetworkMonitor','Probe OK'],
  ['E','BluetoothAdapter','Service disconnected']
];
const CON_SEED = [{ c:'pm list packages -3 | wc -l', o:'36' }, { c:'dumpsys deviceidle get deep', o:'ACTIVE' }];
const CON_OUT = {
  getprop:'[ro.product.model]: [sdk_google_atv]',
  'dumpsys battery':'AC powered: true\nlevel: 100',
  'wm size':'Physical size: 1920x1080',
  'settings get global low_power':'0',
  whoami:'shell'
};
const SYS_SEED = {
  Global: [['airplane_mode_on','0'],['stay_on_while_plugged_in','3'],['wifi_on','1'],['window_animation_scale','1.0']],
  Seguro: [['android_id','9f2c1a77b4e1d0c3'],['screen_brightness','128'],['accessibility_display_inversion_enabled','null'],['wifi_scan_always_enabled','0'],['ui_night_mode','auto']],
  Sistema: [['screen_off_timeout','60000'],['font_scale','1.0'],['sound_effects_enabled','1']]
};
const DEB_SEED = [
  { n:'Movies Anywhere', p:'com.moviesanywhere · no usada', a:'M', bg:'#2a2416', fg:'#ffd79a', on:1 },
  { n:'Google Play Games', p:'com.google.android.play.games · duplicado', a:'G', bg:'#1f2a22', fg:'#a7f3c4', on:1 },
  { n:'Samsung TV Plus', p:'com.samsung.tvplus · sustituible', a:'S', bg:'#2a1f1f', fg:'#ffb4b4', on:0 }
];
const FS_SEED = {
  '/sdcard/Download': [
    { n:'AdbOptimizer', d:1 },
    { n:'Movies', d:1 },
    { n:'youtube-tv.apk', s:'24.8 MB · 23 oct', t:'APK' },
    { n:'registro-2026-10-27.txt', s:'412 KB · hoy', t:'LOG' }
  ],
  '/sdcard/Download/AdbOptimizer': [
    { n:'perfiles.json', s:'2 KB · ayer', t:'JSON' },
    { n:'respaldo.zip', s:'8.1 MB · 20 oct', t:'ZIP' },
    { n:'notas.txt', s:'1 KB · 18 oct', t:'TXT' }
  ],
  '/sdcard/Download/Movies': []
};
const PROFILES = [
  ['amber','Streaming','YouTube, Netflix, Plex · 3 apps'],
  ['cyan','Mínimo','Solo apps esenciales · 8 apps'],
  ['green','Gaming','Juegos y mando · 5 apps']
];
const POWER = [
  ['grey','power','Apagar pantalla','Mantiene el equipo encendido'],
  ['amber','refresh','Reiniciar','Reinicia el sistema'],
  ['red','power','Apagar','Apaga por completo el equipo']
];
const TOOL_IDS = {
  'Registro del sistema':'log', 'Consola':'consola', 'Doze y energía':'doze',
  'Ajustes del sistema':'sysset', 'Archivos':'files', 'Debloater':'debloat', 'Perfiles':'profiles'
};
const TOOL_TITLES = {
  log:'Registro', consola:'Consola', doze:'Doze y energía', sysset:'Ajustes del sistema',
  debloat:'Debloater', files:'Archivos', profiles:'Perfiles', power:'Opciones de energía', dpc:'Administración (DPC)'
};

function ensureTools() {
  if (S.toolsInit) return;
  S.toolsInit = true;
  S.log = LOG_SEED.map((l, i) => ({ l:l[0], t:l[1], m:l[2], ts:`03:04:0${5 + i}.${100 + i * 137}` }));
  S.logLive = true; S.logLvl = 'all'; S.logQ = '';
  S.con = CON_SEED.map(x => ({ ...x })); S.conInput = ''; S.conOut = null;
  S.doze = 'ACTIVE';
  S.sys = JSON.parse(JSON.stringify(SYS_SEED)); S.sysTab = 'Seguro'; S.sysQ = ''; S.sysHist = [];
  S.deb = DEB_SEED.map(x => ({ ...x })); S.debQ = '';
  S.fs = JSON.parse(JSON.stringify(FS_SEED)); S.cwd = '/sdcard/Download';
  S.mirrorOn = false;
  S.pending = null;
}
function openTool(id) {
  ensureTools();
  S.tool = id; S.screen = 'tool'; S.modal = null; S.focus = 0; render();
}
function toolBack() {
  if (S.screen === 'tool') return go('tools');
  return back();
}

/* ---------- piezas ---------- */
function toolShell(title, sub, content, right = '') {
  return `<div class="body">
    <div class="appbar">
      <button class="iconbtn tap" data-act="back" aria-label="Atrás">${I('back', 20)}</button>
      <div class="titles"><h2>${title}</h2><p>${sub}</p></div>${right}
    </div>${content}</div>`;
}

function logRow(x) {
  return `<div class="logrow" style="--tint:${LOG_TINT[x.l]}">
    <div><div class="meta"><span class="time">${x.ts}</span><span class="badge">${x.l}</span>
      <span class="tag">${esc(x.t)}</span></div><div class="msg">${esc(x.m)}</div></div></div>`;
}

/* =========================================================================
   TELÉFONO
   ========================================================================= */
function phoneTool() {
  ensureTools();
  switch (S.tool) {
    case 'log': return phoneLog();
    case 'consola': return phoneConsole();
    case 'doze': return phoneDoze();
    case 'sysset': return phoneSysset();
    case 'debloat': return phoneDebloat();
    case 'files': return phoneFiles();
    case 'profiles': return phoneProfiles();
    case 'power': return phonePower();
    case 'dpc': return phoneDpc();
    default: return phoneDpc();
  }
}

function phoneLog() {
  const q = S.logQ.toLowerCase();
  const vis = S.log.filter(x => (S.logLvl === 'all' || x.l === S.logLvl) && (x.t + x.m).toLowerCase().includes(q));
  const cnt = l => S.log.filter(x => x.l === l).length;
  return toolShell('Registro',
    `<span class="${S.logLive ? 'live' : ''}">${S.logLive ? 'EN VIVO' : 'PAUSADO'}</span> · ${S.log.length} líneas · ${vis.length} visibles`,
    `<div class="search">${I('search', 16)}<input id="logQ" placeholder="Buscar etiqueta o mensaje" value="${esc(S.logQ)}" autocomplete="off"></div>
     <div class="chips">
       ${chip('Todo', S.log.length, S.logLvl === 'all', 'data-log-lvl="all"')}
       ${['D','I','W','E'].map(l => chip(`<span class="dot" style="background:${LOG_TINT[l]}"></span>${l}`, cnt(l), S.logLvl === l, `data-log-lvl="${l}"`)).join('')}
     </div>
     <div class="scroll" style="display:flex;flex-direction:column;gap:8px">
       ${vis.length ? vis.slice(0, 60).map(logRow).join('') : empty('Sin líneas', 'Ningún registro coincide con el filtro.')}
     </div>`,
    `<button class="iconbtn primary tap" data-log-play aria-label="${S.logLive ? 'Pausar' : 'Reanudar'}">${S.logLive ? I('term', 18) : I('play', 18)}</button>
     <button class="iconbtn tap" data-log-clear aria-label="Limpiar">${I('refresh', 18)}</button>`);
}

function phoneConsole() {
  return toolShell('Consola', 'shell como uid 2000',
    `<div class="scroll" style="display:flex;flex-direction:column;gap:8px">
      ${S.con.length ? S.con.map((x, i) => `<div class="card" style="padding:12px;flex:none">
        <div class="tiny muted" style="margin-bottom:6px;font-family:ui-monospace,monospace">${esc(x.c)}</div>
        <div class="term" style="background:transparent;padding:0">${esc(x.o) || '(sin salida)'}</div></div>`).join('')
        : empty('Consola vacía', 'Escribe un comando ADB para empezar.')}
     </div>
     <div class="chips" style="margin:8px 0 0">
       ${['getprop','dumpsys battery','wm size','whoami'].map(c => `<button class="chip tap" data-con-chip="${esc(c)}">${esc(c)}</button>`).join('')}
     </div>
     <div class="conbar">
       <div class="search">${I('term', 16)}<input id="conIn" placeholder="adb shell …" value="${esc(S.conInput)}" autocomplete="off"></div>
       <button class="btn solid tap" data-con-run>Ejecutar</button>
     </div>`,
    `<button class="iconbtn tap" data-con-hist aria-label="Historial">${I('history', 18)}</button>`);
}

function phoneDoze() {
  const active = S.doze === 'ACTIVE';
  return toolShell('Doze y energía', 'Estado de reposo y acciones',
    `<div class="scroll">
      <div class="card" style="margin-bottom:12px">
        <div class="row" style="padding:0;border:0">
          <div class="iconbig i-${active ? 'green' : 'amber'}" style="width:44px;height:44px;border-radius:12px;display:grid;place-items:center">${I('moon', 22)}</div>
          <div class="txt" style="flex:1"><b>${active ? 'Equipo activo' : 'Reposo profundo'}</b>
            <span>${active ? 'El equipo está despierto.' : 'Las apps en segundo plano están detenidas.'}</span></div>
        </div>
        <div class="chips" style="margin-top:10px"><span class="chip ${active ? 'on' : ''}">${S.doze}</span></div>
      </div>
      <div class="group">
        <button class="row tap" data-doze="idle"><div class="ico i-amber">${I('moon')}</div>
          <div class="txt"><b>Forzar reposo profundo</b><span>Detiene las apps en segundo plano</span></div>
          <div class="chev">${I('chev', 16)}</div></button>
        <button class="row tap" data-doze="active"><div class="ico i-green">${I('bolt')}</div>
          <div class="txt"><b>Salir de reposo</b><span>Vuelve a estado activo</span></div>
          <div class="chev">${I('chev', 16)}</div></button>
      </div>
    </div>`);
}

function phoneSysset() {
  const q = S.sysQ.toLowerCase();
  const list = S.sys[S.sysTab].map((x, i) => [x, i]).filter(([x]) => x[0].includes(q));
  return toolShell('Ajustes del sistema', `${list.length} claves visibles`,
    `<div class="search">${I('search', 16)}<input id="sysQ" placeholder="Buscar clave" value="${esc(S.sysQ)}" autocomplete="off"></div>
     <div class="chips">${Object.keys(S.sys).map(k => chip(k, null, S.sysTab === k, `data-systab="${k}"`)).join('')}</div>
     <div class="scroll card tight">
       ${list.length ? list.map(([x, i]) => `<button class="kv tap" data-syskey="${i}">
         <div class="k"><b>${esc(x[0])}</b><span>${esc(x[1])}</span></div>
         <span class="chev">${I('edit', 15)}</span></button>`).join('')
         : empty('Sin claves', 'Ninguna clave coincide con la búsqueda.')}
     </div>`,
    `<button class="iconbtn tap" data-syshist aria-label="Historial">${I('history', 18)}</button>`);
}

function phoneDebloat() {
  const q = S.debQ.toLowerCase();
  const list = S.deb.filter(d => (d.n + d.p).toLowerCase().includes(q));
  const sel = S.deb.filter(d => d.on).length;
  return toolShell('Debloater', `${S.deb.length} sugeridas · ${sel} seleccionadas`,
    `<div class="search">${I('search', 16)}<input id="debQ" placeholder="Buscar app del sistema" value="${esc(S.debQ)}" autocomplete="off"></div>
     <div class="scroll card tight" style="margin-bottom:12px">
       ${list.length ? list.map(d => `<button class="row tap" data-deb="${esc(d.p)}">
         <div class="appicon" style="background:${d.bg};color:${d.fg}">${d.a}</div>
         <div class="txt"><b>${esc(d.n)}</b><span>${esc(d.p)}</span></div>
         <div class="cb ${d.on ? 'on' : ''}">${d.on ? I('check', 12) : ''}</div></button>`).join('')
         : empty('Nada que revisar', 'No quedan apps sugeridas.')}
     </div>
     <button class="btn ${sel ? 'danger' : 'off'}" data-deb-go ${sel ? '' : 'disabled'}>${sel ? `Desinstalar ${sel} apps` : 'Selecciona apps'}</button>`);
}

function phoneFiles() {
  const list = S.fs[S.cwd] || [];
  return toolShell('Archivos', S.cwd,
    `<div class="scroll card tight">
      ${list.length ? list.map((f, i) => f.d
        ? `<button class="row tap" data-path="${esc(S.cwd + '/' + f.n)}">
            <div class="ico i-${i % 2 ? 'cyan' : 'amber'}">${I('folder')}</div>
            <div class="txt"><b>${esc(f.n)}</b><span>Carpeta · ${(S.fs[S.cwd + '/' + f.n] || []).length} elementos</span></div>
            <div class="chev">${I('chev', 16)}</div></button>`
        : `<button class="row tap" data-file="${i}">
            <div class="appicon">${f.t}</div>
            <div class="txt"><b>${esc(f.n)}</b><span>${esc(f.s)}</span></div>
            <div class="chev">${I('chev', 16)}</div></button>`).join('')
        : empty('Carpeta vacía', 'No hay elementos en esta ruta.')}
     </div>
     <button class="btn" style="margin-top:12px" data-act="back">${S.cwd === '/sdcard/Download' ? 'Volver a Herramientas' : 'Subir un nivel'}</button>`,
    `<button class="iconbtn tap" data-files-root aria-label="Raíz">${I('folder', 18)}</button>`);
}

function phoneProfiles() {
  return toolShell('Perfiles', 'Aplica un conjunto de apps de una vez',
    `<div class="scroll group">
      ${PROFILES.map((p, i) => `<button class="row tap" data-prof-apply="${i}">
        <div class="ico i-${p[0]}">${I('layers')}</div>
        <div class="txt"><b>${p[1]}</b><span>${p[2]}</span></div>
        <span class="tiny muted">Aplicar</span></button>`).join('')}
     </div>
     <p class="intro-line" style="margin-top:12px">Los perfiles agrupan recetas y paquetes; se aplican con las mismas confirmaciones.</p>`);
}

function phonePower() {
  return toolShell('Opciones de energía', 'Pantalla, reinicio y apagado',
    `<div class="scroll group">
      ${POWER.map((p, i) => `<button class="row tap" data-pw="${i}">
        <div class="ico i-${p[0]}">${I(p[1])}</div>
        <div class="txt"><b>${p[2]}</b><span>${p[3]}</span></div>
        <div class="chev">${I('chev', 16)}</div></button>`).join('')}
     </div>`);
}

function phoneDpc() {
  return toolShell('Administración (DPC)', 'Perfil de trabajo',
    `<div class="scroll">
      <div class="warnbox">
        <b>Sin perfil de trabajo activo</b>
        <p>La administración de dispositivos (DPC) solo está disponible cuando la app se instala como administrador de un perfil de trabajo.</p>
      </div>
      <div class="group">
        ${row('grey','info','Qué permite','Políticas de apps, restricciones y bloqueo','data-act="dpcInfo"')}
        ${row('grey','layers','Perfiles administrados','Apps del perfil de trabajo','data-act="dpcInfo"')}
      </div>
    </div>`);
}

function phoneRemote() {
  return toolShell('Control remoto', device().name,
    `<div class="scroll">
      <div class="card" style="margin-bottom:12px">
        <div class="row" style="padding:0;border:0">
          <div class="appicon" style="background:var(--primary-soft);color:var(--primary)">${device().short}</div>
          <div class="txt"><b>${esc(device().name)}</b><span>192.168.1.50:5555 · conectado</span></div>
          <span class="dot-ok"></span>
        </div>
      </div>
      <div class="dpad">
        <span></span><button data-remote="up">▲</button><span></span>
        <button data-remote="left">◀</button><button class="ok" data-remote="ok">OK</button><button data-remote="right">▶</button>
        <span></span><button data-remote="down">▼</button><span></span>
      </div>
      <div class="remote-row">
        <button class="btn tap" data-remote="back">Atrás</button>
        <button class="btn tap" data-remote="home">Inicio</button>
      </div>
      <div class="remote-row">
        <button class="btn tap" data-remote="vol-">Vol −</button>
        <button class="btn tap" data-remote="vol+">Vol +</button>
      </div>
      <p class="intro-line" style="text-align:center;margin-top:14px">Se envía al equipo activo. Todo simulado.</p>
    </div>`);
}

function phoneMirror() {
  return toolShell('Espejo', 'Pantalla de ' + device().name,
    `<div class="scroll">
      <div class="mirror">
        <span class="glow"></span>
        <b>${S.mirrorOn ? 'Espejo activo' : 'Espejo en pausa'}</b>
        <span>${S.mirrorOn ? '1920 × 1080 · 60 fps' : 'Toca para iniciar la transmisión'}</span>
      </div>
      <button class="btn ${S.mirrorOn ? '' : 'solid'}" style="margin-top:12px" data-mirror-toggle>${S.mirrorOn ? 'Detener espejo' : 'Iniciar espejo'}</button>
      <p class="intro-line" style="margin-top:12px">El espejo es de solo lectura: no permite tocar la pantalla del equipo.</p>
    </div>`);
}

/* =========================================================================
   TELEVISOR · paneles de herramienta
   ========================================================================= */
function tvTool() {
  ensureTools();
  const title = TOOL_TITLES[S.tool] || 'Herramientas';
  const sub = {
    log: `${S.log.length} líneas · ${S.logLive ? 'en vivo' : 'pausado'}`,
    consola: 'Comandos ejecutados en esta sesión',
    doze: S.doze === 'ACTIVE' ? 'Equipo activo' : 'Reposo profundo',
    sysset: `${S.sys[S.sysTab].length} claves`,
    debloat: `${S.deb.filter(d => d.on).length} seleccionadas`,
    files: S.cwd,
    profiles: 'Aplica un conjunto de apps',
    power: 'Se piden confirmación',
    dpc: 'Requiere perfil de trabajo'
  }[S.tool] || '';

  let body = '';
  if (S.tool === 'log') {
    body = `<div class="stack">${S.log.slice(0, 5).map((x, i) =>
      `<div class="toolrow ${i === S.focus % 5 ? 'f' : ''}" data-act="logLine">
        <div class="ico i-${x.l === 'E' ? 'red' : x.l === 'W' ? 'amber' : x.l === 'D' ? 'cyan' : 'green'}">${I('log', 17)}</div>
        <div class="t"><b>${esc(x.t)}</b><span>${esc(x.m)}</span></div>
        <span class="value-pill">${x.l} · ${x.ts.slice(0, 8)}</span></div>`).join('')}</div>`;
  } else if (S.tool === 'consola') {
    body = `<div class="stack">${S.con.slice(-4).reverse().map((x, i) =>
      `<button class="toolrow ${i === S.focus % 4 ? 'f' : ''}" data-con-out="${esc(x.c)}">
        <div class="ico i-cyan">${I('term', 17)}</div>
        <div class="t"><b>${esc(x.c)}</b><span>${esc((x.o || '').split('\n')[0])}</span></div>
        <span class="value-pill">Ver</span></button>`).join('')}</div>`;
  } else if (S.tool === 'doze') {
    body = `<div class="stack">
      <button class="setrow ${S.focus % 2 === 0 ? 'f' : ''}" data-doze="idle"><div class="t"><b>Forzar reposo profundo</b><span>Detiene las apps en segundo plano</span></div><span class="value-pill">Aplicar</span></button>
      <button class="setrow" data-doze="active"><div class="t"><b>Salir de reposo</b><span>Vuelve al estado activo</span></div><span class="value-pill">Aplicar</span></button>
      <button class="setrow" data-doze="close"><div class="t"><b>Estado actual</b><span>${S.doze}</span></div><span class="value-pill" style="color:var(--primary)">${S.doze}</span></button>
    </div>`;
  } else if (S.tool === 'sysset') {
    body = `<div class="filterbar">${Object.keys(S.sys).map(k => `<button class="fchip ${S.sysTab === k ? 'on' : ''}" data-systab="${k}">${k}<span class="n">${S.sys[k].length}</span></button>`).join('')}</div>
      <div class="stack">${S.sys[S.sysTab].map((x, i) =>
        `<button class="toolrow ${i === S.focus % S.sys[S.sysTab].length ? 'f' : ''}" data-syskey="${i}">
          <div class="t"><b>${esc(x[0])}</b><span>${esc(S.sys[S.sysTab][i][1])}</span></div>
          <span class="value-pill">Cambiar</span></button>`).join('')}</div>`;
  } else if (S.tool === 'debloat') {
    body = `<div class="stack">${S.deb.map((d, i) =>
      `<button class="toolrow ${i === S.focus % Math.max(1, S.deb.length) ? 'f' : ''}" data-deb="${esc(d.p)}">
        <div class="ico i-red">${I('trash', 17)}</div>
        <div class="t"><b>${esc(d.n)}</b><span>${esc(d.p)}</span></div>
        <div class="toggle ${d.on ? 'on' : ''}"><i></i></div></button>`).join('')}
        <button class="setrow" data-deb-go><div class="t"><b>Desinstalar seleccionadas</b><span>${S.deb.filter(d => d.on).length} apps · se puede deshacer</span></div><span class="value-pill" style="color:var(--danger)">Aplicar</span></button>
      </div>`;
  } else if (S.tool === 'files') {
    const list = S.fs[S.cwd] || [];
    body = `<div class="stack">${list.length ? list.map((f, i) =>
      `<button class="toolrow ${i === S.focus % Math.max(1, list.length) ? 'f' : ''}" ${f.d ? `data-path="${esc(S.cwd + '/' + f.n)}"` : `data-act="fileInfo"`}>
        <div class="ico i-${f.d ? 'amber' : 'cyan'}">${I(f.d ? 'folder' : 'edit', 17)}</div>
        <div class="t"><b>${esc(f.n)}</b><span>${esc(f.d ? 'Carpeta' : f.s)}</span></div>
        <span class="value-pill">${f.d ? 'Abrir' : 'Ver'}</span></button>`).join('')
      : `<div class="appnote">Carpeta vacía</div>`}</div>`;
  } else if (S.tool === 'profiles') {
    body = `<div class="stack">${PROFILES.map((p, i) =>
      `<button class="toolrow ${i === S.focus % 3 ? 'f' : ''}" data-prof-apply="${i}">
        <div class="ico i-${p[0]}">${I('layers', 17)}</div>
        <div class="t"><b>${p[1]}</b><span>${p[2]}</span></div>
        <span class="value-pill">Aplicar</span></button>`).join('')}</div>`;
  } else if (S.tool === 'power') {
    body = `<div class="stack">${POWER.map((p, i) =>
      `<button class="toolrow ${i === S.focus % 3 ? 'f' : ''}" data-pw="${i}">
        <div class="ico i-${p[0]}">${I(p[1], 17)}</div>
        <div class="t"><b>${p[2]}</b><span>${p[3]}</span></div>
        <span class="value-pill">Aplicar</span></button>`).join('')}</div>`;
  } else {
    body = `<div class="stack"><div class="appnote">La administración (DPC) solo está disponible con un perfil de trabajo activo en este equipo.</div></div>`;
  }

  return `<div class="tvpanel">
    <div class="h"><h2>${title}</h2><p>${esc(sub)}</p></div>
    ${body}
  </div>`;
}

/* =========================================================================
   OVERLAYS de herramientas
   ========================================================================= */
function toolOverlay() {
  const m = S.modal;
  if (!m) return '';
  const phone = !isTV();
  const wrap = (inner) => phone
    ? `<div class="overlay show center" data-close><div class="sheet dlg">${inner}</div></div>`
    : `<div class="dialog" data-close><div class="sheet">${inner}</div></div>`;
  const acts = (cancel, okLabel, attr, cls = 'solid') => phone
    ? `<div class="acts"><button class="btn tap" data-close>${cancel}</button><button class="btn ${cls} tap" ${attr}>${okLabel}</button></div>`
    : `<div class="btns" style="margin-top:1.2cqw"><button class="dbtn" data-close>${cancel}</button><button class="dbtn ${cls === 'danger' ? '' : 'solid'}" ${attr} style="${cls === 'danger' ? 'color:var(--danger)' : ''}">${okLabel}</button></div>`;

  if (m.type === 'sysEdit') {
    const [k, v] = S.sys[m.tab][m.idx];
    if (phone) {
      return wrap(`<h4>${esc(k)}</h4><p>Valor actual: <b style="color:var(--text)">${esc(v)}</b></p>
        <input class="inp" id="sysVal" value="${esc(v === 'null' ? '' : v)}">
        ${acts('Cancelar', 'Guardar', 'data-sys-save')}`);
    }
    return wrap(`<h3>${esc(k)}</h3><p>Valor actual: <b style="color:var(--text)">${esc(v)}</b></p>
      <div class="btns" style="justify-content:flex-start;flex-wrap:wrap">
        ${['0','1','null'].map(val => `<button class="dbtn" data-sys-set="${val}">${val}</button>`).join('')}
      </div>
      <div class="btns" style="margin-top:1cqw"><button class="dbtn" data-close>Cerrar</button></div>`);
  }
  if (m.type === 'sysHist') {
    return wrap(`<h4>Historial</h4><p>${S.sysHist.length} cambios en esta sesión.</p>
      <div class="group" style="background:var(--surface3)">
        ${S.sysHist.length ? S.sysHist.slice().reverse().map(h => `<div class="row"><div class="txt"><b>${esc(h.key)}</b><span>${esc(h.old)} → ${esc(h.now)}</span></div></div>`).join('')
          : `<div class="row"><div class="txt"><span>Aún no hay cambios.</span></div></div>`}
      </div>
      ${acts('Cerrar', S.sysHist.length ? 'Deshacer último' : 'Cerrar', S.sysHist.length ? 'data-sys-undo' : 'data-close')}`);
  }
  if (m.type === 'conHist') {
    const uniq = [...new Set(S.con.map(x => x.c))].reverse();
    return wrap(`<h4>Historial</h4><p>Toca un comando para reutilizarlo.</p>
      <div class="group" style="background:var(--surface3)">
        ${uniq.map((c, i) => `<button class="kv tap" data-con-use="${esc(c)}"><div class="k"><b>${esc(c)}</b></div></button>`).join('')}
      </div>
      <div class="acts"><button class="btn tap" data-close>Cerrar</button></div>`);
  }
  if (m.type === 'conOut') {
    return wrap(`<h4>${esc(m.cmd)}</h4><div class="term" style="background:var(--surface3)">${esc(m.out)}</div>
      ${acts('Cerrar', 'Reutilizar', 'data-con-use="' + esc(m.cmd) + '"')}`);
  }
  if (m.type === 'file') {
    return wrap(`<h4>${esc(m.name)}</h4><p>${esc(m.path)} · ${esc(m.size || 'archivo')}</p>
      <div class="stack"><button class="btn tonal tap" data-file-copy>Copiar ruta</button>
      <button class="btn danger tap" data-file-del>Borrar</button></div>
      ${acts('Cerrar', 'Cerrar', 'data-close')}`);
  }
  if (m.type === 'confirmTool') {
    return wrap(`<h4>${esc(m.title)}</h4><p>${esc(m.text)}</p>
      ${acts('Cancelar', m.ok || 'Confirmar', 'data-confirm-tool', m.danger ? 'danger' : 'solid')}`);
  }
  if (m.type === 'logLine') {
    return wrap(`<h4>${esc(m.tag)}</h4><p class="mono">${esc(m.msg)}</p>
      <div class="acts"><button class="btn tap" data-close>Cerrar</button></div>`);
  }
  return '';
}

/* =========================================================================
   EVENTOS de herramientas
   ========================================================================= */
document.addEventListener('click', e => {
  const lvl = e.target.closest('[data-log-lvl]');
  if (lvl) { S.logLvl = lvl.dataset.logLvl; return render(); }
  if (e.target.closest('[data-log-play]')) { S.logLive = !S.logLive; render(); return toast(S.logLive ? 'Modo en vivo' : 'Pausado'); }
  if (e.target.closest('[data-log-clear]')) {
    return openModal({ type:'confirmTool', title:'Limpiar registro', text:'Se vaciará el búfer de logcat en pantalla.', ok:'Limpiar', danger:true, action:'logClear' });
  }

  if (e.target.closest('[data-con-run]')) return runCommand(S.conInput);
  const chipCmd = e.target.closest('[data-con-chip]');
  if (chipCmd) { S.conInput = chipCmd.dataset.conChip; render(); return refocus('conIn'); }
  if (e.target.closest('[data-con-hist]')) {
    const uniq = [...new Set(S.con.map(x => x.c))].reverse();
    return openModal({ type:'conHist' });
  }
  const useCmd = e.target.closest('[data-con-use]');
  if (useCmd) { S.conInput = useCmd.dataset.conUse; closeModal(); render(); return refocus('conIn'); }
  const conOut = e.target.closest('[data-con-out]');
  if (conOut) {
    const x = S.con.find(c => c.c === conOut.dataset.conOut);
    return openModal({ type:'conOut', cmd:x.c, out:x.o || '(sin salida)' });
  }

  const doze = e.target.closest('[data-doze]');
  if (doze) {
    const v = doze.dataset.doze;
    if (v === 'close') return back();
    S.doze = v === 'idle' ? 'IDLE' : 'ACTIVE';
    render();
    return toast(v === 'idle' ? 'Reposo profundo activado' : 'Equipo activo');
  }

  const st = e.target.closest('[data-systab]');
  if (st) { S.sysTab = st.dataset.systab; S.sysQ = ''; return render(); }
  const sk = e.target.closest('[data-syskey]');
  if (sk) {
    return openModal({ type:'sysEdit', tab:S.sysTab, idx:+sk.dataset.syskey });
  }
  if (e.target.closest('[data-sys-save]')) {
    const val = document.getElementById('sysVal');
    const m = S.modal, arr = S.sys[m.tab], old = arr[m.idx][1];
    const now = (val ? val.value.trim() : '') || 'null';
    arr[m.idx][1] = now;
    S.sysHist.push({ tab:m.tab, key:arr[m.idx][0], old, now });
    closeModal(); render();
    return toast('Guardado · se puede deshacer');
  }
  const setVal = e.target.closest('[data-sys-set]');
  if (setVal) {
    const m = S.modal, arr = S.sys[m.tab], old = arr[m.idx][1];
    arr[m.idx][1] = setVal.dataset.sysSet;
    S.sysHist.push({ tab:m.tab, key:arr[m.idx][0], old, now:arr[m.idx][1] });
    closeModal(); render();
    return toast('Guardado · se puede deshacer');
  }
  if (e.target.closest('[data-syshist]')) return openModal({ type:'sysHist' });
  if (e.target.closest('[data-sys-undo]')) {
    const h = S.sysHist.pop();
    if (h) { S.sys[h.tab].forEach(x => { if (x[0] === h.key) x[1] = h.old; }); }
    closeModal(); render();
    return toast('Cambio deshecho');
  }

  const deb = e.target.closest('[data-deb]');
  if (deb) {
    const d = S.deb.find(x => x.p === deb.dataset.deb);
    if (d) d.on = d.on ? 0 : 1;
    return render();
  }
  if (e.target.closest('[data-deb-go]')) {
    const sel = S.deb.filter(d => d.on);
    if (!sel.length) return toast('Selecciona al menos una app', 'warn');
    return openModal({
      type:'confirmTool', title:`Desinstalar ${sel.length} apps`,
      text: sel.map(d => d.n).join(', ') + '. Queda en el historial para deshacer.',
      ok:'Desinstalar', danger:true, action:'debloat'
    });
  }

  const path = e.target.closest('[data-path]');
  if (path) {
    const p = path.dataset.path;
    if (!S.fs[p]) S.fs[p] = [];
    S.cwd = p;
    return render();
  }
  if (e.target.closest('[data-files-root]')) { S.cwd = '/sdcard/Download'; return render(); }
  const file = e.target.closest('[data-file]');
  if (file) {
    const f = (S.fs[S.cwd] || [])[+file.dataset.file];
    if (f) return openModal({ type:'file', name:f.n, path:S.cwd + '/' + f.n, size:f.s, index:+file.dataset.file });
  }
  if (e.target.closest('[data-file-copy]')) { closeModal(); return toast('Ruta copiada'); }
  if (e.target.closest('[data-file-del]')) {
    const m = S.modal;
    return openModal({ type:'confirmTool', title:'Borrar archivo', text:`${m.name} se eliminará del equipo.`, ok:'Borrar', danger:true, action:'fileDel', index:m.index });
  }

  const prof = e.target.closest('[data-prof-apply]');
  if (prof) {
    const p = PROFILES[+prof.dataset.profApply];
    return openModal({ type:'confirmTool', title:`Aplicar perfil ${p[1]}`, text:`${p[2]}. Se ejecutará en ${device().name}.`, ok:'Aplicar', action:'applyProfileTool' });
  }

  const pw = e.target.closest('[data-pw]');
  if (pw) {
    const p = POWER[+pw.dataset.pw];
    return openModal({ type:'confirmTool', title:p[2], text:`${p[3]} en ${device().name}.`, ok:p[2], danger:p[0] === 'red', action:'powerTool', name:p[2] });
  }

  if (e.target.closest('[data-confirm-tool]')) {
    const m = S.modal;
    const action = m.action;
    if (action === 'logClear') { S.log = []; closeModal(); render(); return toast('Registro limpio'); }
    if (action === 'debloat') { const n = S.deb.filter(d => d.on).length; S.deb = S.deb.filter(d => !d.on); closeModal(); render(); return toast(`${n} apps desinstaladas`); }
    if (action === 'fileDel') { (S.fs[S.cwd] || []).splice(m.index, 1); closeModal(); render(); return toast('Archivo borrado'); }
    if (action === 'applyProfileTool') { closeModal(); return toast('Perfil aplicado'); }
    if (action === 'powerTool') { closeModal(); return toast(m.name + ': orden enviada'); }
    closeModal();
    return;
  }

  const remote = e.target.closest('[data-remote]');
  if (remote) return toast('Tecla ' + remote.dataset.remote + ' enviada');
  if (e.target.closest('[data-mirror-toggle]')) {
    S.mirrorOn = !S.mirrorOn;
    render();
    return toast(S.mirrorOn ? 'Espejo iniciado' : 'Espejo detenido');
  }
});

document.addEventListener('input', e => {
  if (e.target.id === 'logQ') { S.logQ = e.target.value; render(); refocus('logQ'); }
  if (e.target.id === 'conIn') { S.conInput = e.target.value; }
  if (e.target.id === 'sysQ') { S.sysQ = e.target.value; render(); refocus('sysQ'); }
  if (e.target.id === 'debQ') { S.debQ = e.target.value; render(); refocus('debQ'); }
});

document.addEventListener('keydown', e => {
  if (e.target && e.target.id === 'conIn' && e.key === 'Enter') {
    e.preventDefault();
    runCommand(S.conInput);
  }
});

function runCommand(raw) {
  const v = (raw || '').trim();
  if (!v) return toast('Escribe un comando', 'warn');
  S.con.push({ c:v, o: CON_OUT[v] || '(sin salida)' });
  S.conInput = '';
  render();
  refocus('conIn');
  toast('Comando ejecutado');
}

/* registro en vivo (línea nueva arriba, sin saltos de scroll) */
setInterval(() => {
  if (!S.toolsInit || !S.logLive) return;
  if (S.screen !== 'tool' || S.tool !== 'log' || !connected()) return;
  const g = LOG_GEN[Math.floor(Math.random() * LOG_GEN.length)];
  const d = new Date();
  S.log.unshift({ l:g[0], t:g[1], m:g[2], ts:d.toTimeString().slice(0, 8) + '.' + String(d.getMilliseconds()).padStart(3, '0') });
  if (S.log.length > 200) S.log.pop();
  render();
}, 1600);
