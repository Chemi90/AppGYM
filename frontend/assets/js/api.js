import { API_BASE, authHeaders } from "./utils.js";

export const api = {
  get : (url)  => fetch(`${API_BASE}${url}`, { headers: authHeaders() }).then(r => r.json()),
  post: (url, body) => fetch(`${API_BASE}${url}`, {
            method:"POST", headers:{...authHeaders(),"Content-Type":"application/json"},
            body:JSON.stringify(body)}).then(r=>r.json()),
  put : (url, body) => fetch(`${API_BASE}${url}`, {
            method:"PUT", headers:{...authHeaders(),"Content-Type":"application/json"},
            body:JSON.stringify(body)}),
  del : (url)  => fetch(`${API_BASE}${url}`, { method:"DELETE", headers: authHeaders() })
};
