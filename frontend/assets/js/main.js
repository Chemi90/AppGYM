/* =========================================================================
   Gym-Tracker · Front-end
   Enrutador principal – incluye vista RPG, sin Quick-Add
   ========================================================================= */

import { Router }            from "./router.js";
import { qs }                from "./utils.js";
import { api }               from "./api.js";

/* vistas --------------------------------------------------------------- */
import { loadProfile }       from "./views/profile.js";
import { loadStats }         from "./views/stats.js";
import { loadMachines }      from "./views/machines.js";
import { loadDaily }         from "./views/daily.js";
import { loadReports }       from "./views/reports.js";
import { loadRpg }           from "./views/rpg.js";     // ← NUEVA vista RPG

/* utilidades extra ----------------------------------------------------- */
import { initAdvancedTimer } from "./timer.js";

/* ───────── logout global ───────── */
qs("#logout").onclick = () => {
  localStorage.clear();
  location.href = "index.html";
};

/* ───────── Router ───────── */
new Router(
  {
    /* --- PERFIL ------------------------------------------------------- */
    profile : loadProfile,

    /* --- MEDIDAS ------------------------------------------------------ */
    stats   : loadStats,

    /* --- MÁQUINAS ----------------------------------------------------- */
    machines: loadMachines,

    /* --- DIARIO + temporizador --------------------------------------- */
    daily   : async container => {
      /* bloqueamos UI en utils -> showLoader() automático */
      const list = await api.get("/api/machines");
      await loadDaily(container, list);

      /* temporizador avanzado anclado bajo el título Daily */
      initAdvancedTimer(container.querySelector(".view-title"));
    },

    /* --- INFORMES ----------------------------------------------------- */
    reports : loadReports,

    /* --- RPG / Gamificación ------------------------------------------ */
    rpg     : loadRpg
  },
  qs("#view-container")
);
