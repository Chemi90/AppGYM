import { Router }            from "./router.js";
import { qs, dbg }           from "./utils.js";
import { api }               from "./api.js";

import { loadProfile } from "./views/profile.js";
import { loadStats   } from "./views/stats.js";
import { loadMachines} from "./views/machines.js";
import { loadDaily   } from "./views/daily.js";
import { loadReports } from "./views/reports.js";
import { loadRpg     } from "./views/rpg.js";

import { initAdvancedTimer } from "./timer.js";

dbg('BOOT', '🚀 Iniciando frontend');

qs('#logout').onclick = () => { localStorage.clear(); location.href = 'index.html'; };

new Router({
  profile : loadProfile,
  stats   : loadStats,
  machines: loadMachines,
  daily   : async c => {
    dbg('DAILY', 'Cargando lista máquinas');
    const list = await api.get('/api/machines');
    await loadDaily(c, list);
    initAdvancedTimer(c.querySelector('.view-title'));
  },
  reports : loadReports,
  rpg     : loadRpg
}, qs('#view-container'));
