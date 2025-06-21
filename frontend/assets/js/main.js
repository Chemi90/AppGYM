/* =========================================================================
   Gym-Tracker · Main Router (sin Quick-Add, con vista RPG)
   ========================================================================= */

import { Router }            from './router.js';
import { qs }                from './utils.js';
import { api }               from './api.js';

/* ------------ Vistas core ------------ */
import { loadProfile }       from './views/profile.js';
import { loadStats }         from './views/stats.js';
import { loadMachines }      from './views/machines.js';
import { loadDaily }         from './views/daily.js';
import { loadReports }       from './views/reports.js';
import { loadRpg }           from './views/rpg.js';   /* ← nueva */

/* ------------ Extras ------------ */
import { initAdvancedTimer } from './timer.js';

/* —— Logout global —— */
qs('#logout').onclick = () => {
  localStorage.clear();
  location.href = 'index.html';
};

/* —— Router SPA —— */
new Router(
  {
    /* Perfil ----------------------------------------------------------- */
    profile : loadProfile,

    /* Medidas ---------------------------------------------------------- */
    stats   : loadStats,

    /* Máquinas --------------------------------------------------------- */
    machines: loadMachines,

    /* Diario + temporizador avanzado ---------------------------------- */
    daily   : async container => {
      const list = await api.get('/api/machines');
      await loadDaily(container, list);
      /* Temporizador anclado debajo del título “Registro diario” */
      initAdvancedTimer(container.querySelector('.view-title'));
    },

    /* Informes PDF ----------------------------------------------------- */
    reports : loadReports,

    /* Gamificación RPG ------------------------------------------------- */
    rpg     : loadRpg
  },
  qs('#view-container')
);
