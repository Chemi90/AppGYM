package com.example.AppGYM.controller;

import com.example.AppGYM.model.DailyEntry;
import com.example.AppGYM.model.User;
import com.example.AppGYM.repository.DailyEntryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/daily")
@RequiredArgsConstructor
public class DailyEntryController {
    private final DailyEntryRepository daily;
    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping
    public DailyEntry get(@AuthenticationPrincipal User user,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return daily.findByUserIdAndWorkoutDate(user.getId(), date).orElse(null);
    }

    @PostMapping
    public DailyEntry save(@AuthenticationPrincipal User user, @RequestBody DailyDto dto) throws Exception {
        DailyEntry entry = daily.findByUserIdAndWorkoutDate(user.getId(), dto.getDate()).orElseGet(() -> {
            DailyEntry de = new DailyEntry();
            de.setUser(user);
            de.setWorkoutDate(dto.getDate());
            return de;
        });
        entry.setDetails(mapper.writeValueAsString(dto.getDetails()));
        return daily.save(entry);
    }

    @GetMapping("/history")
    public List<DailyEntry> latest(@AuthenticationPrincipal User user) {
        return daily.findTop30ByUserIdOrderByWorkoutDateDesc(user.getId());
    }

    @Data
    public static class DailyDto {
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate date;
        private Map<Long, Double> details;
    }
}