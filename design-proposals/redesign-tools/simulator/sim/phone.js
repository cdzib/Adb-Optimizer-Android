/* Marco del teléfono: barra de estado, pantalla actual, navegación y overlays */
function renderPhone() {
  const screen = S.screen;
  let body = '';
  switch (screen) {
    case 'home': body = phoneHome(); break;
    case 'apps': body = phoneApps(); break;
    case 'destine': body = phoneDestine(); break;
    case 'optimize': body = phoneOptimize(); break;
    case 'tools': body = phoneTools(); break;
    case 'settings': body = phoneSettings(); break;
    case 'wizard': body = phoneWizard(); break;
    case 'connecting': body = phoneConnecting(); break;
    case 'connect': body = phoneConnect(); break;
    case 'pair': body = phoneWizard(); break;
    case 'tool': body = phoneTool(); break;
    case 'remote': body = phoneRemote(); break;
    case 'mirror': body = phoneMirror(); break;
    default: body = S.phase === 'connected' ? phoneHome() : phoneConnect();
  }
  return `<div class="phone">
    <div class="status"><span id="clock">9:41</span><span class="dotrow"><i class="sig"></i><i></i></span></div>
    <div class="viewport">${body}</div>
    ${phoneOverlay()}
    ${S.toast ? `<div class="toast show">${esc(S.toast)}</div>` : ''}
  </div>`;
}

