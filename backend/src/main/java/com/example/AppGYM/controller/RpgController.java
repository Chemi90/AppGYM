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
    private final ObjectMapper  mapper = new ObjectMapper();

    /* ─────────────── NUEVO: devuelve ficha persistida ─────────────── */
    @GetMapping("/character")
    public RpgCharacter getCharacter(Principal principal) {
        return rpgService.getOrCreate(principal.getName());
    }

    /* ───────────────────────── CHAT ───────────────────────── */
    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> req) {
        String reply = geminiService.chat(systemPrompt() + req.get("prompt"));
        return Map.of("reply", reply);
    }

    /* ────────────── EJERCICIO (imagen base64) ────────────── */
    @PostMapping(value = "/exercise", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RpgCharacter exercise(Principal principal,
                                 @RequestBody ExerciseReq req) throws Exception {

        String json = geminiService.vision("""
                Eres un analista deportivo. Devuelve SOLO este JSON:
                {
                  "exercise": "<sentadilla|flexion|plancha|...>",
                  "reps": <entero>,
                  "feedback": "<consejo rápido>"
                }
                """, req.imageBase64());

        JsonNode node     = mapper.readTree(json);
        int reps          = node.path("reps").asInt(0);
        int strDelta      = reps;          // regla simple
        int xpDelta       = reps / 2;

        return rpgService.applyDelta(principal.getName(),
                strDelta, 0, 0, 0, xpDelta);
    }

    /* ─────────────── COMIDA (imagen base64) ─────────────── */
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

    /* ---------------- DTOs ---------------- */
    private record ExerciseReq(String imageBase64) {}
    private record MealReq(String imageBase64)    {}

    /* ------------- prompt base ------------- */
    private String systemPrompt() {
        return """
                Actúa como un maestro de rol épico y motivador.
                Atributos:
                 • Fuerza     = ejercicio
                 • Vitalidad  = comida
                 • Energía    = descanso
                Responde siempre en castellano y con tono entusiasta.
                ---
                """;
    }
}
