import { API_BASE, authHeaders, showLoader } from "./utils.js";

/* Envoltura que muestra/oculta el overlay mientras dura la llamada */
async function wrap(promise){
  showLoader(true);
  try{
    const res = await promise;
    if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
    const ct = res.headers.get("content-type") || "";
    return ct.includes("application/json") ? res.json()
         : ct.startsWith("text/")          ? res.text()
         :                                   res.blob();
  }finally{
    showLoader(false);
  }
}

export const api = {
  get : (url)        => wrap(fetch(API_BASE + url, { headers: authHeaders() })),
  post: (url, body)  => wrap(fetch(API_BASE + url, {
                     method:"POST",
                     headers:{ ...authHeaders(), "Content-Type":"application/json" },
                     body:JSON.stringify(body) })),
  put : (url, body)  => wrap(fetch(API_BASE + url, {
                     method:"PUT",
                     headers:{ ...authHeaders(), "Content-Type":"application/json" },
                     body:JSON.stringify(body) })),
  del : (url)        => wrap(fetch(API_BASE + url, { method:"DELETE", headers: authHeaders() }))
};