function phoneOverlay() {
  const m = S.modal;
  if (!m) return '';
  const close = `<div class="overlay show" data-close></div>`;
  const acts = (a, b) => `<div class="acts">
    <button class="btn tap" data-close>${a}</button>
    <button class="btn ${b.cls || 'solid'} tap" ${b.attr}>${b.label}</button></div>`;

  if (m.type === 'kind') {
    const opts = [['tv','Televisión',''],['projector','Proyector','Sin sintonizador'],['box','Caja / Stick','Chromecast, Fire TV, Mi Box'],['phone','Teléfono / Tablet','']];
    return `<div class="overlay show center" data-close>
      <div class="sheet dlg">
        <h4>¿Qué es este equipo?</h4>
        <p>Detectado: ${esc(device().label)}. Puedes corregirlo; se recuerda para este dispositivo.</p>
        ${opts.map(([k,l,s]) => `<button class="row tap" data-pick="${k}" style="padding:10px 0">
          <div class="ico i-grey">${S.pendingKind === k ? I('check', 16) : ''}</div>
          <div class="txt"><b>${l}</b>${s ? `<span>${s}</span>` : ''}</div></button>`).join('')}
        ${acts('Cancelar', { label:'Guardar', attr:'data-act="applyKind"' })}
      </div></div>`;
  }
  if (m.type === 'pair') {
    return `<div class="overlay show center" data-close>
      <div class="sheet dlg">
        <h4>Vincular ${esc(device().name)}</h4>
        <p>Introduce los datos que aparecen en la pantalla de vinculación del equipo.</p>
        <div class="field"><label>Dirección IP</label><div class="val">192.168.1.50</div></div>
        <div class="field"><label>Puerto de vinculación</label><div class="val">41235</div></div>
        <div class="tiny muted" style="margin-bottom:6px">CÓDIGO DE 6 DÍGITOS</div>
        <div class="codebox">${'123456'.split('').map(d => `<i>${d}</i>`).join('')}</div>
        ${acts('Cancelar', { label:'Vincular', attr:'data-act="connect"' })}
      </div></div>`;
  }
  if (m.type === 'qr') {
    return `<div class="overlay show center" data-close>
      <div class="sheet dlg" style="text-align:center">
        <h4>Vincular teléfono</h4>
        <p>Este teléfono muestra el QR; el otro lo escanea.</p>
        <div class="qrbox"></div>
        <p class="qrcap">Código QR de vinculación</p>
        ${acts('Cancelar', { label:'Usar código', attr:'data-modal="pair"' })}
      </div></div>`;
  }
  if (m.type === 'manual') {
    return `<div class="overlay show center" data-close>
      <div class="sheet dlg">
        <h4>Conectar por dirección IP</h4>
        <p>Ten a mano la IP y el puerto ADB del equipo.</p>
        <div class="field"><label>Dirección IP</label><input value="192.168.1.50" inputmode="decimal"></div>
        <div class="field"><label>Puerto</label><input value="5555" inputmode="numeric"></div>
        ${acts('Cancelar', { label:'Conectar', attr:'data-act="connect"' })}
      </div></div>`;
  }
  if (m.type === 'system') {
    return `<div class="overlay show center" data-close>
      <div class="sheet dlg">
        <h4>Lista de apps</h4>
        <p>Las apps de sistema solo se muestran si lo activas en Ajustes.</p>
        <button class="row tap" data-pref="showSystem" style="padding:10px 0">
          <div class="ico i-grey">${I('grid')}</div>
          <div class="txt"><b>Mostrar apps del sistema</b><span>Incluye apps de sistema en la lista</span></div>
          <div class="tog ${S.prefs.showSystem ? 'on' : ''}"></div></button>
        ${acts('Cerrar', { label:'Hecho', attr:'data-close' })}
      </div></div>`;
  }
  if (m.type === 'app') {
    const a = S.apps.find(x => x.p === m.pkg);
    if (!a) return '';
    return `<div class="overlay show" data-close>
      <div class="sheet">
        <div class="grab"></div>
        <div class="row" style="padding:0 0 12px;border:0">
          <div class="appicon" style="background:${a.bg};color:${a.fg}">${a.a}</div>
          <div class="txt"><b>${esc(a.n)}</b><span>${esc(a.p)}</span></div></div>
        <div class="stack">
          <button class="btn tonal tap" data-act="openApp">Abrir</button>
          <button class="btn tap" data-act="stopApp">Forzar detención</button>
          ${a.sys ? `<button class="btn off">Protegida por el sistema</button>`
            : `<button class="btn danger tap" data-act="uninstallApp">Desinstalar</button>`}
        </div>
        <div class="acts"><button class="btn tap" data-close>Cerrar</button></div>
      </div></div>`;
  }
  if (m.type === 'explore') {
    const c = EXPLORE.find(x => x.id === m.id);
    return `<div class="overlay show" data-close>
      <div class="sheet">
        <div class="grab"></div>
        <h4>${c.n}</h4><p>${c.d}</p>
        <div class="group" style="background:var(--surface3)">
          ${c.rows.map(([k, v]) => `<div class="row"><div class="txt"><b>${esc(k)}</b></div>
            <span class="small muted">${esc(v())}</span></div>`).join('')}
        </div>
        <div class="acts"><button class="btn tap" data-close>Cerrar</button></div>
      </div></div>`;
  }
  if (m.type === 'profiles') {
    const prof = [['amber','Streaming','YouTube, Netflix, Plex · 3 apps'],['cyan','Mínimo','Solo apps esenciales · 8 apps'],['green','Gaming','Juegos y mando · 5 apps']];
    return `<div class="overlay show" data-close>
      <div class="sheet"><div class="grab"></div>
        <h4>Perfiles</h4><p>Aplica un conjunto de apps de una vez.</p>
        <div class="group" style="background:var(--surface3)">
          ${prof.map(p => `<div class="row"><div class="ico i-${p[0]}">${I('layers')}</div>
            <div class="txt"><b>${p[1]}</b><span>${p[2]}</span></div>
            <button class="btn tap" style="height:32px;width:auto;padding:0 12px" data-act="applyProfile">Aplicar</button></div>`).join('')}
        </div>
        <div class="acts"><button class="btn tap" data-close>Cerrar</button></div>
      </div></div>`;
  }
  if (m.type === 'power') {
    const pw = [['grey','power','Apagar pantalla','Mantiene el equipo encendido'],['amber','refresh','Reiniciar','Reinicia el sistema'],['red','power','Apagar','Apaga por completo el equipo']];
    return `<div class="overlay show" data-close>
      <div class="sheet"><div class="grab"></div>
        <h4>Opciones de energía</h4><p>Se enviarán a ${esc(device().name)}.</p>
        <div class="group" style="background:var(--surface3)">
          ${pw.map(p => `<button class="row tap" data-act="power" data-what="${p[2]}">
            <div class="ico i-${p[0]}">${I(p[1])}</div>
            <div class="txt"><b>${p[2]}</b><span>${p[3]}</span></div></button>`).join('')}
        </div>
        <div class="acts"><button class="btn tap" data-close>Cerrar</button></div>
      </div></div>`;
  }
  if (typeof toolOverlay === 'function') { const extra = toolOverlay(); if (extra) return extra; }
  if (m.type === 'confirm' || m.type === 'confirmApply') {
    return `<div class="overlay show center" data-close>
      <div class="sheet dlg">
        <h4>${esc(m.title)}</h4><p>${esc(m.text)}</p>
        ${acts('Cancelar', { label:m.ok || 'Aplicar', cls:m.danger ? 'danger' : 'solid', attr:`data-act="${m.action || 'applyNow'}"` })}
      </div></div>`;
  }
  return '';
}
