// backend/src/main/java/com/example/AppGYM/controller/DailyEntryController.java
package com.example.AppGYM.controller;

import com.example.AppGYM.dto.DailyDto;
import com.example.AppGYM.dto.MachineEntryDto;
import com.example.AppGYM.model.DailyEntry;
import com.example.AppGYM.model.User;
import com.example.AppGYM.repository.DailyEntryRepository;
import com.example.AppGYM.repository.MachineRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/daily")
@RequiredArgsConstructor
public class DailyEntryController {

    private final DailyEntryRepository repo;
    private final MachineRepository machines;

    @GetMapping
    public List<DailyEntry> range(@AuthenticationPrincipal User u,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return repo.findByUserIdAndDateBetweenOrderByDateAsc(u.getId(),from,to);
    }

    @PostMapping @Transactional
    public void save(@AuthenticationPrincipal User u,
                     @RequestBody DailyDto dto) {

        DailyEntry e = repo.findByUserIdAndDate(u.getId(), dto.getDate())
                .orElseGet(() -> {
                    DailyEntry x = new DailyEntry();
                    x.setUser(u); x.setDate(dto.getDate());
                    return x;
                });

        Map<Long,DailyEntry.Exercise> map = dto.getExercises().stream().collect(Collectors.toMap(
                ex -> machines.findByName(ex.getName()).orElseThrow().getId(),
                ex -> {
                    DailyEntry.Exercise exo = new DailyEntry.Exercise();
                    exo.setWeightKg(ex.getWeightKg());
                    exo.setReps(ex.getReps());
                    exo.setSets(ex.getSets());
                    return exo;
                }
        ));

        e.setDetails(map);
        repo.save(e);
    }
}
