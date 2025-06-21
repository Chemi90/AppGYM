/*  VIEW  ──────────────────────────────────────────────────────────────
    Pantalla de gamificación RPG. 100 % frontend, sin persistencia.
    Usa un mock de Gemini; sustituye callGeminiMock() por la llamada real.

    export default function loadRPG(container) { … }
   ------------------------------------------------------------------ */
import { qs, create } from "../utils.js";

const state = { cls:"🗡️", str:5, eng:5, vit:5, wis:5, xp:0, lvl:1 };

export default function loadRPG(container){
  container.innerHTML = rpgHtml();         // pinta estructura
  hookDom();                               // enlaza eventos
  render();                                // dibuja stats iniciales
  log("sys","¡Bienvenido aventurero! El Maestro aguarda tus logros.");
}

/* ---------- HTML ---------------------------------------------------------------- */
function rpgHtml(){
return /*html*/`
<section id="rpg-root">
  <h2 class="view-title">Modo RPG</h2>

  <div class="rpg-flex">
    <span class="avatar" id="cls">${state.cls}</span>
    <div>
      <div><progress id="xpBar" max="100" value="0"></progress></div>
      <div class="stat">Fuerza<b id="str">5</b></div>
      <div class="stat">Energía<b id="eng">5</b></div>
      <div class="stat">Vitalidad<b id="vit">5</b></div>
      <div class="stat">Sabiduría<b id="wis">5</b></div>
    </div>
  </div>

  <fieldset>
    <legend>Describe tu acción / sube foto</legend>
    <textarea id="txt" rows="3" class="input" placeholder="Ej: 20 flexiones…"></textarea>

    <div class="rpg-flex mt">
      <label class="btn-icon">📸
        <input type="file" id="file" accept="image/*" hidden>
      </label>
      <select id="classSel" class="input" style="width:max-content">
        <option value="🗡️">Guerrero</option><option value="🏹">Explorador</option>
        <option value="🪄">Mago</option><option value="🙏">Monje</option>
      </select>
      <button id="send" class="btn">Enviar</button>
    </div>
  </fieldset>

  <article id="chat"></article>

  <section id="arena" class="rpg-flex mt">
    <img id="you" src="https://i.imgur.com/FYypRdt.gif">
    <span id="vs" style="font-size:2rem">⚔️</span>
    <img id="foe" src="https://i.imgur.com/X4Fq2Ef.gif">
  </section>
</section>`}

/* ---------- DOM refs + listeners ------------------------------------------- */
let txt, fileIn, classSel, sendBtn, chat, xpBar, str, eng, vit, wis, arena, vs;

function hookDom(){
  txt       = qs("#txt");
  fileIn    = qs("#file");
  classSel  = qs("#classSel");
  sendBtn   = qs("#send");
  chat      = qs("#chat");
  xpBar     = qs("#xpBar");
  str       = qs("#str"); eng=qs("#eng"); vit=qs("#vit"); wis=qs("#wis");
  arena     = qs("#arena"); vs=qs("#vs");

  classSel.onchange = ()=>{ state.cls = classSel.value; qs("#cls").textContent = state.cls; };

  sendBtn.onclick = async ()=>{
    const text = txt.value.trim();
    const file = fileIn.files[0];
    if(!text && !file){ alert("Escribe algo o selecciona una imagen"); return; }
    log("user", text || "[Imagen]");

    const imgB64 = file ? await toBase64(file) : null;
    const {delta,msg} = await callGeminiMock(text,imgB64);
    applyDelta(delta);
    log("sys",msg);
    if(Math.random() < .25) miniBattle();
    txt.value=""; fileIn.value="";
  };
}

/* ---------- Estado & render ----------------------------------------------- */
function applyDelta(d){
  Object.entries(d).forEach(([k,v])=>state[k]=(state[k]??0)+v);
  if(state.xp>=100){ state.lvl++; state.xp-=100; log("sys",`🎉 ¡Subes a nivel ${state.lvl}!`); }
  render();
}
function render(){
  xpBar.value = state.xp;
  str.textContent = state.str; eng.textContent = state.eng;
  vit.textContent = state.vit; wis.textContent = state.wis;
}

/* ---------- Chat util ------------------------------------------------------ */
function log(type,txt){
  const div=create("div","msg "+type); div.textContent=txt;
  chat.appendChild(div); chat.scrollTop=chat.scrollHeight;
}

/* ---------- Mock Gemini (sustituir por fetch real) ------------------------ */
async function callGeminiMock(prompt,b64){
  await new Promise(r=>setTimeout(r,700));
  if(b64) return {delta:{vit:7,xp:10},msg:"La comida se ve saludable → +7 Vitalidad +10 XP"};
  if(/flexiones|sentadillas|cardio|correr/i.test(prompt)){
    const reps=(prompt.match(/\d+/)||[10])[0]*1;
    return {delta:{str:reps,xp:5},msg:`📈 Detecto ejercicio → +${reps} Fuerza +5 XP`};
  }
  if(/dorm(i|í)/i.test(prompt)){
    const h=(prompt.match(/\d+(?:[.,]\d+)?/)||[7])[0]*1;
    return {delta:{eng:h,xp:3},msg:`😴 Buen descanso de ${h}h → +${h} Energía +3 XP`};
  }
  return {delta:{wis:1,xp:1},msg:"Constancia anotada → +1 Sabiduría +1 XP"};
}

/* ---------- Mini-arena ----------------------------------------------------- */
function miniBattle(){
  arena.style.display="flex";
  log("sys","¡Un rival aparece!");
  const you = state.str+state.vit+Math.random()*6;
  const foe = state.lvl*10+Math.random()*8;
  setTimeout(()=>{
    if(you>fo){
      vs.textContent="🏆";
      log("sys","🏆 Victoria → +10 XP"); applyDelta({xp:10});
    }else{
      vs.textContent="💀";
      log("sys","💀 Derrota… pierdes 2 Vitalidad"); applyDelta({vit:-2});
    }
    setTimeout(()=>arena.style.display="none",2500);
  },1500);
}

/* ---------- helpers ------------------------------------------------------- */
function toBase64(file){
  return new Promise(r=>{
    const fr=new FileReader();
    fr.onload=e=>r(e.target.result.split(",")[1]);
    fr.readAsDataURL(file);
  });
}
