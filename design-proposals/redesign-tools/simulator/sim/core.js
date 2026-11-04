/* =========================================================================
   AdbOptimizer · simulador de dispositivo
   Estado, datos y pantallas. El sistema visual es el de app-redesign.html
   (teléfono) y tv-redesign.html (televisor).
   ========================================================================= */

/* ---------- helpers ---------- */
const I = (n, s = 18) => `<svg width="${s}" height="${s}" aria-hidden="true"><use href="#i-${n}"/></svg>`;
const esc = s => String(s).replace(/[&<>"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));
const row = (c, ic, t, s, attr = '', right = `<div class="chev">${I('chev', 16)}</div>`) =>
  `<button class="row tap" ${attr}><div class="ico i-${c}">${I(ic)}</div><div class="txt"><b>${t}</b><span>${s}</span></div>${right}</button>`;
const chip = (label, n, on, attr) =>
  `<button class="chip tap ${on ? 'on' : ''}" ${attr}>${label}${n != null ? ` <span class="n">${n}</span>` : ''}</button>`;
const empty = (t, s) => `<div class="empty"><div class="circle">${I('search', 24)}</div><b>${t}</b><span>${s}</span></div>`;

/* ---------- datos ---------- */
const DEVICES = {
  tv: { label:'TV', short:'TV', name:'Google TV · sdk_google_atv', sub:'Android 16 · 1080p',
        android:'Android 16 · 1080p', ram:{ used:1.5, total:1.9 }, storage:{ free:9.2, total:16 },
        apps:48, sys:12, battery:false },
  projector: { label:'Proyector', short:'PJ', name:'Proyector del salón', sub:'Android 12 · 1080p',
        android:'Android 12 · 1080p', ram:{ used:2.1, total:3.0 }, storage:{ free:24.6, total:64 },
        apps:36, sys:14, battery:false },
  box: { label:'Caja / Stick', short:'MB', name:'Caja del salón · Mi Box', sub:'Android 11 · 4K',
        android:'Android 11 · 4K', ram:{ used:1.2, total:2.0 }, storage:{ free:5.4, total:8 },
        apps:41, sys:16, battery:false },
  phone: { label:'Teléfono', short:'AD', name:'Pixel 7', sub:'Android 17 · SDK 37 · arm64-v8a',
        android:'Android 17 · SDK 37 · arm64-v8a', ram:{ used:2.3, total:3.9 }, storage:{ free:41.2, total:128 },
        apps:266, sys:259, battery:true }
};

const APPS_TV = [
  { n:'YouTube Music', p:'com.google.android.youtube.tvmusic', s:'2.4 MB', a:'YT', bg:'#1d2a3a', fg:'#8ec5ff' },
  { n:'Netflix', p:'com.netflix.ninja', s:'84 MB', a:'NF', bg:'#2a1f1f', fg:'#ffb4b4' },
  { n:'Spotify', p:'com.spotify.tv.android', s:'42 MB', a:'SP', bg:'#1f2a22', fg:'#a7f3c4' },
  { n:'Plex', p:'com.plexapp.android', s:'31 MB', a:'PL', bg:'#241f2f', fg:'#d6c4ff' },
  { n:'Kodi', p:'org.xbmc.kodi', s:'96 MB', a:'KO', bg:'#1d2a3a', fg:'#9ecbff' },
  { n:'Twitch', p:'tv.twitch.android.app', s:'58 MB', a:'TW', bg:'#241f2f', fg:'#d6c4ff' },
  { n:'Disney+', p:'com.disney.disneyplus', s:'61 MB', a:'D+', bg:'#1d2a3a', fg:'#9ecbff' },
  { n:'Prime Video', p:'com.amazon.amazonvideo.livingroom', s:'72 MB', a:'PV', bg:'#1d2a3a', fg:'#9ecbff' },
  { n:'Launcher de TV', p:'com.google.android.tvlauncher', s:'sistema', a:'TV', bg:'#2a2416', fg:'#ffd79a', sys:1 },
  { n:'Play Store', p:'com.android.vending', s:'sistema', a:'PS', bg:'#1f2a22', fg:'#a7f3c4', sys:1 },
  { n:'Servicios de Google Play', p:'com.google.android.gms', s:'sistema', a:'GP', bg:'#1d2a3a', fg:'#8ec5ff', sys:1 },
  { n:'Ajustes de TV', p:'com.android.tv.settings', s:'sistema', a:'AJ', bg:'#1d2734', fg:'#cfe0f2', sys:1 }
];

const APPS_PHONE = [
  { n:'YouTube Music', p:'com.google.android.apps.youtube.music', s:'38 MB', a:'YM', bg:'#2a1f1f', fg:'#ffb4b4' },
  { n:'Netflix', p:'com.netflix.mediaclient', s:'112 MB', a:'NF', bg:'#2a1f1f', fg:'#ffb4b4' },
  { n:'Spotify', p:'com.spotify.music', s:'64 MB', a:'SP', bg:'#1f2a22', fg:'#a7f3c4' },
  { n:'Instagram', p:'com.instagram.android', s:'71 MB', a:'IG', bg:'#241f2f', fg:'#d6c4ff' },
  { n:'WhatsApp', p:'com.whatsapp', s:'58 MB', a:'WA', bg:'#1f2a22', fg:'#a7f3c4' },
  { n:'Chrome', p:'com.android.chrome', s:'146 MB', a:'CH', bg:'#1d2a3a', fg:'#8ec5ff' },
  { n:'Maps', p:'com.google.android.apps.maps', s:'92 MB', a:'MP', bg:'#1f2a22', fg:'#a7f3c4' },
  { n:'ADB Optimizer', p:'com.optimizer.tools.debug', s:'26 MB', a:'AD', bg:'#1d2a3a', fg:'#8ec5ff' },
  { n:'Launcher', p:'com.google.android.apps.nexuslauncher', s:'sistema', a:'LN', bg:'#1d2a3a', fg:'#8ec5ff', sys:1 },
  { n:'Servicios de Google Play', p:'com.google.android.gms', s:'sistema', a:'GP', bg:'#1d2a3a', fg:'#8ec5ff', sys:1 },
  { n:'Play Store', p:'com.android.vending', s:'sistema', a:'PS', bg:'#1f2a22', fg:'#a7f3c4', sys:1 },
  { n:'Ajustes', p:'com.android.settings', s:'sistema', a:'AJ', bg:'#1d2734', fg:'#cfe0f2', sys:1 }
];

const RECIPES = [
  { id:'anim', t:'Apagar animaciones', s:'Escalas de ventana, transición y animador a 0', risk:'safe', on:true },
  { id:'res', t:'Limitar la interfaz a 1080p', s:'Menos trabajo para la GPU en paneles 4K', risk:'mod', on:true },
  { id:'cache', t:'Limpiar caché de apps', s:'Libera espacio temporal sin tocar datos', risk:'safe', on:true },
  { id:'bg', t:'Restringir procesos en segundo plano', s:'Máximo dos procesos en caché', risk:'mod', on:false },
  { id:'dns', t:'DNS privado', s:'Resolución más rápida y segura', risk:'safe', on:false },
  { id:'dpi', t:'Reducir densidad de pantalla', s:'Puede cambiar el tamaño de la interfaz del equipo', risk:'adv', on:false }
];

const TOOLS = [
  ['Diagnóstico', [
    ['green', 'log', 'Registro del sistema', 'Logcat con niveles y modo en vivo'],
    ['cyan', 'term', 'Consola', 'Comandos ADB con historial'],
    ['amber', 'moon', 'Doze y energía', 'Estado de reposo y acciones']
  ]],
  ['Sistema', [
    ['green', 'sliders', 'Ajustes del sistema', 'Global, seguro y sistema con deshacer'],
    ['cyan', 'folder', 'Archivos', 'Explorar, copiar y borrar en el equipo'],
    ['red', 'trash', 'Debloater', 'Revisar y desinstalar apps del sistema']
  ]],
  ['Apps', [
    ['amber', 'layers', 'Perfiles', 'Conjuntos de apps para aplicar de una vez']
  ]]
];

const EXPLORE = [
  { id:'dev', n:'Dispositivo', d:'Modelo, fabricante y tipo de equipo', c:'green', i:'phone',
    rows:[['Marca', () => S.kind === 'phone' ? 'Google' : 'Google'], ['Modelo', () => DEVICES[S.kind].name], ['Tipo de equipo', () => DEVICES[S.kind].label], ['USB host', () => 'Sí']] },
  { id:'display', n:'Pantalla', d:'Resolución, densidad y frecuencia', c:'cyan', i:'display',
    rows:[['Resolución', () => S.kind === 'phone' ? '1344 × 2992 px' : '1920 × 1080 px'], ['Densidad', () => S.kind === 'phone' ? '560 dpi' : '320 dpi'], ['Refresco', () => S.kind === 'phone' ? '120 Hz' : '60 Hz']] },
  { id:'sensors', n:'Sensores', d:'Inventario de sensores disponibles', c:'amber', i:'sensors',
    rows:[['Sensores detectados', () => S.kind === 'phone' ? '32' : '4'], ['Acelerómetro', () => S.kind === 'phone' ? 'Sí' : 'No'], ['Luz ambiental', () => S.kind === 'phone' ? 'Sí' : 'No']] },
  { id:'network', n:'Red', d:'Interfaces activas y direcciones IP', c:'cyan', i:'wifi',
    rows:[['Wi-Fi', () => 'wlan0 · 192.168.1.50'], ['Datos', () => S.kind === 'phone' ? 'rmnet0 · activo' : 'No disponible']] },
  { id:'system', n:'Sistema', d:'Android, acceso y tiempo encendido', c:'green', i:'layers',
    rows:[['Android', () => DEVICES[S.kind].android], ['Acceso', () => S.phase === 'connected' ? 'ADB autorizado' : 'Sin sesión'], ['Encendido', () => '3 h 12 min']] },
  { id:'cpu', n:'CPU', d:'Uso, carga y arquitectura', c:'red', i:'cpu',
    rows:[['Uso', () => S.metrics.cpu + ' %'], ['Carga', () => '0.42 · 0.38 · 0.31'], ['Arquitectura', () => 'arm64-v8a']] },
  { id:'battery', n:'Batería', d:'Nivel de batería', c:'amber', i:'battery',
    rows:[['Nivel', () => S.kind === 'phone' ? '100 %' : 'Sin batería'], ['Temperatura', () => '31.2 °C'], ['Estado', () => S.prefs.keepScreenOn ? 'Pantalla encendida' : 'Normal']] },
  { id:'memory', n:'Memoria', d:'RAM y almacenamiento', c:'cyan', i:'memory',
    rows:[['RAM', () => `${S.metrics.ram} GB de ${DEVICES[S.kind].ram.total} GB`], ['Almacenamiento', () => `${S.metrics.storage} GB libres de ${DEVICES[S.kind].storage.total} GB`]] }
];

/* ---------- estado ---------- */
const S = {
  kind: 'tv',
  phase: 'disconnected',      // disconnected | connecting | connected
  screen: 'connect',          // connect | pair | connecting | home | apps | destine | optimize | tools | settings
  appsF: 'usr',
  q: '',
  toolsQ: '',
  optProfile: 'auto',
  prefs: { showSystem: false, keepScreenOn: true, autoMetrics: true },
  refreshSec: 30,
  metrics: { ram: 1.5, storage: 9.2, cpu: 18, at: Date.now() },
  progress: 0,
  applying: false,
  modal: null,
  toast: null,
  toastKind: 'ok',
  focus: 0,
  undo: false,
  history: [],
  apps: []
};

function device() { return DEVICES[S.kind]; }
const isTV = () => S.kind !== 'phone';
const connected = () => S.phase === 'connected';
const selCount = () => RECIPES.filter(r => r.on).length;
const riskyCount = () => RECIPES.filter(r => r.on && r.risk !== 'safe').length;

function resetMetrics(force) {
  const d = device();
  const jitter = force ? 0 : (Math.random() * 0.3 - 0.15);
  S.metrics = {
    ram: +(d.ram.used + jitter).toFixed(1),
    storage: +(d.storage.free - Math.random() * 0.2).toFixed(1),
    cpu: Math.round(14 + Math.random() * 12),
    at: Date.now()
  };
}

function resetState(keepKind) {
  if (!keepKind) S.kind = 'tv';
  S.phase = 'disconnected';
  S.screen = 'connect';
  S.appsF = 'usr';
  S.q = '';
  S.toolsQ = '';
  S.optProfile = null;
  S.prefs = { showSystem: false, keepScreenOn: true, autoMetrics: true };
  S.refreshSec = 30;
  RECIPES.forEach(r => { r.on = ['anim', 'res', 'cache'].includes(r.id); });
  S.apps = (S.kind === 'phone' ? APPS_PHONE : APPS_TV).map(a => ({ ...a }));
  S.progress = 0;
  S.applying = false;
  S.modal = null;
  S.toast = null;
  S.focus = 0;
  S.undo = false;
  S.history = [];
  S.toolsInit = false;
  S.tool = null;
  S.pending = null;
  resetMetrics(true);
}

/* ---------- utilidades de estado ---------- */
let toastTimer = null;
function toast(msg, kind = 'ok') {
  S.toast = msg; S.toastKind = kind; render();
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { if (S.toast === msg) { S.toast = null; render(); } }, 2200);
}
function go(screen) {
  S.screen = screen; S.modal = null; S.focus = 0; render();
}
function back() {
  if (S.modal) return closeModal();
  if (S.screen === 'tool') {
    if (S.tool === 'files' && S.cwd !== '/sdcard/Download') {
      S.cwd = S.cwd.slice(0, S.cwd.lastIndexOf('/'));
      return render();
    }
    return go('tools');
  }
  if (S.screen === 'remote' || S.screen === 'mirror') return go('destine');
  if (S.screen === 'tools' || S.screen === 'wizard' || S.screen === 'pair') {
    return go(connected() ? 'home' : 'connect');
  }
  if (S.screen === 'connecting') return;
  return go(connected() ? 'home' : 'connect');
}
function openModal(m) {
  S.modal = m;
  if (m && m.type === 'kind') S.pendingKind = S.kind;
  render();
}
function closeModal() { S.modal = null; render(); }

function setKind(k) {
  const wasConnected = connected();
  if (k !== S.kind) {
    S.kind = k;
    S.apps = (k === 'phone' ? APPS_PHONE : APPS_TV).map(a => ({ ...a }));
    resetMetrics(true);
    if (k === 'phone') S.screen = wasConnected ? 'home' : 'connect';
    else S.screen = wasConnected ? 'home' : 'connect';
  }
  render();
  toast('Equipo: ' + DEVICES[k].label);
}

/* ---------- conexión ---------- */
function connect() {
  S.phase = 'connecting'; S.progress = 0; S.screen = 'connecting'; render();
  const t = setInterval(() => {
    S.progress = Math.min(100, S.progress + 20);
    render();
    if (S.progress >= 100) {
      clearInterval(t);
      S.phase = 'connected';
      S.screen = 'home';
      resetMetrics(true);
      toast(isTV() ? 'Conectado a ' + device().name : 'Sesión local iniciada');
    }
  }, 220);
}
function disconnect() {
  S.phase = 'disconnected';
  S.screen = 'connect';
  S.modal = null;
  render();
}

/* ---------- métricas automáticas ---------- */
setInterval(() => {
  if (S.prefs.autoMetrics && connected() && Date.now() - S.metrics.at >= S.refreshSec * 1000) {
    resetMetrics(false);
    if (document.getElementById('device')) render();
  } else {
    const el = document.getElementById('metricsAge');
    if (el) el.textContent = metricsAge();
  }
}, 1000);

function metricsAge() {
  const sec = Math.max(0, Math.round((Date.now() - S.metrics.at) / 1000));
  if (!S.prefs.autoMetrics) return 'Manual · hace ' + sec + ' s';
  return 'Actualizado hace ' + sec + ' s';
}

/* =========================================================================
   PANTALLAS · TELÉFONO
   ========================================================================= */
function phoneHome() {
  const d = device();
  const stat = S.kind === 'phone'
    ? `<div class="stat">
         <div><div class="label">Batería</div><div class="value">100%</div><div class="hint">Nivel actual</div></div>
         <div><div class="label">RAM en uso</div><div class="value">${S.metrics.ram} GB</div><div class="hint">de ${d.ram.total} GB</div></div>
       </div>`
    : `<div class="stat">
         <div><div class="label">RAM en uso</div><div class="value">${S.metrics.ram} GB</div><div class="hint">de ${d.ram.total} GB</div></div>
         <div><div class="label">Almacenamiento</div><div class="value">${S.metrics.storage} GB</div><div class="hint">libres de ${d.storage.total} GB</div></div>
       </div>`;
  return `
  <div class="body">
    <div class="appbar">
      <div class="titles"><h2>Inicio</h2><p>${esc(sessionLabel())}</p></div>
      <button class="iconbtn tap" data-act="refresh" aria-label="Actualizar métricas">${I('refresh', 20)}</button>
    </div>
    <div class="scroll">
      ${stat}
      <div class="health">
        <div><b>Buen estado</b><span>76/100 · ${S.metrics.cpu} % de CPU</span></div>
        <button class="health-btn" data-go="optimize">Optimizar</button>
      </div>
      <div class="section-label">Accesos rápidos</div>
      <div class="card tight" style="margin-bottom:10px">
        ${row('green','bolt','Optimizar','Recetas disponibles para este equipo','data-go="optimize"')}
        ${row('cyan','term','Herramientas','Utilidades para el destino activo','data-go="tools"')}
        ${row('amber','layers','Perfiles de apps','Snapshots de estado y vigilancia de cambios','data-tool="profiles"')}
        ${row('grey','power','Opciones de energía','Pantalla, reinicio y apagado','data-tool="power"')}
      </div>
      <div class="section-label">Del dispositivo</div>
      <div class="group" style="margin-bottom:12px">
        <div class="row">
          <div class="appicon" style="background:var(--primary-soft);color:var(--primary)">${d.short}</div>
          <div class="txt"><b>${esc(d.name)}</b>
            <span>${esc(d.android)}<br>Sesión activa · ${S.kind === 'phone' ? 'este teléfono' : 'remoto'}</span></div>
          <span class="dot-ok"></span>
        </div>
        ${row('amber','refresh','Shizuku · Revisar acceso','Permisos para optimizar y modificar ajustes','data-act="shizuku"')}
      </div>
      ${S.prefs.keepScreenOn ? `<div class="warnbox" style="background:var(--surface);border-color:var(--line)">
        <b style="color:var(--primary)">Pantalla encendida</b>
        <p>Activo mientras la app está en primer plano.</p></div>` : ''}
      <div class="section-label">Explora tu equipo</div>
      <div class="group">
        ${EXPLORE.filter(e => S.kind === 'phone' || ['dev','display','network','system','memory'].includes(e.id))
          .map(e => row(e.c, e.i, e.n, e.d, `data-explore="${e.id}"`)).join('')}
      </div>
    </div>
  </div>
  ${phoneNav('home')}`;
}

function phoneConnect() {
  return `
  <div class="body">
    <div class="appbar"><div class="titles"><h2>Conectar</h2><p>Vincula el equipo que quieres controlar</p></div></div>
    <div class="scroll">
      <div class="wizard-title">Vincula tu equipo en un minuto</div>
      <p class="wizard-sub">Misma red Wi-Fi. Sin cables, sin cuentas y sin instalar nada raro.</p>

      <div class="group" style="margin-bottom:14px">
        <div class="row"><div class="ico i-green" style="font-size:13px;font-weight:800">1</div>
          <div class="txt"><b>Ajustes → Información</b><span>Toca siete veces «Compilación» para activar Opciones de desarrollador.</span></div></div>
        <div class="row"><div class="ico i-green" style="font-size:13px;font-weight:800">2</div>
          <div class="txt"><b>Depuración inalámbrica</b><span>Actívala y toca «Vincular dispositivo con código».</span></div></div>
        <div class="row"><div class="ico i-green" style="font-size:13px;font-weight:800">3</div>
          <div class="txt"><b>Escribe el código</b><span>Seis dígitos. El puerto cambia cada vez; el simulador lo resuelve.</span></div></div>
      </div>

      <div class="card" style="margin-bottom:12px">
        <div class="row" style="padding:0 0 12px;border:0">
          <div class="appicon" style="background:var(--primary-soft);color:var(--primary)">${device().short}</div>
          <div class="txt"><b>${esc(device().name)}</b><span>192.168.1.50 · misma red</span></div>
          <span class="dot-ok"></span>
        </div>
        <button class="btn solid" data-modal="pair">Vincular con código</button>
        <button class="btn" style="margin-top:8px" data-act="connectLocal">Usar ADB local</button>
      </div>

      <button class="wlink" data-act="discover">Buscar otros equipos</button>
      <button class="wlink" data-modal="manual" style="padding-top:0">Conectar por dirección IP</button>
      <p class="intro-line" style="margin-top:10px">Nada se modifica hasta que lo confirmes, y todo se puede deshacer.</p>
    </div>
  </div>`;
}

function phoneApps() {
  const q = S.q.toLowerCase();
  const all = S.apps;
  const visible = all.filter(a => S.prefs.showSystem || !a.sys);
  const user = visible.filter(a => !a.sys).length;
  const sys = all.filter(a => a.sys).length;
  const list = visible
    .filter(a => (S.appsF === 'all' || (S.appsF === 'usr' ? !a.sys : a.sys)) && (a.n + a.p).toLowerCase().includes(q));
  return `
  <div class="body">
    <div class="appbar">
      <div class="titles"><h2>Apps instaladas</h2><p>${visible.length} de ${all.length} apps</p></div>
      <button class="iconbtn tap" data-modal="system" aria-label="Ajustes de apps">${I('sliders', 20)}</button>
      <button class="iconbtn tap" data-act="refresh" aria-label="Actualizar">${I('refresh', 20)}</button>
    </div>
    <div class="search">${I('search', 16)}<input id="appsQ" placeholder="Buscar app" autocomplete="off" value="${esc(S.q)}"></div>
    <div class="chips">
      ${chip('Todas', visible.length, S.appsF === 'all', 'data-f="all"')}
      ${chip('Usuario', user, S.appsF === 'usr', 'data-f="usr"')}
      ${chip('Sistema', sys, S.appsF === 'sys', 'data-f="sys"')}
    </div>
    <div class="scroll card tight">
      ${list.length ? list.map(a => `<button class="row tap" data-app="${esc(a.p)}">
        <div class="appicon" style="background:${a.bg};color:${a.fg}">${a.a}</div>
        <div class="txt"><b>${esc(a.n)}</b><span>${esc(a.p)} · ${esc(a.s)}</span></div>
        <div class="chev">${I('chev', 16)}</div></button>`).join('')
        : empty('Sin resultados', 'Prueba con otro nombre o paquete.')}
    </div>
  </div>
  ${phoneNav('apps')}`;
}

function phoneDestine() {
  const remote = connected();
  return `
  <div class="body">
    <div class="appbar">
      <div class="titles"><h2>Destino</h2><p>Elige el equipo a controlar</p></div>
      <div class="iconbtn primary">${I('phone', 20)}</div>
    </div>
    <div class="chips">
      ${chip('Este teléfono', null, !remote, 'data-mode="local"')}
      ${chip('Remoto' + (remote ? ' · ' + esc(device().label) : ''), null, remote, 'data-mode="remote"')}
    </div>
    <div class="scroll">
      <div class="card" style="margin-bottom:10px">
        <div class="row" style="padding:0 0 12px;border:0">
          <div class="appicon" style="background:var(--primary-soft);color:var(--primary)">${remote ? device().short : I('phone', 18)}</div>
          <div class="txt"><b>${remote ? esc(device().name) : 'Este teléfono'}</b>
            <span>${remote ? '192.168.1.50:5555 · conectado' : 'Trabajando en local'}</span></div>
          <span style="width:8px;height:8px;border-radius:50%;background:${remote ? 'var(--primary)' : 'var(--muted)'}"></span>
        </div>
        ${remote ? `<div style="display:flex;gap:8px">
            <button class="btn tonal tap" style="flex:1;height:38px" data-screen="remote">Control remoto</button>
            <button class="btn tap" style="flex:1;height:38px" data-screen="mirror">Espejo</button>
          </div>
          <button class="btn tap" style="height:38px;margin-top:10px" data-act="disconnect">Desconectar</button>`
        : `<button class="btn tonal tap" style="height:38px" data-act="connectLocal">Conectar este teléfono</button>`}
        <div class="row tap" data-go="tools" style="padding:12px 0 0;border:0">
          <div class="ico i-grey">${I('term')}</div>
          <div class="txt"><b>Herramientas de este equipo</b><span>Registro, consola, archivos, debloater…</span></div>
          <div class="chev">${I('chev', 16)}</div>
        </div>
      </div>
      <div class="section-label">Dispositivos conocidos</div>
      <div class="group" style="margin-bottom:10px">
        ${row('cyan','phone','Pixel 7','192.168.1.22 · TLS','data-modal="pair"','<span class="tiny muted">Conectar</span>')}
        ${row('amber','phone','TV del salón','192.168.1.51 · clásico','data-modal="pair"','<span class="tiny muted">Conectar</span>')}
      </div>
      <button class="btn tap" data-screen="wizard" style="height:42px">Añadir dispositivo</button>
    </div>
  </div>
  ${phoneNav('destine')}`;
}

function phoneOptimize() {
  const sel = selCount(), risky = riskyCount();
  return `
  <div class="body" style="padding-top:8px">
    <div class="appbar" style="padding:2px 0 8px"><div class="titles"><h2>Optimizar</h2>
      <p>Perfil: ${optProfileLabel()} · ${RECIPES.length} recetas</p></div></div>
    <div class="scroll">
      <p class="intro-line">Revisa los cambios disponibles y previsualízalos antes de aplicarlos.</p>
      <div class="chips">
        ${[['phone','Teléfono'],['tv','TV'],['none','Sin perfil']].map(([id,l]) =>
          `<button class="pchip ${optProfile() === id ? 'on' : ''}" data-prof="${id}">${l}</button>`).join('')}
      </div>
      <div class="countline"><span><b style="color:var(--text)">${sel}</b> de ${RECIPES.length} disponibles</span>
        <span style="color:var(--tertiary)">${risky ? risky + (risky === 1 ? ' requiere' : ' requieren') + ' confirmación' : ''}</span></div>
      ${RECIPES.map((r, i) => `<button class="plan tap ${r.on ? 'on' : ''}" data-rec="${i}">
        <div class="cb">${r.on ? I('check', 14) : ''}</div>
        <div class="txt"><b>${esc(r.t)}</b><span>${esc(r.s)}</span></div>
        <span class="riskpill ${r.risk}">${r.risk === 'safe' ? 'Seguro' : r.risk === 'mod' ? 'Moderado' : 'Avanzado'}</span>
      </button>`).join('')}
      ${S.undo ? `<div class="countline" style="margin-top:4px"><span style="color:var(--secondary)">Cambios aplicados</span>
        <button class="pchip" data-act="undo">Deshacer</button></div>` : ''}
    </div>
    <div class="applybar">
      <div class="small muted" style="flex:1">${S.applying ? 'Aplicando ' + S.progress + '%' : sel + ' de ' + RECIPES.length + ' seleccionadas'}
        ${S.applying ? `<div class="progress"><i style="width:${S.progress}%"></i></div>` : ''}</div>
      <button class="fab" data-act="apply" ${S.applying || !sel ? 'disabled' : ''} aria-label="Aplicar">${I('bolt', 22)}</button>
    </div>
  </div>
  ${phoneNav('optimize')}`;
}

function phoneTools() {
  const q = S.toolsQ.toLowerCase();
  let html = '', n = 0;
  TOOLS.forEach(([g, items]) => {
    const m = items.filter(i => (i[2] + i[3]).toLowerCase().includes(q));
    if (!m.length) return;
    n += m.length;
    html += `<div class="section-label">${g}</div><div class="group" style="margin-bottom:12px">
      ${m.map(i => row(i[0], i[1], i[2], i[3], `data-tool="${TOOL_IDS[i[2]] || esc(i[2])}"`)).join('')}</div>`;
  });
  return `
  <div class="body">
    <div class="appbar">
      <button class="iconbtn tap" data-act="back" aria-label="Atrás">${I('back', 20)}</button>
      <div class="titles"><h2>Herramientas</h2><p>${isTV() ? 6 : 7} utilidades · ${esc(device().name)}</p></div>
    </div>
    <div class="search">${I('search', 16)}<input id="toolsQ" placeholder="Buscar herramienta" autocomplete="off" value="${esc(S.toolsQ)}"></div>
    <div class="scroll">${n ? html : empty('Sin resultados', 'No hay herramientas con ese nombre.')}</div>
  </div>`;
}

function phoneSettings() {
  return `
  <div class="body">
    <div class="appbar"><div class="titles"><h2>Ajustes</h2><p>Preferencias de la app</p></div></div>
    <div class="scroll">
      <div class="section-label">Apariencia</div>
      <div class="group" style="margin-bottom:12px">
        ${row('grey','moon','Tema', S.theme || 'Seguir el sistema','data-act="theme"','<div class="chev">'+I('chevdown',16)+'</div>')}
      </div>
      <div class="section-label">Apps</div>
      <div class="group" style="margin-bottom:12px">
        <button class="row tap" data-pref="showSystem"><div class="ico i-grey">${I('grid')}</div>
          <div class="txt"><b>Mostrar apps del sistema</b><span>Incluye apps de sistema en la lista</span></div>
          <div class="tog ${S.prefs.showSystem ? 'on' : ''}"></div></button>
      </div>
      <div class="section-label">Conexión y dispositivo</div>
      <div class="group" style="margin-bottom:12px">
        <button class="row tap" data-pref="keepScreenOn"><div class="ico i-grey">${I('phone')}</div>
          <div class="txt"><b>Mantener pantalla encendida</b><span>Evita que se apague mientras usas la app</span></div>
          <div class="tog ${S.prefs.keepScreenOn ? 'on' : ''}"></div></button>
        <button class="row tap" data-pref="autoMetrics"><div class="ico i-grey">${I('refresh')}</div>
          <div class="txt"><b>Actualizar métricas</b><span>${S.prefs.autoMetrics ? 'Automáticamente · cada ' + S.refreshSec + ' s' : 'Desactivado · solo manual'}</span></div>
          <div class="tog ${S.prefs.autoMetrics ? 'on' : ''}"></div></button>
      </div>
      <div class="section-label">Avanzado</div>
      <div class="group">
        ${row('grey','layers','Administración (DPC)','Solo con un perfil de trabajo activo','data-act="dpc"')}
      </div>
    </div>
  </div>
  ${phoneNav('settings')}`;
}

function phoneWizard() {
  return `
  <div class="body">
    <div class="appbar">
      <button class="iconbtn tap" data-act="back" aria-label="Atrás">${I('back', 20)}</button>
      <div class="titles"><h2>Nuevo dispositivo</h2><p>PASO 1 DE 2</p></div>
      <div class="iconbtn">${I('info', 18)}</div>
    </div>
    <div class="scroll">
      <div class="wizard-title">¿Qué quieres conectar?</div>
      <p class="wizard-sub">Toca un equipo para empezar a vincular.</p>
      <button class="wcard tap" data-modal="pair"><div class="ico">${I('display', 26)}</div>
        <div class="txt"><b>Android TV / TV Box</b><span>Vinculación con código</span></div>
        <div class="chev">${I('chev', 18)}</div></button>
      <button class="wcard tap" data-modal="qr"><div class="ico">${I('phone', 26)}</div>
        <div class="txt"><b>Teléfono o tablet</b><span>Vinculación con QR</span></div>
        <div class="chev">${I('chev', 18)}</div></button>
      <div class="wsep"></div>
      <div style="font-size:14px;font-weight:600">¿Ya lo habías vinculado?</div>
      <button class="wlink" data-act="discover">Buscar en mi red</button>
      <button class="wlink" data-modal="manual">Conectar por dirección IP</button>
    </div>
  </div>`;
}

function phoneConnecting() {
  return `
  <div class="body">
    <div class="appbar"><div class="titles"><h2>Conectando</h2><p>${esc(device().name)}</p></div></div>
    <div class="scroll">
      <div class="card">
        <div class="row" style="padding:0">
          <div class="appicon" style="background:var(--primary-soft);color:var(--primary)">${device().short}</div>
          <div class="txt"><b>Abriendo sesión ADB</b><span>Handshake TLS y autorización</span></div>
          <span class="small muted">${S.progress}%</span>
        </div>
        <div class="progress"><i style="width:${S.progress}%"></i></div>
      </div>
      <p class="intro-line" style="margin-top:12px">En la app real el puerto se descubre por mDNS y se vincula con el código de 6 dígitos.</p>
    </div>
  </div>`;
}

function phoneNav(active) {
  const NAV = [['home','home','Inicio'],['apps','grid','Apps'],['destine','phone','Destino'],['optimize','bolt','Optimizar'],['settings','sliders','Ajustes']];
  return `<div class="nav">${NAV.map(([id, ic, l]) =>
    `<button class="tab tap ${id === active ? 'on' : ''}" data-go="${id}"><span class="pillnav">${I(ic)}</span>${l}</button>`).join('')}</div>`;
}

function optProfile() { return S.optProfile || (S.kind === 'phone' ? 'phone' : 'tv'); }
function optProfileLabel() {
  const p = optProfile();
  return p === 'none' ? 'Sin perfil' : p === 'tv' ? 'TV' : 'Teléfono';
}
function sessionLabel() {
  if (!connected()) return 'Sin sesión';
  return S.kind === 'phone' ? 'Este teléfono · local' : device().name + ' · conectado';
}

/* =========================================================================
   PANTALLAS · TELEVISOR
   ========================================================================= */
function tvConnect() {
  if (S.screen === 'pair' || S.screen === 'connecting') {
    const card = S.screen === 'connect' ? '' : tvStepsCard();
    return `<div class="tvcenter"><div class="tvcenter-card">${card}</div></div>`;
  }
  return `
  <div class="tvsetup">
    <div class="setup-copy">
      <h2>Vincula ${S.kind === 'box' ? 'esta caja' : S.kind === 'projector' ? 'este proyector' : 'esta TV'} en un minuto</h2>
      <p>Misma red Wi-Fi que tu teléfono. Sin cables, sin cuentas y sin instalar nada raro.</p>
      <ol class="steps">
        <li><span class="n">1</span><div><b>Ajustes → Información</b><small>Toca siete veces «Compilación» para activar Opciones de desarrollador.</small></div></li>
        <li><span class="n">2</span><div><b>Depuración inalámbrica</b><small>Actívala y toca «Vincular dispositivo con código». El puerto aparece ahí.</small></div></li>
        <li><span class="n">3</span><div><b>Escribe el código</b><small>Seis dígitos. El puerto cambia cada vez, pero el simulador lo resuelve.</small></div></li>
      </ol>
      <div class="setup-note"><span class="dot live" style="background:var(--muted)"></span>Nada se modifica hasta que lo confirmes, y todo se puede deshacer.</div>
    </div>
    <div class="setup-card">
      <div class="row" style="padding:0;border:0"><span class="dot-ok" style="margin-left:0"></span>
        <div class="txt" style="flex:1"><b>${esc(device().name)}</b><small>192.168.1.50 · misma red</small></div>
        <span class="chipstate">Encontrado</span></div>
      <div class="setup-divider"></div>
      <div class="setup-label">Depuración inalámbrica</div>
      <div class="setup-toggle"><span>Activada en el equipo</span><span class="chipstate">Lista</span></div>
      <div class="setup-label">Puerto de vinculación</div>
      <div class="setup-port">41235</div>
      <button class="tvbtn tonal" data-screen="pair">Vincular con código</button>
      <button class="tvbtn" data-act="connect">Probar ADB local</button>
      <small class="setup-help">En la app real el puerto se descubre por mDNS; aquí está simulado.</small>
    </div>
  </div>`;
}

function tvStepsCard() {
  if (S.screen === 'connecting') {
    return `<div class="card tvcodes" style="padding:2.4cqw">
      <div class="row" style="padding:0;border:0"><div class="appicon" style="background:var(--primary-soft);color:var(--primary)">${device().short}</div>
        <div class="txt" style="flex:1"><b>Abriendo sesión ADB</b><small>Handshake TLS y autorización</small></div>
        <span class="chipstate">${S.progress}%</span></div>
      <div class="progress" style="margin-top:1.4cqw"><i style="width:${S.progress}%"></i></div></div>`;
  }
  return `<div class="card tvcodes" style="padding:2.4cqw">
    <h3 class="setup-label" style="font-size:1.3cqw;letter-spacing:0;text-transform:none;color:var(--text)">Código de vinculación</h3>
    <p class="setup-help" style="margin-bottom:1.4cqw">Android muestra este código en el diálogo del equipo.</p>
    <div class="codebox" style="gap:1cqw;margin:0 0 1.8cqw">${'123456'.split('').map(d=>`<i style="height:6cqw;font-size:2.6cqw">${d}</i>`).join('')}</div>
    <button class="tvbtn tonal" data-act="connect" style="width:100%;justify-content:center">Vincular</button>
    <p class="setup-help">El puerto cambia en cada vinculación; el simulador lo resuelve por ti.</p>
  </div>`;
}

function tvHome() {
  const d = device();
  return `
  <div class="tvpanel">
    <div class="h"><h2>Inicio</h2><p>${S.kind === 'phone' ? 'Este teléfono' : d.label + ' · conectado'} · ${metricsAge()}</p></div>
    <div class="hero">
      <div class="big">${d.short}</div>
      <div class="info"><b>${esc(d.name)}</b><span>${esc(d.android)} · ${d.apps} apps · ${S.metrics.ram} GB de ${d.ram.total} GB en uso</span></div>
      <div class="actions">
        <button class="tvbtn tonal" data-go="optimize">${I('bolt', 13)}Optimizar</button>
      </div>
    </div>
    <div class="stats">
      <div class="tstat"><div class="l">RAM</div><div class="v">${S.metrics.ram} GB</div><div class="s">en uso de ${d.ram.total} GB</div></div>
      <div class="tstat"><div class="l">Almacenamiento</div><div class="v">${S.metrics.storage} GB</div><div class="s">libres de ${d.storage.total} GB</div></div>
      <div class="tstat"><div class="l">Apps</div><div class="v">${d.apps}</div><div class="s">${d.sys} de sistema</div></div>
    </div>
    <div class="rowtitle">Accesos directos</div>
    <div class="grid4">
      <button class="tile" data-go="optimize"><div class="ico i-green">${I('bolt', 17)}</div><div class="txt"><b>Optimizar</b><span>${selCount()} recetas</span></div></button>
      <button class="tile" data-go="tools"><div class="ico i-amber">${I('trash', 17)}</div><div class="txt"><b>Debloater</b><span>3 sugeridas</span></div></button>
      <button class="tile" data-tool="profiles"><div class="ico i-green">${I('layers', 17)}</div><div class="txt"><b>Perfiles</b><span>3 conjuntos</span></div></button>
      <button class="tile" data-go="tools"><div class="ico i-cyan">${I('log', 17)}</div><div class="txt"><b>Registro</b><span>En vivo</span></div></button>
    </div>
    <div class="rowtitle">Historial reciente</div>
    <button class="toolrow" data-act="undo">
      <div class="ico i-cyan">${I('history', 17)}</div>
      <div class="t"><b>${S.history[0] || 'Forzar reposo profundo'}</b><span>${S.history.length ? 'hace un momento · aplicada' : 'hace 12 min · aplicada'}</span></div>
      <span class="value-pill" style="color:var(--secondary)">DESHACER</span>
    </button>
  </div>`;
}

function tvApps() {
  const q = S.q.toLowerCase();
  const all = S.apps;
  const visible = all.filter(a => S.prefs.showSystem || !a.sys);
  const user = visible.filter(a => !a.sys).length;
  const sys = all.filter(a => a.sys).length;
  const list = visible.filter(a => (S.appsF === 'all' || (S.appsF === 'usr' ? !a.sys : a.sys)) && (a.n + a.p).toLowerCase().includes(q));
  const focusApp = list[S.focus % Math.max(1, list.length)] || list[0];
  return `
  <div class="tvpanel">
    <div class="h"><h2>Apps</h2><p>${visible.length} apps · ${sys} de sistema</p></div>
    <div class="filterbar">
      <button class="fchip ${S.appsF === 'all' ? 'on' : ''}" data-f="all">Todas<span class="n">${visible.length}</span></button>
      <button class="fchip ${S.appsF === 'usr' ? 'on' : ''}" data-f="usr">Usuario<span class="n">${user}</span></button>
      <button class="fchip ${S.appsF === 'sys' ? 'on' : ''}" data-f="sys">Sistema<span class="n">${sys}</span></button>
    </div>
    <div class="appgrid">
      ${list.slice(0, 10).map((a, i) => `<button class="app ${i === S.focus % Math.max(1, list.slice(0,10).length) ? 'f' : ''}" data-app="${esc(a.p)}">
        <div class="sq" style="background:${a.bg};color:${a.fg}">${a.a}</div><b>${esc(a.n)}</b><span>${esc(a.s)}</span></button>`).join('')}
    </div>
    <div class="rowtitle">Acciones de ${esc(focusApp ? focusApp.n : 'la app')}</div>
    <div class="tiles">
      <button class="tile" data-act="aot"><div class="ico i-green">${I('play', 17)}</div><div class="txt"><b>Compilar AOT</b><span>Arranque más rápido</span></div></button>
      <button class="tile" data-act="restrict"><div class="ico i-amber">${I('bolt', 17)}</div><div class="txt"><b>Restringir</b><span>Segundo plano</span></div></button>
      <button class="tile" data-act="uninstall"><div class="ico i-red">${I('trash', 17)}</div><div class="txt"><b>Desinstalar</b><span>Solo este usuario</span></div></button>
    </div>
  </div>`;
}

function tvOptimize() {
  return `
  <div class="tvpanel">
    <div class="h"><h2>Optimizar</h2><p>${RECIPES.length} recetas compatibles · ${RECIPES.filter(r=>r.applied).length} aplicadas · sin root</p></div>
    <div class="list">
      ${RECIPES.map((r, i) => `<button class="recipe ${i === S.focus % RECIPES.length ? 'f' : ''}" data-rec="${i}">
        <div class="t"><b>${esc(r.t)}</b><span>${esc(r.s)}</span></div>
        <span class="risk ${r.applied ? 'done' : r.risk}">${r.applied ? 'APLICADA' : r.risk === 'safe' ? 'SEGURO' : r.risk === 'mod' ? 'MEDIO' : 'AVANZADO'}</span>
      </button>`).join('')}
    </div>
  </div>`;
}

function tvTools() {
  return `
  <div class="tvpanel">
    <div class="h"><h2>Herramientas</h2><p>Todo se ejecuta en este ${S.kind === 'phone' ? 'teléfono' : 'televisor'}</p></div>
    <div class="stack">
      ${TOOLS.flatMap(([, items]) => items).slice(0, 6).map((t, i) =>
        `<button class="toolrow ${i === S.focus % 6 ? 'f' : ''}" data-tool="${TOOL_IDS[t[2]] || esc(t[2])}">
          <div class="ico i-${t[0]}">${I(t[1], 17)}</div>
          <div class="t"><b>${esc(t[2])}</b><span>${esc(t[3])}</span></div>
          <span class="value-pill">Abrir</span></button>`).join('')}
    </div>
  </div>`;
}

function tvSettings() {
  const rows = [
    { k:'showSystem', t:'Mostrar apps del sistema', s:'Incluye apps de sistema en la lista' },
    { k:'keepScreenOn', t:'Mantener pantalla encendida', s:'Evita que la TV entre en reposo mientras se usa la app' },
    { k:'autoMetrics', t:'Actualizar métricas', s: S.prefs.autoMetrics ? 'Automáticamente · cada ' + S.refreshSec + ' s' : 'Desactivado · solo manual' }
  ];
  return `
  <div class="tvpanel">
    <div class="h"><h2>Ajustes</h2><p>Se guardan en este ${S.kind === 'phone' ? 'teléfono' : 'televisor'}</p></div>
    <div class="stack">
      ${rows.map((r, i) => `<button class="setrow ${i === S.focus % rows.length ? 'f' : ''}" data-pref="${r.k}">
        <div class="t"><b>${r.t}</b><span>${r.s}</span></div>
        <div class="toggle ${S.prefs[r.k] ? 'on' : ''}"><i></i></div></button>`).join('')}
      <button class="setrow" data-act="theme"><div class="t"><b>Tema</b><span>${S.theme || 'Seguir el sistema'}</span></div><span class="value-pill">Cambiar</span></button>
      <button class="setrow" data-modal="about"><div class="t"><b>Acerca de AdbOptimizer</b><span>Versión, licencia y permisos</span></div><span class="value-pill">Abrir</span></button>
    </div>
  </div>`;
}

function tvScreen() {
  const map = { home: tvHome, apps: tvApps, optimize: tvOptimize, tools: tvTools, settings: tvSettings, tool: tvTool };
  return (map[S.screen] || tvHome)();
}

function tvRail() {
  const items = [['home','home','Inicio'],['apps','grid','Apps'],['optimize','bolt','Optimizar'],['tools','term','Herramientas'],['settings','sliders','Ajustes']];
  const active = S.screen === 'tool' ? 'tools' : S.screen;
  return items.map(([id, ic, l]) =>
    `<button class="${active === id ? 'on' : ''}" data-go="${id}">${I(ic, 16)}${l}</button>`).join('');
}

/* =========================================================================
   ACCIONES
   ========================================================================= */
function togglePref(k) {
  S.prefs[k] = !S.prefs[k];
  if (k === 'showSystem') S.appsF = S.prefs.showSystem ? 'all' : (S.appsF === 'sys' ? 'usr' : S.appsF);
  if (k === 'autoMetrics' && S.prefs.autoMetrics) S.metrics.at = Date.now();
  render();
  const label = k === 'showSystem' ? (S.prefs[k] ? 'Apps del sistema visibles' : 'Apps del sistema ocultas')
    : k === 'keepScreenOn' ? (S.prefs[k] ? 'Pantalla siempre encendida' : 'La pantalla puede apagarse')
    : (S.prefs[k] ? 'Métricas cada ' + S.refreshSec + ' s' : 'Métricas solo manuales');
  toast(label);
}

function startApply() {
  const sel = selCount();
  if (!sel) return toast('Selecciona al menos una receta', 'warn');
  S.applying = true; S.progress = 0; closeModal(); render();
  const t = setInterval(() => {
    S.progress = Math.min(100, S.progress + 20);
    render();
    if (S.progress >= 100) {
      clearInterval(t);
      S.applying = false; S.undo = true;
      RECIPES.forEach(r => { if (r.on) r.applied = true; });
      S.history.unshift('Aplicar ' + sel + ' recetas');
      render();
      toast(sel + ' recetas aplicadas y verificadas');
    }
  }, 220);
}

function undoApply() {
  if (!S.undo) return toast('No hay cambios que deshacer', 'warn');
  RECIPES.forEach(r => { if (r.on) r.applied = false; });
  S.undo = false; S.history.shift();
  render();
  toast('Cambios revertidos a los valores anteriores');
}

function askApply() {
  const sel = selCount(), risky = riskyCount();
  if (!sel) return toast('Selecciona al menos una receta', 'warn');
  if (risky) {
    openModal({
      type: 'confirmApply',
      title: 'Revisar ajustes moderados',
      text: `Vas a aplicar ${risky} cambio${risky === 1 ? '' : 's'} que pueden alterar el equipo. Se leerán de vuelta y podrás deshacerlos.`,
      action: 'applyNow'
    });
  } else {
    openModal({
      type: 'confirmApply',
      title: `Aplicar ${sel} recetas`,
      text: `Se ejecutarán en ${device().name}. Todo se puede deshacer.`,
      action: 'applyNow'
    });
  }
}

/* =========================================================================
   EVENTOS
   ========================================================================= */
document.addEventListener('click', e => {
  if (e.target.classList && e.target.classList.contains('overlay')) return closeModal();
  const close = e.target.closest('button[data-close]');
  if (close) return closeModal();

  const goEl = e.target.closest('[data-go]');
  if (goEl) return go(goEl.dataset.go);

  const scr = e.target.closest('[data-screen]');
  if (scr) return go(scr.dataset.screen);

  const modal = e.target.closest('[data-modal]');
  if (modal) return openModal({ type: modal.dataset.modal });

  const f = e.target.closest('[data-f]');
  if (f) {
    if (f.dataset.f === 'sys' && !S.prefs.showSystem) {
      S.prefs.showSystem = true;
      toast('Apps del sistema activadas');
    }
    S.appsF = f.dataset.f; S.focus = 0; return render();
  }

  const prof = e.target.closest('[data-prof]');
  if (prof) { S.optProfile = prof.dataset.prof; return render(); }

  const rec = e.target.closest('[data-rec]');
  if (rec) {
    const r = RECIPES[+rec.dataset.rec];
    if (isTV()) {
      return openModal({
        type: 'confirmApply',
        title: r.applied ? 'Revertir “' + r.t + '”' : 'Aplicar “' + r.t + '”',
        text: r.applied ? 'Volverá al valor anterior del sistema.' : 'Se cambiarán los ajustes del sistema y se leerán de vuelta para confirmar. Podrás deshacerlo desde el historial.',
        action: 'applyNow'
      });
    }
    r.on = !r.on; return render();
  }

  const app = e.target.closest('[data-app]');
  if (app) return openModal({ type: 'app', pkg: app.dataset.app });

  const exp = e.target.closest('[data-explore]');
  if (exp) return openModal({ type: 'explore', id: exp.dataset.explore });

  const pref = e.target.closest('[data-pref]');
  if (pref) return togglePref(pref.dataset.pref);

  const pick = e.target.closest('[data-pick]');
  if (pick) { S.pendingKind = pick.dataset.pick; return render(); }

  const tool = e.target.closest('[data-tool]');
  if (tool) return openTool(tool.dataset.tool);

  const mode = e.target.closest('[data-mode]');
  if (mode) {
    if (mode.dataset.mode === 'local') {
      if (connected()) return toast('Ya estás en este teléfono');
      return connect();
    }
    return toast('Elige un dispositivo de la lista');
  }

  const act = e.target.closest('[data-act]');
  if (!act) return;
  const a = act.dataset.act;
  if (a === 'refresh') { resetMetrics(false); render(); return toast('Métricas actualizadas'); }
  if (a === 'back') return back();
  if (a === 'apply') return askApply();
  if (a === 'applyNow') return startApply();
  if (a === 'undo') return undoApply();
  if (a === 'connect' || a === 'connectLocal') return connect();
  if (a === 'disconnect') return disconnect();
  if (a === 'applyKind') {
    const k = S.pendingKind || S.kind;
    S.modal = null; S.pendingKind = null;
    return setKind(k);
  }
  if (a === 'remote') return toast('Control remoto abierto');
  if (a === 'mirror') return toast('Espejo iniciado');
  if (a === 'shizuku') return toast('Shizuku: permiso pendiente');
  if (a === 'discover') return toast('Buscando dispositivos en la red…');
  if (a === 'theme') {
    const list = ['Seguir el sistema', 'Oscuro', 'Claro'];
    S.theme = list[(list.indexOf(S.theme || list[0]) + 1) % list.length];
    render(); return toast('Tema: ' + S.theme);
  }
  if (a === 'dpc') return toast('Administración (DPC): requiere perfil de trabajo');
  if (a === 'aot') return toast('Compilando AOT…');
  if (a === 'restrict') return toast('Restringida en segundo plano');
  if (a === 'uninstall' || a === 'uninstallApp') {
    const pkg = S.modal && S.modal.pkg;
    if (pkg) S.apps = S.apps.filter(x => x.p !== pkg);
    closeModal(); render(); return toast('Desinstalada para este usuario');
  }
  if (a === 'openApp') { closeModal(); return toast('Abriendo la app'); }
  if (a === 'stopApp') { closeModal(); return toast('App detenida'); }
  if (a === 'applyProfile') { closeModal(); return toast('Perfil aplicado'); }
  if (a === 'power') { const what = act.dataset.what; closeModal(); return toast(what + ': orden enviada'); }
  if (a === 'push') return;
});

document.addEventListener('input', e => {
  if (e.target.id === 'appsQ') { S.q = e.target.value; render(); refocus('appsQ'); }
  if (e.target.id === 'toolsQ') { S.toolsQ = e.target.value; render(); refocus('toolsQ'); }
});

function refocus(id) {
  const el = document.getElementById(id);
  if (!el) return;
  el.focus();
  try { el.setSelectionRange(el.value.length, el.value.length); } catch (err) {}
}

/* teclado */
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') { if (S.modal) return closeModal(); return back(); }
  if (!connected() || !isTV() || S.modal) return;
  if (e.key === 'Enter' || e.key === ' ') {
    const f = document.querySelector('.tvframe .f');
    if (f) { e.preventDefault(); f.click(); }
    return;
  }
  const dir = { ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1 }[e.key];
  if (dir == null) return;
  const items = [...document.querySelectorAll('.tvframe .tvpanel button')];
  if (!items.length) return;
  e.preventDefault();
  S.focus = ((S.focus + dir) % items.length + items.length) % items.length;
  render();
});

