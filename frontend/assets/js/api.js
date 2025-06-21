import { API_BASE, authHeaders, showLoader, hideLoader, dbg } from "./utils.js";

/* wrapper con logs */
async function wrap (promise, info = '') {
  showLoader();
  dbg('API', info);
  try {
    const res = await promise;
    dbg('API', info, res.status);
    if (!res.ok) throw new Error(`${res.status} ${res.statusText}`);
    const ct = res.headers.get('content-type') || '';
    return ct.includes('application/json') ? res.json()
         : ct.startsWith('text/')          ? res.text()
         :                                   res.blob();
  } finally { hideLoader(); }
}

export const api = {
  get : url            => wrap(fetch(API_BASE + url, { headers: authHeaders() }), 'GET ' + url),
  post: (url, body)    => wrap(fetch(API_BASE + url, {
                         method:'POST', headers:{ ...authHeaders(), 'Content-Type':'application/json' },
                         body:JSON.stringify(body) }), 'POST ' + url),
  put : (url, body)    => wrap(fetch(API_BASE + url, {
                         method:'PUT', headers:{ ...authHeaders(), 'Content-Type':'application/json' },
                         body:JSON.stringify(body) }), 'PUT ' + url),
  del : url            => wrap(fetch(API_BASE + url, { method:'DELETE', headers: authHeaders() }), 'DEL ' + url)
};
