import { Router }               from "./router.js";
import { qs }                   from "./utils.js";
import { loadProfile }          from "./views/profile.js";
import { loadStats }            from "./views/stats.js";
import { loadMachines }         from "./views/machines.js";
import { loadDaily }            from "./views/daily.js";
import { loadReports }          from "./views/reports.js";
import { api }                  from "./api.js";
import { initAdvancedTimer }    from "./timer.js";

/* ───────── logout ───────── */
qs("#logout").onclick = () => {
  localStorage.clear();
  location.href = "index.html";
};

/* ───────── Router ───────── */
new Router({
  profile : loadProfile,
  stats   : loadStats,
  machines: loadMachines,

  /* --- DAILY -------------------------------------------------------- */
  daily   : async container => {
    const list = await api.get("/api/machines");
    await loadDaily(container, list);

    /* Timer avanzado siempre visible arriba de #daily */
    initAdvancedTimer(container.querySelector(".view-title"));
  },

  reports : loadReports
}, qs("#view-container"));
