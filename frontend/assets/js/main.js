import { Router }               from "./views/router.js";
import { qs }                   from "./utils.js";
import { loadProfile }          from "./views/profile.js";
import { loadStats }            from "./views/stats.js";
import { loadMachines }         from "./views/machines.js";
import { loadDaily }            from "./views/daily.js";
import { loadReports }          from "./views/reports.js";
import { initQuickAdd }         from "./views/quickAdd.js";
import { api }                  from "./api.js";
import { initAdvancedTimer }    from "./views/timer.js";

/* ---------------- logout ---------------- */
qs("#logout").onclick = () => { localStorage.clear(); location.href="index.html"; };

/* ---------------- router ---------------- */
new Router({
  profile : loadProfile,
  stats   : loadStats,
  machines: loadMachines,
  daily   : async (c)=>{            // daily necesita lista máquinas para quick-add
              const list = await api.get("/api/machines");
              await loadDaily(c, list);
              initQuickAdd(list);
              initAdvancedTimer(qs("#timer-controls"));
            },
  reports : loadReports
}, qs("#view-container"));
