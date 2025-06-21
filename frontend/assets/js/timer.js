/* =========================================================================
   TIMER 2.3 · Cronómetro + Cuenta-atrás
   Sonido (Web-Audio) + Vibración + Notif. persistente  +  Wake-Lock
   Con LOGS dbg() para depuración
   ========================================================================= */

import { qs, create, dbg } from "./utils.js";

/* ------------------------------------------------------------------ CONST */
const VIB_PATTERN = new Array(10).fill([300, 300]).flat(); // 10 s tic-tac

let ctx, osc, gain;                        // Web-Audio nodes

/* ------------------------------------------------------------------ AUDIO */
function startTone () {
  dbg('TIMER', 'startTone()');
  if (!ctx) ctx = new (window.AudioContext || window.webkitAudioContext)();
  if (ctx.state === 'suspended') ctx.resume();

  osc  = ctx.createOscillator();
  gain = ctx.createGain();
  gain.gain.value     = 0.12;          // volumen bajo
  osc.frequency.value = 880;           // A5
  osc.type            = 'square';

  osc.connect(gain).connect(ctx.destination);
  osc.start();
}
function stopTone () {
  if (osc) { osc.stop(); osc.disconnect(); gain.disconnect(); dbg('TIMER', 'stopTone()'); }
  osc = gain = null;
}

/* ------------------------------------------------------------------ WAKE-LOCK */
let wakelock = null;
async function keepAwake () {
  try {
    wakelock = await navigator.wakeLock?.request('screen');
    dbg('TIMER', 'Wake-lock ON');
  } catch { /* ignored */ }
}
function releaseAwake () {
  wakelock?.release?.();
  wakelock = null;
  dbg('TIMER', 'Wake-lock OFF');
}

/* ------------------------------------------------------------------ UI */
export function initAdvancedTimer (anchor) {
  dbg('TIMER', 'initAdvancedTimer()', { anchor });

  if (qs('#timer-box')) {
    dbg('TIMER', 'Ya existía, skip');
    return;                             // sólo uno por página
  }

  /* ------------- Mark-up ------------- */
  const box = create('div', 'timer-box');
  box.id = 'timer-box';
  box.innerHTML = /*html*/`
    <span  id="series-timer" class="timer">00:00:00</span>

    <select id="timer-mode">
      <option value="stopwatch">Cronómetro</option>
      <option value="countdown">Cuenta atrás</option>
    </select>

    <input id="h" type="number" min="0" max="23" value="0" class="input w-16">
    <span>:</span>
    <input id="m" type="number" min="0" max="59" value="1" class="input w-16">
    <span>:</span>
    <input id="s" type="number" min="0" max="59" value="0" class="input w-16">

    <select id="alert-mode">
      <option value="sound">Sonido</option>
      <option value="vibrate">Vibrar</option>
      <option value="soundvib">Ambos</option>
    </select>

    <button id="btn-toggle" class="btn-icon" title="Iniciar">▶</button>
    <button id="btn-reset"  class="btn-icon" title="Reset">⭯</button>
  `;
  anchor.after(box);

  /* ------------- Elements ------------- */
  const disp    = qs('#series-timer');
  const modeSel = qs('#timer-mode');
  const hIn     = qs('#h'),  mIn = qs('#m'),  sIn = qs('#s');
  const toggle  = qs('#btn-toggle');
  const reset   = qs('#btn-reset');
  const alertEl = qs('#alert-mode');

  /* ------------- State ------------- */
  let id = null, start = 0, remain = 0, running = false;

  /* ------------- Buttons ------------- */
  toggle.onclick = () => running ? pause() : startTimer();
  reset .onclick = hardReset;

  /* ------------- Core ------------- */
  function startTimer () {
    dbg('TIMER', '▶ start', modeSel.value);
    stopAlert(); keepAwake();

    if (modeSel.value === 'stopwatch') {
      start = Date.now() - remain;                  // resume si estaba pausado
      id = setInterval(() => update(Date.now() - start), 1000);
    } else {
      remain = (+hIn.value * 3600 + +mIn.value * 60 + +sIn.value) * 1000;
      if (remain <= 0) { dbg('TIMER', 'Cuenta atrás = 0'); return; }
      update(remain);
      id = setInterval(() => {
        remain -= 1000;
        update(remain);
        if (remain <= 0) { clearInterval(id); id = null; fireAlert(); }
      }, 1000);
    }
    running = true; toggle.textContent = '⏸';
  }

  function pause () {
    dbg('TIMER', '⏸ pause');
    clearInterval(id); id = null;
    running = false; toggle.textContent = '▶';
    releaseAwake();
  }

  function hardReset () {
    dbg('TIMER', '⭯ reset hard');
    pause(); remain = 0; update(0); stopAlert();
  }

  function update (ms) {
    if (ms < 0) ms = 0;
    const sec = (ms / 1000) | 0;
    const hh  = String((sec / 3600)     | 0).padStart(2, '0');
    const mm  = String(((sec / 60) % 60)| 0).padStart(2, '0');
    const ss  = String(sec % 60).padStart(2, '0');
    disp.textContent = `${hh}:${mm}:${ss}`;
    disp.classList.toggle('running', running);
  }

  /* ------------- Alerts ------------- */
  function fireAlert () {
    dbg('TIMER', '💥 ALERT', alertEl.value);
    running = false; toggle.textContent = '▶'; releaseAwake();

    const mode = alertEl.value;
    if (mode.includes('sound'))   startTone();
    if (mode.includes('vibrate')) vibrateLoop();

    showNotif();
  }
  function stopAlert () {
    stopTone();
    navigator.vibrate?.(0);
    navigator.serviceWorker?.getRegistration()
      .then(r => r?.getNotifications({ tag: 'gym-timer' })
      .then(list => list.forEach(n => n.close())));
  }

  /* vibración prolongada loop */
  function vibrateLoop () {
    if (!navigator.vibrate) return;
    navigator.vibrate(VIB_PATTERN);
    const h = setInterval(() => navigator.vibrate(VIB_PATTERN), 10_000);
    window.addEventListener('pointerdown', () => {
      clearInterval(h); navigator.vibrate(0);
    }, { once: true, passive: true });
  }

  /* notificación (PWA/bg) */
  async function showNotif () {
    if (!('Notification' in window)) return;
    if (Notification.permission === 'default') await Notification.requestPermission();
    if (Notification.permission !== 'granted') return;

    navigator.serviceWorker.getRegistration().then(reg => {
      reg?.showNotification('¡Tiempo!', {
        body: 'Siguiente serie',
        tag: 'gym-timer',
        vibrate: VIB_PATTERN,
        requireInteraction: true
      });
    });
  }

  /* stop alerts on any interaction */
  ['pointerdown', 'keydown', 'touchstart'].forEach(ev =>
    window.addEventListener(ev, stopAlert, { passive: true })
  );
}