/* =========================================================================
   ARRANQUE
   ========================================================================= */
function render() {
  const el = document.getElementById('device');
  if (!el) return;
  el.innerHTML = isTV() ? renderTv() : renderPhone();
  document.querySelectorAll('.bench [data-kind]').forEach(b => b.classList.toggle('on', b.dataset.kind === S.kind));
  const st = document.getElementById('state');
  if (st) st.textContent = connected() ? 'Conectado · ' + device().label
    : (S.phase === 'connecting' ? 'Conectando…' : 'Sin conexión');
}

function startClock() {
  const tick = () => {
    const el = document.getElementById('clock');
    if (el) { const d = new Date(); el.textContent = String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0'); }
    const tvclock = document.querySelector('.tvframe .clock');
    if (tvclock) { const d = new Date(); tvclock.textContent = String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0'); }
  };
  tick();
  setInterval(tick, 30000);
}

function boot() {
  resetState();
  const q = new URLSearchParams(location.search);
  if (q.get('kind')) S.kind = q.get('kind');
  S.apps = (S.kind === 'phone' ? APPS_PHONE : APPS_TV).map(a => ({ ...a }));
  if (q.get('connected') === '1') { S.phase = 'connected'; S.screen = q.get('screen') || 'home'; }
  if (q.get('screen')) S.screen = q.get('screen');
  if (q.get('tool')) { ensureTools(); S.tool = q.get('tool'); S.screen = 'tool'; }
  if (q.get('modal') === 'kind') S.modal = { type: 'kind' };
  if (q.get('modal') === 'app') S.modal = { type: 'app', pkg: q.get('pkg') || S.apps[0].p };
  if (q.get('modal') === 'about') S.modal = { type: 'about' };
  if (q.get('modal') === 'pair') S.modal = { type: 'pair' };
  if (q.get('modal') === 'manual') S.modal = { type: 'manual' };
  if (q.get('modal') === 'qr') S.modal = { type: 'qr' };
  if (q.get('modal') === 'confirm') S.modal = { type: 'confirmApply', title: 'Aplicar “Apagar animaciones”', text: 'Se cambiarán los ajustes del sistema y se leerán de vuelta para confirmar. Podrás deshacerlo desde el historial.', action: 'applyNow' };
  resetMetrics(true);
  render();
  startClock();
}
