/* Temporizador avanzado  ⏱️
   – Cronómetro, cuenta-atrás y alarma
   – Alertas: sonido (YouTube hidden player) y/o vibración continua
------------------------------------------------------------------- */
import { create, qs, qsa } from "./utils.js";

const YT_ID = "JoolQUDWq-k";          // vídeo con sonido de alarma
let ytPlayer = null, ytReady = false;

/* === API pública (llamada desde daily.js) ============================ */
export function initAdvancedTimer(host) {

  /* evita duplicar controles si el usuario vuelve al tab diario */
  if (qs("#timer-box", host)) return;

  host.innerHTML = markup();          // inyecta UI

  /* ---------- referencias DOM ---------- */
  const box        = qs("#timer-box");
  const display    = qs("#series-timer");
  const modeSel    = qs("#timer-mode");
  const timeInput  = qs("#timer-input");
  const presetSel  = qs("#preset-list");
  const starBtn    = qs("#add-preset");
  const playBtn    = qs("#timer-start");
  const pauseBtn   = qs("#timer-pause");
  const resetBtn   = qs("#timer-reset");
  const alertSel   = qs("#alert-mode");

  /* ---------- presets ---------- */
  const KEY = "gym_presets_v1";
  const presets = JSON.parse(localStorage.getItem(KEY) || "[]");
  const paintPresets = () => {
    presetSel.innerHTML = "<option value=''>Presets</option>" +
      presets.map(p => `<option>${p}</option>`).join("");
  };
  paintPresets();
  starBtn.onclick = () => {
    if (timeInput.value) {
      presets.push(timeInput.value);
      localStorage.setItem(KEY, JSON.stringify(presets));
      paintPresets();
    }
  };
  presetSel.onchange = () => { if (presetSel.value) timeInput.value = presetSel.value; };

  /* ---------- campos solo para cuenta-atrás / alarma ---------- */
  modeSel.onchange = () =>
    qsa(".countdown-field", box).forEach(el =>
      el.style.display = modeSel.value === "stopwatch" ? "none" : "inline-flex");
  modeSel.onchange();

  /* ---------- core timer state ---------- */
  let timerId = null, tStart = 0, tRemain = 0, vibLoop = null, vibOn = false;

  playBtn.onclick  = () => { if (!timerId) start(); };
  pauseBtn.onclick = () => { clearInterval(timerId); timerId = null; };
  resetBtn.onclick = () => { clearInterval(timerId); timerId = null; update(0); stopAlerts(); };

  /* reinicio auto al terminar serie */
  document.addEventListener("series-changed", () => {
    if (modeSel.value === "stopwatch") { resetBtn.onclick(); }    // cronómetro → reinicia
    if (modeSel.value === "countdown") { start(); }               // cuenta-atrás → reinicia
  });

  /* una interacción del usuario silencia alarmas */
  ["pointerdown","touchstart","keydown"]
    .forEach(ev => window.addEventListener(ev, stopAlerts, { passive:true }));

  /* ---------- funciones internas ---------- */
  function start() {
    stopAlerts();
    clearInterval(timerId);

    if (modeSel.value === "stopwatch") {
      tStart = Date.now();
      timerId = setInterval(() => update(Date.now() - tStart), 1000);
    } else {
      tRemain = modeSel.value === "countdown"
              ? hmsToMs(timeInput.value)
              : msToNextAbsolute(timeInput.value);
      update(tRemain);
      timerId = setInterval(() => {
        tRemain -= 1000; update(tRemain);
        if (tRemain <= 0) { clearInterval(timerId); timerId = null; fireAlert(); }
      }, 1000);
    }
  }

  function update(ms) {
    if (ms < 0) ms = 0;
    const s = Math.floor(ms / 1000) % 60,
          m = Math.floor(ms / 60000) % 60,
          h = Math.floor(ms / 3600000);
    display.textContent = [h,m,s].map(v=>String(v).padStart(2,"0")).join(":");
    display.classList.toggle("running", !!timerId);
  }

  /* ---------- alertas ---------- */
  function fireAlert() {
    const m = alertSel.value;
    if (m.includes("sound"))    playAudio();
    if (m.includes("vibrate"))  vibrateLoop();
  }
  function vibrateLoop() {
    if (!navigator.vibrate) return;
    vibOn = true;
    navigator.vibrate(10000);                       // trig inicial
    vibLoop = setInterval(() => { if (vibOn) navigator.vibrate(10000); }, 10000);
  }
  function stopAlerts() {
    vibOn = false; clearInterval(vibLoop); navigator.vibrate?.(0);
    if (ytReady && ytPlayer) ytPlayer.stopVideo?.();
  }

  /* ---------- YouTube “audio-only” ---------- */
  loadYT();
  function playAudio() {
    if (!ytReady) { setTimeout(playAudio, 400); return; }
    ytPlayer.seekTo(0); ytPlayer.playVideo();
  }
  function loadYT() {
    /* la API global ya está inyectada en dashboard.html */
    if (window.YT && window.YT.Player) return build();
    window.onYouTubeIframeAPIReady = build;
    function build() {
      const div = create("div"); div.id = "yt-audio";
      div.style.cssText="position:absolute;width:0;height:0;overflow:hidden;";
      document.body.appendChild(div);
      ytPlayer = new YT.Player("yt-audio", {
        videoId: YT_ID,
        playerVars: { controls:0, fs:0, rel:0 },
        events: { onReady: () => { ytReady = true; ytPlayer.setVolume(85);} }
      });
    }
  }
}

/* ---------- helpers de tiempo ---------- */
const hmsToMs = t => {
  const [h=0,m=0,s=0] = t.split(":").map(Number);
  return ((h*60 + m)*60 + s) * 1000;
};
const msToNextAbsolute = hhmmss => {
  const [h=0,m=0,s=0] = hhmmss.split(":").map(Number);
  const now = new Date();
  const tgt = new Date(now);
  tgt.setHours(h, m, s, 0);
  if (tgt <= now) tgt.setDate(tgt.getDate() + 1);
  return tgt - now;
};

/* ---------- UI markup ---------- */
const markup = () => /*html*/`
  <div id="timer-box" class="timer-box">
    <span id="series-timer" class="timer">00:00:00</span>

    <select id="timer-mode">
      <option value="stopwatch">Cronómetro</option>
      <option value="countdown">Cuenta atrás</option>
      <option value="alarm">Alarma</option>
    </select>

    <input  id="timer-input"  class="input countdown-field" type="text"
            placeholder="hh:mm:ss" style="width:7rem">
    <select id="preset-list"  class="countdown-field"></select>
    <button id="add-preset"   class="btn-icon countdown-field" title="Guardar preset">★</button>

    <select id="alert-mode">
      <option value="sound">Sonido</option>
      <option value="vibrate">Vibrar</option>
      <option value="soundvib">Ambos</option>
    </select>

    <button id="timer-start" class="btn-icon" title="Start">▶</button>
    <button id="timer-pause" class="btn-icon" title="Pause">⏸</button>
    <button id="timer-reset" class="btn-icon" title="Reset">⭯</button>
  </div>
`;
