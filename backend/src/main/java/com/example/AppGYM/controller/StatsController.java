package com.example.AppGYM.controller;

import com.example.AppGYM.dto.BodyStatsDto;
import com.example.AppGYM.model.BodyStats;
import com.example.AppGYM.model.User;
import com.example.AppGYM.repository.BodyStatsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * End-points REST para registrar y consultar medidas / fotos.
 *  – POST  /api/stats            ⇒ recibe JSON (@RequestBody)
 *  – GET   /api/stats            ⇒ último registro del usuario
 *  – GET   /api/stats/range      ⇒ rango de fechas (from,to)
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final BodyStatsRepository repo;

    /* ────────────────────────────────────────────────────────────────
       1) Última medición
       ──────────────────────────────────────────────────────────────── */
    @GetMapping
    public BodyStats latest(@AuthenticationPrincipal User u) {
        return repo.findTopByUserIdOrderByDateDesc(u.getId()).orElse(null);
    }

    /* ────────────────────────────────────────────────────────────────
       2) Rango de fechas
       ──────────────────────────────────────────────────────────────── */
    @GetMapping("/range")
    public List<BodyStats> range(@AuthenticationPrincipal User u,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return repo.findByUserIdAndDateBetweenOrderByDateAsc(u.getId(), from, to);
    }

    /* ────────────────────────────────────────────────────────────────
       3) Alta / actualización  (ahora acepta JSON)
       ──────────────────────────────────────────────────────────────── */
    @PostMapping
    @Transactional
    public void upsert(@AuthenticationPrincipal User u,
                       @RequestBody BodyStatsDto dto) {

        /* si ya existe registro para esa fecha, lo actualizamos */
        BodyStats bs = repo.findByUserIdAndDate(u.getId(), dto.date())
                .orElseGet(() -> {
                    BodyStats x = new BodyStats();
                    x.setUser(u);
                    x.setDate(dto.date() == null ? LocalDate.now() : dto.date());
                    return x;
                });

        /* copia de campos (todos opcionales) */
        bs.setWeightKg(dto.weightKg());
        bs.setNeckCm(dto.neckCm());
        bs.setChestCm(dto.chestCm());
        bs.setWaistCm(dto.waistCm());
        bs.setLowerAbsCm(dto.lowerAbsCm());
        bs.setHipCm(dto.hipCm());
        bs.setBicepsCm(dto.bicepsCm());
        bs.setBicepsFlexCm(dto.bicepsFlexCm());
        bs.setForearmCm(dto.forearmCm());
        bs.setThighCm(dto.thighCm());
        bs.setCalfCm(dto.calfCm());

        repo.save(bs);
    }
}
