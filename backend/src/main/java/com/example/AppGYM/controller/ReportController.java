// backend/src/main/java/com/example/AppGYM/controller/ReportController.java
package com.example.AppGYM.controller;

import com.example.AppGYM.model.User;
import com.example.AppGYM.repository.UserRepository;
import com.example.AppGYM.service.JwtService;
import com.example.AppGYM.service.PdfService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final PdfService pdf;
    private final JwtService jwt;
    private final UserRepository users;
    private final HttpServletRequest request;          // ← para leer el header

    /* -------- completo -------- */
    @GetMapping("/full")
    public ResponseEntity<byte[]> full(
            @AuthenticationPrincipal User u,
            @RequestParam(required=false) String token) {

        log.debug("GET /report/full  principal={}  tokenParam={}", u, token);
        u = resolveUser(u, token);                       // valida usuario

        byte[] bytes = pdf.buildFull(u);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=progreso.pdf")
                .body(bytes);
    }

    /* -------- período -------- */
    @GetMapping("/period")
    public ResponseEntity<byte[]> period(
            @AuthenticationPrincipal User u,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required=false) String token) {

        log.debug("GET /report/period  principal={}  tokenParam={}", u, token);
        u = resolveUser(u, token);

        LocalDate f = LocalDate.parse(from);
        LocalDate t = LocalDate.parse(to);

        byte[] bytes = pdf.buildPeriod(u, f, t);
        String file = String.format("progreso_%s_%s.pdf", from, to);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename="+file)
                .body(bytes);
    }

    /* ===== helper: obtiene User por token ===== */
    private User resolveUser(User u, String token){
        if (u != null) return u;             // autenticado por filtro

        /* — 1) token ?token=…  — */
        if (token == null){
            /* — 2) Authorization header — */
            String h = request.getHeader("Authorization");
            if (h != null && h.startsWith("Bearer "))
                token = h.substring(7);
        }
        if (token == null) throw new RuntimeException("Unauthorized");

        Claims c = jwt.extractAllClaims(token);
        return users.findByEmail(c.getSubject())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
