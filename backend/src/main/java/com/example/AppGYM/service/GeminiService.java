// backend/src/main/java/com/example/AppGYM/service/GeminiService.java
package com.example.AppGYM.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GeminiService {

    private final RestTemplate rest = new RestTemplate();
    private final String chatUrl;
    private final String visionUrl;
    private final List<Map<String,Object>> history = new ArrayList<>();

    public GeminiService(@Value("${gemini.api.key}") String key) {
        String base = "https://generativelanguage.googleapis.com/v1/models/";
        this.chatUrl   = base + "gemini-1.5-pro-latest:generateContent?key=" + key;
        this.visionUrl = base + "gemini-1.5-pro-vision-latest:generateContent?key=" + key;
    }

    /* ---------- chat texto ---------- */
    public String chat(String prompt) {
        history.add(Map.of("role","user","parts", List.of(Map.of("text", prompt))));
        String reply = send(chatUrl, Map.of("contents", history));
        history.add(Map.of("role","model","parts", List.of(Map.of("text", reply))));
        return reply;
    }

    /* ---------- visión ---------- */
    public String vision(String prompt, String base64Png) {
        Map<String,Object> imagePart = Map.of(
                "inlineData", Map.of("mimeType", "image/png", "data", base64Png));
        var content = List.of(Map.of("role","user",
                "parts", List.of(Map.of("text", prompt), imagePart)));
        return send(visionUrl, Map.of("contents", content));
    }

    /* ---------- llamada POST + parseo ---------- */
    private String send(String url, Object body) {
        HttpHeaders h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON);
        var res = rest.postForEntity(url, new HttpEntity<>(body, h), GeminiResp.class);

        return Optional.ofNullable(res.getBody())
                .map(r -> r.candidates().get(0)                // Java 17 ⇒ get(0)
                        .content().parts().get(0).text())   // text()  (record getter)
                .orElse("(sin respuesta)");
    }

    /* ---------- DTOs ---------- */
    @JsonIgnoreProperties(ignoreUnknown=true)
    private record GeminiResp(List<Candidate> candidates) {}
    @JsonIgnoreProperties(ignoreUnknown=true)
    private record Candidate(Content content) {}
    @JsonIgnoreProperties(ignoreUnknown=true)
    private record Content(List<Part> parts) {}
    @JsonIgnoreProperties(ignoreUnknown=true)
    private record Part(String text) {}
}
