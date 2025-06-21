/* =========================================================================
   Vista RPG · Gamificación básica
   -------------------------------------------------------------------------
   • Muestra la ficha del héroe y los atributos en tiempo real.
   • No almacena nada en BD: todos los datos llegan en el objeto `state`.
   • Más adelante podrás ampliar con combates, avatares GIF, etc.
   ========================================================================= */

import { qs, create } from "../utils.js";

/**
 * Carga la pantalla RPG.
 * @param {HTMLElement} container  – nodo del <main> donde inyectar la vista
 * @param {Object} [state]         – datos opcionales (XP, niveles, etc.)
 */
export async function loadRpg(container, state = demoState()) {
  container.innerHTML = /*html*/`
    <section class="rpg-view fade-in">
      <h2 class="view-title">Aventura RPG</h2>

      <div class="grid gap-6 md:grid-cols-2">
        <!-- ── FICHA ───────────────────────────────────────────── -->
        <article class="card shadow">
          <header class="card-header">
            <h3>${state.hero.name} <small class="text-muted">· Lvl ${state.hero.level}</small></h3>
          </header>

          <div class="card-body grid gap-2">
            <p><strong>Clase:</strong> ${state.hero.class}</p>
            <p><strong>Experiencia:</strong> ${state.hero.xp} / ${state.hero.xpNext} XP</p>

            <table class="w-full text-sm">
              <tbody>
                ${attrRow("Fuerza",     state.stats.str)}
                ${attrRow("Energía",    state.stats.eng)}
                ${attrRow("Vitalidad",  state.stats.vit)}
                ${attrRow("Sabiduría",  state.stats.wis)}
              </tbody>
            </table>
          </div>
        </article>

        <!-- ── REGISTRO DE EVENTOS ─────────────────────────────── -->
        <article class="card shadow overflow-auto" style="max-height:28rem;">
          <header class="card-header"><h3>Diario de misiones</h3></header>
          <ul id="log-box" class="log-list p-4 space-y-2 text-sm"></ul>
        </article>
      </div>
    </section>
  `;

  /* muestra últimas 15 entradas de log */
  const logBox = qs("#log-box");
  state.log.slice(-15).reverse().forEach(msg => {
    const li = create("li", "log-entry");
    li.textContent = msg;
    logBox.appendChild(li);
  });
}

/* helpers ---------------------------------------------------------------- */

function attrRow(label, value) {
  return `<tr><td class="pr-2">${label}</td><td><progress max="100" value="${value}"></progress> ${value}</td></tr>`;
}

/* demo inicial si todavía no existen datos reales */
function demoState() {
  return {
    hero: {
      name   : "Athena",
      class  : "Exploradora",
      level  : 3,
      xp     : 230,
      xpNext : 400
    },
    stats: {
      str: 48,   // Fuerza
      eng: 62,   // Energía
      vit: 55,   // Vitalidad
      wis: 40    // Sabiduría
    },
    log: [
      "🏋️ Hiciste 15 flexiones ➜ +15 Fuerza, +10 XP",
      "🍎 Comiste fruta fresca ➜ +5 Vitalidad",
      "💤 Dormiste 7 h 30 m ➜ +10 Energía",
      "⚔️ Derrotaste al 'Monstruo de la Pereza'"
    ]
  };
}
