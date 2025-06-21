// backend/src/main/java/com/example/AppGYM/controller/RpgController.java
package com.example.AppGYM.controller;

import com.example.AppGYM.model.RpgCharacter;
import com.example.AppGYM.service.GeminiService;
import com.example.AppGYM.service.RpgService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/rpg")
@RequiredArgsConstructor
public class RpgController {

    private final GeminiService geminiService;
    private final RpgService    rpgService;
    private final ObjectMapper  mapper = new ObjectMapper();   // parseo rápido de JSON

    /* ──────────────────────────── CHAT ─────────────────────────────── */

    /** Chat estilo Dungeon-Master (solo texto, sin cambios de stats) */
    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> req) {
        String reply = geminiService.chat(systemPrompt() + req.get("prompt"));
        return Map.of("reply", reply);
    }

    /* ────────────────────── EJERCICIO (imagen) ─────────────────────── */

    /**
     * Analiza imagen de ejercicio, actualiza atributos y devuelve la ficha
     * del personaje ya persistida.
     *
     * @param principal  viene del filtro JWT – `principal.getName()` == username
     */
    @PostMapping(value = "/exercise", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RpgCharacter exercise(Principal principal,
                                 @RequestBody ExerciseReq req) throws Exception {

        /* 1) Preguntamos a Gemini-Vision                                  */
        String json = geminiService.vision("""
                Eres un analista deportivo. Devuelve SOLO este JSON:
                {
                  "exercise": "<sentadilla|flexion|plancha|...>",
                  "reps": <entero>,
                  "feedback": "<consejo rápido>"
                }
                """, req.imageBase64());

        /* 2) Extraemos reps del JSON                                       */
        JsonNode node = mapper.readTree(json);
        int reps = node.path("reps").asInt(0);

        /* 3) Reglas de gamificación (ejemplo muy simple)                   */
        int strDelta = reps;          // +1 Fuerza por repetición
        int xpDelta  = reps / 2;      // +0.5 XP

        /* 4) Persistimos usando el username que viene en el JWT            */
        return rpgService.applyDelta(principal.getName(),
                strDelta, 0, 0, 0, xpDelta);
    }

    /* ─────────────────────── COMIDA (imagen) ────────────────────────── */

    /** Devuelve sólo el análisis; (opcional) aquí también podrías llamar
     a `applyDelta` para Vitalidad. */
    @PostMapping(value = "/meal", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> meal(@RequestBody MealReq req) {

        String analysis = geminiService.vision("""
                Eres nutricionista. Devuélveme SOLO este JSON:
                {
                  "ingredients": ["..."],
                  "kCal":       <aprox entero>,
                  "quality":    "<saludable|procesado|mixto>"
                }
                """, req.imageBase64());

        return Map.of("analysis", analysis);
    }

    /* ─────────────────────────── DTOs ──────────────────────────────── */

    private record ExerciseReq(String imageBase64) {}
    private record MealReq(String imageBase64) {}

    /* ───────────────────── PROMPT BASE DEL MÁSTER ───────────────────── */

    private String systemPrompt() {
        return """
                Actúa como un maestro de rol épico y motivador.
                Atributos:
                 • Fuerza     = ejercicio
                 • Vitalidad  = calidad de comida
                 • Energía    = descanso
                Responde SIEMPRE en castellano y con tono entusiasta.
                ---
                """;
    }
}
