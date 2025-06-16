// backend/src/main/java/com/example/AppGYM/controller/ReportController.java
package com.example.AppGYM.controller;

import com.example.AppGYM.model.User;
import com.example.AppGYM.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final PdfService pdf;

    @GetMapping("/full")
    public ResponseEntity<byte[]> full(@AuthenticationPrincipal User u) {
        byte[] bytes = pdf.buildReport(u,null,null);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=progress.pdf")
                .body(bytes);
    }

    @GetMapping("/period")
    public ResponseEntity<byte[]> period(@AuthenticationPrincipal User u,
                                         @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {

        byte[] bytes = pdf.buildReport(u,from,to);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=progress-"+from+"_"+to+".pdf")
                .body(bytes);
    }
}
