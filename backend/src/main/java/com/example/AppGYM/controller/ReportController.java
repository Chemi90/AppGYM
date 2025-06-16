// backend/src/main/java/com/example/AppGYM/controller/ReportController.java
package com.example.AppGYM.controller;

import com.example.AppGYM.model.User;
import com.example.AppGYM.repository.UserRepository;
import com.example.AppGYM.service.JwtService;
import com.example.AppGYM.service.PdfService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final PdfService pdf;
    private final JwtService jwt;
    private final UserRepository users;

    /* -------- completo -------- */
    @GetMapping("/full")
    public ResponseEntity<byte[]> full(@AuthenticationPrincipal User u,
                                       @RequestParam(required=false) String token){

        u = resolveUser(u, token);
        byte[] bytes = pdf.buildFull(u);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=progreso.pdf")
                .body(bytes);
    }

    /* -------- período -------- */
    @GetMapping("/period")
    public ResponseEntity<byte[]> period(@AuthenticationPrincipal User u,
                                         @RequestParam String from,
                                         @RequestParam String to,
                                         @RequestParam(required=false) String token){

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

    /* -------- helper -------- */
    private User resolveUser(User u,String token){
        if(u!=null) return u;
        if(token==null) throw new RuntimeException("Unauthorized");
        Claims c = jwt.extractAllClaims(token);
        return users.findByEmail(c.getSubject()).orElseThrow();
    }
}
