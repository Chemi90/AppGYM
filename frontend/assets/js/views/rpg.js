/* =========================================================================
   VISTA RPG  –  Con ficha real, chat y subida de imágenes
   (corrige ReferenceError y añade Ctrl+Enter para salto de línea)
   ========================================================================= */

import { api }        from "../api.js";
import { qs, create } from "../utils.js";

/* ────────────────────────── HELPERS UI ────────────────────────── */

/*  Barra de atributo  */
function bar(label, val) {
  return `<p>${label}
            <progress value="${val}" max="999"></progress>
            <b>${val}</b>
          </p>`;
}

/*  Convierte archivo a base-64 sin la cabecera data:  */
const toBase64 = file =>
  new Promise(res => {
    const fr = new FileReader();
    fr.onload = e => res(e.target.result.split(",")[1]);
    fr.readAsDataURL(file);
  });

/* ─────────────────────────── MAIN ────────────────────────────── */

export async function loadRpg(container) {

  container.innerHTML = /*html*/`
    <section class="rpg-view fade-in">
      <h2 class="view-title">Aventura RPG</h2>

      <!-- Ficha -->
      <div id="rpg-sheet" class="grid gap-6 md:grid-cols-2 mb-6"></div>

      <!-- Chat & acciones -->
      <div class="flex gap-2 items-start mb-4">
        <textarea id="chat-input"
                  class="input flex-1 resize-y min-h-10"
                  rows="2"
                  placeholder="Habla con tu maestro… (Ctrl+Enter para salto)"></textarea>
        <button id="chat-send" class="btn">Enviar</button>

        <input  id="photo-input" type="file" accept="image/*" hidden />
        <button id="photo-btn" class="btn-icon" title="Subir foto">📷</button>
      </div>

      <!-- Conversación -->
      <div id="chat-box"
           class="space-y-2 bg-gray-900 text-gray-100 p-4 rounded-md
                  h-64 overflow-auto text-sm"></div>
    </section>
  `;

  /* ---------- DOM refs ---------- */
  const sheetEl = qs("#rpg-sheet",  container);
  const chatBox = qs("#chat-box",   container);
  const chatIn  = qs("#chat-input", container);
  const sendBt  = qs("#chat-send",  container);
  const photoBt = qs("#photo-btn",  container);
  const photoIn = qs("#photo-input",container);

  /* ---------- Carga ficha ---------- */
  let char = null;
  try { char = await api.get("/api/rpg/character"); } catch {}
  if (char) renderSheet(char);

  /* ---------- Chat ---------- */
  sendBt.onclick = sendChat;
  chatIn.addEventListener("keydown", e => {
    /* Ctrl+Enter = salto de línea;  Enter sin Ctrl = enviar */
    if (e.key === "Enter" && !e.shiftKey) {
      if (e.ctrlKey) {
        /* inserta salto de línea manualmente */
        const { selectionStart, selectionEnd, value } = chatIn;
        chatIn.value = value.slice(0, selectionStart) + "\n" + value.slice(selectionEnd);
        chatIn.selectionStart = chatIn.selectionEnd = selectionStart + 1;
        e.preventDefault();
      } else {
        e.preventDefault();
        sendChat();
      }
    }
  });

  async function sendChat() {
    const txt = chatIn.value.trim();
    if (!txt) return;
    append("user", txt);
    chatIn.value = "";

    try {
      const { reply } = await api.post("/api/rpg/chat", { prompt: txt });
      append("ai", reply);
    } catch (err) {
      append("sys", "❌ Error enviando mensaje");
      console.error(err);
    }
  }

  /* ---------- Subida de foto ---------- */
  photoBt.onclick   = () => photoIn.click();
  photoIn.onchange  = async () => {
    const file = photoIn.files[0];
    if (!file) return;

    const base64 = await toBase64(file);
    const exercise = confirm("Aceptar = Ejercicio · Cancelar = Comida");

    try {
      if (exercise) {
        const pj = await api.post("/api/rpg/exercise", { imageBase64: base64 });
        renderSheet(pj);
        append("ai", `🏋️ ¡Fuerza +${pj.str} ·  XP ${pj.xp}/${pj.xpNext}!`);
      } else {
        const { analysis } = await api.post("/api/rpg/meal", { imageBase64: base64 });
        append("ai", `🍽️ ${analysis}`);
      }
    } catch (err) {
      append("sys", "❌ Error procesando la imagen");
      console.error(err);
    }
    photoIn.value = "";   // reset
  };

  /* ─────────────────── helpers internos ─────────────────── */

  function renderSheet(pj) {
    sheetEl.innerHTML = /*html*/`
      <article class="card shadow">
        <header class="card-header">
          <h3>${pj.clazz} · Nivel ${pj.level}</h3>
        </header>
        <div class="card-body grid gap-2">
          <p><strong>XP:</strong> ${pj.xp} / ${pj.xpNext}</p>
          ${bar("Fuerza",    pj.str)}
          ${bar("Energía",   pj.eng)}
          ${bar("Vitalidad", pj.vit)}
          ${bar("Sabiduría", pj.wis)}
        </div>
      </article>
    `;
  }

  function append(role, txt) {
    const p = create("p", role === "ai" ? "sys"
                        : role === "user" ? "user"
                        : "sys");
    p.textContent = txt;
    chatBox.appendChild(p);
    chatBox.scrollTop = chatBox.scrollHeight;
  }
}
