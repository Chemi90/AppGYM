/* VIEW: Informes PDF -----------------------------------------------------
   Descarga PDF completo o por intervalo.
----------------------------------------------------------------------- */
import { api, authHeaders } from "../api.js";
import { qs, create } from "../utils.js";

export async function loadReports(container) {
  container.innerHTML = /*html*/`
    <h2 class="view-title">Informes PDF</h2>
    <button id="full-pdf" class="btn mb-4">PDF completo</button>
    <div class="grid" style="grid-template-columns:repeat(2,1fr);gap:1rem;margin-bottom:1rem;">
      <input id="from" type="date" class="input">
      <input id="to"   type="date" class="input">
    </div>
    <button id="range-pdf" class="btn">PDF intervalo</button>
  `;

  qs("#full-pdf").onclick = () => download("/api/report/full", "progreso.pdf");

  qs("#range-pdf").onclick = () => {
    const f = qs("#from").value, t = qs("#to").value;
    if (!f || !t) return alert("Seleccione ambas fechas");
    download(`/api/report/period?from=${f}&to=${t}`, `progreso_${f}_${t}.pdf`);
  };

  async function download(url, filename) {
    const blob = await fetch(api.API_BASE + url, { headers: authHeaders() }).then(r=>r.blob());
    const href = URL.createObjectURL(blob);
    const a = create("a"); a.href = href; a.download = filename; a.style.display = "none";
    document.body.appendChild(a); a.click();
    setTimeout(()=>{ URL.revokeObjectURL(href); a.remove(); }, 800);
  }
}
