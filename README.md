# Gym Tracker — README completo 📚💪

> **Versión:** 2025-06 — *Backend Spring Boot 3 · Front-end Vanilla JS + Workbox PWA*
> **Demo:** [https://appgymregistro.netlify.app](https://appgymregistro.netlify.app)   |   **API Prod:** [https://appgym-production-64ac.up.railway.app](https://appgym-production-64ac.up.railway.app)

---

## Índice rápido

1. [Visión general](#visión-general)
2. [Arquitectura](#arquitectura)
3. [Flujo de instalación & despliegue](#flujo-de-instalación--despliegue)
4. [Front-end (Netlify + PWA)](#front-end-netlify--pwa)
5. [Back-end (Railway Spring Boot)](#back-end-railway-spring-boot)
6. [Base de datos](#base-de-datos)
7. [Autenticación & Seguridad](#autenticación--seguridad)
8. [API REST](#api-rest)
9. [Funcionalidades clave](#funcionalidades-clave)
10. [Testing & lint](#testing--lint)
11. [Roadmap / ideas futuras](#roadmap--ideas-futuras)
12. [Licencia](#licencia)

---

## Visión general

Gym Tracker es una aplicación web progresiva (PWA) para registrar **rutinas, medidas corporales y progreso fotográfico** de forma extremadamente rápida desde cualquier dispositivo (móvil, foldable, tablet o PC). Está diseñada tanto para uso personal como para entrenadores que gestionan múltiples clientes.

* **Quick-Add ＋** ― registra un día de entrenamiento con un solo toque.
* **Temporizador inteligente** ― cronómetro / cuenta-atrás con vibración prolongada y sonido incluso con la pantalla bloqueada (Android 12+).
* **Informes PDF** ― genera reportes completos o por intervalo de fechas.
* **Modo fuera de línea** ― gracias a Workbox y estrategia `NetworkFirst + Cache`.
* **Backend seguro** con JWT, Spring Security y MySQL en Railway.

---

## Arquitectura

```text
┌─────────────┐   HTTPS    ┌───────────────────┐    JDBC     ┌────────────┐
│  Frontend   │◀──────────▶│  Spring Boot API  │◀───────────▶│   MySQL    │
│  (Netlify)  │            │  (Railway)        │            │  (Railway) │
└─────────────┘            └───────────────────┘            └────────────┘
        ▲                               ▲
        │ Service Worker                │ JWT  HS256
        ▼                               ▼
   ◄──Offline cache──►            ▲────────►ReCaptcha opc. (desactivado)
```

* **Front-end**: Vanilla JS (ES Modules), Tailwind-like CSS, Workbox service-worker
  Deploy → **Netlify** (CI/CD con cada `git push`).
* **Back-end**: Spring Boot 3 + Spring Security + JPA (Hibernate)
  Deploy → **Railway** (build automático vía Maven).
* **Auth**: JWT HS256 (24 h de validez).
* **CORS**: `https://appgymregistro.netlify.app` autorizado.

---

## Flujo de instalación & despliegue

### 1. Requisitos

| Módulo  | Versión mínima |
| ------- | -------------- |
| Node.js | 18 LTS         |
| Java    | 17             |
| Maven   | 3.9            |
| MySQL   | 8.x            |
| Git     | —              |

### 2. Clonar

```bash
git clone https://github.com/<tu-usuario>/gym-tracker.git
cd gym-tracker
```

### 3. Backend (local)

```bash
cd backend
./mvnw spring-boot:run \
  -DJWT_SECRET=<clave> \
  -DSPRING_DATASOURCE_URL=jdbc:mysql://localhost/gym \
  -DSPRING_DATASOURCE_USERNAME=root \
  -DSPRING_DATASOURCE_PASSWORD=1234
```

### 4. Front-end (local PWA)

```bash
cd frontend
npm install          # instala workbox-cli y http-server
npm run build-sw     # genera sw.js + precache
npm run dev          # abre http://localhost:8080
```

> **Consejo:** usa la Live Preview de VS Code para HTTPS si quieres probar notificaciones.

### 5. Despliegue

| Parte    | Hosting | Acción                                                                                                             |
| -------- | ------- | ------------------------------------------------------------------------------------------------------------------ |
| API      | Railway | Conecta repo → “Deploy” (detectará Docker Maven).  <br>Configura variables: `SPRING_…`, `JWT_SECRET`.              |
| Frontend | Netlify | Arrastra carpeta **frontend/** o enlaza repo.  <br>Build command: `npm run build-sw` <br>Publish dir.: `frontend/` |

---

## Front-end (Netlify + PWA)

### Estructura

```
frontend/
├─ assets/
│  ├─ css/           (styles.css)
│  └─ js/
│     ├─ api.js
│     ├─ utils.js
│     ├─ router.js
│     ├─ timer.js
│     ├─ quickAdd.js
│     ├─ views/
│     │  ├─ daily.js
│     │  ├─ machines.js
│     │  ├─ profile.js
│     │  ├─ stats.js
│     │  └─ reports.js
├─ index.html        (login)
├─ dashboard.html    (shell)
├─ manifest.webmanifest
└─ sw.js / workbox-*.js (generados)
```

### PWA / APK / IPA

| Plataforma  | Cómo instalar                                                                                                                                                       |
| ----------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Android** | 1) Abre Chrome/Edge → *Añadir a pantalla de inicio*.<br>2) *(Opcional)* usa **PWABuilder** para empaquetar APK y firmar (el resultado es `app-release-signed.apk`). |
| **iOS**     | Safari → *Compartir* → *Añadir a Home Screen*.  iOS instala la PWA (no requiere IPA).                                                                               |

> En iOS no es posible reproducir audio con pantalla bloqueada (limitación del WKWebView).

---

## Back-end (Railway Spring Boot)

* Java 17, Spring Boot 3.3.0
* **Módulos**
  `config`, `controller`, `dto`, `model`, `repository`, `service`
* Filtro JWT (`JwtFilter`) + `SecurityConfig`
* S3/Cloudinary ready (`StorageService`, ahora local `uploads/`)
* PDF generación con Apache PDFBox (`PdfService`)
* CORS restrictivo (solo Netlify origin).

### Variables Railway

```
SPRING_DATASOURCE_URL      jdbc:mysql://containers-us-west…/railway
SPRING_DATASOURCE_USERNAME root
SPRING_DATASOURCE_PASSWORD ****
JWT_SECRET                 <clave 32+ chars>
```

---

## Base de datos

Diagrama simplificado (MySQL 8):

```
users (User) 1 ─── n daily_entry
      |                 |
      |                 └── details (Map<machine_id, Exercise>)
      |
      ├── n body_stats
      ├── n progress_photo
      └── n user_machine ─── 1 machine
```

`schema.sql` se crea automáticamente (`ddl-auto=update`).

---

## Autenticación & Seguridad

| Capa             | Tecnología                      |
| ---------------- | ------------------------------- |
| Login / Registro | `/api/auth/login` & `/register` |
| Token            | JWT HS256, 24 h                 |
| Passwords        | `BCryptPasswordEncoder`         |
| Roles            | `ROLE_USER` (single)            |
| CORS             | `CorsConfigurationSource`       |
| CSRF             | Desactivado (solo API)          |

---

## API REST (principales)

| Método | Path                 | Descripción                          |
| ------ | -------------------- | ------------------------------------ |
| POST   | `/api/auth/login`    | Login, devuelve `token`              |
| POST   | `/api/auth/register` | Registro rápido                      |
| GET    | `/api/profile`       | Perfil actual                        |
| PUT    | `/api/profile`       | Actualiza perfil                     |
| GET    | `/api/machines`      | Lista de máquinas + pesos            |
| POST   | `/api/machines`      | Crea/actualiza                       |
| DELETE | `/api/machines/{id}` | Elimina                              |
| POST   | `/api/daily`         | Crear/actualizar registro diario     |
| GET    | `/api/report/full`   | PDF completo                         |
| GET    | `/api/report/period` | PDF intervalo `?from=yyyy-mm-dd&to=` |

> Añade el header `Authorization: Bearer <token>` salvo en `/api/auth/**`.

---

## Funcionalidades clave

| Módulo             | Detalle                                                                                                      |
| ------------------ | ------------------------------------------------------------------------------------------------------------ |
| **Quick-Add ＋**    | FAB flotante → duplica el último peso/reps/series en 1-tap.                                                  |
| **Temporizador**   | Cronómetro / Cuenta-atrás: sonido (via YouTube invisible) + vibración 10 s bucle. Mantiene estado al pausar. |
| **Responsive**     | CSS utilitario, breakpoints móviles / foldables / desktop.                                                   |
| **Service-Worker** | `generateSW` (precache estático + runtime caching API & pages).                                              |
| **PWA metadata**   | `manifest.webmanifest`, icons 192 / 512, theme-color.                                                        |

---

## Testing & lint

* **Front**: ESLint (`npx eslint assets/js --fix`)
  *Pendiente*: pruebas con Playwright.
* **Back**: JUnit 5 (`mvn test`) con contenedor Testcontainers-MySQL.

---

## Roadmap / ideas futuras

* Notificaciones push (Web Push + FCM) cuando se genere el PDF.
* Multi-cliente (entrenadores) con roles `COACH / ADMIN`.
* Integración con **Apple Health** / **Google Fit**.
* Sincronizar a **Strava** series cardio.
* Live widgets (Android 14) con progreso semanal.

---

## Licencia

MIT © 2025 — *Siéntete libre de bifurcar y mejorar 💜*
