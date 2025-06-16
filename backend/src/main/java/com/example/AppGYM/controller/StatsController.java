// backend/src/main/java/com/example/AppGYM/controller/StatsController.java
package com.example.AppGYM.controller;

import com.example.AppGYM.dto.BodyStatsDto;
import com.example.AppGYM.model.BodyStats;
import com.example.AppGYM.model.User;
import com.example.AppGYM.repository.BodyStatsRepository;
import com.example.AppGYM.service.StorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final BodyStatsRepository repo;
    private final StorageService storage;

    @GetMapping("/latest")
    public BodyStatsDto latest(@AuthenticationPrincipal User u){
        return repo.findTopByUserIdOrderByDateDesc(u.getId())
                .map(this::toDto)
                .orElse(null);
    }

    @PostMapping @Transactional
    public void save(@AuthenticationPrincipal User u,
                     @RequestPart("data") BodyStatsDto dto,
                     @RequestPart(value="front",required=false) MultipartFile front,
                     @RequestPart(value="side", required=false) MultipartFile side,
                     @RequestPart(value="back", required=false) MultipartFile back){

        BodyStats bs = new BodyStats();
        bs.setUser(u);
        bs.setDate(dto.getDate());
        bs.setWeightKg(dto.getWeightKg());
        bs.setNeckCm(dto.getNeckCm()); bs.setChestCm(dto.getChestCm());
        bs.setWaistCm(dto.getWaistCm()); bs.setLowerAbsCm(dto.getLowerAbsCm());
        bs.setHipCm(dto.getHipCm());
        bs.setBicepsCm(dto.getBicepsCm()); bs.setBicepsFlexCm(dto.getBicepsFlexCm());
        bs.setForearmCm(dto.getForearmCm()); bs.setThighCm(dto.getThighCm());
        bs.setCalfCm(dto.getCalfCm());

        if(front!=null) bs.setFrontImgUrl(storage.save(front));
        if(side !=null) bs.setSideImgUrl (storage.save(side));
        if(back !=null) bs.setBackImgUrl (storage.save(back));

        repo.save(bs);
    }

    /* ---------------- */
    private BodyStatsDto toDto(BodyStats b){
        BodyStatsDto d=new BodyStatsDto();
        d.setDate(b.getDate()); d.setWeightKg(b.getWeightKg());
        d.setNeckCm(b.getNeckCm()); d.setChestCm(b.getChestCm());
        d.setWaistCm(b.getWaistCm()); d.setLowerAbsCm(b.getLowerAbsCm());
        d.setHipCm(b.getHipCm());
        d.setBicepsCm(b.getBicepsCm()); d.setBicepsFlexCm(b.getBicepsFlexCm());
        d.setForearmCm(b.getForearmCm()); d.setThighCm(b.getThighCm()); d.setCalfCm(b.getCalfCm());
        d.setFrontImgUrl(b.getFrontImgUrl()); d.setSideImgUrl(b.getSideImgUrl()); d.setBackImgUrl(b.getBackImgUrl());
        return d;
    }
}
