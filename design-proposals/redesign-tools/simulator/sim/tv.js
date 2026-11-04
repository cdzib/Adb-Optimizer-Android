/* Marco del televisor: barra, rail lateral, panel, foco y overlays */
function renderTv() {
  const d = device();
  const bar = `<div class="tvbar">
    <div class="brand"><span class="dot">${I('bolt', 11)}</span>AdbOptimizer</div>
    <span class="chipstate">${connected() ? (S.kind === 'phone' ? 'Conectado · este equipo' : d.label + ' · local') : 'Sin conexión'}</span>
    <span class="clock">${new Date().toTimeString().slice(0, 5)}</span>
  </div>`;

  if (!connected()) {
    return `<div class="tvframe">
      <div class="safearea"></div>${bar}${tvConnect()}${tvOverlay()}
      ${S.toast ? `<div class="toast show" style="position:absolute;left:8cqw;right:8cqw;bottom:3cqw;font-size:1.1cqw">${esc(S.toast)}</div>` : ''}
    </div>`;
  }

  return `<div class="tvframe">
    <div class="safearea"></div>${bar}
    <div class="tvstage">
      <div class="rail">${tvRail()}</div>
      ${tvScreen()}
    </div>
    <div class="hint">
      <span class="key">◀ ▶ ▲ ▼</span> mover <span class="sep">·</span>
      <span class="key">OK</span> abrir <span class="sep">·</span>
      <span class="key">Atrás</span> volver
      <span class="grow"><span class="dotlive"></span>${S.prefs.keepScreenOn ? 'Pantalla encendida' : 'Permiso ADB activo'}</span>
    </div>
    ${tvOverlay()}
    ${S.toast ? `<div class="toast show" style="position:absolute;left:8cqw;right:8cqw;bottom:3cqw;font-size:1.1cqw">${esc(S.toast)}</div>` : ''}
  </div>`;
}

function tvOverlay() {
  const m = S.modal;
  if (!m) return '';
  const wrap = (inner) => `<div class="dialog" data-close>${inner}</div>`;
  if (typeof toolOverlay === 'function') { const extra = toolOverlay(); if (extra) return extra; }

  if (m.type === 'kind') {
    const opts = [['tv','Televisión',''],['projector','Proyector','Sin sintonizador'],['box','Caja / Stick','Chromecast, Fire TV, Mi Box'],['phone','Teléfono / Tablet','']];
    return wrap(`<div class="sheet">
      <h3>¿Qué es este equipo?</h3>
      <p>Detectado: ${esc(device().label)}. Puedes corregirlo; se recuerda para este dispositivo.</p>
      ${opts.map(([k,l,s]) => `<button class="toolrow" data-pick="${k}" style="margin-bottom:.5cqw">
        <div class="ico i-grey">${S.pendingKind === k ? I('check', 15) : ''}</div>
        <div class="t"><b>${l}</b>${s ? `<span>${s}</span>` : ''}</div></button>`).join('')}
      <div class="btns" style="margin-top:1.2cqw">
        <button class="dbtn" data-close>Cancelar</button>
        <button class="dbtn solid" data-act="applyKind">Guardar</button></div>
    </div>`);
  }
  if (m.type === 'confirmApply') {
    return wrap(`<div class="sheet">
      <h3>${esc(m.title)}</h3><p>${esc(m.text)}</p>
      <div class="btns"><button class="dbtn" data-close>Cancelar</button>
        <button class="dbtn solid" data-act="applyNow">Aplicar</button></div>
    </div>`);
  }
  if (m.type === 'app') {
    const a = S.apps.find(x => x.p === m.pkg);
    if (!a) return '';
    return wrap(`<div class="sheet">
      <h3>${esc(a.n)}</h3><p>${esc(a.p)} · ${esc(a.s)}</p>
      <div class="btns" style="justify-content:flex-start;flex-wrap:wrap">
        <button class="dbtn solid" data-act="openApp">Abrir</button>
        <button class="dbtn" data-act="stopApp">Forzar detención</button>
        ${a.sys ? '<span class="value-pill" style="align-self:center">Protegida por el sistema</span>'
          : '<button class="dbtn" data-act="uninstallApp" style="color:var(--danger)">Desinstalar</button>'}
      </div>
      <div class="btns" style="margin-top:1.2cqw"><button class="dbtn" data-close>Cerrar</button></div>
    </div>`);
  }
  if (m.type === 'about') {
    return wrap(`<div class="sheet">
      <h3>AdbOptimizer 2.0</h3>
      <p>Simulador de dispositivo. No ejecuta comandos ADB reales.<br>
      ${esc(device().name)} · ${esc(device().android)}</p>
      <div class="btns"><button class="dbtn solid" data-close>Cerrar</button></div>
    </div>`);
  }
  if (m.type === 'profiles') {
    return wrap(`<div class="sheet">
      <h3>Perfiles</h3><p>Aplica un conjunto de apps de una vez.</p>
      <div class="btns" style="justify-content:flex-start;flex-wrap:wrap">
        <button class="dbtn" data-act="applyProfile">Streaming · 3 apps</button>
        <button class="dbtn" data-act="applyProfile">Mínimo · 8 apps</button>
        <button class="dbtn" data-act="applyProfile">Gaming · 5 apps</button>
      </div>
      <div class="btns" style="margin-top:1.2cqw"><button class="dbtn" data-close>Cerrar</button></div>
    </div>`);
  }
  return '';
}
