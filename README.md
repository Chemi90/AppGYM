# Gym Tracker

Full‑stack template: **HTML/CSS/JS** frontend + **Spring Boot/MySQL** backend.

* **Frontend** → Netlify (static hosting)
* **Backend** → Railway (Docker‑free JVM deploy)

Clone, set your environment variables, connect both platforms and you’re live.

/ (repo root)
│  README.md
│  netlify.toml
│  .env.example
├─ frontend/
│   ├─ index.html
│   ├─ dashboard.html
│   └─ assets/
│        ├─ css/styles.css
│        └─ js/app.js
└─ backend/
    ├─ pom.xml
    └─ src/main/java/com/example/gymapp/
        ├─ GymAppApplication.java
        ├─ config/
        │    ├─ SecurityConfig.java
        │    ├─ JwtFilter.java
        │    └─ RecaptchaConfig.java
        ├─ service/
        │    ├─ JwtService.java
        │    └─ AuthService.java
        ├─ controller/
        │    ├─ AuthController.java
        │    ├─ ProfileController.java
        │    ├─ MachineController.java
        │    └─ DailyEntryController.java
        ├─ model/
        │    ├─ User.java
        │    ├─ Machine.java
        │    ├─ UserMachine.java
        │    └─ DailyEntry.java
        ├─ repository/
        │    ├─ UserRepository.java
        │    ├─ MachineRepository.java
        │    ├─ UserMachineRepository.java
        │    └─ DailyEntryRepository.java
        └─ dto/
             ├─ LoginDto.java
             ├─ RegisterDto.java
             └─ RecaptchaResponse.java