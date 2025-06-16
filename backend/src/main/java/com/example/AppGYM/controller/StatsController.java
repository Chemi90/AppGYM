package com.example.AppGYM.controller;

import com.example.AppGYM.dto.BodyStatsDto;
import com.example.AppGYM.model.BodyStats;
import com.example.AppGYM.model.ProgressPhoto;
import com.example.AppGYM.model.User;
import com.example.AppGYM.repository.BodyStatsRepository;
import com.example.AppGYM.repository.ProgressPhotoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final BodyStatsRepository statsRepo;
    private final ProgressPhotoRepository photoRepo;

    /* ────── histórico completo ────── */
    @GetMapping
    public List<BodyStats> list(@AuthenticationPrincipal User u) {
        return statsRepo.findByUserIdOrderByDateAsc(u.getId());
    }

    /* ────── última medición ────── */
    @GetMapping("/last")
    public BodyStats last(@AuthenticationPrincipal User u) {
        return statsRepo.findTopByUserIdOrderByDateDesc(u.getId())
                .orElse(null);
    }

    /* ────── guardar medición + fotos ────── */
    @PostMapping @Transactional
    public void save(@AuthenticationPrincipal User u,
                     @ModelAttribute BodyStatsDto dto,
                     @RequestPart(required = false) MultipartFile front,
                     @RequestPart(required = false) MultipartFile side,
                     @RequestPart(required = false) MultipartFile back) {

        /* ---- medidas ---- */
        BodyStats s = new BodyStats();
        s.setUser(u);
        s.setDate(dto.date() == null ? LocalDate.now() : dto.date());
        s.setWeightKg(dto.weightKg());
        s.setNeckCm(dto.neckCm());
        s.setChestCm(dto.chestCm());
        s.setWaistCm(dto.waistCm());
        s.setLowerAbsCm(dto.lowerAbsCm());
        s.setHipCm(dto.hipCm());
        s.setBicepsCm(dto.bicepsCm());
        s.setBicepsFlexCm(dto.bicepsFlexCm());
        s.setForearmCm(dto.forearmCm());
        s.setThighCm(dto.thighCm());
        s.setCalfCm(dto.calfCm());
        statsRepo.save(s);

        /* ---- fotos opcionales ---- */
        uploadPhoto(u, s.getDate(), ProgressPhoto.Type.FRONT, front);
        uploadPhoto(u, s.getDate(), ProgressPhoto.Type.SIDE , side );
        uploadPhoto(u, s.getDate(), ProgressPhoto.Type.BACK , back );
    }

    /* helper */
    private void uploadPhoto(User u, LocalDate date,
                             ProgressPhoto.Type type,
                             MultipartFile file) {
        if (file == null || file.isEmpty()) return;

        // En producción subirías a S3 / Cloudinary.  Aquí guardamos un
        // objeto de referencia solamente (url=fake).
        ProgressPhoto p = new ProgressPhoto();
        p.setUser(u);
        p.setDate(date);
        p.setType(type);
        p.setUrl("uploaded://" + file.getOriginalFilename());
        photoRepo.save(p);
    }
}
