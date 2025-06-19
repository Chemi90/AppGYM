import { Router }            from "./router.js";
import { qs }                from "./utils.js";
import { loadProfile }       from "./views/profile.js";
import { loadStats }         from "./views/stats.js";
import { loadMachines }      from "./views/machines.js";
import { loadDaily }         from "./views/daily.js";
import { loadReports }       from "./views/reports.js";
import { initQuickAdd }      from "./quickAdd.js";
import { api }               from "./api.js";

/* logout global ------------------------------------------------------- */
qs("#logout").onclick = () => { localStorage.clear(); location.href = "index.html"; };

/* router -------------------------------------------------------------- */
new Router({
  profile : loadProfile,
  stats   : loadStats,
  machines: loadMachines,
  daily   : async (c) => {
              const machines = await api.get("/api/machines");
              await loadDaily(c, machines);          // ← timer se inyecta dentro
              initQuickAdd(machines);
            },
  reports : loadReports
}, qs("#view-container"));
